package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val HighDensityColorScheme = lightColorScheme(
    primary = HighDensityPrimary,
    onPrimary = HighDensityOnPrimary,
    secondary = HighDensityPrimary, // Mapping secondary to main accent purple for design continuity
    onSecondary = HighDensityOnPrimary,
    tertiary = HighDensityPrimary,
    onTertiary = HighDensityOnPrimary,
    background = HighDensityBg,
    onBackground = HighDensityTextPrimary,
    surface = HighDensitySurface,
    onSurface = HighDensityOnSurface,
    surfaceVariant = HighDensitySecondary,
    onSurfaceVariant = HighDensityTextSecondary,
    outline = HighDensityBorder
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = false, // Set to light by default to match "High Density" HTML theme
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = HighDensityColorScheme,
        typography = Typography,
        content = content
    )
}
