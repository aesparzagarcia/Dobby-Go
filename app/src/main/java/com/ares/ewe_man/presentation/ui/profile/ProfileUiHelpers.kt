package com.ares.ewe_man.presentation.ui.profile

import com.ares.ewe_man.BuildConfig

/** URL absoluta para Coil; rutas relativas (p. ej. `/uploads/...`) se resuelven contra el host del API. */
fun resolveProfileImageUrl(profilePhotoUrl: String?): String? {
    val raw = profilePhotoUrl?.trim()?.takeIf { it.isNotEmpty() } ?: return null
    if (raw.startsWith("http://", ignoreCase = true) || raw.startsWith("https://", ignoreCase = true)) {
        return raw
    }
    val origin = BuildConfig.BASE_URL
        .trim()
        .trimEnd('/')
        .removeSuffix("/api")
        .trimEnd('/')
    val path = raw.trimStart('/')
    return "$origin/$path"
}

fun levelEmoji(levelKey: String): String = when (levelKey) {
    "NOVATO" -> "🥉"
    "RAPIDO" -> "🥈"
    "PRO" -> "🥇"
    "ELITE" -> "🔥"
    "MASTER_DOB" -> "👑"
    else -> "⭐"
}

/** Etiqueta en español para [DeliveryMan.status]. */
fun connectionStatusLabel(status: String): String = when (status.uppercase()) {
    "OFFLINE" -> "Desconectado"
    "ONLINE" -> "Conectado"
    "ON_DELIVERY" -> "En reparto"
    else -> status
}

fun levelDisplayName(levelKey: String): String = when (levelKey) {
    "NOVATO" -> "Novato"
    "RAPIDO" -> "Rápido"
    "PRO" -> "Pro"
    "ELITE" -> "Elite"
    "MASTER_DOB" -> "Master Dobby"
    else -> levelKey
}
