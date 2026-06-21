package com.example.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

private val LuxuryColorScheme = darkColorScheme(
    primary = LuxuryNeonCyan,
    secondary = LuxuryGold,
    tertiary = LuxuryGold,
    background = LuxuryBlack,
    surface = LuxuryDarkSurface,
    onPrimary = LuxuryBlack,
    onSecondary = LuxuryBlack,
    onTertiary = LuxuryBlack,
    onBackground = LuxuryTextPrimary,
    onSurface = LuxuryTextPrimary,
    error = LuxuryError,
    onError = LuxuryBlack
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = true, // Force dark theme for luxury aesthetic
    dynamicColor: Boolean = false, // Disable dynamic colors to keep neon cyan and gold
    content: @Composable () -> Unit,
) {
    MaterialTheme(colorScheme = LuxuryColorScheme, typography = Typography, content = content)
}
