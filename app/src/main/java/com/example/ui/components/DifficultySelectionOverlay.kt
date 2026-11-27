package com.example.ui.components

import androidx.activity.compose.BackHandler
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Bolt
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Shield
import androidx.compose.material.icons.rounded.Whatshot
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.game.model.BotDifficulty
import com.example.ui.theme.ArenaDeepNavy
import com.example.ui.theme.DangerCrimson
import com.example.ui.theme.HudBorderStroke
import com.example.ui.theme.HudGlassSurface
import com.example.ui.theme.NeonCyan
import com.example.ui.theme.NeonSnakeTheme
import com.example.ui.theme.SpaceObsidian
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.ToxicLime

@Composable
fun DifficultySelectionOverlay(
    selectedDifficulty: BotDifficulty,
    onSelectDifficulty: (BotDifficulty) -> Unit,
    onConfirmStart: (BotDifficulty) -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    BackHandler(onBack = onDismiss)

    val selectedAccent by animateColorAsState(
        targetValue = difficultyColor(selectedDifficulty),
        label = "difficulty_overlay_accent",
    )

    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .fillMaxSize()
            .background(SpaceObsidian.copy(alpha = 0.84f))
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onDismiss,
            )
            .windowInsetsPadding(WindowInsets.safeDrawing)
            .padding(horizontal = 20.dp, vertical = 16.dp)
            .testTag("difficulty_selection_overlay"),
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .widthIn(max = 460.dp)
                .fillMaxWidth()
                .clip(RoundedCornerShape(28.dp))
                .background(
                    brush = Brush.verticalGradient(
                        colors = listOf(ArenaDeepNavy.copy(alpha = 0.96f), HudGlassSurface),
                    ),
                )
                .border(1.5.dp, selectedAccent.copy(alpha = 0.55f), RoundedCornerShape(28.dp))
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = {},
                )
                .verticalScroll(rememberScrollState())
                .padding(20.dp),
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(
                    text = stringResource(R.string.difficulty_overlay_title),
                    style = MaterialTheme.typography.headlineMedium,
                    color = TextPrimary,
                    modifier = Modifier.weight(1f),
                )
                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(SpaceObsidian.copy(alpha = 0.55f))
                        .border(1.dp, HudBorderStroke, CircleShape)
                        .testTag("close_difficulty_overlay_button"),
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Close,
                        contentDescription = stringResource(R.string.cd_close_overlay),
                        tint = TextSecondary,
                    )
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = stringResource(R.string.difficulty_overlay_subtitle),
                style = MaterialTheme.typography.bodyMedium,
                color = TextSecondary,
                textAlign = TextAlign.Start,
                modifier = Modifier.fillMaxWidth(),
            )

            Spacer(modifier = Modifier.height(16.dp))

            DifficultyOptionCard(
                difficulty = BotDifficulty.EASY,
                title = stringResource(R.string.difficulty_easy_title),
                description = stringResource(R.string.difficulty_easy_desc),
                icon = Icons.Rounded.Shield,
                accentColor = ToxicLime,
                levelTier = 1,
                isSelected = selectedDifficulty == BotDifficulty.EASY,
                onClick = { onSelectDifficulty(BotDifficulty.EASY) },
                testTagName = "difficulty_option_easy",
            )

            Spacer(modifier = Modifier.height(10.dp))

            DifficultyOptionCard(
                difficulty = BotDifficulty.MEDIUM,
                title = stringResource(R.string.difficulty_medium_title),
                description = stringResource(R.string.difficulty_medium_desc),
                icon = Icons.Rounded.Bolt,
                accentColor = NeonCyan,
                levelTier = 2,
                isSelected = selectedDifficulty == BotDifficulty.MEDIUM,
                onClick = { onSelectDifficulty(BotDifficulty.MEDIUM) },
                testTagName = "difficulty_option_medium",
            )

            Spacer(modifier = Modifier.height(10.dp))

            DifficultyOptionCard(
                difficulty = BotDifficulty.HARD,
                title = stringResource(R.string.difficulty_hard_title),
                description = stringResource(R.string.difficulty_hard_desc),
                icon = Icons.Rounded.Whatshot,
                accentColor = DangerCrimson,
                levelTier = 3,
                isSelected = selectedDifficulty == BotDifficulty.HARD,
                onClick = { onSelectDifficulty(BotDifficulty.HARD) },
                testTagName = "difficulty_option_hard",
            )

            Spacer(modifier = Modifier.height(18.dp))

            Button(
                onClick = { onConfirmStart(selectedDifficulty) },
                shape = RoundedCornerShape(18.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = selectedAccent,
                    contentColor = SpaceObsidian,
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp)
                    .testTag("confirm_difficulty_button"),
            ) {
                Icon(
                    imageVector = Icons.Rounded.PlayArrow,
                    contentDescription = null,
                    modifier = Modifier.size(24.dp),
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = stringResource(R.string.difficulty_confirm_play),
                    style = MaterialTheme.typography.titleMedium,
                )
            }
        }
    }
}

@Composable
private fun DifficultyOptionCard(
    difficulty: BotDifficulty,
    title: String,
    description: String,
    icon: ImageVector,
    accentColor: Color,
    levelTier: Int,
    isSelected: Boolean,
    onClick: () -> Unit,
    testTagName: String,
) {
    val scale by animateFloatAsState(
        targetValue = if (isSelected) 1.015f else 1.0f,
        animationSpec = spring(),
        label = "difficulty_card_scale",
    )
    val bgColor by animateColorAsState(
        targetValue = if (isSelected) accentColor.copy(alpha = 0.14f) else SpaceObsidian.copy(alpha = 0.60f),
        label = "difficulty_card_bg",
    )
    val borderColor by animateColorAsState(
        targetValue = if (isSelected) accentColor else HudBorderStroke,
        label = "difficulty_card_border",
    )

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .scale(scale)
            .minimumInteractiveComponentSize()
            .clip(RoundedCornerShape(18.dp))
            .background(bgColor)
            .border(width = if (isSelected) 2.dp else 1.dp, color = borderColor, shape = RoundedCornerShape(18.dp))
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = ripple(bounded = true, color = accentColor),
                onClick = onClick,
            )
            .padding(horizontal = 14.dp, vertical = 12.dp)
            .testTag(testTagName),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.weight(1f),
            ) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(38.dp)
                        .clip(CircleShape)
                        .background(accentColor.copy(alpha = 0.18f))
                        .border(1.dp, accentColor.copy(alpha = 0.55f), CircleShape),
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = accentColor,
                        modifier = Modifier.size(20.dp),
                    )
                }
                Column {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleMedium,
                        color = if (isSelected) accentColor else TextPrimary,
                    )
                    Text(
                        text = description,
                        style = MaterialTheme.typography.bodyMedium.copy(fontSize = 12.sp, lineHeight = 16.sp),
                        color = TextSecondary,
                    )
                }
            }

            if (isSelected) {
                Icon(
                    imageVector = Icons.Rounded.CheckCircle,
                    contentDescription = null,
                    tint = accentColor,
                    modifier = Modifier.size(22.dp),
                )
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        Row(
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier.fillMaxWidth(),
        ) {
            DifficultyStatMeter(
                label = stringResource(R.string.difficulty_stat_reaction),
                filledSegments = levelTier,
                accentColor = accentColor,
                modifier = Modifier.weight(1f),
            )
            DifficultyStatMeter(
                label = stringResource(R.string.difficulty_stat_boost),
                filledSegments = levelTier,
                accentColor = accentColor,
                modifier = Modifier.weight(1f),
            )
            DifficultyStatMeter(
                label = "${stringResource(R.string.difficulty_stat_aggression)} ${difficulty.aggressiveSharePercent}%",
                filledSegments = levelTier,
                accentColor = accentColor,
                modifier = Modifier.weight(1.25f),
            )
        }
    }
}

@Composable
private fun DifficultyStatMeter(
    label: String,
    filledSegments: Int,
    accentColor: Color,
    modifier: Modifier = Modifier,
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(4.dp),
        modifier = modifier,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium.copy(fontSize = 10.sp, lineHeight = 12.sp),
            color = TextSecondary,
            maxLines = 1,
            softWrap = false,
        )
        Row(
            horizontalArrangement = Arrangement.spacedBy(3.dp),
            modifier = Modifier.fillMaxWidth(),
        ) {
            for (step in 1..3) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(4.dp)
                        .clip(RoundedCornerShape(2.dp))
                        .background(
                            if (step <= filledSegments) accentColor else HudBorderStroke,
                        ),
                )
            }
        }
    }
}

private fun difficultyColor(difficulty: BotDifficulty): Color {
    return when (difficulty) {
        BotDifficulty.EASY -> ToxicLime
        BotDifficulty.MEDIUM -> NeonCyan
        BotDifficulty.HARD -> DangerCrimson
    }
}

@Preview(showBackground = true, widthDp = 390, heightDp = 844)
@Composable
private fun DifficultySelectionOverlayPreview() {
    NeonSnakeTheme {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(SpaceObsidian),
        ) {
            DifficultySelectionOverlay(
                selectedDifficulty = BotDifficulty.MEDIUM,
                onSelectDifficulty = {},
                onConfirmStart = {},
                onDismiss = {},
            )
        }
    }
}
