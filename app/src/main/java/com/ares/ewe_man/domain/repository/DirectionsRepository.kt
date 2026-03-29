package com.ares.ewe_man.domain.repository

import com.google.android.gms.maps.model.LatLng

data class DrivingRouteInfo(
    val points: List<LatLng>,
    val durationSeconds: Int?,
    val durationText: String?,
    val distanceMeters: Int?,
    val distanceText: String?
)

interface DirectionsRepository {

    suspend fun getDrivingRoute(origin: LatLng, destination: LatLng): Result<DrivingRouteInfo>
}
