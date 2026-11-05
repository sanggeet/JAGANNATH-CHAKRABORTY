package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val SanggeetDarkColorScheme = darkColorScheme(
    primary = GoldPrimary,
    onPrimary = Color.Black,
    primaryContainer = GoldContainer,
    onPrimaryContainer = OnGoldContainer,
    secondary = GoldLight,
    onSecondary = Color.Black,
    secondaryContainer = DarkSurfaceHighlight,
    onSecondaryContainer = GoldLight,
    tertiary = RubyHeart,
    onTertiary = Color.White,
    background = DarkBackground,
    onBackground = TextWhite,
    surface = DarkSurface,
    onSurface = TextWhite,
    surfaceVariant = DarkSurfaceElevated,
    onSurfaceVariant = TextMuted,
    outline = DarkBorder,
    outlineVariant = DarkSurfaceHighlight
)

@Composable
fun SanggeetTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    // Music apps look best in high-contrast immersive dark mode by default
    MaterialTheme(
        colorScheme = SanggeetDarkColorScheme,
        typography = Typography,
        content = content
    )
}
