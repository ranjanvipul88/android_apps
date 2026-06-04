package com.ranjanvipul.relayguard.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val LightColors = lightColorScheme(
    primary = Color(0xFF0B6B61),
    onPrimary = Color.White,
    secondary = Color(0xFF5B5F97),
    tertiary = Color(0xFFB44B37),
    background = Color(0xFFFAFBF8),
    surface = Color(0xFFFFFFFF),
    surfaceVariant = Color(0xFFE3E7E2),
    error = Color(0xFFB3261E)
)

private val DarkColors = darkColorScheme(
    primary = Color(0xFF70D6C9),
    secondary = Color(0xFFC4C6FF),
    tertiary = Color(0xFFFFB4A5),
    background = Color(0xFF111412),
    surface = Color(0xFF191C1A),
    surfaceVariant = Color(0xFF3F4945),
    error = Color(0xFFFFB4AB)
)

@Composable
fun RelayGuardTheme(darkTheme: Boolean = false, content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColors else LightColors,
        typography = MaterialTheme.typography,
        content = content
    )
}
