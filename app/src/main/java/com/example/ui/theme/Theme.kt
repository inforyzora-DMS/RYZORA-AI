package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val RyzoraDarkColorScheme = darkColorScheme(
    primary = RyzoraCyan,
    onPrimary = Color.Black,
    primaryContainer = Color(0xFF083344),
    onPrimaryContainer = RyzoraCyan,
    secondary = RyzoraEmerald,
    onSecondary = Color.Black,
    secondaryContainer = Color(0xFF064E3B),
    onSecondaryContainer = RyzoraEmerald,
    tertiary = RyzoraAmber,
    onTertiary = Color.Black,
    background = ObsidianBlack,
    onBackground = TextPrimary,
    surface = SurfaceDark,
    onSurface = TextPrimary,
    surfaceVariant = SurfaceCard,
    onSurfaceVariant = TextSecondary,
    outline = SurfaceBorder,
    error = RyzoraRose,
    onError = Color.White
)

@Composable
fun RyzoraTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = RyzoraDarkColorScheme,
        typography = Typography,
        content = content
    )
}

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = true,
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    RyzoraTheme(content = content)
}
