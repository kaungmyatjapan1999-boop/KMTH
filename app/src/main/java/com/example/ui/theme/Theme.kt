package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val KmthDarkColorScheme = darkColorScheme(
    primary = ElectricBlue,
    onPrimary = Color.Black,
    primaryContainer = CyberDarkSurfaceVariant,
    onPrimaryContainer = ElectricBlue,
    secondary = NeonPurple,
    onSecondary = Color.White,
    secondaryContainer = Color(0x33A855F7),
    onSecondaryContainer = NeonPurpleLight,
    tertiary = VividOrange,
    onTertiary = Color.Black,
    background = CyberDarkBg,
    onBackground = TextPrimary,
    surface = CyberDarkSurface,
    onSurface = TextPrimary,
    surfaceVariant = CyberDarkSurfaceVariant,
    onSurfaceVariant = TextSecondary,
    outline = CyberGlassBorder,
    error = RedDisconnected,
    onError = Color.White
)

@Composable
fun KmthTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = KmthDarkColorScheme,
        typography = Typography,
        content = content
    )
}
