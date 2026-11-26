package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.game.model.ArenaHudSnapshot
import com.example.ui.theme.HudBorderStroke
import com.example.ui.theme.HudGlassSurface
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary

@Composable
fun GameHud(
    snapshot: ArenaHudSnapshot,
    accentColor: Color,
    onBackToLobby: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.Top,
        modifier = modifier
            .fillMaxWidth()
            .windowInsetsPadding(WindowInsets.safeDrawing)
            .padding(horizontal = 16.dp, vertical = 12.dp),
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                IconButton(
                    onClick = onBackToLobby,
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(HudGlassSurface)
                        .border(1.dp, HudBorderStroke, CircleShape)
                        .testTag("exit_to_lobby_button"),
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                        contentDescription = stringResource(R.string.cd_back_to_lobby),
                        tint = TextPrimary,
                    )
                }

                // Score & Length Card
                Column(
                    modifier = Modifier
                        .clip(RoundedCornerShape(16.dp))
                        .background(HudGlassSurface)
                        .border(1.dp, HudBorderStroke, RoundedCornerShape(16.dp))
                        .padding(horizontal = 14.dp, vertical = 8.dp)
                        .testTag("hud_score_card"),
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        Column {
                            Text(
                                text = stringResource(R.string.hud_score_label),
                                style = MaterialTheme.typography.bodyMedium,
                                color = TextSecondary,
                            )
                            Text(
                                text = snapshot.score.toString(),
                                style = MaterialTheme.typography.titleLarge,
                                color = accentColor,
                                modifier = Modifier.testTag("hud_score_value"),
                            )
                        }

                        Box(
                            modifier = Modifier
                                .width(1.dp)
                                .height(28.dp)
                                .background(HudBorderStroke),
                        )

                        Column {
                            Text(
                                text = stringResource(R.string.hud_length_label),
                                style = MaterialTheme.typography.bodyMedium,
                                color = TextSecondary,
                            )
                            Text(
                                text = snapshot.length.toString(),
                                style = MaterialTheme.typography.titleLarge,
                                color = TextPrimary,
                                modifier = Modifier.testTag("hud_length_value"),
                            )
                        }
                    }
                }
            }
        }

        // Top corner Mini-map relative to 3000x3000 arena boundaries
        MiniMap(
            playerX = snapshot.playerX,
            playerY = snapshot.playerY,
            playerAngleRadians = snapshot.playerAngleRadians,
            accentColor = accentColor,
            foodBlips = snapshot.foodBlips,
            arenaWidth = snapshot.arenaWidth,
            arenaHeight = snapshot.arenaHeight,
        )
    }
}
