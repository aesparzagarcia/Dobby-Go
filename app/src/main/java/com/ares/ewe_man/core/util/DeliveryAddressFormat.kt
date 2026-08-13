package com.ares.ewe_man.core.util

import java.util.Locale

private val countryTokens = setOf(
    "mexico",
    "méxico",
    "mx",
    "usa",
    "u.s.a",
    "united states",
    "estados unidos",
)

/**
 * Parte la dirección como en iOS DobbyGo: calle en la primera línea y el resto
 * (colonia, CP, ciudad, estado) en la segunda — solo se omite el país.
 */
fun splitDeliveryAddressForDisplay(address: String): Pair<String, String?> {
    val parts = address.split(",").map { it.trim() }.filter { it.isNotEmpty() }
    if (parts.isEmpty()) return address.trim() to null
    if (parts.size == 1) return parts[0] to null

    val street = parts[0]
    val tail = parts.drop(1).toMutableList()

    while (tail.isNotEmpty() && isCountryToken(tail.last())) {
        tail.removeAt(tail.size - 1)
    }
    if (tail.isEmpty()) return street to null

    return street to tail.joinToString(", ")
}

private fun isCountryToken(s: String): Boolean {
    val n = s.lowercase(Locale.getDefault()).removeSuffix(".").trim()
    return countryTokens.contains(n)
}
