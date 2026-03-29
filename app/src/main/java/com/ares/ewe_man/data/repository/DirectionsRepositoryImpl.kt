package com.ares.ewe_man.data.repository

import android.util.Log
import com.ares.ewe_man.data.remote.api.GoogleDirectionsApi
import com.ares.ewe_man.data.remote.model.GoogleDirectionsResponse
import com.ares.ewe_man.data.util.PolylineDecoder
import com.ares.ewe_man.domain.repository.DirectionsRepository
import com.ares.ewe_man.domain.repository.DrivingRouteInfo
import com.google.android.gms.maps.model.LatLng
import com.google.gson.Gson
import javax.inject.Inject

private const val TAG = "DirectionsRepo"

class DirectionsRepositoryImpl @Inject constructor(
    private val api: GoogleDirectionsApi
) : DirectionsRepository {

    override suspend fun getDrivingRoute(origin: LatLng, destination: LatLng): Result<DrivingRouteInfo> {
        return try {
            val originStr = "${origin.latitude},${origin.longitude}"
            val destStr = "${destination.latitude},${destination.longitude}"
            val key = com.ares.ewe_man.BuildConfig.MAPS_API_KEY
            if (key.isBlank()) {
                Log.e(TAG, "MAPS_API_KEY is empty")
                return Result.failure(IllegalStateException("MAPS_API_KEY is not set. Add it to local.properties."))
            }
            val response = api.getDirections(
                origin = originStr,
                destination = destStr,
                key = key
            )
            val body = response.body()
            if (!response.isSuccessful || body == null) {
                val errorBody = response.errorBody()?.string()
                val parsed = errorBody?.let {
                    Gson().fromJson(it, GoogleDirectionsResponse::class.java)
                }
                val status = parsed?.status ?: "HTTP ${response.code()}"
                val msg = parsed?.errorMessage ?: errorBody ?: status
                Log.e(TAG, "Directions API failed: status=$status, message=$msg")
                return Result.failure(IllegalStateException("Directions API: $status. $msg"))
            }
            if (body.status != "OK") {
                val msg = body.errorMessage ?: "Directions API: ${body.status}"
                Log.e(TAG, "Directions API status not OK: ${body.status}, $msg")
                return Result.failure(IllegalStateException(msg))
            }
            val route = body.routes?.firstOrNull()
            val encoded = route?.overviewPolyline?.points
            val points = if (!encoded.isNullOrBlank()) PolylineDecoder.decode(encoded) else emptyList()
            val leg = route?.legs?.firstOrNull()
            val info = DrivingRouteInfo(
                points = points,
                durationSeconds = leg?.duration?.value,
                durationText = leg?.duration?.text,
                distanceMeters = leg?.distance?.value,
                distanceText = leg?.distance?.text
            )
            Log.d(TAG, "Route decoded: ${points.size} points, ETA=${info.durationText}")
            Result.success(info)
        } catch (e: Exception) {
            Log.e(TAG, "Directions request failed", e)
            Result.failure(e)
        }
    }
}
