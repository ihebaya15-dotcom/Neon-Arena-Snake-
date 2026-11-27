package com.example.ui.viewmodel

import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.ui.graphics.Color
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.PersistenceManager
import com.example.game.economy.EconomyManager
import com.example.game.economy.PurchaseValidationResult
import com.example.game.engine.ArenaEngine
import com.example.game.engine.EngineStepOutcome
import com.example.game.model.ArenaHudSnapshot
import com.example.game.model.BotDifficulty
import com.example.game.model.EyeStyle
import com.example.game.model.GamePhase
import com.example.game.model.HubSection
import com.example.game.model.PlayerProgress
import com.example.game.model.SnakeSkin
import com.example.game.model.StoreCategory
import com.example.game.model.TrailStyle
import com.example.game.model.UiAccentPreset
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class StoreValidationFeedback(
    val itemId: String,
    val requiredSparks: Int,
    val currentBalance: Int,
    val missingSparks: Int,
)

data class GameUiState(
    val phase: GamePhase = GamePhase.Lobby,
    val activeHubSection: HubSection = HubSection.PLAY,
    val activeStoreCategory: StoreCategory = StoreCategory.SKINS,
    val progress: PlayerProgress = PlayerProgress(),
    val selectedDifficulty: BotDifficulty = BotDifficulty.MEDIUM,
    val isDifficultyOverlayVisible: Boolean = false,
    val storeValidationFeedback: StoreValidationFeedback? = null,
    val hudSnapshot: ArenaHudSnapshot = ArenaHudSnapshot(),
) {
    val selectedSkin: SnakeSkin get() = progress.equippedSkin
    val selectedTrail: TrailStyle get() = progress.equippedTrail
    val selectedEye: EyeStyle get() = progress.equippedEye
    val coordinatedAccentColor: Color get() = progress.coordinatedAccentColor
    val sessionBestScore: Int get() = progress.bestScore
}

class GameViewModel(
    private val persistenceManager: PersistenceManager? = null,
    val economyManager: EconomyManager = EconomyManager(),
) : ViewModel() {
    val engine: ArenaEngine = ArenaEngine()

    private val _uiState = MutableStateFlow(GameUiState())
    val uiState: StateFlow<GameUiState> = _uiState.asStateFlow()

    // Lightweight frame invalidation tick read inside Canvas draw scope to maintain 60 FPS
    // without recomposing the entire Compose hierarchy on every animation frame.
    val frameTick = mutableLongStateOf(0L)

    private var hudRefreshCounter = 0
    private var lastHudScore = 0

    init {
        if (persistenceManager != null) {
            viewModelScope.launch {
                persistenceManager.progressFlow.collect { savedProgress ->
                    _uiState.update { it.copy(progress = savedProgress) }
                }
            }
        }
    }

    fun selectHubSection(section: HubSection) {
        _uiState.update {
            it.copy(
                activeHubSection = section,
                storeValidationFeedback = null,
            )
        }
    }

    fun selectStoreCategory(category: StoreCategory) {
        _uiState.update {
            it.copy(
                activeStoreCategory = category,
                storeValidationFeedback = null,
            )
        }
    }

    fun dismissStoreValidationFeedback() {
        _uiState.update { it.copy(storeValidationFeedback = null) }
    }

    fun selectSkin(skin: SnakeSkin) {
        val current = _uiState.value.progress
        if (skin.id !in current.unlockedSkinIds) return
        _uiState.update { state ->
            state.copy(
                progress = state.progress.copy(equippedSkinId = skin.id),
                storeValidationFeedback = null,
            )
        }
        persistenceManager?.let { pm ->
            viewModelScope.launch { pm.equipSkin(skin.id) }
        }
    }

    fun selectTrail(trail: TrailStyle) {
        val current = _uiState.value.progress
        if (trail.id !in current.unlockedTrailIds) return
        _uiState.update { state ->
            state.copy(
                progress = state.progress.copy(equippedTrailId = trail.id),
                storeValidationFeedback = null,
            )
        }
        persistenceManager?.let { pm ->
            viewModelScope.launch { pm.equipTrail(trail.id) }
        }
    }

    fun selectEye(eye: EyeStyle) {
        val current = _uiState.value.progress
        if (eye.id !in current.unlockedEyeIds) return
        _uiState.update { state ->
            state.copy(
                progress = state.progress.copy(equippedEyeId = eye.id),
                storeValidationFeedback = null,
            )
        }
        persistenceManager?.let { pm ->
            viewModelScope.launch { pm.equipEye(eye.id) }
        }
    }

    fun selectAccentPreset(preset: UiAccentPreset) {
        _uiState.update { state ->
            state.copy(progress = state.progress.copy(accentPresetId = preset.id))
        }
        persistenceManager?.let { pm ->
            viewModelScope.launch { pm.selectAccentPreset(preset.id) }
        }
    }

    fun purchaseOrEquipSkin(skin: SnakeSkin) {
        when (val validation = economyManager.validateSkinPurchase(skin, _uiState.value.progress)) {
            is PurchaseValidationResult.AlreadyOwned -> selectSkin(skin)
            is PurchaseValidationResult.InsufficientFunds -> emitInsufficientFunds(validation)
            is PurchaseValidationResult.InvalidCost -> Unit
            is PurchaseValidationResult.Valid -> {
                if (persistenceManager != null) {
                    viewModelScope.launch {
                        handleRepositoryValidation(persistenceManager.purchaseSkin(skin))
                    }
                } else {
                    _uiState.update { state ->
                        val p = state.progress
                        state.copy(
                            storeValidationFeedback = null,
                            progress = p.copy(
                                sparksBalance = validation.remainingBalance,
                                unlockedSkinIds = p.unlockedSkinIds + skin.id,
                                equippedSkinId = skin.id,
                            ),
                        )
                    }
                }
            }
        }
    }

    fun purchaseOrEquipTrail(trail: TrailStyle) {
        when (val validation = economyManager.validateTrailPurchase(trail, _uiState.value.progress)) {
            is PurchaseValidationResult.AlreadyOwned -> selectTrail(trail)
            is PurchaseValidationResult.InsufficientFunds -> emitInsufficientFunds(validation)
            is PurchaseValidationResult.InvalidCost -> Unit
            is PurchaseValidationResult.Valid -> {
                if (persistenceManager != null) {
                    viewModelScope.launch {
                        handleRepositoryValidation(persistenceManager.purchaseTrail(trail))
                    }
                } else {
                    _uiState.update { state ->
                        val p = state.progress
                        state.copy(
                            storeValidationFeedback = null,
                            progress = p.copy(
                                sparksBalance = validation.remainingBalance,
                                unlockedTrailIds = p.unlockedTrailIds + trail.id,
                                equippedTrailId = trail.id,
                            ),
                        )
                    }
                }
            }
        }
    }

    fun purchaseOrEquipEye(eye: EyeStyle) {
        when (val validation = economyManager.validateEyePurchase(eye, _uiState.value.progress)) {
            is PurchaseValidationResult.AlreadyOwned -> selectEye(eye)
            is PurchaseValidationResult.InsufficientFunds -> emitInsufficientFunds(validation)
            is PurchaseValidationResult.InvalidCost -> Unit
            is PurchaseValidationResult.Valid -> {
                if (persistenceManager != null) {
                    viewModelScope.launch {
                        handleRepositoryValidation(persistenceManager.purchaseEye(eye))
                    }
                } else {
                    _uiState.update { state ->
                        val p = state.progress
                        state.copy(
                            storeValidationFeedback = null,
                            progress = p.copy(
                                sparksBalance = validation.remainingBalance,
                                unlockedEyeIds = p.unlockedEyeIds + eye.id,
                                equippedEyeId = eye.id,
                            ),
                        )
                    }
                }
            }
        }
    }

    private fun handleRepositoryValidation(result: PurchaseValidationResult) {
        when (result) {
            is PurchaseValidationResult.InsufficientFunds -> emitInsufficientFunds(result)
            else -> _uiState.update { it.copy(storeValidationFeedback = null) }
        }
    }

    private fun emitInsufficientFunds(result: PurchaseValidationResult.InsufficientFunds) {
        _uiState.update {
            it.copy(
                storeValidationFeedback = StoreValidationFeedback(
                    itemId = result.itemId,
                    requiredSparks = result.requiredSparks,
                    currentBalance = result.currentBalance,
                    missingSparks = result.missingSparks,
                ),
            )
        }
    }

    fun openDifficultySelector() {
        _uiState.update { it.copy(isDifficultyOverlayVisible = true) }
    }

    fun dismissDifficultySelector() {
        _uiState.update { it.copy(isDifficultyOverlayVisible = false) }
    }

    fun selectDifficulty(difficulty: BotDifficulty) {
        _uiState.update { it.copy(selectedDifficulty = difficulty) }
    }

    fun confirmDifficultyAndStart(difficulty: BotDifficulty) {
        _uiState.update {
            it.copy(
                selectedDifficulty = difficulty,
                isDifficultyOverlayVisible = false,
            )
        }
        startGame(difficulty)
    }

    fun startGame(difficulty: BotDifficulty = _uiState.value.selectedDifficulty) {
        val currentState = _uiState.value
        engine.startNewGame(
            playerSkin = currentState.selectedSkin,
            currentBestScore = currentState.sessionBestScore,
            difficulty = difficulty,
            playerTrail = currentState.selectedTrail,
            playerEye = currentState.selectedEye,
        )
        hudRefreshCounter = 0
        lastHudScore = 0
        frameTick.longValue = 1L
        _uiState.update {
            it.copy(
                phase = GamePhase.Playing,
                selectedDifficulty = difficulty,
                isDifficultyOverlayVisible = false,
                storeValidationFeedback = null,
                hudSnapshot = engine.buildHudSnapshot(forceRefreshFoodBlips = true),
            )
        }
    }

    fun returnToLobby() {
        engine.playerController.reset()
        _uiState.update {
            it.copy(
                phase = GamePhase.Lobby,
                isDifficultyOverlayVisible = false,
            )
        }
    }

    fun onJoystickVectorChanged(vectorX: Float, vectorY: Float) {
        engine.playerController.updateJoystickVector(vectorX, vectorY)
    }

    fun onToggleBoost() {
        if (_uiState.value.phase != GamePhase.Playing) return
        engine.togglePlayerBoost()
        _uiState.update { it.copy(hudSnapshot = engine.buildHudSnapshot()) }
    }

    fun stepSimulation(dtSeconds: Float) {
        if (_uiState.value.phase != GamePhase.Playing) return

        when (val outcome = engine.step(dtSeconds)) {
            EngineStepOutcome.Running -> {
                frameTick.longValue += 1L
                val currentScore = engine.playerSnake?.score ?: 0
                hudRefreshCounter++
                if (hudRefreshCounter >= 6 || currentScore != lastHudScore) {
                    hudRefreshCounter = 0
                    lastHudScore = currentScore
                    _uiState.update { it.copy(hudSnapshot = engine.buildHudSnapshot()) }
                }
            }

            is EngineStepOutcome.Ended -> {
                val baseResult = outcome.result
                val earnedSparks = economyManager.calculateMatchSparks(
                    result = baseResult,
                    difficulty = engine.currentDifficulty,
                )
                val result = baseResult.copy(earnedSparks = earnedSparks)
                frameTick.longValue += 1L

                if (persistenceManager != null) {
                    viewModelScope.launch {
                        persistenceManager.recordMatchResult(result, earnedSparks)
                    }
                    _uiState.update { state ->
                        state.copy(
                            phase = GamePhase.GameOver(result),
                            hudSnapshot = engine.buildHudSnapshot(),
                        )
                    }
                } else {
                    _uiState.update { state ->
                        val p = state.progress
                        val nextBestRank = if (p.bestRank == 0) result.bestRank else minOf(p.bestRank, result.bestRank)
                        state.copy(
                            phase = GamePhase.GameOver(result),
                            progress = p.copy(
                                sparksBalance = p.sparksBalance + earnedSparks,
                                totalSparksEarned = p.totalSparksEarned + earnedSparks,
                                bestScore = maxOf(p.bestScore, result.finalScore),
                                totalMatches = p.totalMatches + 1,
                                totalEliminations = p.totalEliminations + result.eliminationsCount,
                                totalFoodEaten = p.totalFoodEaten + result.foodEaten,
                                bestRank = nextBestRank,
                            ),
                            hudSnapshot = engine.buildHudSnapshot(),
                        )
                    }
                }
            }
        }
    }
}
