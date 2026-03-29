package com.ares.ewe_man.data.location

import com.google.android.gms.maps.model.LatLng

/**
 * Snapshot from Fused Location: position plus motion hints for heading-up map rotation.
 */
data class LocationUpdate(
    val latLng: LatLng,
    /** Bearing from GPS in degrees (0–360, clockwise from north), if [android.location.Location.hasBearing]. */
    val bearingDegrees: Float?,
    /** Speed in m/s if [android.location.Location.hasSpeed]. */
    val speedMetersPerSecond: Float?
)
