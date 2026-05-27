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

/** Código postal mexicano (5 dígitos). */
private val mexicanPostalCode = Regex("\\b\\d{5}\\b")

/**
 * Parte la dirección en calle (primera coma) y una segunda línea solo con
 * colonia, ciudad y estado — sin CP ni país (p. ej. «San Javier, Tala, Jal»).
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

    val estadoRaw = tail.last().takeIf { looksLikeStateToken(it) }
    val middleParts = if (estadoRaw != null) {
        tail.removeAt(tail.size - 1)
        tail
    } else {
        tail
    }

    val estado = estadoRaw?.let(::normalizeStateLabel)
    val middleJoined = middleParts.joinToString(", ").trim()
    val withoutCp = middleJoined
        .replace(mexicanPostalCode, " ")
        .replace(Regex("\\s+"), " ")
        .trim()

    if (withoutCp.isEmpty()) {
        return street to estado
    }

    val words = withoutCp.split(Regex("\\s+")).filter { it.isNotBlank() }
    val secondLine = when {
        estado != null && words.size >= 2 -> {
            val ciudad = words.last()
            val colonia = words.dropLast(1).joinToString(" ")
            "$colonia, $ciudad, $estado"
        }
        estado != null && words.size == 1 -> "${words[0]}, $estado"
        estado != null -> "$withoutCp, $estado"
        else -> withoutCp
    }

    return street to secondLine
}

private fun isCountryToken(s: String): Boolean {
    val n = s.lowercase(Locale.getDefault()).removeSuffix(".").trim()
    return countryTokens.contains(n) || n == "mx"
}

private fun looksLikeStateToken(s: String): Boolean {
    val t = s.trim().lowercase(Locale.getDefault()).removeSuffix(".").removeSuffix(",")
    if (t.isEmpty()) return false
    if (mexicanStateAbbreviations.contains(t)) return true
    return mexicanStateFullNames.contains(t)
}

private val mexicanStateAbbreviations = setOf(
    "jal", "nl", "cdmx", "gto", "mich", "yuc", "sin", "bc", "bcs", "son", "chih", "coah",
    "tam", "ver", "oax", "chiap", "camp", "gro", "hgo", "mex", "mor", "pue", "qro", "qroo",
    "slp", "tab", "tlax", "zac", "ags", "df", "edomex",
)

private val mexicanStateFullNames = setOf(
    "jalisco", "nuevo leon", "nuevo león", "guanajuato", "veracruz", "puebla", "yucatan", "yucatán",
    "quintana roo", "ciudad de mexico", "cdmx", "guerrero", "hidalgo", "michoacan", "michoacán",
    "morelos", "oaxaca", "queretaro", "querétaro", "san luis potosi", "san luis potosí", "sinaloa",
    "sonora", "tabasco", "tamaulipas", "tlaxcala", "zacatecas", "aguascalientes", "baja california",
    "baja california sur", "campeche", "chiapas", "chihuahua", "coahuila", "colima", "durango",
    "nayarit", "nuevo leon", "zacatecas",
)

private fun normalizeStateLabel(s: String): String {
    return s.trim().removeSuffix(".").removeSuffix(",")
}
