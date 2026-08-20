package com.ares.ewe_man.presentation.viewmodel.deliverymap

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ares.ewe_man.core.network.toUserFacingMessage
import com.ares.ewe_man.data.location.LocationProvider
import com.ares.ewe_man.domain.repository.DirectionsRepository
import com.ares.ewe_man.domain.repository.DrivingRouteInfo
import com.ares.ewe_man.domain.repository.OrderRepository
import com.ares.ewe_man.presentation.ui.map.CourierHeading
import dagger.hilt.android.lifecycle.HiltViewModel
import com.google.android.gms.maps.model.LatLng
import java.util.Locale
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import javax.inject.Inject
import kotlin.math.roundToInt

/** Distance in meters within which we consider the delivery man "arrived" at destination */
private const val ARRIVAL_RADIUS_METERS = 150.0

/** After marking arrived, wait this long before enabling “call customer”. */
private const val CALL_CUSTOMER_DELAY_MS = 2 * 60 * 1000L

/** Refresh driving directions + ETA while moving (avoid hammering the API). */
private const val ROUTE_AND_ETA_REFRESH_MS = 30_000L

/** ~25 km/h average for rough ETA if Directions API fails */
private const val ROUGH_SPEED_METERS_PER_MIN = 420.0

data class DeliveryMapUiState(
    val deliveryLatLng: LatLng? = null,
    val deliveryAddress: String? = null,
    /** Indicaciones del cliente para la entrega (si el backend las envía). */
    val customerInstructions: String? = null,
    val customerPhone: String? = null,
    val currentLocation: LatLng? = null,
    val routePoints: List<LatLng> = emptyList(),
    /** Localized duration text from Directions, e.g. "23 min" */
    val etaText: String? = null,
    /** Localized remaining distance, e.g. "8,2 km" */
    val remainingDistanceText: String? = null,
    /** True when ETA is a rough estimate (Directions unavailable). */
    val etaIsApproximate: Boolean = false,
    val isLoading: Boolean = true,
    val isNearDestination: Boolean = false,
    /** True when backend already has arrival or we just succeeded marking it. */
    val hasMarkedArrived: Boolean = false,
    val isMarkingArrived: Boolean = false,
    /** True once 2 minutes have passed since arrived and a phone number exists. */
    val canCallCustomer: Boolean = false,
    /** Countdown label while waiting to unlock call, e.g. "Podrás llamar en 1:45". */
    val callCustomerCountdownText: String? = null,
    val deliveryCodeInput: String = "",
    val deliveryCodeValid: Boolean? = null,
    val isVerifyingDeliveryCode: Boolean = false,
    val isMarkingDelivered: Boolean = false,
    val isDelivered: Boolean = false,
    /** After delivery, show optional customer rating step in the bottom sheet. */
    val showCustomerRating: Boolean = false,
    val customerRatingStars: Int = 0,
    val customerPunctual: Boolean = false,
    val customerPaysWell: Boolean = false,
    val customerTipped: Boolean = false,
    val customerRecommended: Boolean = false,
    val isSubmittingCustomerRating: Boolean = false,
    val errorMessage: String? = null,
    /** Camera bearing (0=north, clockwise), for heading-up / Waze-style map rotation. */
    val headingDegrees: Float = 0f
)

@HiltViewModel
class DeliveryMapViewModel @Inject constructor(
    private val orderRepository: OrderRepository,
    private val directionsRepository: DirectionsRepository,
    private val locationProvider: LocationProvider,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    val orderId: String = savedStateHandle.get<String>("orderId").orEmpty()

    private val _uiState = MutableStateFlow(DeliveryMapUiState())
    val uiState: StateFlow<DeliveryMapUiState> = _uiState.asStateFlow()

    private var locationPollJob: Job? = null
    private var callCountdownJob: Job? = null
    private var destinationLatLng: LatLng? = null
    private var lastRouteFetchAt = 0L
    private var previousLatLng: LatLng? = null
    private var smoothedHeading: Float = 0f
    private var lastPostedEtaMinutes: Int? = null
    private var verifyDeliveryCodeJob: Job? = null

    init {
        loadData()
    }

    override fun onCleared() {
        super.onCleared()
        locationPollJob?.cancel()
        verifyDeliveryCodeJob?.cancel()
        callCountdownJob?.cancel()
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
            val deliveryLat = order.lat
            val deliveryLng = order.lng
            val deliveryLatLng = if (deliveryLat != null && deliveryLng != null) {
                LatLng(deliveryLat, deliveryLng)
            } else null
            destinationLatLng = deliveryLatLng
            val alreadyArrived = !order.arrivedAtCustomerAt.isNullOrBlank()
            val phone = order.resolvedCustomerPhone
            _uiState.value = _uiState.value.copy(
                deliveryLatLng = deliveryLatLng,
                deliveryAddress = order.deliveryAddress,
                customerPhone = phone,
                hasMarkedArrived = alreadyArrived,
            )
            if (alreadyArrived) {
                val arrivedAt = parseIsoMillis(order.arrivedAtCustomerAt)
                    ?: System.currentTimeMillis()
                startCallCustomerCountdown(arrivedAt)
            }
            locationProvider.getCurrentLocation()
                .onSuccess { update ->
                    val latLng = update.latLng
                    val heading = computeHeadingForUpdate(
                        latLng,
                        update.bearingDegrees,
                        update.speedMetersPerSecond
                    )
                    previousLatLng = latLng
                    _uiState.value = _uiState.value.copy(
                        currentLocation = latLng,
                        headingDegrees = heading,
                        isLoading = false
                    )
                    orderRepository.updateLocation(latLng.latitude, latLng.longitude)
                    deliveryLatLng?.let { dest ->
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
                    pushEtaMinutesFromRoute(info, origin, destination)
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
                    pushEtaMinutesFromRoughDistance(meters)
                }
        }
    }

    private fun pushEtaMinutesFromRoute(info: DrivingRouteInfo, origin: LatLng, destination: LatLng) {
        val maxMin = 24 * 60
        val sec = info.durationSeconds
        val minutes = if (sec != null && sec > 0) {
            ((sec + 59) / 60).coerceAtLeast(1).coerceAtMost(maxMin)
        } else {
            val meters = info.distanceMeters?.takeIf { it > 0 }?.toDouble()
                ?: distanceInMeters(origin, destination)
            (meters / ROUGH_SPEED_METERS_PER_MIN).roundToInt().coerceAtLeast(1).coerceAtMost(maxMin)
        }
        pushEtaMinutesToBackend(minutes)
    }

    private fun pushEtaMinutesFromRoughDistance(meters: Double) {
        val maxMin = 24 * 60
        val minutes = (meters / ROUGH_SPEED_METERS_PER_MIN).roundToInt().coerceAtLeast(1).coerceAtMost(maxMin)
        pushEtaMinutesToBackend(minutes)
    }

    private fun pushEtaMinutesToBackend(minutes: Int) {
        if (orderId.isBlank()) return
        if (lastPostedEtaMinutes == minutes) return
        lastPostedEtaMinutes = minutes
        viewModelScope.launch {
            orderRepository.updateDeliveryEta(orderId, minutes)
        }
    }

    private fun fallbackRoute(origin: LatLng, destination: LatLng): List<LatLng> =
        listOf(origin, destination)

    /** Poll interval: 500ms for smoother heading-up; route/ETA refresh still throttled in fetchRoute. */
    private fun startLocationPolling(destination: LatLng) {
        locationPollJob?.cancel()
        destinationLatLng = destination
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
                        previousLatLng = latLng
                        val distance = distanceInMeters(latLng, destination)
                        val near = distance <= ARRIVAL_RADIUS_METERS
                        _uiState.value = _uiState.value.copy(
                            currentLocation = latLng,
                            headingDegrees = heading,
                            isNearDestination = near || _uiState.value.isNearDestination
                        )
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

    fun markArrived() {
        if (orderId.isBlank()) return
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isMarkingArrived = true, errorMessage = null)
            orderRepository.markArrivedAtCustomer(orderId)
                .onSuccess {
                    val now = System.currentTimeMillis()
                    _uiState.value = _uiState.value.copy(
                        isMarkingArrived = false,
                        hasMarkedArrived = true,
                        deliveryCodeInput = "",
                        deliveryCodeValid = null,
                        canCallCustomer = false,
                    )
                    startCallCustomerCountdown(now)
                }
                .onFailure { e ->
                    _uiState.value = _uiState.value.copy(
                        isMarkingArrived = false,
                        errorMessage = e.toUserFacingMessage()
                    )
                }
        }
    }

    private fun startCallCustomerCountdown(arrivedAtMillis: Long) {
        callCountdownJob?.cancel()
        callCountdownJob = viewModelScope.launch {
            while (isActive) {
                val remainingMs = CALL_CUSTOMER_DELAY_MS - (System.currentTimeMillis() - arrivedAtMillis)
                val phone = _uiState.value.customerPhone
                if (remainingMs <= 0L) {
                    _uiState.value = _uiState.value.copy(
                        canCallCustomer = !phone.isNullOrBlank(),
                        callCustomerCountdownText = null,
                    )
                    break
                }
                val totalSec = ((remainingMs + 999) / 1000).toInt().coerceAtLeast(1)
                val minutes = totalSec / 60
                val seconds = totalSec % 60
                _uiState.value = _uiState.value.copy(
                    canCallCustomer = false,
                    callCustomerCountdownText = if (phone.isNullOrBlank()) {
                        null
                    } else {
                        String.format(Locale.getDefault(), "Podrás llamar en %d:%02d", minutes, seconds)
                    },
                )
                delay(1_000L)
            }
        }
    }

    private fun parseIsoMillis(raw: String?): Long? {
        if (raw.isNullOrBlank()) return null
        return try {
            java.time.Instant.parse(raw).toEpochMilli()
        } catch (_: Exception) {
            null
        }
    }

    fun onDeliveryCodeChange(raw: String) {
        val digits = raw.filter { it.isDigit() }.take(6)
        _uiState.value = _uiState.value.copy(
            deliveryCodeInput = digits,
            deliveryCodeValid = if (digits.length == 6) null else false,
        )
        verifyDeliveryCodeJob?.cancel()
        if (digits.length != 6 || orderId.isBlank()) return
        verifyDeliveryCodeJob = viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isVerifyingDeliveryCode = true)
            orderRepository.verifyDeliveryCode(orderId, digits)
                .onSuccess { valid ->
                    _uiState.value = _uiState.value.copy(
                        isVerifyingDeliveryCode = false,
                        deliveryCodeValid = valid,
                    )
                }
                .onFailure {
                    _uiState.value = _uiState.value.copy(
                        isVerifyingDeliveryCode = false,
                        deliveryCodeValid = false,
                    )
                }
        }
    }

    fun markDelivered(onSuccess: () -> Unit) {
        val state = _uiState.value
        val code = state.deliveryCodeInput
        if (orderId.isBlank() ||
            !state.hasMarkedArrived ||
            code.length != 6 ||
            state.deliveryCodeValid != true
        ) {
            return
        }
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isMarkingDelivered = true, errorMessage = null)
            orderRepository.markDelivered(orderId, code)
                .onSuccess {
                    _uiState.value = _uiState.value.copy(
                        isMarkingDelivered = false,
                        isDelivered = true,
                        showCustomerRating = true,
                    )
                    locationPollJob?.cancel()
                    // Keep map open for optional rating; [onSuccess] runs after skip/submit.
                    // Stash callback via state is awkward — callers should not auto-navigate.
                    pendingAfterRating = onSuccess
                }
                .onFailure { e ->
                    _uiState.value = _uiState.value.copy(
                        isMarkingDelivered = false,
                        errorMessage = e.toUserFacingMessage()
                    )
                }
        }
    }

    private var pendingAfterRating: (() -> Unit)? = null

    fun setCustomerRatingStars(stars: Int) {
        val next = if (_uiState.value.customerRatingStars == stars) 0 else stars.coerceIn(0, 5)
        _uiState.value = _uiState.value.copy(customerRatingStars = next)
    }

    fun toggleCustomerPunctual() {
        _uiState.value = _uiState.value.copy(customerPunctual = !_uiState.value.customerPunctual)
    }

    fun toggleCustomerPaysWell() {
        _uiState.value = _uiState.value.copy(customerPaysWell = !_uiState.value.customerPaysWell)
    }

    fun toggleCustomerTipped() {
        _uiState.value = _uiState.value.copy(customerTipped = !_uiState.value.customerTipped)
    }

    fun toggleCustomerRecommended() {
        _uiState.value = _uiState.value.copy(customerRecommended = !_uiState.value.customerRecommended)
    }

    fun skipCustomerRating() {
        finishCustomerRatingFlow()
    }

    fun submitCustomerRating() {
        if (orderId.isBlank() || _uiState.value.isSubmittingCustomerRating) return
        val s = _uiState.value
        val stars = s.customerRatingStars.takeIf { it in 1..5 }
        val punctual = s.customerPunctual.takeIf { it }
        val paysWell = s.customerPaysWell.takeIf { it }
        val tipped = s.customerTipped.takeIf { it }
        val recommended = s.customerRecommended.takeIf { it }
        val hasAny =
            stars != null || punctual == true || paysWell == true || tipped == true || recommended == true
        if (!hasAny) {
            finishCustomerRatingFlow()
            return
        }
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isSubmittingCustomerRating = true, errorMessage = null)
            orderRepository.rateCustomer(
                orderId = orderId,
                stars = stars,
                punctual = punctual,
                paysWell = paysWell,
                tipped = tipped,
                recommended = recommended,
            ).onSuccess {
                _uiState.value = _uiState.value.copy(isSubmittingCustomerRating = false)
                finishCustomerRatingFlow()
            }.onFailure { e ->
                _uiState.value = _uiState.value.copy(
                    isSubmittingCustomerRating = false,
                    errorMessage = e.toUserFacingMessage(),
                )
            }
        }
    }

    private fun finishCustomerRatingFlow() {
        _uiState.value = _uiState.value.copy(showCustomerRating = false)
        val done = pendingAfterRating
        pendingAfterRating = null
        done?.invoke()
    }

    fun clearError() {
        _uiState.value = _uiState.value.copy(errorMessage = null)
    }

    /**
     * Tip of the rider arrow / map bearing: only updates while moving (Waze-style).
     */
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
