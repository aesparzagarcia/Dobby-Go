package com.ares.ewe_man.core.theme

import android.os.Build
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

private val LightColorScheme = lightColorScheme(
    primary = DobbyGoColors.Primary,
    onPrimary = Color.White,
    primaryContainer = DobbyGoColors.Light,
    onPrimaryContainer = DobbyGoColors.Dark,
    secondary = DobbyGoColors.Accent,
    onSecondary = Color.White,
    tertiary = DobbyGoColors.Warning,
    onTertiary = DobbyGoColors.Dark,
    background = Color.White,
    onBackground = DobbyGoColors.Dark,
    surface = DobbyGoColors.Surface,
    onSurface = DobbyGoColors.Dark,
    surfaceVariant = DobbyGoColors.Light,
    onSurfaceVariant = DobbyGoColors.Dark.copy(alpha = 0.72f),
    outline = DobbyGoColors.Border,
)

@Composable
fun DobbyGoTheme(
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            dynamicLightColorScheme(context)
        }
        else -> LightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
