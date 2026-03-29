package com.ares.ewe_man.data.util

import com.google.android.gms.maps.model.LatLng

/**
 * Decodes Google's encoded polyline format into a list of LatLng.
 * @see https://developers.google.com/maps/documentation/utilities/polylinealgorithm
 */
object PolylineDecoder {

    fun decode(encoded: String): List<LatLng> {
        if (encoded.isEmpty()) return emptyList()
        val points = mutableListOf<LatLng>()
        var index = 0
        var lat = 0
        var lng = 0
        while (index < encoded.length) {
            var b: Int
            var shift = 0
            var result = 0
            do {
                b = encoded[index++].code - 63
                result = result or (b and 0x1f shl shift)
                shift += 5
            } while (b >= 0x20)
            val dlat = if (result and 1 != 0) (result shr 1).inv() else result shr 1
            lat += dlat
            shift = 0
            result = 0
            do {
                b = encoded[index++].code - 63
                result = result or (b and 0x1f shl shift)
                shift += 5
            } while (b >= 0x20)
            val dlng = if (result and 1 != 0) (result shr 1).inv() else result shr 1
            lng += dlng
            points.add(
                LatLng(
                    lat / 1e5,
                    lng / 1e5
                )
            )
        }
        return points
    }
}
