package com.example.ui.screens

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.EmojiEvents
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.game.model.SnakeSkin
import com.example.game.model.SnakeSkins
import com.example.ui.theme.ArenaDeepNavy
import com.example.ui.theme.GridLineBlue
import com.example.ui.theme.HudBorderStroke
import com.example.ui.theme.HudGlassSurface
import com.example.ui.theme.SolarGold
import com.example.ui.theme.SpaceObsidian
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import kotlin.math.PI
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun LobbyScreen(
    selectedSkin: SnakeSkin,
    sessionBestScore: Int,
    onSelectSkin: (SnakeSkin) -> Unit,
    onStartGame: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val infiniteTransition = rememberInfiniteTransition(label = "lobby_anim")
    val wavePhase by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = (2f * PI.toFloat()),
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 2800, easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "snake_wave_phase",
    )

    val animatedAccent by animateColorAsState(
        targetValue = selectedSkin.primaryColor,
        label = "lobby_accent",
    )

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(SpaceObsidian),
    ) {
        // Animated grid & ambient glow backdrop
        Canvas(modifier = Modifier.fillMaxSize()) {
            drawRect(
                brush = Brush.radialGradient(
                    colors = listOf(ArenaDeepNavy, SpaceObsidian),
                    center = Offset(size.width * 0.5f, size.height * 0.35f),
                    radius = size.maxDimension * 0.8f,
                ),
            )
            val step = 42.dp.toPx()
            val scrollOffset = (wavePhase / (2f * PI.toFloat())) * step
            var x = 0f
            while (x <= size.width) {
                drawLine(
                    color = GridLineBlue.copy(alpha = 0.65f),
                    start = Offset(x, 0f),
                    end = Offset(x, size.height),
                    strokeWidth = 1f,
                )
                x += step
            }
            var y = -step + scrollOffset
            while (y <= size.height) {
                drawLine(
                    color = GridLineBlue.copy(alpha = 0.65f),
                    start = Offset(0f, y),
                    end = Offset(size.width, y),
                    strokeWidth = 1f,
                )
                y += step
            }
            drawCircle(
                color = animatedAccent.copy(alpha = 0.14f),
                radius = size.width * 0.52f,
                center = Offset(size.width * 0.5f, size.height * 0.38f),
            )
        }

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.safeDrawing)
                .widthIn(max = 560.dp)
                .align(Alignment.Center)
                .padding(horizontal = 24.dp, vertical = 20.dp),
        ) {
            // Top Title + Best Score
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.padding(top = 12.dp),
            ) {
                Text(
                    text = "NEON SNAKE",
                    style = MaterialTheme.typography.displayMedium,
                    color = animatedAccent,
                    textAlign = TextAlign.Center,
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = stringResource(R.string.lobby_subtitle),
                    style = MaterialTheme.typography.titleMedium,
                    color = TextSecondary,
                    textAlign = TextAlign.Center,
                )

                if (sessionBestScore > 0) {
                    Spacer(modifier = Modifier.height(14.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier
                            .clip(RoundedCornerShape(24.dp))
                            .background(HudGlassSurface)
                            .border(1.dp, SolarGold.copy(alpha = 0.5f), RoundedCornerShape(24.dp))
                            .padding(horizontal = 16.dp, vertical = 7.dp),
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.EmojiEvents,
                            contentDescription = null,
                            tint = SolarGold,
                            modifier = Modifier.size(18.dp),
                        )
                        Text(
                            text = stringResource(R.string.lobby_best_score, sessionBestScore),
                            style = MaterialTheme.typography.labelLarge,
                            color = SolarGold,
                        )
                    }
                }
            }

            // Live Interactive Snake Preview Card
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(190.dp)
                    .clip(RoundedCornerShape(28.dp))
                    .background(HudGlassSurface)
                    .border(
                        width = 1.5.dp,
                        color = animatedAccent.copy(alpha = 0.45f),
                        shape = RoundedCornerShape(28.dp),
                    ),
            ) {
                SnakePreviewCanvas(
                    skin = selectedSkin,
                    wavePhase = wavePhase,
                    modifier = Modifier.fillMaxSize(),
                )
            }

            // Skin Picker + Play Button
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(
                    text = stringResource(R.string.lobby_choose_skin),
                    style = MaterialTheme.typography.titleMedium,
                    color = TextPrimary,
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = selectedSkin.displayName,
                    style = MaterialTheme.typography.bodyMedium,
                    color = animatedAccent,
                )
                Spacer(modifier = Modifier.height(14.dp))

                Row(
                    horizontalArrangement = Arrangement.spacedBy(14.dp, Alignment.CenterHorizontally),
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    SnakeSkins.all.forEach { skin ->
                        SkinSwatchButton(
                            skin = skin,
                            isSelected = skin.id == selectedSkin.id,
                            onClick = { onSelectSkin(skin) },
                        )
                    }
                }

                Spacer(modifier = Modifier.height(28.dp))

                Button(
                    onClick = onStartGame,
                    shape = RoundedCornerShape(20.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = animatedAccent,
                        contentColor = SpaceObsidian,
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(58.dp)
                        .testTag("start_game_button"),
                ) {
                    Icon(
                        imageVector = Icons.Rounded.PlayArrow,
                        contentDescription = null,
                        modifier = Modifier.size(26.dp),
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = stringResource(R.string.lobby_start_play),
                        style = MaterialTheme.typography.titleMedium,
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = stringResource(R.string.lobby_controls_hint),
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextSecondary,
                    textAlign = TextAlign.Center,
                )
            }
        }
    }
}

@Composable
private fun SkinSwatchButton(
    skin: SnakeSkin,
    isSelected: Boolean,
    onClick: () -> Unit,
) {
    val scale by animateFloatAsState(
        targetValue = if (isSelected) 1.14f else 1.0f,
        animationSpec = spring(),
        label = "swatch_scale",
    )
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .minimumInteractiveComponentSize()
            .size(50.dp)
            .scale(scale)
            .clip(CircleShape)
            .background(
                brush = Brush.linearGradient(
                    colors = listOf(skin.primaryColor, skin.secondaryColor),
                ),
            )
            .border(
                width = if (isSelected) 3.dp else 1.5.dp,
                color = if (isSelected) Color.White else HudBorderStroke,
                shape = CircleShape,
            )
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = ripple(bounded = true),
                onClick = onClick,
            )
            .semantics { contentDescription = skin.displayName }
            .testTag("skin_option_${skin.id}"),
    ) {
        if (isSelected) {
            Icon(
                imageVector = Icons.Rounded.Check,
                contentDescription = null,
                tint = SpaceObsidian,
                modifier = Modifier.size(24.dp),
            )
        }
    }
}

@Composable
private fun SnakePreviewCanvas(
    skin: SnakeSkin,
    wavePhase: Float,
    modifier: Modifier = Modifier,
) {
    Canvas(modifier = modifier) {
        val segmentCount = 18
        val startX = size.width * 0.20f
        val endX = size.width * 0.78f
        val centerY = size.height * 0.5f
        val amplitude = size.height * 0.20f

        val positions = List(segmentCount) { idx ->
            val t = idx.toFloat() / (segmentCount - 1)
            val x = startX + (endX - startX) * t
            val y = centerY + sin(wavePhase - t * 4.5f) * amplitude
            Offset(x, y)
        }

        // Draw body back-to-front
        for (i in 0 until segmentCount - 1) {
            val pos = positions[i]
            val taper = 0.55f + 0.45f * (i.toFloat() / segmentCount)
            val radius = 14.dp.toPx() * taper
            val ringColor = if (i % 3 == 0) skin.secondaryColor else skin.primaryColor

            drawCircle(
                color = skin.glowColor.copy(alpha = 0.22f),
                radius = radius * 1.65f,
                center = pos,
            )
            drawCircle(
                color = ringColor,
                radius = radius,
                center = pos,
            )
            drawCircle(
                color = skin.coreColor.copy(alpha = 0.45f),
                radius = radius * 0.45f,
                center = pos,
            )
        }

        // Head
        val head = positions.last()
        val neck = positions[positions.lastIndex - 1]
        val headAngle = atan2(head.y - neck.y, head.x - neck.x)
        val headR = 18.dp.toPx()

        drawCircle(
            color = skin.glowColor.copy(alpha = 0.34f),
            radius = headR * 1.9f,
            center = head,
        )
        drawCircle(
            color = skin.primaryColor,
            radius = headR,
            center = head,
        )
        drawCircle(
            color = skin.coreColor.copy(alpha = 0.55f),
            radius = headR * 0.6f,
            center = head,
        )

        val eyeDist = headR * 0.46f
        val eyeRadius = headR * 0.30f
        val pupilRadius = eyeRadius * 0.55f
        val lookDx = cos(headAngle) * eyeRadius * 0.34f
        val lookDy = sin(headAngle) * eyeRadius * 0.34f

        listOf(headAngle - 0.52f, headAngle + 0.52f).forEach { eyeAngle ->
            val eyePos = Offset(
                head.x + cos(eyeAngle) * eyeDist,
                head.y + sin(eyeAngle) * eyeDist,
            )
            drawCircle(color = Color.White, radius = eyeRadius, center = eyePos)
            drawCircle(
                color = Color(0xFF070B18),
                radius = pupilRadius,
                center = Offset(eyePos.x + lookDx, eyePos.y + lookDy),
            )
        }
    }
}
