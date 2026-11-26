package com.example.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.withTransform
import com.example.game.engine.ArenaEngine
import com.example.game.model.ArenaConfig
import com.example.game.model.FoodOrb
import com.example.game.model.SnakeEntity
import com.example.ui.theme.ArenaDeepNavy
import com.example.ui.theme.DangerCrimson
import com.example.ui.theme.GridLineBlue
import com.example.ui.theme.GridMajorBlue
import com.example.ui.theme.NeonCyan
import com.example.ui.theme.PlasmaMagenta
import com.example.ui.theme.SolarGold
import com.example.ui.theme.SpaceObsidian
import com.example.ui.theme.ToxicLime
import com.example.ui.theme.UltravioletPulse
import kotlin.math.PI
import kotlin.math.ceil
import kotlin.math.cos
import kotlin.math.floor
import kotlin.math.sin

private val OrbPalette = listOf(
    NeonCyan,
    PlasmaMagenta,
    ToxicLime,
    SolarGold,
    UltravioletPulse,
    Color(0xFF38BDF8),
)

@Composable
fun ArenaCanvas(
    engine: ArenaEngine,
    frameTick: Long,
    modifier: Modifier = Modifier,
) {
    Canvas(modifier = modifier.fillMaxSize()) {
        // Read frameTick inside Canvas draw scope so invalidations happen at 60 FPS
        // without recomposing surrounding Composables.
        @Suppress("UNUSED_VARIABLE")
        val currentTick = frameTick

        // Deep space ambient backdrop
        drawRect(
            brush = Brush.radialGradient(
                colors = listOf(ArenaDeepNavy, SpaceObsidian),
                center = Offset(size.width * 0.5f, size.height * 0.45f),
                radius = size.maxDimension * 0.85f,
            ),
        )

        val cam = engine.camera
        val zoom = cam.zoom
        val screenCenterX = size.width * 0.5f
        val screenCenterY = size.height * 0.5f

        val halfWorldW = screenCenterX / zoom
        val halfWorldH = screenCenterY / zoom
        val cullPad = 80f
        val viewLeft = cam.x - halfWorldW - cullPad
        val viewRight = cam.x + halfWorldW + cullPad
        val viewTop = cam.y - halfWorldH - cullPad
        val viewBottom = cam.y + halfWorldH + cullPad

        withTransform({
            translate(left = screenCenterX, top = screenCenterY)
            scale(scaleX = zoom, scaleY = zoom, pivot = Offset.Zero)
            translate(left = -cam.x, top = -cam.y)
        }) {
            drawArenaGrid(
                viewLeft = viewLeft,
                viewRight = viewRight,
                viewTop = viewTop,
                viewBottom = viewBottom,
                arenaW = engine.arenaWidth,
                arenaH = engine.arenaHeight,
            )

            drawArenaBorders(
                arenaW = engine.arenaWidth,
                arenaH = engine.arenaHeight,
                worldTime = engine.worldTimeSeconds,
            )

            drawVisibleFoodOrbs(
                orbs = engine.foodOrbs,
                viewLeft = viewLeft,
                viewRight = viewRight,
                viewTop = viewTop,
                viewBottom = viewBottom,
                worldTime = engine.worldTimeSeconds,
            )

            val snakes = engine.snakes
            for (i in snakes.indices) {
                drawSnake(
                    snake = snakes[i],
                    worldTime = engine.worldTimeSeconds,
                )
            }
        }
    }
}

private fun DrawScope.drawArenaGrid(
    viewLeft: Float,
    viewRight: Float,
    viewTop: Float,
    viewBottom: Float,
    arenaW: Float,
    arenaH: Float,
) {
    val cell = ArenaConfig.GRID_CELL_SIZE
    val startX = (floor(viewLeft.coerceAtLeast(0f) / cell) * cell).coerceAtLeast(0f)
    val endX = (ceil(viewRight.coerceAtMost(arenaW) / cell) * cell).coerceAtMost(arenaW)
    val startY = (floor(viewTop.coerceAtLeast(0f) / cell) * cell).coerceAtLeast(0f)
    val endY = (ceil(viewBottom.coerceAtMost(arenaH) / cell) * cell).coerceAtMost(arenaH)

    val lineTop = viewTop.coerceAtLeast(0f)
    val lineBottom = viewBottom.coerceAtMost(arenaH)
    val lineLeft = viewLeft.coerceAtLeast(0f)
    val lineRight = viewRight.coerceAtMost(arenaW)

    if (lineBottom > lineTop) {
        var x = startX
        while (x <= endX) {
            val isMajor = ((x / cell).toInt() % 5) == 0
            drawLine(
                color = if (isMajor) GridMajorBlue else GridLineBlue,
                start = Offset(x, lineTop),
                end = Offset(x, lineBottom),
                strokeWidth = if (isMajor) 2.2f else 1.2f,
            )
            x += cell
        }
    }

    if (lineRight > lineLeft) {
        var y = startY
        while (y <= endY) {
            val isMajor = ((y / cell).toInt() % 5) == 0
            drawLine(
                color = if (isMajor) GridMajorBlue else GridLineBlue,
                start = Offset(lineLeft, y),
                end = Offset(lineRight, y),
                strokeWidth = if (isMajor) 2.2f else 1.2f,
            )
            y += cell
        }
    }
}

private fun DrawScope.drawArenaBorders(
    arenaW: Float,
    arenaH: Float,
    worldTime: Float,
) {
    val pulse = 0.65f + 0.35f * sin(worldTime * 3.0f)

    // Danger zone tint just outside the playable arena
    val band = 900f
    val dangerFill = DangerCrimson.copy(alpha = 0.08f)
    drawRect(color = dangerFill, topLeft = Offset(-band, -band), size = Size(arenaW + band * 2f, band))
    drawRect(color = dangerFill, topLeft = Offset(-band, arenaH), size = Size(arenaW + band * 2f, band))
    drawRect(color = dangerFill, topLeft = Offset(-band, 0f), size = Size(band, arenaH))
    drawRect(color = dangerFill, topLeft = Offset(arenaW, 0f), size = Size(band, arenaH))

    // Multi-pass glowing neon boundary perimeter
    val arenaSize = Size(arenaW, arenaH)
    drawRect(
        color = DangerCrimson.copy(alpha = 0.22f * pulse),
        topLeft = Offset.Zero,
        size = arenaSize,
        style = Stroke(width = 28f),
    )
    drawRect(
        color = PlasmaMagenta.copy(alpha = 0.55f * pulse),
        topLeft = Offset.Zero,
        size = arenaSize,
        style = Stroke(width = 10f),
    )
    drawRect(
        color = Color(0xFFFFB3CE),
        topLeft = Offset.Zero,
        size = arenaSize,
        style = Stroke(width = 3.5f),
    )

    // Corner beacons
    val corners = listOf(
        Offset(0f, 0f),
        Offset(arenaW, 0f),
        Offset(0f, arenaH),
        Offset(arenaW, arenaH),
    )
    for (c in corners) {
        drawCircle(color = DangerCrimson.copy(alpha = 0.35f * pulse), radius = 34f, center = c)
        drawCircle(color = Color.White, radius = 8f, center = c)
    }
}

private fun DrawScope.drawVisibleFoodOrbs(
    orbs: List<FoodOrb>,
    viewLeft: Float,
    viewRight: Float,
    viewTop: Float,
    viewBottom: Float,
    worldTime: Float,
) {
    for (i in orbs.indices) {
        val orb = orbs[i]
        if (orb.x < viewLeft || orb.x > viewRight || orb.y < viewTop || orb.y > viewBottom) {
            continue
        }
        val color = OrbPalette[orb.colorIndex % OrbPalette.size]
        val pulse = 1f + 0.14f * sin(worldTime * 4.0f + orb.pulsePhase)
        val r = orb.radius * pulse
        val center = Offset(orb.x, orb.y)

        drawCircle(
            color = color.copy(alpha = if (orb.isBoostDrop) 0.18f else 0.24f),
            radius = r * 2.25f,
            center = center,
        )
        drawCircle(
            color = color.copy(alpha = 0.90f),
            radius = r,
            center = center,
        )
        drawCircle(
            color = Color.White.copy(alpha = 0.85f),
            radius = r * 0.42f,
            center = center,
        )
    }
}

private fun DrawScope.drawSnake(
    snake: SnakeEntity,
    worldTime: Float,
) {
    val segments = snake.segments
    if (segments.isEmpty()) return

    val skin = snake.skin
    val boostWave = if (snake.isBoosting) {
        0.75f + 0.25f * sin(worldTime * 18f)
    } else {
        1f
    }

    // Draw back-to-front (tail to neck) so rings overlap cleanly toward the head
    for (i in segments.lastIndex downTo 1) {
        val seg = segments[i]
        val center = Offset(seg.x, seg.y)

        val tailProgress = (segments.lastIndex - i).coerceAtMost(5) / 5f
        val taper = 0.56f + 0.44f * tailProgress
        val radius = snake.bodyRadius * taper

        val isBand = (i % 3 == 0)
        val ringColor = if (isBand) skin.secondaryColor else skin.primaryColor

        // Outer neon halo
        drawCircle(
            color = skin.glowColor.copy(
                alpha = (if (snake.isBoosting) 0.30f else 0.15f) * boostWave,
            ),
            radius = radius * (if (snake.isBoosting) 1.78f else 1.48f),
            center = center,
        )

        // Main neon ring
        drawCircle(
            color = ringColor,
            radius = radius,
            center = center,
        )

        // Luminous inner core
        drawCircle(
            color = skin.coreColor.copy(alpha = if (snake.isBoosting) 0.72f else 0.40f),
            radius = radius * 0.46f,
            center = center,
        )
    }

    // Draw Snake Head
    val headCenter = Offset(snake.headX, snake.headY)
    val headR = snake.headRadius
    val angle = snake.angleRadians

    // Head aura
    drawCircle(
        color = skin.glowColor.copy(alpha = if (snake.isBoosting) 0.42f else 0.25f),
        radius = headR * 1.95f,
        center = headCenter,
    )
    drawCircle(
        color = skin.primaryColor,
        radius = headR,
        center = headCenter,
    )
    drawCircle(
        color = skin.coreColor.copy(alpha = 0.55f),
        radius = headR * 0.62f,
        center = headCenter,
    )

    // Expressive directional eyes tracking movement angle
    val eyeForwardDist = headR * 0.46f
    val eyeSpreadRad = 0.52f
    val eyeRadius = headR * 0.30f
    val pupilRadius = eyeRadius * 0.54f
    val pupilLookDist = eyeRadius * 0.34f

    val lookDx = cos(angle) * pupilLookDist
    val lookDy = sin(angle) * pupilLookDist

    val leftEyeAngle = angle - eyeSpreadRad
    val rightEyeAngle = angle + eyeSpreadRad

    val leftEyePos = Offset(
        x = headCenter.x + cos(leftEyeAngle) * eyeForwardDist,
        y = headCenter.y + sin(leftEyeAngle) * eyeForwardDist,
    )
    val rightEyePos = Offset(
        x = headCenter.x + cos(rightEyeAngle) * eyeForwardDist,
        y = headCenter.y + sin(rightEyeAngle) * eyeForwardDist,
    )

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

    // Tiny subtle eye glint
    drawCircle(
        color = Color.White.copy(alpha = 0.9f),
        radius = pupilRadius * 0.32f,
        center = Offset(leftEyePos.x + lookDx - 1.2f, leftEyePos.y + lookDy - 1.2f),
    )
    drawCircle(
        color = Color.White.copy(alpha = 0.9f),
        radius = pupilRadius * 0.32f,
        center = Offset(rightEyePos.x + lookDx - 1.2f, rightEyePos.y + lookDy - 1.2f),
    )
}
