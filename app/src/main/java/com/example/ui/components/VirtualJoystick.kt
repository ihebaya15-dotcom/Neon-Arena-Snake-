package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.ui.theme.TextSecondary
import kotlin.math.hypot

/**
 * Custom Jetpack Compose Virtual Joystick that spawns at the user's touch location,
 * captures continuous drag gestures, and converts the touch delta into a normalized
 * vector (x, y) clamped inside the unit circle [-1f, 1f].
 */
@Composable
fun VirtualJoystick(
    accentColor: Color,
    onVectorChanged: (x: Float, y: Float) -> Unit,
    modifier: Modifier = Modifier,
) {
    val maxRadiusPx = with(LocalDensity.current) { 58.dp.toPx() }
    val currentOnVectorChanged by rememberUpdatedState(onVectorChanged)

    var isActive by remember { mutableStateOf(false) }
    var origin by remember { mutableStateOf(Offset.Zero) }
    var knobOffset by remember { mutableStateOf(Offset.Zero) }
    var hasUsedOnce by remember { mutableStateOf(false) }

    Box(
        modifier = modifier
            .fillMaxSize()
            .testTag("virtual_joystick_area")
            .pointerInput(maxRadiusPx) {
                awaitEachGesture {
                    val down = awaitFirstDown(
                        requireUnconsumed = true,
                        pass = PointerEventPass.Main,
                    )
                    val activePointerId = down.id
                    hasUsedOnce = true
                    isActive = true
                    origin = down.position
                    knobOffset = Offset.Zero
                    currentOnVectorChanged(0f, 0f)

                    try {
                        while (true) {
                            val event = awaitPointerEvent(PointerEventPass.Main)
                            val change = event.changes.firstOrNull { it.id == activePointerId }
                                ?: break

                            if (!change.pressed) {
                                break
                            }

                            val delta = change.position - origin
                            val distance = hypot(delta.x, delta.y)
                            val clampedDelta = if (distance > maxRadiusPx && distance > 0.001f) {
                                delta * (maxRadiusPx / distance)
                            } else {
                                delta
                            }

                            knobOffset = clampedDelta
                            val normX = (clampedDelta.x / maxRadiusPx).coerceIn(-1f, 1f)
                            val normY = (clampedDelta.y / maxRadiusPx).coerceIn(-1f, 1f)
                            currentOnVectorChanged(normX, normY)
                            change.consume()
                        }
                    } finally {
                        isActive = false
                        knobOffset = Offset.Zero
                        currentOnVectorChanged(0f, 0f)
                    }
                }
            },
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            if (!isActive) return@Canvas

            val knobCenter = origin + knobOffset

            // Outer glowing ring
            drawCircle(
                color = accentColor.copy(alpha = 0.12f),
                radius = maxRadiusPx + 10f,
                center = origin,
            )
            drawCircle(
                color = accentColor.copy(alpha = 0.48f),
                radius = maxRadiusPx,
                center = origin,
                style = Stroke(width = 3.dp.toPx()),
            )

            // Center anchor dot
            drawCircle(
                color = accentColor.copy(alpha = 0.45f),
                radius = 5.dp.toPx(),
                center = origin,
            )

            // Tether line from origin to thumb knob
            if (knobOffset.getDistance() > 4f) {
                drawLine(
                    color = accentColor.copy(alpha = 0.65f),
                    start = origin,
                    end = knobCenter,
                    strokeWidth = 3.dp.toPx(),
                    cap = StrokeCap.Round,
                )
            }

            // Thumbstick glow + solid neon cap
            drawCircle(
                color = accentColor.copy(alpha = 0.35f),
                radius = 28.dp.toPx(),
                center = knobCenter,
            )
            drawCircle(
                color = accentColor,
                radius = 19.dp.toPx(),
                center = knobCenter,
            )
            drawCircle(
                color = Color.White.copy(alpha = 0.85f),
                radius = 8.dp.toPx(),
                center = knobCenter,
            )
        }

        AnimatedVisibility(
            visible = !isActive && !hasUsedOnce,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 128.dp),
        ) {
            Text(
                text = stringResource(R.string.joystick_hint),
                style = MaterialTheme.typography.bodyMedium,
                color = TextSecondary.copy(alpha = 0.78f),
            )
        }
    }
}
