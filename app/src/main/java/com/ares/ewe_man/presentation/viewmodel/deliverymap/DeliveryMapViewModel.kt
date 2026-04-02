package com.ares.ewe_man.presentation.viewmodel.deliverymap

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ares.ewe_man.core.network.toUserFacingMessage
import com.ares.ewe_man.data.location.LocationProvider
import com.ares.ewe_man.domain.repository.DirectionsRepository
import com.ares.ewe_man.domain.repository.DrivingRouteInfo
import com.ares.ewe_man.domain.repository.OrderRepository
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
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.roundToInt

/** Distance in meters within which we consider the delivery man "arrived" at destination */
private const val ARRIVAL_RADIUS_METERS = 150.0

/** Refresh driving directions + ETA while moving (avoid hammering the API). */
private const val ROUTE_AND_ETA_REFRESH_MS = 30_000L

/** ~25 km/h average for rough ETA if Directions API fails */
private const val ROUGH_SPEED_METERS_PER_MIN = 420.0

data class DeliveryMapUiState(
    val deliveryLatLng: LatLng? = null,
    val deliveryAddress: String? = null,
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
    val isMarkingDelivered: Boolean = false,
    val isDelivered: Boolean = false,
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
    private var destinationLatLng: LatLng? = null
    private var lastRouteFetchAt = 0L
    private var previousLatLng: LatLng? = null
    private var smoothedHeading: Float = 0f
    private var lastPostedEtaMinutes: Int? = null

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
            val deliveryLat = order.lat
            val deliveryLng = order.lng
            val deliveryLatLng = if (deliveryLat != null && deliveryLng != null) {
                LatLng(deliveryLat, deliveryLng)
            } else null
            destinationLatLng = deliveryLatLng
            val alreadyArrived = !order.arrivedAtCustomerAt.isNullOrBlank()
            _uiState.value = _uiState.value.copy(
                deliveryLatLng = deliveryLatLng,
                deliveryAddress = order.deliveryAddress,
                hasMarkedArrived = alreadyArrived
            )
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
                    _uiState.value = _uiState.value.copy(
                        isMarkingArrived = false,
                        hasMarkedArrived = true
                    )
                }
                .onFailure { e ->
                    _uiState.value = _uiState.value.copy(
                        isMarkingArrived = false,
                        errorMessage = e.toUserFacingMessage()
                    )
                }
        }
    }

    fun markDelivered(onSuccess: () -> Unit) {
        if (orderId.isBlank()) return
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isMarkingDelivered = true, errorMessage = null)
            orderRepository.markDelivered(orderId)
                .onSuccess {
                    _uiState.value = _uiState.value.copy(
                        isMarkingDelivered = false,
                        isDelivered = true
                    )
                    locationPollJob?.cancel()
                    onSuccess()
                }
                .onFailure { e ->
                    _uiState.value = _uiState.value.copy(
                        isMarkingDelivered = false,
                        errorMessage = e.toUserFacingMessage()
                    )
                }
        }
    }

    fun clearError() {
        _uiState.value = _uiState.value.copy(errorMessage = null)
    }

    /**
     * Heading-up bearing: prefer GPS bearing when moving; otherwise bearing from last→current fix.
     * Smoothed to reduce jitter (Waze-like).
     */
    private fun computeHeadingForUpdate(
        latLng: LatLng,
        bearingFromGps: Float?,
        speedMps: Float?
    ): Float {
        // GPS course when available; speed gate avoids stale bearing when nearly stopped (optional).
        val target: Float? = when {
            bearingFromGps != null && (speedMps == null || speedMps > 0.12f) ->
                normalizeHeadingDegrees(bearingFromGps)
            else -> {
                // Movement vector: needs small threshold so heading updates at city speeds (every 1s poll).
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

    /** Stronger correction on sharp turns so the map “follows” the nose like Waze. */
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
