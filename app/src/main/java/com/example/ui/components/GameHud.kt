package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.EmojiEvents
import androidx.compose.material.icons.rounded.Shield
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.game.model.ArenaHudSnapshot
import com.example.game.model.BotMapBlip
import com.example.game.model.LeaderboardEntry
import com.example.ui.theme.HudBorderStroke
import com.example.ui.theme.HudGlassSurface
import com.example.ui.theme.NeonCyan
import com.example.ui.theme.NeonSnakeTheme
import com.example.ui.theme.PlasmaMagenta
import com.example.ui.theme.SolarGold
import com.example.ui.theme.SpaceObsidian
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.ToxicLime

@Composable
fun GameHud(
    snapshot: ArenaHudSnapshot,
    accentColor: Color,
    onBackToLobby: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .windowInsetsPadding(WindowInsets.safeDrawing)
            .padding(horizontal = 12.dp, vertical = 10.dp),
    ) {
        Row(
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Top,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Column(
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier
                    .weight(1f)
                    .padding(end = 10.dp),
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

                    // Compact Score, Length & Eliminations Card sized cleanly for 360dp portrait
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier
                            .clip(RoundedCornerShape(16.dp))
                            .background(HudGlassSurface)
                            .border(1.dp, HudBorderStroke, RoundedCornerShape(16.dp))
                            .padding(horizontal = 10.dp, vertical = 6.dp)
                            .testTag("hud_score_card"),
                    ) {
                        HudMetric(
                            label = stringResource(R.string.hud_score_label),
                            value = snapshot.score.toString(),
                            valueColor = accentColor,
                            tag = "hud_score_value",
                        )
                        Box(
                            modifier = Modifier
                                .width(1.dp)
                                .height(24.dp)
                                .background(HudBorderStroke),
                        )
                        HudMetric(
                            label = stringResource(R.string.hud_length_label),
                            value = snapshot.length.toString(),
                            valueColor = TextPrimary,
                            tag = "hud_length_value",
                        )
                        Box(
                            modifier = Modifier
                                .width(1.dp)
                                .height(24.dp)
                                .background(HudBorderStroke),
                        )
                        HudMetric(
                            label = stringResource(R.string.hud_kills_label),
                            value = snapshot.eliminationsCount.toString(),
                            valueColor = SolarGold,
                            tag = "hud_kills_value",
                        )
                    }
                }

                // Compact Top-3 Leaderboard Card
                if (snapshot.topThree.isNotEmpty()) {
                    LeaderboardCard(
                        topThree = snapshot.topThree,
                        playerRankEntry = snapshot.playerRankEntry,
                        accentColor = accentColor,
                    )
                }
            }

            // Top corner Mini-map with food + 20 bots
            MiniMap(
                playerX = snapshot.playerX,
                playerY = snapshot.playerY,
                playerAngleRadians = snapshot.playerAngleRadians,
                accentColor = accentColor,
                foodBlips = snapshot.foodBlips,
                botBlips = snapshot.botBlips,
                arenaWidth = snapshot.arenaWidth,
                arenaHeight = snapshot.arenaHeight,
                mapSize = 104.dp,
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Kill Banner & Spawn Shield Status
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.fillMaxWidth(),
        ) {
            AnimatedVisibility(
                visible = snapshot.spawnProtectionRemainingSec > 0f,
                enter = fadeIn(),
                exit = fadeOut(),
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier
                        .clip(RoundedCornerShape(14.dp))
                        .background(HudGlassSurface)
                        .border(1.dp, NeonCyan.copy(alpha = 0.65f), RoundedCornerShape(14.dp))
                        .padding(horizontal = 12.dp, vertical = 5.dp)
                        .testTag("spawn_shield_badge"),
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Shield,
                        contentDescription = null,
                        tint = NeonCyan,
                        modifier = Modifier.size(16.dp),
                    )
                    Text(
                        text = stringResource(R.string.hud_spawn_shield),
                        style = MaterialTheme.typography.bodyMedium,
                        color = NeonCyan,
                    )
                }
            }

            val killEvent = snapshot.latestPlayerKill
            AnimatedVisibility(
                visible = killEvent != null,
                enter = fadeIn() + slideInVertically { -it / 2 },
                exit = fadeOut() + slideOutVertically { -it / 2 },
            ) {
                if (killEvent != null) {
                    Box(
                        modifier = Modifier
                            .padding(top = 4.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .background(ToxicLime.copy(alpha = 0.22f))
                            .border(1.dp, ToxicLime.copy(alpha = 0.8f), RoundedCornerShape(14.dp))
                            .padding(horizontal = 14.dp, vertical = 5.dp)
                            .testTag("kill_feed_banner"),
                    ) {
                        Text(
                            text = stringResource(
                                R.string.hud_kill_banner,
                                killEvent.victimName,
                                killEvent.rewardPoints,
                            ),
                            style = MaterialTheme.typography.labelLarge,
                            color = ToxicLime,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun HudMetric(
    label: String,
    value: String,
    valueColor: Color,
    tag: String,
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium.copy(fontSize = 11.sp, lineHeight = 14.sp),
            color = TextSecondary,
            maxLines = 1,
            softWrap = false,
        )
        Text(
            text = value,
            style = MaterialTheme.typography.titleMedium.copy(fontSize = 16.sp, lineHeight = 20.sp),
            color = valueColor,
            maxLines = 1,
            softWrap = false,
            modifier = Modifier.testTag(tag),
        )
    }
}

@Composable
private fun LeaderboardCard(
    topThree: List<LeaderboardEntry>,
    playerRankEntry: LeaderboardEntry?,
    accentColor: Color,
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(3.dp),
        modifier = Modifier
            .widthIn(min = 156.dp, max = 190.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(HudGlassSurface)
            .border(1.dp, HudBorderStroke, RoundedCornerShape(14.dp))
            .padding(horizontal = 10.dp, vertical = 7.dp)
            .testTag("leaderboard_card"),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(5.dp),
        ) {
            Icon(
                imageVector = Icons.Rounded.EmojiEvents,
                contentDescription = null,
                tint = SolarGold,
                modifier = Modifier.size(14.dp),
            )
            Text(
                text = stringResource(R.string.hud_leaderboard_title),
                style = MaterialTheme.typography.labelSmall,
                color = TextSecondary,
            )
        }

        topThree.forEach { entry ->
            LeaderboardRow(
                entry = entry,
                highlightColor = if (entry.isPlayer) accentColor else entry.color,
            )
        }

        if (playerRankEntry != null) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(1.dp)
                    .background(HudBorderStroke),
            )
            LeaderboardRow(
                entry = playerRankEntry,
                highlightColor = accentColor,
            )
        }
    }
}

@Composable
private fun LeaderboardRow(
    entry: LeaderboardEntry,
    highlightColor: Color,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(5.dp),
            modifier = Modifier.weight(1f),
        ) {
            Text(
                text = "#${entry.rank}",
                style = MaterialTheme.typography.labelSmall,
                color = if (entry.rank == 1) SolarGold else TextSecondary,
            )
            Box(
                modifier = Modifier
                    .size(7.dp)
                    .clip(CircleShape)
                    .background(highlightColor),
            )
            Text(
                text = if (entry.isPlayer) stringResource(R.string.hud_player_label) else entry.displayName,
                style = MaterialTheme.typography.bodyMedium.copy(fontSize = 12.sp),
                color = if (entry.isPlayer) highlightColor else TextPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        Spacer(modifier = Modifier.width(6.dp))
        Text(
            text = entry.score.toString(),
            style = MaterialTheme.typography.labelSmall,
            color = if (entry.isPlayer) highlightColor else TextSecondary,
        )
    }
}

@Preview(showBackground = true, widthDp = 390, heightDp = 320, backgroundColor = 0xFF060812)
@Composable
private fun GameHudPreview() {
    NeonSnakeTheme {
        Box(modifier = Modifier.background(SpaceObsidian)) {
            GameHud(
                snapshot = ArenaHudSnapshot(
                    score = 1480,
                    length = 42,
                    eliminationsCount = 3,
                    topThree = listOf(
                        LeaderboardEntry("1", "APEX-9", 2150, 1, PlasmaMagenta, false),
                        LeaderboardEntry("2", "أنت", 1480, 2, NeonCyan, true),
                        LeaderboardEntry("3", "VIPER-X", 1120, 3, ToxicLime, false),
                    ),
                    botBlips = listOf(
                        BotMapBlip(0.25f, 0.30f, PlasmaMagenta, true),
                        BotMapBlip(0.75f, 0.65f, ToxicLime, false),
                    ),
                ),
                accentColor = NeonCyan,
                onBackToLobby = {},
            )
        }
    }
}
