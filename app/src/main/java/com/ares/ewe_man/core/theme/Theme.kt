package com.ares.ewe_man.core.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

private val LightColorScheme = lightColorScheme(
    primary = DobbyGoColors.Primary,
    onPrimary = DobbyGoColors.OnPrimary,
    primaryContainer = DobbyPureScale.Mist,
    onPrimaryContainer = DobbyGoColors.TextPrimary,
    secondary = DobbyGoColors.Accent,
    onSecondary = DobbyPureScale.Pure,
    tertiary = DobbyGoColors.Warning,
    onTertiary = DobbyGoColors.TextPrimary,
    background = DobbyGoColors.ScreenBackground,
    onBackground = DobbyGoColors.TextPrimary,
    surface = DobbyGoColors.CardSurface,
    onSurface = DobbyGoColors.TextPrimary,
    surfaceVariant = DobbyPureScale.Fog,
    onSurfaceVariant = DobbyGoColors.TextSecondary,
    outline = DobbyGoColors.IconBorder,
    outlineVariant = DobbyPureScale.Mist,
)

@Composable
fun DobbyGoTheme(
    @Suppress("UNUSED_PARAMETER") dynamicColor: Boolean = false,
    content: @Composable () -> Unit,
) {
    MaterialTheme(
        colorScheme = LightColorScheme,
        typography = Typography,
        content = content,
    )
}
