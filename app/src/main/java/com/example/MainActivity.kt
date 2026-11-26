package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.Crossfade
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.game.model.GamePhase
import com.example.ui.screens.GameScreen
import com.example.ui.screens.LobbyScreen
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
            NeonSnakeTheme {
                val gameViewModel: GameViewModel = viewModel()
                val uiState by gameViewModel.uiState.collectAsStateWithLifecycle()

                Crossfade(
                    targetState = uiState.phase is GamePhase.Lobby,
                    label = "root_screen_transition",
                    modifier = Modifier.fillMaxSize(),
                ) { isLobby ->
                    if (isLobby) {
                        LobbyScreen(
                            selectedSkin = uiState.selectedSkin,
                            sessionBestScore = uiState.sessionBestScore,
                            onSelectSkin = gameViewModel::selectSkin,
                            onStartGame = gameViewModel::startGame,
                        )
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
