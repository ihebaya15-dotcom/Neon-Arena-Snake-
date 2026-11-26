package com.example.ui.viewmodel

import androidx.compose.runtime.mutableLongStateOf
import androidx.lifecycle.ViewModel
import com.example.game.engine.ArenaEngine
import com.example.game.engine.EngineStepOutcome
import com.example.game.model.ArenaHudSnapshot
import com.example.game.model.GamePhase
import com.example.game.model.SnakeSkin
import com.example.game.model.SnakeSkins
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

data class GameUiState(
    val phase: GamePhase = GamePhase.Lobby,
    val selectedSkin: SnakeSkin = SnakeSkins.CYBER_CYAN,
    val sessionBestScore: Int = 0,
    val hudSnapshot: ArenaHudSnapshot = ArenaHudSnapshot(),
)

class GameViewModel : ViewModel() {
    val engine: ArenaEngine = ArenaEngine()

    private val _uiState = MutableStateFlow(GameUiState())
    val uiState: StateFlow<GameUiState> = _uiState.asStateFlow()

    // Lightweight frame invalidation tick read inside Canvas draw scope to maintain 60 FPS
    // without recomposing the entire Compose hierarchy on every animation frame.
    val frameTick = mutableLongStateOf(0L)

    private var hudRefreshCounter = 0
    private var lastHudScore = 0

    fun selectSkin(skin: SnakeSkin) {
        _uiState.update { it.copy(selectedSkin = skin) }
    }

    fun startGame() {
        val currentState = _uiState.value
        engine.startNewGame(
            playerSkin = currentState.selectedSkin,
            currentBestScore = currentState.sessionBestScore,
        )
        hudRefreshCounter = 0
        lastHudScore = 0
        frameTick.longValue = 1L
        _uiState.update {
            it.copy(
                phase = GamePhase.Playing,
                hudSnapshot = engine.buildHudSnapshot(forceRefreshFoodBlips = true),
            )
        }
    }

    fun returnToLobby() {
        engine.playerController.reset()
        _uiState.update { it.copy(phase = GamePhase.Lobby) }
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
                val result = outcome.result
                frameTick.longValue += 1L
                _uiState.update { state ->
                    state.copy(
                        phase = GamePhase.GameOver(result),
                        sessionBestScore = maxOf(state.sessionBestScore, result.finalScore),
                        hudSnapshot = engine.buildHudSnapshot(),
                    )
                }
            }
        }
    }
}
