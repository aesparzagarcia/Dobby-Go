package com.ares.ewe_man.presentation.viewmodel.pickupmap

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ares.ewe_man.core.network.toUserFacingMessage
import com.ares.ewe_man.data.location.LocationProvider
import com.ares.ewe_man.domain.repository.DirectionsRepository
import com.ares.ewe_man.domain.repository.OrderRepository
import com.google.android.gms.maps.model.LatLng
import dagger.hilt.android.lifecycle.HiltViewModel
import java.util.Locale
import javax.inject.Inject
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

private const val ROUTE_AND_ETA_REFRESH_MS = 30_000L
private const val ROUGH_SPEED_METERS_PER_MIN = 420.0
/** Si el repartidor está a esta distancia o menos del punto de recogida, puede iniciar el envío. */
private const val PICKUP_PROXIMITY_THRESHOLD_METERS = 120.0

data class PickupMapUiState(
    val pickupLatLng: LatLng? = null,
    val pickupTitle: String? = null,
    val pickupAddress: String? = null,
    /** Cliente al que se entregará el pedido (tras recoger en el restaurante). */
    val customerName: String? = null,
    val currentLocation: LatLng? = null,
    /** Distancia en línea recta al punto de recogida; null si falta ubicación o tienda. */
    val distanceToPickupMeters: Double? = null,
    val canStartDelivery: Boolean = false,
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

    private var locationPollJob: Job? = null
    private var lastRouteFetchAt = 0L
    private var previousLatLng: LatLng? = null
    private var smoothedHeading: Float = 0f

    init {
        loadData()
    }

    override fun onCleared() {
        super.onCleared()
        locationPollJob?.cancel()
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
            val title = order.shopName ?: "Restaurante"
            _uiState.value = _uiState.value.copy(
                pickupLatLng = pickupLatLng,
                pickupTitle = title,
                pickupAddress = order.shopAddress,
                customerName = order.customerName
            )
            locationProvider.getCurrentLocation()
                .onSuccess { update ->
                    val latLng = update.latLng
                    val heading = computeHeadingForUpdate(
                        latLng,
                        update.bearingDegrees,
                        update.speedMetersPerSecond
                    )
                    val (dist, canStart) = proximityFromCurrentPickup(latLng, pickupLatLng)
                    _uiState.value = _uiState.value.copy(
                        currentLocation = latLng,
                        headingDegrees = heading,
                        isLoading = false,
                        distanceToPickupMeters = dist,
                        canStartDelivery = canStart
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
                        val (dist, canStart) = proximityFromCurrentPickup(latLng, destination)
                        _uiState.value = _uiState.value.copy(
                            currentLocation = latLng,
                            headingDegrees = heading,
                            distanceToPickupMeters = dist,
                            canStartDelivery = canStart
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

    fun startDelivery(onSuccess: () -> Unit) {
        if (orderId.isBlank() || !_uiState.value.canStartDelivery) return
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isStartingDelivery = true, errorMessage = null)
            orderRepository.startDelivery(orderId)
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

    private fun proximityFromCurrentPickup(current: LatLng, pickup: LatLng?): Pair<Double?, Boolean> {
        if (pickup == null) return null to false
        val meters = distanceInMeters(current, pickup)
        return meters to (meters <= PICKUP_PROXIMITY_THRESHOLD_METERS)
    }

    private fun computeHeadingForUpdate(
        latLng: LatLng,
        bearingFromGps: Float?,
        speedMps: Float?
    ): Float {
        val target: Float? = when {
            bearingFromGps != null && (speedMps == null || speedMps > 0.12f) ->
                normalizeHeadingDegrees(bearingFromGps)
            else -> {
                val prev = previousLatLng
                if (prev != null && distanceInMeters(prev, latLng) > 0.65) {
                    computeBearingBetween(prev, latLng)
                } else {
                    null
                }
            }
        }
        if (target != null) {
            smoothedHeading = smoothHeadingToward(smoothedHeading, target)
        }
        return smoothedHeading
    }

    private fun normalizeHeadingDegrees(deg: Float): Float =
        ((deg % 360f) + 360f) % 360f

    private fun smoothHeadingToward(current: Float, target: Float): Float {
        val diff = ((target - current + 540f) % 360f) - 180f
        val ad = abs(diff)
        val alpha = when {
            ad > 50f -> 0.82f
            ad > 20f -> 0.58f
            else -> 0.38f
        }
        return normalizeHeadingDegrees(current + diff * alpha)
    }

    private fun computeBearingBetween(from: LatLng, to: LatLng): Float {
        val lat1 = Math.toRadians(from.latitude)
        val lat2 = Math.toRadians(to.latitude)
        val dLng = Math.toRadians(to.longitude - from.longitude)
        val y = sin(dLng) * cos(lat2)
        val x = cos(lat1) * sin(lat2) - sin(lat1) * cos(lat2) * cos(dLng)
        val bearing = Math.toDegrees(atan2(y, x))
        return normalizeHeadingDegrees(bearing.toFloat())
    }

    private fun distanceInMeters(a: LatLng, b: LatLng): Double {
        val earthRadius = 6_371_000.0
        val dLat = Math.toRadians(b.latitude - a.latitude)
        val dLng = Math.toRadians(b.longitude - a.longitude)
        val x = Math.sin(dLat / 2) * Math.sin(dLat / 2) +
            Math.cos(Math.toRadians(a.latitude)) * Math.cos(Math.toRadians(b.latitude)) *
            Math.sin(dLng / 2) * Math.sin(dLng / 2)
        val c = 2 * Math.atan2(Math.sqrt(x), Math.sqrt(1 - x))
        return earthRadius * c
    }
}
