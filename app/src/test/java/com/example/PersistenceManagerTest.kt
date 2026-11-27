package com.example

import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import com.example.data.PersistenceManager
import com.example.game.economy.EconomyManager
import com.example.game.economy.PurchaseValidationResult
import com.example.game.model.DeathReason
import com.example.game.model.EyeStyle
import com.example.game.model.GameResult
import com.example.game.model.SnakeSkins
import com.example.game.model.SparksEconomy
import com.example.game.model.TrailStyle
import com.example.game.model.UiAccentPreset
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File

class PersistenceManagerTest {

    @get:Rule
    val tempFolder = TemporaryFolder()

    @Test
    fun persistenceManager_savesSparksUnlockedItemsAndHighScoresAcrossInstances() = runBlocking {
        val backingFile = File(tempFolder.root, "neon_snake_test.preferences_pb")

        val scope1 = CoroutineScope(SupervisorJob() + Dispatchers.IO)
        val dataStore1 = PreferenceDataStoreFactory.create(
            scope = scope1,
            produceFile = { backingFile },
        )
        val manager1 = PersistenceManager(dataStore1, EconomyManager())

        // Initial defaults
        val initial = manager1.getSnapshot()
        assertEquals(SparksEconomy.INITIAL_STARTER_SPARKS, initial.sparksBalance)
        assertEquals(0, initial.bestScore)
        assertEquals(setOf(SnakeSkins.CYBER_CYAN.id), initial.unlockedSkinIds)

        // Record a match earning Sparks and a new high score
        val matchResult = GameResult(
            finalScore = 1650,
            peakLength = 52,
            survivedSeconds = 60,
            foodEaten = 30,
            eliminationsCount = 3,
            bestRank = 1,
            isNewBest = true,
            deathReason = DeathReason.ARENA_BOUNDARY,
        )
        manager1.recordMatchResult(matchResult, earnedSparks = 75)

        // Purchase a skin and eye style, and update UI accent preset
        val skinPurchase = manager1.purchaseSkin(SnakeSkins.PLASMA_MAGENTA) // 120 sparks
        assertTrue(skinPurchase is PurchaseValidationResult.Valid)

        val eyePurchase = manager1.purchaseEye(EyeStyle.PREDATOR_SLIT) // 90 sparks
        assertTrue(eyePurchase is PurchaseValidationResult.Valid)

        // Attempt purchase exceeding remaining balance (180 + 75 - 120 - 90 = 45 < 190)
        val blockedTrail = manager1.purchaseTrail(TrailStyle.PULSE_RINGS)
        assertTrue(blockedTrail is PurchaseValidationResult.InsufficientFunds)

        manager1.selectAccentPreset(UiAccentPreset.SOLAR_GOLD.id)
        manager1.updateHighScore(2100)
        manager1.updateHighScore(900) // Lower score must not overwrite high score

        scope1.cancel()

        // Re-open with a brand-new DataStore & PersistenceManager on the same file to verify offline persistence
        val scope2 = CoroutineScope(SupervisorJob() + Dispatchers.IO)
        val dataStore2 = PreferenceDataStoreFactory.create(
            scope = scope2,
            produceFile = { backingFile },
        )
        val manager2 = PersistenceManager(dataStore2, EconomyManager())

        val reloaded = manager2.getSnapshot()
        assertEquals(45, reloaded.sparksBalance)
        assertEquals(SparksEconomy.INITIAL_STARTER_SPARKS + 75, reloaded.totalSparksEarned)
        assertEquals(2100, reloaded.bestScore)
        assertEquals(1, reloaded.totalMatches)
        assertEquals(3, reloaded.totalEliminations)
        assertEquals(1, reloaded.bestRank)
        assertTrue(SnakeSkins.PLASMA_MAGENTA.id in reloaded.unlockedSkinIds)
        assertTrue(EyeStyle.PREDATOR_SLIT.id in reloaded.unlockedEyeIds)
        assertFalse(TrailStyle.PULSE_RINGS.id in reloaded.unlockedTrailIds)
        assertEquals(SnakeSkins.PLASMA_MAGENTA.id, reloaded.equippedSkinId)
        assertEquals(EyeStyle.PREDATOR_SLIT.id, reloaded.equippedEyeId)
        assertEquals(UiAccentPreset.SOLAR_GOLD.id, reloaded.accentPresetId)

        scope2.cancel()
    }
}
