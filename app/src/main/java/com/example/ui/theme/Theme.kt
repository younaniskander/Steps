package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

// For visually impaired users, high-contrast dark palette is the most legible and battery efficient
private val StepSightColorScheme = darkColorScheme(
    primary = HighVisYellow,
    onPrimary = TextDark,
    primaryContainer = HighVisYellowDark,
    onPrimaryContainer = TextWhite,
    secondary = NavBlue,
    onSecondary = TextDark,
    secondaryContainer = DarkSurfaceCardHover,
    onSecondaryContainer = TextHighContrast,
    tertiary = SafeGreen,
    onTertiary = TextDark,
    error = HazardRed,
    onError = TextWhite,
    background = DarkBackground,
    onBackground = TextWhite,
    surface = DarkSurface,
    onSurface = TextWhite,
    surfaceVariant = DarkSurfaceCard,
    onSurfaceVariant = TextHighContrast,
    outline = HighVisYellow
)

@Composable
fun StepSightTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = StepSightColorScheme,
        typography = Typography,
        content = content
    )
}
