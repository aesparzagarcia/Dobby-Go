package com.ares.ewe_man.core.theme

import androidx.compose.ui.graphics.Color

/**
 * Escala pura — de Onyx a Pure (misma que Dobby consumidor).
 * Nav/headers: Onyx · Texto secundario: Ash · Íconos/bordes: Graphite · Cards: Pure
 */
object DobbyPureScale {
    val Onyx = Color(0xFF0D0D0D)
    val Carbon = Color(0xFF1F1F1F)
    val Graphite = Color(0xFF3A3A3A)
    val Ash = Color(0xFF8A8A8A)
    val Mist = Color(0xFFE8E8E8)
    val Fog = Color(0xFFF5F5F5)
    val Pure = Color(0xFFFFFFFF)
}

/** Tokens semánticos — paridad con [com.ares.ewe.core.theme.DobbyColors]. */
object DobbyGoColors {
    val NavHeader = DobbyPureScale.Onyx
    val TextPrimary = DobbyPureScale.Onyx
    val TextSecondary = DobbyPureScale.Ash
    val IconBorder = DobbyPureScale.Graphite
    val CardSurface = DobbyPureScale.Pure
    val ScreenBackground = DobbyPureScale.Fog
    val Divider = DobbyPureScale.Mist
    val SurfaceMuted = DobbyPureScale.Fog

    /** CTAs principales. */
    val Primary = DobbyPureScale.Onyx
    val OnPrimary = DobbyPureScale.Pure

    val Dark = DobbyPureScale.Onyx
    val Light = DobbyPureScale.Fog
    val Carbon = DobbyPureScale.Carbon

    val Accent = Color(0xFF00C2A8)
    val Warning = Color(0xFFFFB800)

    /** Legacy names used across courier screens — map to brand colors. */
    val Purple = Primary
    val PurpleDark = Dark
    val PurpleLight = Light

    val Background = ScreenBackground
    val Surface = CardSurface
    val Border = Divider
    val Blue = Primary
    val BlueLight = Light
    val Orange = Warning

    /** Estados semánticos (entregado, en línea). */
    val Green = Color(0xFF22C55E)
    val GreenLight = Color(0xFFECFDF5)
}
