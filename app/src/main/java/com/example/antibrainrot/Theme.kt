package com.example.antibrainrot

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val LightBlue = Color(0xFFBBDEFB)

private val AntiBrainRotDarkColorScheme = darkColorScheme(
    primary = Color(0xFFBB86FC),
    onPrimary = Color(0xFF000000),
    primaryContainer = Color(0xFF3700B3),
    onPrimaryContainer = Color(0xFFEDE7F6),
    secondary = Color(0xFFBDBDBD),
    onSecondary = Color(0xFF000000),
    secondaryContainer = Color(0xFF424242),
    onSecondaryContainer = Color(0xFFF5F5F5),
    tertiary = LightBlue,
    background = Color(0xFF000000),
    onBackground = Color(0xFFE6E6E6),
    surface = Color(0xFF121212),
    onSurface = Color(0xFFF2F2F2),
    surfaceVariant = Color(0xFF1E1E1E),
    onSurfaceVariant = Color(0xFFBDBDBD),
    surfaceContainer = Color(0xFF1A1A1A),
    surfaceContainerHigh = Color(0xFF242424),
    surfaceContainerHighest = Color(0xFF2E2E2E),
    error = Color(0xFFCF6679),
    onError = Color(0xFF000000)
)

@Composable
fun AntiBrainRotTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = AntiBrainRotDarkColorScheme,
        content = content
    )
}
