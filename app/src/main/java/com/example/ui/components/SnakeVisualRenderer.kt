package com.example.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import com.example.game.model.EyeStyle
import com.example.game.model.SnakeSkin
import com.example.game.model.TrailStyle
import kotlin.math.PI
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin

fun DrawScope.drawSnakeTrailSegment(
    center: Offset,
    radius: Float,
    segmentIndex: Int,
    totalSegments: Int,
    skin: SnakeSkin,
    trailStyle: TrailStyle,
    isBoosting: Boolean,
    worldTime: Float,
    shieldAlpha: Float,
) {
    val boostFactor = if (isBoosting) 1.35f else 1.0f
    when (trailStyle) {
        TrailStyle.CLASSIC_GLOW -> {
            drawCircle(
                color = skin.glowColor.copy(
                    alpha = (if (isBoosting) 0.28f else 0.14f) * shieldAlpha,
                ),
                radius = radius * (if (isBoosting) 1.78f else 1.48f),
                center = center,
            )
        }

        TrailStyle.PLASMA_SPARKS -> {
            drawCircle(
                color = skin.glowColor.copy(alpha = 0.22f * boostFactor * shieldAlpha),
                radius = radius * 1.65f,
                center = center,
            )
            if (segmentIndex % 2 == 0) {
                val wobble = sin(worldTime * 11f + segmentIndex * 0.9f) * radius * 0.72f
                val sparkAlpha = (0.45f + 0.35f * cos(worldTime * 8f + segmentIndex)) * shieldAlpha
                drawCircle(
                    color = skin.secondaryColor.copy(alpha = sparkAlpha.coerceIn(0f, 1f)),
                    radius = radius * 0.32f * boostFactor,
                    center = Offset(center.x + wobble, center.y - wobble * 0.55f),
                )
                drawCircle(
                    color = Color.White.copy(alpha = (sparkAlpha * 0.7f).coerceIn(0f, 1f)),
                    radius = radius * 0.16f * boostFactor,
                    center = Offset(center.x - wobble * 0.65f, center.y + wobble * 0.45f),
                )
            }
        }

        TrailStyle.PULSE_RINGS -> {
            drawCircle(
                color = skin.glowColor.copy(alpha = 0.18f * shieldAlpha),
                radius = radius * 1.55f,
                center = center,
            )
            if (segmentIndex % 3 == 0 || segmentIndex == totalSegments - 1) {
                val pulse = 0.85f + 0.28f * sin(worldTime * 9f - segmentIndex * 0.65f)
                drawCircle(
                    color = skin.secondaryColor.copy(alpha = (0.55f * boostFactor * shieldAlpha).coerceAtMost(0.9f)),
                    radius = radius * 1.48f * pulse,
                    center = center,
                    style = Stroke(width = (radius * 0.20f).coerceAtLeast(1.8f)),
                )
            }
        }

        TrailStyle.STARLIGHT_RIBBON -> {
            drawCircle(
                color = skin.glowColor.copy(alpha = 0.22f * boostFactor * shieldAlpha),
                radius = radius * 1.70f,
                center = center,
            )
            if (segmentIndex % 3 == 1) {
                val starR = radius * (if (isBoosting) 1.05f else 0.82f)
                val phaseOffset = worldTime * 3.5f + segmentIndex * 0.5f
                val starColor = Color.White.copy(alpha = 0.75f * shieldAlpha)
                val dx1 = cos(phaseOffset) * starR
                val dy1 = sin(phaseOffset) * starR
                val dx2 = cos(phaseOffset + PI.toFloat() / 2f) * starR
                val dy2 = sin(phaseOffset + PI.toFloat() / 2f) * starR
                drawLine(
                    color = starColor,
                    start = Offset(center.x - dx1, center.y - dy1),
                    end = Offset(center.x + dx1, center.y + dy1),
                    strokeWidth = 1.8f,
                    cap = StrokeCap.Round,
                )
                drawLine(
                    color = starColor,
                    start = Offset(center.x - dx2, center.y - dy2),
                    end = Offset(center.x + dx2, center.y + dy2),
                    strokeWidth = 1.8f,
                    cap = StrokeCap.Round,
                )
            }
        }
    }
}

fun DrawScope.drawSnakeEyes(
    headCenter: Offset,
    headRadius: Float,
    angleRadians: Float,
    eyeStyle: EyeStyle,
    skin: SnakeSkin,
) {
    val eyeForwardDist = headRadius * 0.46f
    val eyeSpreadRad = 0.52f
    val eyeRadius = headRadius * 0.30f
    val pupilRadius = eyeRadius * 0.54f
    val pupilLookDist = eyeRadius * 0.34f

    val lookDx = cos(angleRadians) * pupilLookDist
    val lookDy = sin(angleRadians) * pupilLookDist

    val leftEyePos = Offset(
        x = headCenter.x + cos(angleRadians - eyeSpreadRad) * eyeForwardDist,
        y = headCenter.y + sin(angleRadians - eyeSpreadRad) * eyeForwardDist,
    )
    val rightEyePos = Offset(
        x = headCenter.x + cos(angleRadians + eyeSpreadRad) * eyeForwardDist,
        y = headCenter.y + sin(angleRadians + eyeSpreadRad) * eyeForwardDist,
    )

    when (eyeStyle) {
        EyeStyle.CYBER_ROUND -> {
            drawCircle(color = Color.White, radius = eyeRadius, center = leftEyePos)
            drawCircle(color = Color.White, radius = eyeRadius, center = rightEyePos)
            drawCircle(
                color = Color(0xFF070B18),
                radius = pupilRadius,
                center = Offset(leftEyePos.x + lookDx, leftEyePos.y + lookDy),
            )
            drawCircle(
                color = Color(0xFF070B18),
                radius = pupilRadius,
                center = Offset(rightEyePos.x + lookDx, rightEyePos.y + lookDy),
            )
        }

        EyeStyle.PREDATOR_SLIT -> {
            drawCircle(color = Color(0xFFFFF3C4), radius = eyeRadius * 1.05f, center = leftEyePos)
            drawCircle(color = Color(0xFFFFF3C4), radius = eyeRadius * 1.05f, center = rightEyePos)
            val perpAngle = angleRadians + (PI.toFloat() / 2f)
            val slitHalf = eyeRadius * 0.68f
            listOf(leftEyePos, rightEyePos).forEach { eyePos ->
                val cx = eyePos.x + lookDx * 0.8f
                val cy = eyePos.y + lookDy * 0.8f
                drawLine(
                    color = Color(0xFF070B18),
                    start = Offset(cx - cos(perpAngle) * slitHalf, cy - sin(perpAngle) * slitHalf),
                    end = Offset(cx + cos(perpAngle) * slitHalf, cy + sin(perpAngle) * slitHalf),
                    strokeWidth = (eyeRadius * 0.55f).coerceAtLeast(2.2f),
                    cap = StrokeCap.Round,
                )
            }
        }

        EyeStyle.NEON_VISOR -> {
            val visorPath = Path().apply {
                val frontLeft = Offset(
                    headCenter.x + cos(angleRadians - 0.62f) * (headRadius * 0.68f),
                    headCenter.y + sin(angleRadians - 0.62f) * (headRadius * 0.68f),
                )
                val frontRight = Offset(
                    headCenter.x + cos(angleRadians + 0.62f) * (headRadius * 0.68f),
                    headCenter.y + sin(angleRadians + 0.62f) * (headRadius * 0.68f),
                )
                val backRight = Offset(
                    headCenter.x + cos(angleRadians + 0.72f) * (headRadius * 0.25f),
                    headCenter.y + sin(angleRadians + 0.72f) * (headRadius * 0.25f),
                )
                val backLeft = Offset(
                    headCenter.x + cos(angleRadians - 0.72f) * (headRadius * 0.25f),
                    headCenter.y + sin(angleRadians - 0.72f) * (headRadius * 0.25f),
                )
                moveTo(frontLeft.x, frontLeft.y)
                lineTo(frontRight.x, frontRight.y)
                lineTo(backRight.x, backRight.y)
                lineTo(backLeft.x, backLeft.y)
                close()
            }
            drawPath(path = visorPath, color = Color(0xFF060812))
            drawPath(
                path = visorPath,
                color = skin.coreColor,
                style = Stroke(width = (headRadius * 0.14f).coerceAtLeast(2f)),
            )
            val centerGlint = Offset(
                headCenter.x + cos(angleRadians) * (headRadius * 0.45f),
                headCenter.y + sin(angleRadians) * (headRadius * 0.45f),
            )
            drawCircle(color = Color.White, radius = eyeRadius * 0.42f, center = centerGlint)
        }

        EyeStyle.STAR_CORE -> {
            drawCircle(color = Color.White, radius = eyeRadius * 1.05f, center = leftEyePos)
            drawCircle(color = Color.White, radius = eyeRadius * 1.05f, center = rightEyePos)
            val starR = eyeRadius * 0.65f
            listOf(leftEyePos, rightEyePos).forEach { eyePos ->
                val cx = eyePos.x + lookDx * 0.7f
                val cy = eyePos.y + lookDy * 0.7f
                drawLine(
                    color = skin.primaryColor,
                    start = Offset(cx - starR, cy),
                    end = Offset(cx + starR, cy),
                    strokeWidth = 2.2f,
                    cap = StrokeCap.Round,
                )
                drawLine(
                    color = skin.primaryColor,
                    start = Offset(cx, cy - starR),
                    end = Offset(cx, cy + starR),
                    strokeWidth = 2.2f,
                    cap = StrokeCap.Round,
                )
                drawCircle(color = Color(0xFF070B18), radius = starR * 0.35f, center = Offset(cx, cy))
            }
        }
    }
}

@Composable
fun CoordinatedSnakePreviewCanvas(
    skin: SnakeSkin,
    trailStyle: TrailStyle,
    eyeStyle: EyeStyle,
    wavePhase: Float,
    isBoostPreview: Boolean = false,
    modifier: Modifier = Modifier,
) {
    Canvas(modifier = modifier) {
        val segmentCount = 18
        val startX = size.width * 0.18f
        val endX = size.width * 0.80f
        val centerY = size.height * 0.5f
        val amplitude = size.height * 0.21f

        val positions = List(segmentCount) { idx ->
            val t = idx.toFloat() / (segmentCount - 1)
            val x = startX + (endX - startX) * t
            val y = centerY + sin(wavePhase - t * 4.5f) * amplitude
            Offset(x, y)
        }

        for (i in 0 until segmentCount - 1) {
            val pos = positions[i]
            val taper = 0.54f + 0.46f * (i.toFloat() / segmentCount)
            val radius = 13.5.dp.toPx() * taper
            val ringColor = if (i % 3 == 0) skin.secondaryColor else skin.primaryColor

            drawSnakeTrailSegment(
                center = pos,
                radius = radius,
                segmentIndex = segmentCount - 1 - i,
                totalSegments = segmentCount,
                skin = skin,
                trailStyle = trailStyle,
                isBoosting = isBoostPreview,
                worldTime = wavePhase,
                shieldAlpha = 1f,
            )

            drawCircle(
                color = ringColor,
                radius = radius,
                center = pos,
            )
            drawCircle(
                color = skin.coreColor.copy(alpha = if (isBoostPreview) 0.72f else 0.45f),
                radius = radius * 0.45f,
                center = pos,
            )
        }

        val head = positions.last()
        val neck = positions[positions.lastIndex - 1]
        val headAngle = atan2(head.y - neck.y, head.x - neck.x)
        val headR = 17.5.dp.toPx()

        drawCircle(
            color = skin.glowColor.copy(alpha = if (isBoostPreview) 0.44f else 0.30f),
            radius = headR * 1.95f,
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

        drawSnakeEyes(
            headCenter = head,
            headRadius = headR,
            angleRadians = headAngle,
            eyeStyle = eyeStyle,
            skin = skin,
        )
    }
}
