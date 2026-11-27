package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Bolt
import androidx.compose.material.icons.rounded.Palette
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.AbsoluteAlignment
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.game.model.DeathReason
import com.example.game.model.GamePhase
import com.example.game.model.GameResult
import com.example.ui.components.ArenaCanvas
import com.example.ui.components.BoostToggleButton
import com.example.ui.components.GameHud
import com.example.ui.components.VirtualJoystick
import com.example.ui.theme.DangerCrimson
import com.example.ui.theme.HudBorderStroke
import com.example.ui.theme.HudGlassSurface
import com.example.ui.theme.NeonCyan
import com.example.ui.theme.NeonSnakeTheme
import com.example.ui.theme.SolarGold
import com.example.ui.theme.SpaceObsidian
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.viewmodel.GameUiState
import com.example.ui.viewmodel.GameViewModel

@Composable
fun GameScreen(
    viewModel: GameViewModel,
    uiState: GameUiState,
    modifier: Modifier = Modifier,
) {
    BackHandler {
        viewModel.returnToLobby()
    }

    // 60 FPS frame-synchronized simulation loop while playing
    LaunchedEffect(uiState.phase) {
        if (uiState.phase == GamePhase.Playing) {
            var lastFrameNanos = 0L
            while (true) {
                withFrameNanos { frameNanos ->
                    if (lastFrameNanos != 0L) {
                        val dtSeconds = (frameNanos - lastFrameNanos) / 1_000_000_000f
                        viewModel.stepSimulation(dtSeconds)
                    }
                    lastFrameNanos = frameNanos
                }
            }
        }
    }

    val accentColor = uiState.coordinatedAccentColor

    Box(modifier = modifier.fillMaxSize()) {
        // World Arena & Snake Canvas
        ArenaCanvas(
            engine = viewModel.engine,
            frameTick = viewModel.frameTick.longValue,
        )

        if (uiState.phase == GamePhase.Playing) {
            // Full-screen Virtual Joystick touch surface
            VirtualJoystick(
                accentColor = accentColor,
                onVectorChanged = viewModel::onJoystickVectorChanged,
            )

            // Top HUD (Score, Length, Mini-map, Exit)
            GameHud(
                snapshot = uiState.hudSnapshot,
                accentColor = accentColor,
                onBackToLobby = viewModel::returnToLobby,
                modifier = Modifier.align(Alignment.TopCenter),
            )

            // Ergonomic thumb Boost Toggle button in bottom-left safe area
            BoostToggleButton(
                isBoostActive = uiState.hudSnapshot.isBoostActive,
                canBoost = uiState.hudSnapshot.canBoost,
                showMinLengthWarning = uiState.hudSnapshot.boostBlockedByMinLength,
                accentColor = accentColor,
                onToggleBoost = viewModel::onToggleBoost,
                modifier = Modifier
                    .align(AbsoluteAlignment.BottomLeft)
                    .navigationBarsPadding()
                    .padding(horizontal = 22.dp, vertical = 28.dp),
            )
        }

        val gameOverState = uiState.phase as? GamePhase.GameOver
        AnimatedVisibility(
            visible = gameOverState != null,
            enter = fadeIn() + scaleIn(initialScale = 0.92f),
            exit = fadeOut(),
        ) {
            if (gameOverState != null) {
                GameOverOverlay(
                    result = gameOverState.result,
                    accentColor = accentColor,
                    onReplay = viewModel::startGame,
                    onChangeSkin = viewModel::returnToLobby,
                )
            }
        }
    }
}

@Composable
private fun GameOverOverlay(
    result: GameResult,
    accentColor: Color,
    onReplay: () -> Unit,
    onChangeSkin: () -> Unit,
) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .fillMaxSize()
            .background(SpaceObsidian.copy(alpha = 0.78f))
            .windowInsetsPadding(WindowInsets.safeDrawing)
            .padding(24.dp),
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .widthIn(max = 420.dp)
                .fillMaxWidth()
                .clip(RoundedCornerShape(28.dp))
                .background(HudGlassSurface)
                .border(1.5.dp, DangerCrimson.copy(alpha = 0.65f), RoundedCornerShape(28.dp))
                .padding(24.dp)
                .testTag("game_over_card"),
        ) {
            Text(
                text = stringResource(R.string.game_over_title),
                style = MaterialTheme.typography.headlineMedium,
                color = DangerCrimson,
                textAlign = TextAlign.Center,
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = when (result.deathReason) {
                    com.example.game.model.DeathReason.ARENA_BOUNDARY ->
                        stringResource(R.string.game_over_reason_wall)
                    com.example.game.model.DeathReason.ENEMY_BODY_COLLISION ->
                        stringResource(R.string.game_over_reason_enemy, result.killerName.orEmpty())
                    com.example.game.model.DeathReason.HEAD_ON_COLLISION ->
                        stringResource(R.string.game_over_reason_head_on, result.killerName.orEmpty())
                },
                style = MaterialTheme.typography.bodyMedium,
                color = TextSecondary,
                textAlign = TextAlign.Center,
            )

            Spacer(modifier = Modifier.height(20.dp))

            // Stats 2x2 Grid
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxWidth(),
            ) {
                StatTile(
                    label = stringResource(R.string.game_over_final_score),
                    value = result.finalScore.toString(),
                    valueColor = if (result.isNewBest) SolarGold else accentColor,
                    modifier = Modifier.weight(1f),
                )
                StatTile(
                    label = stringResource(R.string.game_over_peak_length),
                    value = result.peakLength.toString(),
                    valueColor = TextPrimary,
                    modifier = Modifier.weight(1f),
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxWidth(),
            ) {
                StatTile(
                    label = stringResource(R.string.game_over_eliminations),
                    value = result.eliminationsCount.toString(),
                    valueColor = SolarGold,
                    modifier = Modifier.weight(1f),
                )
                StatTile(
                    label = stringResource(R.string.game_over_best_rank),
                    value = "#${result.bestRank}",
                    valueColor = accentColor,
                    modifier = Modifier.weight(1f),
                )
            }

            if (result.earnedSparks > 0) {
                Spacer(modifier = Modifier.height(14.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier
                        .clip(RoundedCornerShape(16.dp))
                        .background(SolarGold.copy(alpha = 0.16f))
                        .border(1.dp, SolarGold.copy(alpha = 0.55f), RoundedCornerShape(16.dp))
                        .padding(horizontal = 14.dp, vertical = 7.dp)
                        .testTag("game_over_sparks_pill"),
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Bolt,
                        contentDescription = null,
                        tint = SolarGold,
                        modifier = Modifier.size(18.dp),
                    )
                    Text(
                        text = stringResource(R.string.game_over_sparks_earned, result.earnedSparks),
                        style = MaterialTheme.typography.labelLarge,
                        color = SolarGold,
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            Button(
                onClick = onReplay,
                shape = RoundedCornerShape(18.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = accentColor,
                    contentColor = SpaceObsidian,
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp)
                    .testTag("replay_button"),
            ) {
                Icon(
                    imageVector = Icons.Rounded.Refresh,
                    contentDescription = null,
                    modifier = Modifier.size(22.dp),
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = stringResource(R.string.game_over_replay),
                    style = MaterialTheme.typography.titleMedium,
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            OutlinedButton(
                onClick = onChangeSkin,
                shape = RoundedCornerShape(18.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
                    .testTag("change_skin_button"),
            ) {
                Icon(
                    imageVector = Icons.Rounded.Palette,
                    contentDescription = null,
                    tint = TextPrimary,
                    modifier = Modifier.size(20.dp),
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = stringResource(R.string.game_over_change_skin),
                    style = MaterialTheme.typography.labelLarge,
                    color = TextPrimary,
                )
            }
        }
    }
}

@Composable
private fun StatTile(
    label: String,
    value: String,
    valueColor: Color,
    modifier: Modifier = Modifier,
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .background(SpaceObsidian.copy(alpha = 0.65f))
            .border(1.dp, HudBorderStroke, RoundedCornerShape(16.dp))
            .padding(vertical = 12.dp, horizontal = 8.dp),
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = TextSecondary,
            textAlign = TextAlign.Center,
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = value,
            style = MaterialTheme.typography.titleLarge,
            color = valueColor,
            textAlign = TextAlign.Center,
        )
    }
}

@Preview(showBackground = true, widthDp = 390, heightDp = 844)
@Composable
private fun GameScreenPlayingPreview() {
    NeonSnakeTheme {
        val previewEngine = androidx.compose.runtime.remember {
            com.example.game.engine.ArenaEngine().apply {
                startNewGame(com.example.game.model.SnakeSkins.CYBER_CYAN, currentBestScore = 1850)
            }
        }
        val snapshot = androidx.compose.runtime.remember {
            previewEngine.buildHudSnapshot(forceRefreshFoodBlips = true)
        }
        Box(modifier = Modifier.fillMaxSize()) {
            ArenaCanvas(engine = previewEngine, frameTick = 1L)
            GameHud(
                snapshot = snapshot,
                accentColor = NeonCyan,
                onBackToLobby = {},
                modifier = Modifier.align(Alignment.TopCenter),
            )
            BoostToggleButton(
                isBoostActive = false,
                canBoost = true,
                showMinLengthWarning = false,
                accentColor = NeonCyan,
                onToggleBoost = {},
                modifier = Modifier
                    .align(AbsoluteAlignment.BottomLeft)
                    .navigationBarsPadding()
                    .padding(horizontal = 22.dp, vertical = 28.dp),
            )
        }
    }
}

@Preview(showBackground = true, widthDp = 390, heightDp = 844)
@Composable
private fun GameOverOverlayPreview() {
    NeonSnakeTheme {
        GameOverOverlay(
            result = GameResult(
                finalScore = 2460,
                peakLength = 64,
                survivedSeconds = 95,
                foodEaten = 48,
                eliminationsCount = 4,
                bestRank = 1,
                isNewBest = true,
                deathReason = DeathReason.ENEMY_BODY_COLLISION,
                killerName = "APEX-9",
            ),
            accentColor = NeonCyan,
            onReplay = {},
            onChangeSkin = {},
        )
    }
}

