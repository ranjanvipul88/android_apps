package com.vipul.messages.smsmms.ui

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext

// --- High fidelity color definitions matching Google Messages ---
// Placed at the top to satisfy Kotlin's top-level initialization order constraints!
val Purple80 = androidx.compose.ui.graphics.Color(0xFFD2E3FC)
val PurpleGrey80 = androidx.compose.ui.graphics.Color(0xFFADC6FF)
val Pink80 = androidx.compose.ui.graphics.Color(0xFFC2E7FF)

val Purple40 = androidx.compose.ui.graphics.Color(0xFF1A73E8)
val PurpleGrey40 = androidx.compose.ui.graphics.Color(0xFF004B73)
val Pink40 = androidx.compose.ui.graphics.Color(0xFF004B73)

val md_theme_dark_background = androidx.compose.ui.graphics.Color(0xFF0B0E14)
val md_theme_dark_surface = androidx.compose.ui.graphics.Color(0xFF131722)
val md_theme_dark_onPrimary = androidx.compose.ui.graphics.Color(0xFF051B33)
val md_theme_dark_primaryContainer = androidx.compose.ui.graphics.Color(0xFF004B73)
val md_theme_dark_onPrimaryContainer = androidx.compose.ui.graphics.Color(0xFFC2E7FF)

val md_theme_light_background = androidx.compose.ui.graphics.Color(0xFFF8F9FA)
val md_theme_light_surface = androidx.compose.ui.graphics.Color(0xFFFFFFFF)
val md_theme_light_onPrimary = androidx.compose.ui.graphics.Color(0xFFFFFFFF)
val md_theme_light_primaryContainer = androidx.compose.ui.graphics.Color(0xFFD2E3FC)
val md_theme_light_onPrimaryContainer = androidx.compose.ui.graphics.Color(0xFF001D38)

private val DarkColorScheme = darkColorScheme(
    primary = Purple80,
    secondary = PurpleGrey80,
    tertiary = Pink80,
    background = md_theme_dark_background,
    surface = md_theme_dark_surface,
    onPrimary = md_theme_dark_onPrimary,
    primaryContainer = md_theme_dark_primaryContainer,
    onPrimaryContainer = md_theme_dark_onPrimaryContainer
)

private val LightColorScheme = lightColorScheme(
    primary = Purple40,
    secondary = PurpleGrey40,
    tertiary = Pink40,
    background = md_theme_light_background,
    surface = md_theme_light_surface,
    onPrimary = md_theme_light_onPrimary,
    primaryContainer = md_theme_light_primaryContainer,
    onPrimaryContainer = md_theme_light_onPrimaryContainer
)

@Composable
fun SmsReplicaTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = true, // Enables Android 12+ Material You Wallpaper accent colors!
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}

// Mock Typography
val Typography = Typography()
