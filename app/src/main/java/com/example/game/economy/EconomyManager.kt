package com.example.game.economy

import com.example.game.model.BotDifficulty
import com.example.game.model.EyeStyle
import com.example.game.model.GameResult
import com.example.game.model.PlayerProgress
import com.example.game.model.SnakeSkin
import com.example.game.model.TrailStyle
import kotlin.math.roundToInt

/**
 * Breakdown of 'Neon Sparks' earned at the end of an arena round.
 */
data class SparksRewardBreakdown(
    val durationSparks: Int,
    val eliminationSparks: Int,
    val difficultyMultiplier: Float,
    val totalSparks: Int,
)

/**
 * Typed validation outcome for Store purchase checks before deducting currency.
 */
sealed interface PurchaseValidationResult {
    val itemId: String

    data class Valid(
        override val itemId: String,
        val costSparks: Int,
        val remainingBalance: Int,
    ) : PurchaseValidationResult

    data class AlreadyOwned(
        override val itemId: String,
    ) : PurchaseValidationResult

    data class InsufficientFunds(
        override val itemId: String,
        val requiredSparks: Int,
        val currentBalance: Int,
        val missingSparks: Int,
    ) : PurchaseValidationResult

    data class InvalidCost(
        override val itemId: String,
        val costSparks: Int,
    ) : PurchaseValidationResult
}

/**
 * Centralized economy & purchase validation manager for Neon Sparks.
 *
 * - Calculates Neon Sparks earned strictly from gameplay duration (seconds survived)
 *   and bot eliminations, scaled by the selected match difficulty.
 * - Validates item ownership, price integrity, and sufficient Sparks balance
 *   prior to committing any Store purchase.
 */
class EconomyManager(
    private val sparksPerSecond: Float = DEFAULT_SPARKS_PER_SECOND,
    private val sparksPerBotElimination: Int = DEFAULT_SPARKS_PER_ELIMINATION,
) {
    fun difficultyMultiplier(difficulty: BotDifficulty): Float = when (difficulty) {
        BotDifficulty.EASY -> 1.0f
        BotDifficulty.MEDIUM -> 1.25f
        BotDifficulty.HARD -> 1.5f
    }

    fun calculateRewardBreakdown(
        survivedSeconds: Int,
        eliminationsCount: Int,
        difficulty: BotDifficulty,
    ): SparksRewardBreakdown {
        val safeDuration = survivedSeconds.coerceAtLeast(0)
        val safeEliminations = eliminationsCount.coerceAtLeast(0)
        val multiplier = difficultyMultiplier(difficulty)

        val baseDurationSparks = (safeDuration * sparksPerSecond).roundToInt()
        val baseEliminationSparks = safeEliminations * sparksPerBotElimination

        val scaledDurationSparks = (baseDurationSparks * multiplier).roundToInt()
        val scaledEliminationSparks = (baseEliminationSparks * multiplier).roundToInt()
        val total = (((baseDurationSparks + baseEliminationSparks) * multiplier)).roundToInt()
            .coerceAtLeast(0)

        return SparksRewardBreakdown(
            durationSparks = scaledDurationSparks,
            eliminationSparks = scaledEliminationSparks,
            difficultyMultiplier = multiplier,
            totalSparks = total,
        )
    }

    fun calculateMatchSparks(
        result: GameResult,
        difficulty: BotDifficulty,
    ): Int = calculateRewardBreakdown(
        survivedSeconds = result.survivedSeconds,
        eliminationsCount = result.eliminationsCount,
        difficulty = difficulty,
    ).totalSparks

    fun canAfford(priceSparks: Int, currentBalance: Int): Boolean =
        priceSparks >= 0 && currentBalance >= priceSparks

    fun missingSparks(priceSparks: Int, currentBalance: Int): Int =
        (priceSparks - currentBalance).coerceAtLeast(0)

    fun validatePurchase(
        itemId: String,
        priceSparks: Int,
        isAlreadyOwned: Boolean,
        currentBalance: Int,
    ): PurchaseValidationResult {
        if (priceSparks < 0) {
            return PurchaseValidationResult.InvalidCost(
                itemId = itemId,
                costSparks = priceSparks,
            )
        }
        if (isAlreadyOwned) {
            return PurchaseValidationResult.AlreadyOwned(itemId = itemId)
        }
        if (currentBalance < priceSparks) {
            return PurchaseValidationResult.InsufficientFunds(
                itemId = itemId,
                requiredSparks = priceSparks,
                currentBalance = currentBalance,
                missingSparks = priceSparks - currentBalance,
            )
        }
        return PurchaseValidationResult.Valid(
            itemId = itemId,
            costSparks = priceSparks,
            remainingBalance = currentBalance - priceSparks,
        )
    }

    fun validateSkinPurchase(
        skin: SnakeSkin,
        progress: PlayerProgress,
    ): PurchaseValidationResult = validatePurchase(
        itemId = skin.id,
        priceSparks = skin.priceSparks,
        isAlreadyOwned = skin.id in progress.unlockedSkinIds,
        currentBalance = progress.sparksBalance,
    )

    fun validateTrailPurchase(
        trail: TrailStyle,
        progress: PlayerProgress,
    ): PurchaseValidationResult = validatePurchase(
        itemId = trail.id,
        priceSparks = trail.priceSparks,
        isAlreadyOwned = trail.id in progress.unlockedTrailIds,
        currentBalance = progress.sparksBalance,
    )

    fun validateEyePurchase(
        eye: EyeStyle,
        progress: PlayerProgress,
    ): PurchaseValidationResult = validatePurchase(
        itemId = eye.id,
        priceSparks = eye.priceSparks,
        isAlreadyOwned = eye.id in progress.unlockedEyeIds,
        currentBalance = progress.sparksBalance,
    )

    companion object {
        const val INITIAL_STARTER_SPARKS = 180
        const val DEFAULT_SPARKS_PER_SECOND = 0.5f
        const val DEFAULT_SPARKS_PER_ELIMINATION = 15
    }
}
