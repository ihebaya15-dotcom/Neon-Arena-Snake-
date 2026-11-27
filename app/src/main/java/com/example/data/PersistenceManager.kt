package com.example.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.game.economy.EconomyManager
import com.example.game.economy.PurchaseValidationResult
import com.example.game.model.EyeStyle
import com.example.game.model.GameResult
import com.example.game.model.PlayerProgress
import com.example.game.model.SnakeSkin
import com.example.game.model.SnakeSkins
import com.example.game.model.SparksEconomy
import com.example.game.model.TrailStyle
import com.example.game.model.UiAccentPreset
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import java.io.IOException

private val Context.neonSnakePreferencesDataStore: DataStore<Preferences> by preferencesDataStore(
    name = "neon_snake_progress",
)

/**
 * Offline-first local persistence layer backed by Jetpack DataStore Preferences.
 *
 * Manages durable on-device storage for:
 * - Earned & spendable Neon Sparks currency
 * - Unlocked & equipped customization items (skins/colors, trails, eyes, UI accent sync)
 * - High scores and lifetime arena statistics
 */
class PersistenceManager(
    private val dataStore: DataStore<Preferences>,
    private val economyManager: EconomyManager = EconomyManager(),
) {
    constructor(
        context: Context,
        economyManager: EconomyManager = EconomyManager(),
    ) : this(
        dataStore = context.applicationContext.neonSnakePreferencesDataStore,
        economyManager = economyManager,
    )

    val progressFlow: Flow<PlayerProgress> = dataStore.data
        .catch { exception ->
            if (exception is IOException) {
                emit(emptyPreferences())
            } else {
                throw exception
            }
        }
        .map { prefs -> mapPreferencesToProgress(prefs) }

    suspend fun getSnapshot(): PlayerProgress = progressFlow.first()

    suspend fun addSparks(amount: Int) {
        val safeAmount = amount.coerceAtLeast(0)
        if (safeAmount == 0) return
        dataStore.edit { prefs ->
            val currentBalance = prefs[KEY_SPARKS_BALANCE] ?: SparksEconomy.INITIAL_STARTER_SPARKS
            val currentTotalEarned = prefs[KEY_TOTAL_SPARKS_EARNED] ?: SparksEconomy.INITIAL_STARTER_SPARKS
            prefs[KEY_SPARKS_BALANCE] = currentBalance + safeAmount
            prefs[KEY_TOTAL_SPARKS_EARNED] = currentTotalEarned + safeAmount
        }
    }

    suspend fun updateHighScore(score: Int) {
        val safeScore = score.coerceAtLeast(0)
        dataStore.edit { prefs ->
            val currentBest = prefs[KEY_BEST_SCORE] ?: 0
            if (safeScore > currentBest) {
                prefs[KEY_BEST_SCORE] = safeScore
            }
        }
    }

    suspend fun equipSkin(skinId: String) {
        dataStore.edit { prefs ->
            val unlocked = (prefs[KEY_UNLOCKED_SKINS] ?: emptySet()) + SnakeSkins.CYBER_CYAN.id
            if (skinId in unlocked) {
                prefs[KEY_EQUIPPED_SKIN] = skinId
            }
        }
    }

    suspend fun equipTrail(trailId: String) {
        dataStore.edit { prefs ->
            val unlocked = (prefs[KEY_UNLOCKED_TRAILS] ?: emptySet()) + TrailStyle.CLASSIC_GLOW.id
            if (trailId in unlocked) {
                prefs[KEY_EQUIPPED_TRAIL] = trailId
            }
        }
    }

    suspend fun equipEye(eyeId: String) {
        dataStore.edit { prefs ->
            val unlocked = (prefs[KEY_UNLOCKED_EYES] ?: emptySet()) + EyeStyle.CYBER_ROUND.id
            if (eyeId in unlocked) {
                prefs[KEY_EQUIPPED_EYE] = eyeId
            }
        }
    }

    suspend fun selectAccentPreset(presetId: String) {
        dataStore.edit { prefs ->
            prefs[KEY_ACCENT_PRESET] = UiAccentPreset.byId(presetId).id
        }
    }

    suspend fun purchaseSkin(skin: SnakeSkin): PurchaseValidationResult {
        var result: PurchaseValidationResult = PurchaseValidationResult.InvalidCost(skin.id, skin.priceSparks)
        dataStore.edit { prefs ->
            val unlocked = (prefs[KEY_UNLOCKED_SKINS] ?: emptySet()) + SnakeSkins.CYBER_CYAN.id
            val balance = prefs[KEY_SPARKS_BALANCE] ?: SparksEconomy.INITIAL_STARTER_SPARKS
            val validation = economyManager.validatePurchase(
                itemId = skin.id,
                priceSparks = skin.priceSparks,
                isAlreadyOwned = skin.id in unlocked,
                currentBalance = balance,
            )
            result = validation
            when (validation) {
                is PurchaseValidationResult.AlreadyOwned -> {
                    prefs[KEY_EQUIPPED_SKIN] = skin.id
                }
                is PurchaseValidationResult.Valid -> {
                    prefs[KEY_SPARKS_BALANCE] = validation.remainingBalance
                    prefs[KEY_UNLOCKED_SKINS] = unlocked + skin.id
                    prefs[KEY_EQUIPPED_SKIN] = skin.id
                }
                is PurchaseValidationResult.InsufficientFunds,
                is PurchaseValidationResult.InvalidCost -> Unit
            }
        }
        return result
    }

    suspend fun purchaseTrail(trail: TrailStyle): PurchaseValidationResult {
        var result: PurchaseValidationResult = PurchaseValidationResult.InvalidCost(trail.id, trail.priceSparks)
        dataStore.edit { prefs ->
            val unlocked = (prefs[KEY_UNLOCKED_TRAILS] ?: emptySet()) + TrailStyle.CLASSIC_GLOW.id
            val balance = prefs[KEY_SPARKS_BALANCE] ?: SparksEconomy.INITIAL_STARTER_SPARKS
            val validation = economyManager.validatePurchase(
                itemId = trail.id,
                priceSparks = trail.priceSparks,
                isAlreadyOwned = trail.id in unlocked,
                currentBalance = balance,
            )
            result = validation
            when (validation) {
                is PurchaseValidationResult.AlreadyOwned -> {
                    prefs[KEY_EQUIPPED_TRAIL] = trail.id
                }
                is PurchaseValidationResult.Valid -> {
                    prefs[KEY_SPARKS_BALANCE] = validation.remainingBalance
                    prefs[KEY_UNLOCKED_TRAILS] = unlocked + trail.id
                    prefs[KEY_EQUIPPED_TRAIL] = trail.id
                }
                is PurchaseValidationResult.InsufficientFunds,
                is PurchaseValidationResult.InvalidCost -> Unit
            }
        }
        return result
    }

    suspend fun purchaseEye(eye: EyeStyle): PurchaseValidationResult {
        var result: PurchaseValidationResult = PurchaseValidationResult.InvalidCost(eye.id, eye.priceSparks)
        dataStore.edit { prefs ->
            val unlocked = (prefs[KEY_UNLOCKED_EYES] ?: emptySet()) + EyeStyle.CYBER_ROUND.id
            val balance = prefs[KEY_SPARKS_BALANCE] ?: SparksEconomy.INITIAL_STARTER_SPARKS
            val validation = economyManager.validatePurchase(
                itemId = eye.id,
                priceSparks = eye.priceSparks,
                isAlreadyOwned = eye.id in unlocked,
                currentBalance = balance,
            )
            result = validation
            when (validation) {
                is PurchaseValidationResult.AlreadyOwned -> {
                    prefs[KEY_EQUIPPED_EYE] = eye.id
                }
                is PurchaseValidationResult.Valid -> {
                    prefs[KEY_SPARKS_BALANCE] = validation.remainingBalance
                    prefs[KEY_UNLOCKED_EYES] = unlocked + eye.id
                    prefs[KEY_EQUIPPED_EYE] = eye.id
                }
                is PurchaseValidationResult.InsufficientFunds,
                is PurchaseValidationResult.InvalidCost -> Unit
            }
        }
        return result
    }

    suspend fun recordMatchResult(result: GameResult, earnedSparks: Int) {
        val safeEarned = earnedSparks.coerceAtLeast(0)
        dataStore.edit { prefs ->
            val prevBalance = prefs[KEY_SPARKS_BALANCE] ?: SparksEconomy.INITIAL_STARTER_SPARKS
            val prevTotalEarned = prefs[KEY_TOTAL_SPARKS_EARNED] ?: SparksEconomy.INITIAL_STARTER_SPARKS
            val prevBest = prefs[KEY_BEST_SCORE] ?: 0
            val prevMatches = prefs[KEY_TOTAL_MATCHES] ?: 0
            val prevKills = prefs[KEY_TOTAL_ELIMINATIONS] ?: 0
            val prevFood = prefs[KEY_TOTAL_FOOD_EATEN] ?: 0
            val prevRank = prefs[KEY_BEST_RANK] ?: 0

            prefs[KEY_SPARKS_BALANCE] = prevBalance + safeEarned
            prefs[KEY_TOTAL_SPARKS_EARNED] = prevTotalEarned + safeEarned
            prefs[KEY_BEST_SCORE] = maxOf(prevBest, result.finalScore)
            prefs[KEY_TOTAL_MATCHES] = prevMatches + 1
            prefs[KEY_TOTAL_ELIMINATIONS] = prevKills + result.eliminationsCount
            prefs[KEY_TOTAL_FOOD_EATEN] = prevFood + result.foodEaten
            prefs[KEY_BEST_RANK] = if (prevRank == 0) result.bestRank else minOf(prevRank, result.bestRank)
        }
    }

    suspend fun clearAll() {
        dataStore.edit { prefs -> prefs.clear() }
    }

    private fun mapPreferencesToProgress(prefs: Preferences): PlayerProgress {
        val unlockedSkins = (prefs[KEY_UNLOCKED_SKINS] ?: emptySet()) + SnakeSkins.CYBER_CYAN.id
        val unlockedTrails = (prefs[KEY_UNLOCKED_TRAILS] ?: emptySet()) + TrailStyle.CLASSIC_GLOW.id
        val unlockedEyes = (prefs[KEY_UNLOCKED_EYES] ?: emptySet()) + EyeStyle.CYBER_ROUND.id

        val rawEquippedSkin = prefs[KEY_EQUIPPED_SKIN] ?: SnakeSkins.CYBER_CYAN.id
        val rawEquippedTrail = prefs[KEY_EQUIPPED_TRAIL] ?: TrailStyle.CLASSIC_GLOW.id
        val rawEquippedEye = prefs[KEY_EQUIPPED_EYE] ?: EyeStyle.CYBER_ROUND.id

        return PlayerProgress(
            sparksBalance = prefs[KEY_SPARKS_BALANCE] ?: SparksEconomy.INITIAL_STARTER_SPARKS,
            totalSparksEarned = prefs[KEY_TOTAL_SPARKS_EARNED] ?: SparksEconomy.INITIAL_STARTER_SPARKS,
            unlockedSkinIds = unlockedSkins,
            unlockedTrailIds = unlockedTrails,
            unlockedEyeIds = unlockedEyes,
            equippedSkinId = if (rawEquippedSkin in unlockedSkins) rawEquippedSkin else SnakeSkins.CYBER_CYAN.id,
            equippedTrailId = if (rawEquippedTrail in unlockedTrails) rawEquippedTrail else TrailStyle.CLASSIC_GLOW.id,
            equippedEyeId = if (rawEquippedEye in unlockedEyes) rawEquippedEye else EyeStyle.CYBER_ROUND.id,
            accentPresetId = prefs[KEY_ACCENT_PRESET] ?: UiAccentPreset.SYNC_WITH_SKIN.id,
            bestScore = prefs[KEY_BEST_SCORE] ?: 0,
            totalMatches = prefs[KEY_TOTAL_MATCHES] ?: 0,
            totalEliminations = prefs[KEY_TOTAL_ELIMINATIONS] ?: 0,
            totalFoodEaten = prefs[KEY_TOTAL_FOOD_EATEN] ?: 0,
            bestRank = prefs[KEY_BEST_RANK] ?: 0,
        )
    }

    private companion object {
        val KEY_SPARKS_BALANCE = intPreferencesKey("sparks_balance")
        val KEY_TOTAL_SPARKS_EARNED = intPreferencesKey("total_sparks_earned")
        val KEY_UNLOCKED_SKINS = stringSetPreferencesKey("unlocked_skins")
        val KEY_UNLOCKED_TRAILS = stringSetPreferencesKey("unlocked_trails")
        val KEY_UNLOCKED_EYES = stringSetPreferencesKey("unlocked_eyes")
        val KEY_EQUIPPED_SKIN = stringPreferencesKey("equipped_skin")
        val KEY_EQUIPPED_TRAIL = stringPreferencesKey("equipped_trail")
        val KEY_EQUIPPED_EYE = stringPreferencesKey("equipped_eye")
        val KEY_ACCENT_PRESET = stringPreferencesKey("accent_preset")
        val KEY_BEST_SCORE = intPreferencesKey("best_score")
        val KEY_TOTAL_MATCHES = intPreferencesKey("total_matches")
        val KEY_TOTAL_ELIMINATIONS = intPreferencesKey("total_eliminations")
        val KEY_TOTAL_FOOD_EATEN = intPreferencesKey("total_food_eaten")
        val KEY_BEST_RANK = intPreferencesKey("best_rank")
    }
}
