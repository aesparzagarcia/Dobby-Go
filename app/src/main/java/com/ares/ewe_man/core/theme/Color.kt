package com.ares.ewe_man.core.theme

import androidx.compose.ui.graphics.Color

/** Dobby brand palette (parity with Dobby / DobbyShop). */
object DobbyGoColors {
    val Primary = Color(0xFF0061FF)
    val Accent = Color(0xFF00C2A8)
    val Light = Color(0xFFF0F4FF)
    val Dark = Color(0xFF1D2B4F)
    val Warning = Color(0xFFFFB800)

    /** Legacy names used across courier screens — map to brand colors. */
    val Purple = Primary
    val PurpleDark = Dark
    val PurpleLight = Light

    val Background = Light
    val Surface = Color.White
    val TextPrimary = Dark
    val TextSecondary = Color(0xFF6B7280)
    val Border = Color(0xFFE5E7EB)
    val Green = Color(0xFF22C55E)
    val GreenLight = Color(0xFFECFDF5)
    val Blue = Primary
    val BlueLight = Light
    val Orange = Warning
}
