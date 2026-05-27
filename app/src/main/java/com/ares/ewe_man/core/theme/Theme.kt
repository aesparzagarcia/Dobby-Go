package com.ares.ewe_man.core.theme

import android.os.Build
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

private val LightColorScheme = lightColorScheme(
    primary = DobbyGoColors.Purple,
    onPrimary = Color.White,
    primaryContainer = DobbyGoColors.PurpleLight,
    onPrimaryContainer = DobbyGoColors.PurpleDark,
    secondary = PurpleGrey40,
    tertiary = Pink40,
    background = DobbyGoColors.Background,
    surface = DobbyGoColors.Surface,
    onBackground = DobbyGoColors.TextPrimary,
    onSurface = DobbyGoColors.TextPrimary,
    onSurfaceVariant = DobbyGoColors.TextSecondary,
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
