package com.ares.ewe_man.presentation.viewmodel.pickupmap

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ares.ewe_man.core.network.toUserFacingMessage
import com.ares.ewe_man.data.location.LocationProvider
import com.ares.ewe_man.data.remote.model.DeliveryOrderItemDto
import com.ares.ewe_man.domain.repository.DirectionsRepository
import com.ares.ewe_man.domain.repository.OrderRepository
import com.ares.ewe_man.presentation.ui.map.CourierHeading
import com.google.android.gms.maps.model.LatLng
import dagger.hilt.android.lifecycle.HiltViewModel
import java.util.Locale
import javax.inject.Inject
import kotlin.math.roundToInt
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

private const val ROUTE_AND_ETA_REFRESH_MS = 30_000L
private const val ROUGH_SPEED_METERS_PER_MIN = 420.0
/** Margen para habilitar «Comenzar envío» en el restaurante. */
private const val PICKUP_ARRIVAL_RADIUS_METERS = 20.0
data class PickupMapUiState(
    val pickupLatLng: LatLng? = null,
    val pickupTitle: String? = null,
    val pickupAddress: String? = null,
    /** Cliente al que se entregará el pedido (tras recoger en el restaurante). */
    val customerName: String? = null,
    val customerLastName: String? = null,
    val currentLocation: LatLng? = null,
    /** Distancia en línea recta al punto de recogida; null si falta ubicación o tienda. */
    val distanceToPickupMeters: Double? = null,
    /** Estado del pedido en el servidor (ASSIGNED, ON_DELIVERY, …). */
    val orderStatus: String? = null,
    val pickupCodeInput: String = "",
    /** null = aún no verificado; true/false tras validar con el servidor. */
    val pickupCodeValid: Boolean? = null,
    val isVerifyingPickupCode: Boolean = false,
    /** Pedidos de tienda requieren código; pagos de servicio no. */
    val pickupCodeRequired: Boolean = true,
    val isServicePayment: Boolean = false,
    val serviceItems: List<DeliveryOrderItemDto> = emptyList(),
    /** Dentro de [PICKUP_ARRIVAL_RADIUS_METERS] del restaurante. */
    val isAtPickupLocation: Boolean = false,
    val isStartingDelivery: Boolean = false,
    val routePoints: List<LatLng> = emptyList(),
    val etaText: String? = null,
    val remainingDistanceText: String? = null,
    val etaIsApproximate: Boolean = false,
    val isLoading: Boolean = true,
    val errorMessage: String? = null,
    val headingDegrees: Float = 0f
)

@HiltViewModel
class PickupMapViewModel @Inject constructor(
    private val orderRepository: OrderRepository,
    private val directionsRepository: DirectionsRepository,
    private val locationProvider: LocationProvider,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    val orderId: String = savedStateHandle.get<String>("orderId").orEmpty()

    private val _uiState = MutableStateFlow(PickupMapUiState())
    val uiState: StateFlow<PickupMapUiState> = _uiState.asStateFlow()

    private val _navigateToDelivery = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
    val navigateToDelivery: SharedFlow<Unit> = _navigateToDelivery.asSharedFlow()

    private var locationPollJob: Job? = null
    private var verifyPickupCodeJob: Job? = null
    private var lastRouteFetchAt = 0L
    private var previousLatLng: LatLng? = null
    private var smoothedHeading: Float = 0f

    init {
        loadData()
    }

    override fun onCleared() {
        super.onCleared()
        locationPollJob?.cancel()
        verifyPickupCodeJob?.cancel()
    }

    fun loadData() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)
            val order = orderRepository.getOrderById(orderId).fold(
                onSuccess = { it },
                onFailure = { e ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        errorMessage = e.toUserFacingMessage()
                    )
                    return@launch
                }
            )
            val shopLat = order.shopLat
            val shopLng = order.shopLng
            val pickupLatLng = if (shopLat != null && shopLng != null) {
                LatLng(shopLat, shopLng)
            } else null
            val isServicePayment = order.isServicePayment
            val title = when {
                isServicePayment -> order.shopName ?: "Punto de pago"
                else -> order.shopName ?: "Restaurante"
            }
            val status = order.status.uppercase()
            val pickupCodeRequired = order.pickupCodeRequired
            // Tienda: si ya está ON_DELIVERY, ir al mapa de entrega.
            // Servicio: ON_DELIVERY al aceptar; aquí sigue la ruta al punto de pago.
            if (status == "ON_DELIVERY" && !isServicePayment) {
                _uiState.value = _uiState.value.copy(
                    orderStatus = status,
                    isLoading = false,
                )
                _navigateToDelivery.tryEmit(Unit)
                return@launch
            }
            val canShowPickupRoute =
                status == "ASSIGNED" || (isServicePayment && status == "ON_DELIVERY")
            if (!canShowPickupRoute) {
                _uiState.value = _uiState.value.copy(
                    orderStatus = status,
                    isLoading = false,
                    errorMessage = when (status) {
                        "READY_FOR_PICKUP" ->
                            "Este pedido aún no está asignado a ti. Vuelve al detalle y pulsa «Asignar a mí»."
                        else -> "Este pedido no está listo para recoger (estado: $status)."
                    },
                )
                return@launch
            }
            _uiState.value = _uiState.value.copy(
                pickupLatLng = pickupLatLng,
                pickupTitle = title,
                pickupAddress = order.shopAddress,
                customerName = order.customerName,
                customerLastName = order.customerLastName,
                orderStatus = status,
                pickupCodeInput = "",
                pickupCodeRequired = pickupCodeRequired,
                pickupCodeValid = if (pickupCodeRequired) null else true,
                isServicePayment = isServicePayment,
                serviceItems = if (isServicePayment) order.items else emptyList(),
            )
            locationProvider.getCurrentLocation()
                .onSuccess { update ->
                    val latLng = update.latLng
                    val heading = computeHeadingForUpdate(
                        latLng,
                        update.bearingDegrees,
                        update.speedMetersPerSecond
                    )
                    val dist = distanceToPickupMeters(latLng, pickupLatLng)
                    _uiState.value = _uiState.value.copy(
                        currentLocation = latLng,
                        headingDegrees = heading,
                        isLoading = false,
                        distanceToPickupMeters = dist,
                        isAtPickupLocation = isWithinPickupRadius(dist),
                    )
                    previousLatLng = latLng
                    orderRepository.updateLocation(latLng.latitude, latLng.longitude)
                    pickupLatLng?.let { dest ->
                        fetchRoute(latLng, dest, isPeriodicRefresh = false)
                        startLocationPolling(dest)
                    }
                }
                .onFailure { e ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        errorMessage = e.message ?: "No se pudo obtener tu ubicación"
                    )
                }
        }
    }

    private fun fetchRoute(origin: LatLng, destination: LatLng, isPeriodicRefresh: Boolean) {
        val now = System.currentTimeMillis()
        if (isPeriodicRefresh && now - lastRouteFetchAt < ROUTE_AND_ETA_REFRESH_MS) return
        lastRouteFetchAt = now
        viewModelScope.launch {
            directionsRepository.getDrivingRoute(origin, destination)
                .onSuccess { info ->
                    val routePoints = if (info.points.isNotEmpty()) {
                        info.points
                    } else {
                        fallbackRoute(origin, destination)
                    }
                    _uiState.value = _uiState.value.copy(
                        routePoints = routePoints,
                        etaText = info.durationText,
                        remainingDistanceText = info.distanceText,
                        etaIsApproximate = false,
                        errorMessage = null
                    )
                }
                .onFailure {
                    val meters = distanceInMeters(origin, destination)
                    _uiState.value = _uiState.value.copy(
                        routePoints = fallbackRoute(origin, destination),
                        etaText = roughEtaLabel(meters),
                        remainingDistanceText = formatRemainingDistance(meters),
                        etaIsApproximate = true,
                        errorMessage = if (!isPeriodicRefresh) {
                            "Se muestra línea recta y tiempo aproximado. Revisa Directions API (ver MAPS_SETUP.md)."
                        } else {
                            _uiState.value.errorMessage
                        }
                    )
                }
        }
    }

    private fun fallbackRoute(origin: LatLng, destination: LatLng): List<LatLng> =
        listOf(origin, destination)

    private fun startLocationPolling(destination: LatLng) {
        locationPollJob?.cancel()
        locationPollJob = viewModelScope.launch {
            while (isActive) {
                delay(500L)
                locationProvider.getCurrentLocation()
                    .onSuccess { update ->
                        val latLng = update.latLng
                        val heading = computeHeadingForUpdate(
                            latLng,
                            update.bearingDegrees,
                            update.speedMetersPerSecond
                        )
                        val dist = distanceToPickupMeters(latLng, destination)
                        _uiState.value = _uiState.value.copy(
                            currentLocation = latLng,
                            headingDegrees = heading,
                            distanceToPickupMeters = dist,
                            isAtPickupLocation = isWithinPickupRadius(dist),
                        )
                        previousLatLng = latLng
                        orderRepository.updateLocation(latLng.latitude, latLng.longitude)
                        fetchRoute(latLng, destination, isPeriodicRefresh = true)
                    }
            }
        }
    }

    private fun roughEtaLabel(distanceMeters: Double): String {
        val minutes = (distanceMeters / ROUGH_SPEED_METERS_PER_MIN).roundToInt().coerceAtLeast(1)
        return "~$minutes min"
    }

    private fun formatRemainingDistance(meters: Double): String =
        if (meters >= 1000) {
            String.format(Locale.getDefault(), "%.1f km", meters / 1000.0)
        } else {
            String.format(Locale.getDefault(), "%.0f m", meters)
        }

    fun clearError() {
        _uiState.value = _uiState.value.copy(errorMessage = null)
    }

    fun onPickupCodeChange(raw: String) {
        val digits = raw.filter { it.isDigit() }.take(6)
        _uiState.value = _uiState.value.copy(
            pickupCodeInput = digits,
            pickupCodeValid = if (digits.length == 6) null else false,
        )
        verifyPickupCodeJob?.cancel()
        if (digits.length != 6 || orderId.isBlank()) return
        verifyPickupCodeJob = viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isVerifyingPickupCode = true)
            orderRepository.verifyPickupCode(orderId, digits)
                .onSuccess { valid ->
                    _uiState.value = _uiState.value.copy(
                        isVerifyingPickupCode = false,
                        pickupCodeValid = valid,
                    )
                }
                .onFailure {
                    _uiState.value = _uiState.value.copy(
                        isVerifyingPickupCode = false,
                        pickupCodeValid = false,
                    )
                }
        }
    }

    fun startDelivery(onSuccess: () -> Unit) {
        val state = _uiState.value
        val code = state.pickupCodeInput
        val codeOk = if (state.pickupCodeRequired) {
            code.length == 6 && state.pickupCodeValid == true
        } else {
            true
        }
        val statusOk = state.orderStatus == "ASSIGNED" ||
            (state.isServicePayment && state.orderStatus == "ON_DELIVERY")
        if (orderId.isBlank() || !statusOk || !codeOk || !state.isAtPickupLocation) {
            return
        }
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isStartingDelivery = true, errorMessage = null)
            orderRepository.startDelivery(orderId, if (state.pickupCodeRequired) code else "")
                .onSuccess {
                    _uiState.value = _uiState.value.copy(isStartingDelivery = false)
                    onSuccess()
                }
                .onFailure { e ->
                    _uiState.value = _uiState.value.copy(
                        isStartingDelivery = false,
                        errorMessage = e.toUserFacingMessage()
                    )
                }
        }
    }

    private fun distanceToPickupMeters(current: LatLng, pickup: LatLng?): Double? {
        if (pickup == null) return null
        return CourierHeading.distanceMeters(current, pickup)
    }

    private fun isWithinPickupRadius(distanceMeters: Double?): Boolean =
        distanceMeters != null && distanceMeters <= PICKUP_ARRIVAL_RADIUS_METERS

    private fun computeHeadingForUpdate(
        latLng: LatLng,
        bearingFromGps: Float?,
        speedMps: Float?
    ): Float {
        smoothedHeading = CourierHeading.resolveNavigationHeading(
            current = latLng,
            previous = previousLatLng,
            route = _uiState.value.routePoints,
            bearingFromGps = bearingFromGps,
            speedMps = speedMps,
            previousHeading = smoothedHeading,
        )
        return smoothedHeading
    }

    private fun distanceInMeters(a: LatLng, b: LatLng): Double =
        CourierHeading.distanceMeters(a, b)
}
