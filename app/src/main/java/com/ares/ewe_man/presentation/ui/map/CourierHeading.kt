package com.ares.ewe_man.presentation.ui.map

import com.google.android.gms.maps.model.LatLng
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

/** Heading helpers so the rider arrow tip follows movement / the route polyline. */
object CourierHeading {
    fun normalizeDegrees(deg: Float): Float = ((deg % 360f) + 360f) % 360f

    fun bearingBetween(from: LatLng, to: LatLng): Float {
        val lat1 = Math.toRadians(from.latitude)
        val lat2 = Math.toRadians(to.latitude)
        val dLng = Math.toRadians(to.longitude - from.longitude)
        val y = sin(dLng) * cos(lat2)
        val x = cos(lat1) * sin(lat2) - sin(lat1) * cos(lat2) * cos(dLng)
        return normalizeDegrees(Math.toDegrees(atan2(y, x)).toFloat())
    }

    fun distanceMeters(a: LatLng, b: LatLng): Double {
        val earthRadius = 6_371_000.0
        val dLat = Math.toRadians(b.latitude - a.latitude)
        val dLng = Math.toRadians(b.longitude - a.longitude)
        val x = sin(dLat / 2) * sin(dLat / 2) +
            cos(Math.toRadians(a.latitude)) * cos(Math.toRadians(b.latitude)) *
            sin(dLng / 2) * sin(dLng / 2)
        val c = 2 * atan2(sqrt(x), sqrt(1 - x))
        return earthRadius * c
    }

    /**
     * Bearing from [current] toward a point ~[lookAheadMeters] ahead on [route].
     * Matches “where the route line is pointing” for turns.
     */
    fun alongRoute(
        current: LatLng,
        route: List<LatLng>,
        lookAheadMeters: Double = 45.0,
    ): Float? {
        if (route.size < 2) return null
        var closestIdx = 0
        var closestDist = Double.MAX_VALUE
        for (i in route.indices) {
            val d = distanceMeters(current, route[i])
            if (d < closestDist) {
                closestDist = d
                closestIdx = i
            }
        }
        // Ignore if far off the polyline (e.g. before first route fetch).
        if (closestDist > 120.0) return null

        var traveled = 0.0
        var i = closestIdx
        while (i < route.lastIndex) {
            val a = route[i]
            val b = route[i + 1]
            val seg = distanceMeters(a, b)
            if (traveled + seg >= lookAheadMeters) {
                return bearingBetween(current, b)
            }
            traveled += seg
            i++
        }
        return bearingBetween(current, route.last())
    }

    fun smoothToward(current: Float, target: Float): Float {
        val diff = ((target - current + 540f) % 360f) - 180f
        val ad = kotlin.math.abs(diff)
        val alpha = when {
            ad > 50f -> 0.82f
            ad > 20f -> 0.58f
            else -> 0.38f
        }
        return normalizeDegrees(current + diff * alpha)
    }
}
