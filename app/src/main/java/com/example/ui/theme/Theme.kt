package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

@Composable
fun NeonSnakeTheme(
    primaryAccent: Color = NeonCyan,
    content: @Composable () -> Unit,
) {
    val colorScheme = darkColorScheme(
        primary = primaryAccent,
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
    MaterialTheme(
        colorScheme = colorScheme,
        typography = NeonTypography,
        content = content,
    )
}
