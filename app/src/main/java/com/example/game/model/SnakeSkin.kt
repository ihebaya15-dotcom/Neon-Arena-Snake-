package com.example.game.model

import androidx.compose.ui.graphics.Color
import com.example.ui.theme.NeonCyan
import com.example.ui.theme.PlasmaMagenta
import com.example.ui.theme.SolarGold
import com.example.ui.theme.ToxicLime
import com.example.ui.theme.UltravioletPulse

data class SnakeSkin(
    val id: String,
    val displayName: String,
    val primaryColor: Color,
    val secondaryColor: Color,
    val coreColor: Color,
    val glowColor: Color,
)

object SnakeSkins {
    val CYBER_CYAN = SnakeSkin(
        id = "cyber_cyan",
        displayName = "سيان سيبراني",
        primaryColor = NeonCyan,
        secondaryColor = Color(0xFF0099FF),
        coreColor = Color(0xFFE0FFFF),
        glowColor = Color(0xFF00F5D4),
    )

    val PLASMA_MAGENTA = SnakeSkin(
        id = "plasma_magenta",
        displayName = "ماجنتا بلازمي",
        primaryColor = PlasmaMagenta,
        secondaryColor = Color(0xFFFF6B9D),
        coreColor = Color(0xFFFFE6F1),
        glowColor = Color(0xFFFF2A85),
    )

    val TOXIC_LIME = SnakeSkin(
        id = "toxic_lime",
        displayName = "نيون أخضر",
        primaryColor = ToxicLime,
        secondaryColor = Color(0xFF00E676),
        coreColor = Color(0xFFF3FFE0),
        glowColor = Color(0xFFB5FF00),
    )

    val SOLAR_GOLD = SnakeSkin(
        id = "solar_gold",
        displayName = "ذهبي شمسي",
        primaryColor = SolarGold,
        secondaryColor = Color(0xFFFF5E00),
        coreColor = Color(0xFFFFF8E1),
        glowColor = Color(0xFFFFB800),
    )

    val ULTRAVIOLET = SnakeSkin(
        id = "ultraviolet",
        displayName = "بنفسجي فائق",
        primaryColor = UltravioletPulse,
        secondaryColor = Color(0xFF536DFE),
        coreColor = Color(0xFFF3E8FF),
        glowColor = Color(0xFF9B5DE5),
    )

    val all: List<SnakeSkin> = listOf(
        CYBER_CYAN,
        PLASMA_MAGENTA,
        TOXIC_LIME,
        SOLAR_GOLD,
        ULTRAVIOLET,
    )

    fun byId(id: String): SnakeSkin = all.firstOrNull { it.id == id } ?: CYBER_CYAN
}
