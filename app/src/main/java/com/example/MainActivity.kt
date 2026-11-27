package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.data.PersistenceManager
import com.example.game.economy.EconomyManager
import com.example.game.model.GamePhase
import com.example.game.model.HubSection
import com.example.game.model.PlayerProgress
import com.example.game.model.StoreCategory
import com.example.ui.components.DifficultySelectionOverlay
import com.example.ui.screens.GameScreen
import com.example.ui.screens.NeonHubScreen
import com.example.ui.theme.NeonSnakeTheme
import com.example.ui.viewmodel.GameViewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(android.graphics.Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(android.graphics.Color.TRANSPARENT),
        )
        setContent {
            val gameViewModel: GameViewModel = viewModel(
                factory = viewModelFactory {
                    initializer {
                        val app = checkNotNull(this[APPLICATION_KEY]) {
                            "APPLICATION_KEY missing from CreationExtras"
                        }
                        val economyManager = EconomyManager()
                        GameViewModel(
                            persistenceManager = PersistenceManager(app, economyManager),
                            economyManager = economyManager,
                        )
                    }
                },
            )
            val uiState by gameViewModel.uiState.collectAsStateWithLifecycle()

            NeonSnakeTheme(primaryAccent = uiState.coordinatedAccentColor) {
                Crossfade(
                    targetState = uiState.phase is GamePhase.Lobby,
                    label = "root_screen_transition",
                    modifier = Modifier.fillMaxSize(),
                ) { isLobby ->
                    if (isLobby) {
                        Box(modifier = Modifier.fillMaxSize()) {
                            NeonHubScreen(
                                activeSection = uiState.activeHubSection,
                                activeStoreCategory = uiState.activeStoreCategory,
                                progress = uiState.progress,
                                storeValidationFeedback = uiState.storeValidationFeedback,
                                onSelectSection = gameViewModel::selectHubSection,
                                onSelectStoreCategory = gameViewModel::selectStoreCategory,
                                onSelectSkin = gameViewModel::selectSkin,
                                onSelectTrail = gameViewModel::selectTrail,
                                onSelectEye = gameViewModel::selectEye,
                                onSelectAccentPreset = gameViewModel::selectAccentPreset,
                                onPurchaseOrEquipSkin = gameViewModel::purchaseOrEquipSkin,
                                onPurchaseOrEquipTrail = gameViewModel::purchaseOrEquipTrail,
                                onPurchaseOrEquipEye = gameViewModel::purchaseOrEquipEye,
                                onDismissStoreValidationFeedback = gameViewModel::dismissStoreValidationFeedback,
                                onStartGame = gameViewModel::openDifficultySelector,
                            )
                            AnimatedVisibility(
                                visible = uiState.isDifficultyOverlayVisible,
                                enter = fadeIn() + scaleIn(initialScale = 0.94f),
                                exit = fadeOut(),
                            ) {
                                DifficultySelectionOverlay(
                                    selectedDifficulty = uiState.selectedDifficulty,
                                    onSelectDifficulty = gameViewModel::selectDifficulty,
                                    onConfirmStart = gameViewModel::confirmDifficultyAndStart,
                                    onDismiss = gameViewModel::dismissDifficultySelector,
                                )
                            }
                        }
                    } else {
                        GameScreen(
                            viewModel = gameViewModel,
                            uiState = uiState,
                        )
                    }
                }
            }
        }
    }
}

@Preview(showBackground = true, widthDp = 390, heightDp = 844)
@Composable
fun NeonSnakeAppPreview() {
    NeonSnakeTheme {
        NeonHubScreen(
            activeSection = HubSection.PLAY,
            activeStoreCategory = StoreCategory.SKINS,
            progress = PlayerProgress(bestScore = 1250, sparksBalance = 240),
            storeValidationFeedback = null,
            onSelectSection = {},
            onSelectStoreCategory = {},
            onSelectSkin = {},
            onSelectTrail = {},
            onSelectEye = {},
            onSelectAccentPreset = {},
            onPurchaseOrEquipSkin = {},
            onPurchaseOrEquipTrail = {},
            onPurchaseOrEquipEye = {},
            onDismissStoreValidationFeedback = {},
            onStartGame = {},
        )
    }
}
