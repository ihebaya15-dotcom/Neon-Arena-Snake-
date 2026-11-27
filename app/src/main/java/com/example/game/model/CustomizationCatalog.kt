package com.example.game.model

import androidx.compose.ui.graphics.Color
import com.example.R
import com.example.game.economy.EconomyManager
import com.example.ui.theme.DangerCrimson
import com.example.ui.theme.NeonCyan
import com.example.ui.theme.PlasmaMagenta
import com.example.ui.theme.SolarGold
import com.example.ui.theme.ToxicLime
import com.example.ui.theme.UltravioletPulse

enum class TrailStyle(
    val id: String,
    val nameResId: Int,
    val priceSparks: Int,
    val isStarter: Boolean,
) {
    CLASSIC_GLOW(
        id = "trail_classic",
        nameResId = R.string.trail_classic_name,
        priceSparks = 0,
        isStarter = true,
    ),
    PLASMA_SPARKS(
        id = "trail_sparks",
        nameResId = R.string.trail_sparks_name,
        priceSparks = 110,
        isStarter = false,
    ),
    PULSE_RINGS(
        id = "trail_rings",
        nameResId = R.string.trail_rings_name,
        priceSparks = 190,
        isStarter = false,
    ),
    STARLIGHT_RIBBON(
        id = "trail_stars",
        nameResId = R.string.trail_stars_name,
        priceSparks = 280,
        isStarter = false,
    );

    companion object {
        fun byId(id: String): TrailStyle = entries.firstOrNull { it.id == id } ?: CLASSIC_GLOW
    }
}

enum class EyeStyle(
    val id: String,
    val nameResId: Int,
    val priceSparks: Int,
    val isStarter: Boolean,
) {
    CYBER_ROUND(
        id = "eye_round",
        nameResId = R.string.eye_round_name,
        priceSparks = 0,
        isStarter = true,
    ),
    PREDATOR_SLIT(
        id = "eye_slit",
        nameResId = R.string.eye_slit_name,
        priceSparks = 90,
        isStarter = false,
    ),
    NEON_VISOR(
        id = "eye_visor",
        nameResId = R.string.eye_visor_name,
        priceSparks = 170,
        isStarter = false,
    ),
    STAR_CORE(
        id = "eye_star",
        nameResId = R.string.eye_star_name,
        priceSparks = 250,
        isStarter = false,
    );

    companion object {
        fun byId(id: String): EyeStyle = entries.firstOrNull { it.id == id } ?: CYBER_ROUND
    }
}

enum class UiAccentPreset(
    val id: String,
    val color: Color?,
) {
    SYNC_WITH_SKIN("sync_skin", null),
    CYBER_CYAN("accent_cyan", NeonCyan),
    PLASMA_MAGENTA("accent_magenta", PlasmaMagenta),
    TOXIC_LIME("accent_lime", ToxicLime),
    SOLAR_GOLD("accent_gold", SolarGold),
    ULTRAVIOLET("accent_violet", UltravioletPulse),
    CRIMSON("accent_crimson", DangerCrimson);

    fun resolveColor(equippedSkin: SnakeSkin): Color = color ?: equippedSkin.primaryColor

    companion object {
        fun byId(id: String): UiAccentPreset = entries.firstOrNull { it.id == id } ?: SYNC_WITH_SKIN
    }
}

enum class HubSection {
    PLAY,
    STORE,
    CUSTOMIZE,
    RECORD,
}

enum class StoreCategory {
    SKINS,
    TRAILS,
    EYES,
}

data class PlayerProgress(
    val sparksBalance: Int = SparksEconomy.INITIAL_STARTER_SPARKS,
    val totalSparksEarned: Int = SparksEconomy.INITIAL_STARTER_SPARKS,
    val unlockedSkinIds: Set<String> = setOf(SnakeSkins.CYBER_CYAN.id),
    val unlockedTrailIds: Set<String> = setOf(TrailStyle.CLASSIC_GLOW.id),
    val unlockedEyeIds: Set<String> = setOf(EyeStyle.CYBER_ROUND.id),
    val equippedSkinId: String = SnakeSkins.CYBER_CYAN.id,
    val equippedTrailId: String = TrailStyle.CLASSIC_GLOW.id,
    val equippedEyeId: String = EyeStyle.CYBER_ROUND.id,
    val accentPresetId: String = UiAccentPreset.SYNC_WITH_SKIN.id,
    val bestScore: Int = 0,
    val totalMatches: Int = 0,
    val totalEliminations: Int = 0,
    val totalFoodEaten: Int = 0,
    val bestRank: Int = 0,
) {
    val equippedSkin: SnakeSkin get() = SnakeSkins.byId(equippedSkinId)
    val equippedTrail: TrailStyle get() = TrailStyle.byId(equippedTrailId)
    val equippedEye: EyeStyle get() = EyeStyle.byId(equippedEyeId)
    val accentPreset: UiAccentPreset get() = UiAccentPreset.byId(accentPresetId)
    val coordinatedAccentColor: Color get() = accentPreset.resolveColor(equippedSkin)

    val totalCatalogItemsCount: Int
        get() = SnakeSkins.all.size + TrailStyle.entries.size + EyeStyle.entries.size

    val totalUnlockedItemsCount: Int
        get() = unlockedSkinIds.size + unlockedTrailIds.size + unlockedEyeIds.size

    val nonStarterUnlockedCount: Int
        get() = (totalUnlockedItemsCount - 3).coerceAtLeast(0)
}

object SparksEconomy {
    const val INITIAL_STARTER_SPARKS = EconomyManager.INITIAL_STARTER_SPARKS

    private val defaultEconomyManager = EconomyManager()

    fun calculateMatchReward(
        result: GameResult,
        difficulty: BotDifficulty,
    ): Int = defaultEconomyManager.calculateMatchSparks(
        result = result,
        difficulty = difficulty,
    )
}
