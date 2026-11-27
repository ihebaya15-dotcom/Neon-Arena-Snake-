package com.example

import com.example.game.economy.EconomyManager
import com.example.game.economy.PurchaseValidationResult
import com.example.game.model.BotDifficulty
import com.example.game.model.DeathReason
import com.example.game.model.EyeStyle
import com.example.game.model.GameResult
import com.example.game.model.PlayerProgress
import com.example.game.model.SnakeSkins
import com.example.game.model.TrailStyle
import com.example.ui.viewmodel.GameViewModel
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class EconomyManagerTest {

    private val economyManager = EconomyManager()

    @Test
    fun calculateRewardBreakdown_earnsSparksFromDurationAndBotEliminations() {
        val breakdown = economyManager.calculateRewardBreakdown(
            survivedSeconds = 60,
            eliminationsCount = 3,
            difficulty = BotDifficulty.EASY,
        )

        // 60s * 0.5 = 30 duration sparks; 3 * 15 = 45 elimination sparks; Easy x1.0 -> 75 total
        assertEquals(30, breakdown.durationSparks)
        assertEquals(45, breakdown.eliminationSparks)
        assertEquals(1.0f, breakdown.difficultyMultiplier, 0.001f)
        assertEquals(75, breakdown.totalSparks)
    }

    @Test
    fun calculateMatchSparks_scalesAcrossDifficultyAndGuardsNegativeInputs() {
        val result = GameResult(
            finalScore = 1000,
            peakLength = 50,
            survivedSeconds = 40,
            foodEaten = 20,
            eliminationsCount = 2,
            bestRank = 2,
            isNewBest = false,
            deathReason = DeathReason.ARENA_BOUNDARY,
        )

        assertEquals(50, economyManager.calculateMatchSparks(result, BotDifficulty.EASY))
        assertEquals(63, economyManager.calculateMatchSparks(result, BotDifficulty.MEDIUM))
        assertEquals(75, economyManager.calculateMatchSparks(result, BotDifficulty.HARD))

        val negativeGuard = economyManager.calculateRewardBreakdown(
            survivedSeconds = -15,
            eliminationsCount = -3,
            difficulty = BotDifficulty.HARD,
        )
        assertEquals(0, negativeGuard.totalSparks)
    }

    @Test
    fun validatePurchase_checksBalanceOwnershipAndNegativeCost() {
        val progress = PlayerProgress(
            sparksBalance = 150,
            unlockedSkinIds = setOf(SnakeSkins.CYBER_CYAN.id),
            unlockedTrailIds = setOf(TrailStyle.CLASSIC_GLOW.id),
            unlockedEyeIds = setOf(EyeStyle.CYBER_ROUND.id),
        )

        // Sufficient currency -> Valid with remaining balance
        val valid = economyManager.validateSkinPurchase(SnakeSkins.PLASMA_MAGENTA, progress)
        assertTrue(valid is PurchaseValidationResult.Valid)
        val validResult = valid as PurchaseValidationResult.Valid
        assertEquals(120, validResult.costSparks)
        assertEquals(30, validResult.remainingBalance)

        // Insufficient currency -> InsufficientFunds with exact missing amount
        val insufficient = economyManager.validateTrailPurchase(TrailStyle.PULSE_RINGS, progress)
        assertTrue(insufficient is PurchaseValidationResult.InsufficientFunds)
        val missingResult = insufficient as PurchaseValidationResult.InsufficientFunds
        assertEquals(190, missingResult.requiredSparks)
        assertEquals(150, missingResult.currentBalance)
        assertEquals(40, missingResult.missingSparks)

        // Already owned -> AlreadyOwned
        val owned = economyManager.validateEyePurchase(EyeStyle.CYBER_ROUND, progress)
        assertTrue(owned is PurchaseValidationResult.AlreadyOwned)

        // Invalid negative price -> InvalidCost
        val invalidCost = economyManager.validatePurchase(
            itemId = "corrupt_item",
            priceSparks = -10,
            isAlreadyOwned = false,
            currentBalance = 500,
        )
        assertTrue(invalidCost is PurchaseValidationResult.InvalidCost)
    }

    @Test
    fun gameViewModel_storeValidationEmitsFeedbackOnInsufficientFundsAndClearsOnSuccess() {
        val viewModel = GameViewModel(persistenceManager = null, economyManager = economyManager)

        // Starter balance is 180; Pulse Rings trail costs 190 -> missing 10
        viewModel.purchaseOrEquipTrail(TrailStyle.PULSE_RINGS)
        val feedback = viewModel.uiState.value.storeValidationFeedback
        assertNotNull(feedback)
        assertEquals(TrailStyle.PULSE_RINGS.id, feedback!!.itemId)
        assertEquals(190, feedback.requiredSparks)
        assertEquals(180, feedback.currentBalance)
        assertEquals(10, feedback.missingSparks)
        assertEquals(180, viewModel.uiState.value.progress.sparksBalance)
        assertFalse(TrailStyle.PULSE_RINGS.id in viewModel.uiState.value.progress.unlockedTrailIds)

        // Purchasing an affordable item (Predator Slit eyes = 90) succeeds and clears error feedback
        viewModel.purchaseOrEquipEye(EyeStyle.PREDATOR_SLIT)
        assertNull(viewModel.uiState.value.storeValidationFeedback)
        assertEquals(90, viewModel.uiState.value.progress.sparksBalance)
        assertTrue(EyeStyle.PREDATOR_SLIT.id in viewModel.uiState.value.progress.unlockedEyeIds)
        assertEquals(EyeStyle.PREDATOR_SLIT, viewModel.uiState.value.selectedEye)
    }
}
