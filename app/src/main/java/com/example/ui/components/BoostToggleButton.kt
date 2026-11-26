package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Bolt
import androidx.compose.material.icons.rounded.WarningAmber
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
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.ui.theme.DangerCrimson
import com.example.ui.theme.HudGlassSurface
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary

@Composable
fun BoostToggleButton(
    isBoostActive: Boolean,
    canBoost: Boolean,
    showMinLengthWarning: Boolean,
    accentColor: Color,
    onToggleBoost: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val infiniteTransition = rememberInfiniteTransition(label = "boost_pulse")
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.25f,
        targetValue = 0.65f,
        animationSpec = infiniteRepeatable(
            animation = tween(550),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "boost_glow_alpha",
    )

    val scale by animateFloatAsState(
        targetValue = if (isBoostActive) 1.07f else 1.0f,
        animationSpec = spring(),
        label = "boost_scale",
    )

    val borderColor by animateColorAsState(
        targetValue = when {
            showMinLengthWarning -> DangerCrimson
            isBoostActive -> accentColor
            canBoost -> accentColor.copy(alpha = 0.45f)
            else -> TextSecondary.copy(alpha = 0.28f)
        },
        label = "boost_border_color",
    )

    val containerColor by animateColorAsState(
        targetValue = when {
            showMinLengthWarning -> DangerCrimson.copy(alpha = 0.24f)
            isBoostActive -> accentColor.copy(alpha = 0.28f)
            else -> HudGlassSurface
        },
        label = "boost_bg_color",
    )

    Column(
        horizontalAlignment = Alignment.Start,
        modifier = modifier,
    ) {
        AnimatedVisibility(
            visible = showMinLengthWarning,
            enter = fadeIn() + slideInVertically { it / 2 },
            exit = fadeOut() + slideOutVertically { it / 2 },
        ) {
            Box(
                modifier = Modifier
                    .padding(bottom = 8.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(DangerCrimson.copy(alpha = 0.90f))
                    .padding(horizontal = 10.dp, vertical = 5.dp),
            ) {
                Text(
                    text = stringResource(R.string.boost_min_length_warning),
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextPrimary,
                )
            }
        }

        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .minimumInteractiveComponentSize()
                .size(78.dp)
                .scale(scale)
                .drawBehind {
                    if (isBoostActive) {
                        drawCircle(
                            color = accentColor.copy(alpha = pulseAlpha),
                            radius = size.minDimension * 0.64f,
                        )
                    } else if (showMinLengthWarning) {
                        drawCircle(
                            color = DangerCrimson.copy(alpha = 0.4f),
                            radius = size.minDimension * 0.60f,
                        )
                    }
                }
                .clip(CircleShape)
                .background(containerColor)
                .border(
                    width = if (isBoostActive) 2.5.dp else 1.5.dp,
                    color = borderColor,
                    shape = CircleShape,
                )
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = ripple(bounded = true, color = accentColor),
                    onClick = onToggleBoost,
                )
                .testTag("boost_toggle_button"),
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                Icon(
                    imageVector = if (showMinLengthWarning) Icons.Rounded.WarningAmber else Icons.Rounded.Bolt,
                    contentDescription = stringResource(R.string.boost_label),
                    tint = when {
                        showMinLengthWarning -> DangerCrimson
                        isBoostActive -> Color.White
                        canBoost -> accentColor
                        else -> TextSecondary.copy(alpha = 0.5f)
                    },
                    modifier = Modifier.size(28.dp),
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = if (isBoostActive) {
                        stringResource(R.string.boost_active_label)
                    } else {
                        stringResource(R.string.boost_label)
                    },
                    style = MaterialTheme.typography.labelSmall,
                    color = when {
                        showMinLengthWarning -> DangerCrimson
                        isBoostActive -> Color.White
                        canBoost -> TextPrimary
                        else -> TextSecondary.copy(alpha = 0.5f)
                    },
                )
            }
        }
    }
}
