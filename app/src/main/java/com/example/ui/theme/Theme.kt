package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

private val NeonDarkColorScheme = darkColorScheme(
    primary = NeonCyan,
    onPrimary = SpaceObsidian,
    secondary = PlasmaMagenta,
    onSecondary = TextPrimary,
    tertiary = ToxicLime,
    onTertiary = SpaceObsidian,
    background = SpaceObsidian,
    onBackground = TextPrimary,
    surface = ArenaDeepNavy,
    onSurface = TextPrimary,
    surfaceVariant = HudGlassSurface,
    onSurfaceVariant = TextSecondary,
    error = DangerCrimson,
    onError = TextPrimary,
    outline = HudBorderStroke,
)

@Composable
fun NeonSnakeTheme(
    content: @Composable () -> Unit,
) {
    MaterialTheme(
        colorScheme = NeonDarkColorScheme,
        typography = NeonTypography,
        content = content,
    )
}
