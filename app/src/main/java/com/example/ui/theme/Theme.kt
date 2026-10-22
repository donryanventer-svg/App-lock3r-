package com.example.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DarkColorScheme = darkColorScheme(
    primary = CyanPrimary, // #D0BCFF
    onPrimary = Color(0xFF381E72),
    primaryContainer = IndigoAccent, // #381E72
    onPrimaryContainer = PurpleLightContainer, // #EADDFF
    secondary = PurpleLightContainer,
    onSecondary = PurpleOnContainer,
    secondaryContainer = DarkSurfaceElevated,
    onSecondaryContainer = TextPrimary,
    tertiary = EmeraldSuccess,
    background = DarkBackground, // #1C1B1F
    onBackground = TextPrimary, // #E6E1E5
    surface = DarkSurface, // #2B2930
    onSurface = TextPrimary,
    surfaceVariant = DarkSurfaceVariant, // #49454F
    onSurfaceVariant = TextSecondary, // #CAC4D0
    outline = DarkBorder, // #49454F
    outlineVariant = DarkBorderSubtle,
    error = CrimsonAlert,
    onError = Color(0xFF601410)
)

private val LightColorScheme = lightColorScheme(
    primary = LightPrimary,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFE0F2FE),
    onPrimaryContainer = Color(0xFF0369A1),
    secondary = IndigoAccent,
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFEEF2FF),
    onSecondaryContainer = Color(0xFF3730A3),
    tertiary = EmeraldSuccess,
    background = LightBackground,
    onBackground = Color(0xFF0F172A),
    surface = LightSurface,
    onSurface = Color(0xFF0F172A),
    surfaceVariant = LightSurfaceVariant,
    onSurfaceVariant = Color(0xFF475569),
    error = CrimsonAlert,
    onError = Color.White
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = true, // Default to sleek security dark theme
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme
    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
