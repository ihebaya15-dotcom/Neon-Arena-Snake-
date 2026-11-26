package com.example.ui.components

import android.graphics.Paint
import android.graphics.Typeface
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.graphics.nativeCanvas
import com.example.game.engine.ArenaEngine
import com.example.game.engine.ArenaSpatialGrid
import com.example.game.model.ArenaConfig
import com.example.game.model.DeathBurstParticle
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
    val labelPaint = remember {
        Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = android.graphics.Color.argb(220, 244, 247, 255)
            textSize = 26f
            textAlign = Paint.Align.CENTER
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            setShadowLayer(6f, 0f, 0f, android.graphics.Color.argb(200, 6, 8, 18))
        }
    }

    Canvas(modifier = modifier.fillMaxSize()) {
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
        val cullPad = 95f
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
                spatialGrid = engine.spatialGrid,
                viewLeft = viewLeft,
                viewRight = viewRight,
                viewTop = viewTop,
                viewBottom = viewBottom,
                worldTime = engine.worldTimeSeconds,
            )

            drawVisibleParticles(
                particles = engine.particles,
                viewLeft = viewLeft,
                viewRight = viewRight,
                viewTop = viewTop,
                viewBottom = viewBottom,
            )

            engine.spatialGrid.forEachSnakeInBounds(
                minX = viewLeft,
                minY = viewTop,
                maxX = viewRight,
                maxY = viewBottom,
            ) { snake ->
                if (snake.isAlive) {
                    drawSnake(
                        snake = snake,
                        worldTime = engine.worldTimeSeconds,
                        labelPaint = labelPaint,
                    )
                }
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

    val band = 900f
    val dangerFill = DangerCrimson.copy(alpha = 0.08f)
    drawRect(color = dangerFill, topLeft = Offset(-band, -band), size = Size(arenaW + band * 2f, band))
    drawRect(color = dangerFill, topLeft = Offset(-band, arenaH), size = Size(arenaW + band * 2f, band))
    drawRect(color = dangerFill, topLeft = Offset(-band, 0f), size = Size(band, arenaH))
    drawRect(color = dangerFill, topLeft = Offset(arenaW, 0f), size = Size(band, arenaH))

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
    spatialGrid: ArenaSpatialGrid,
    viewLeft: Float,
    viewRight: Float,
    viewTop: Float,
    viewBottom: Float,
    worldTime: Float,
) {
    spatialGrid.forEachFoodInBounds(
        minX = viewLeft,
        minY = viewTop,
        maxX = viewRight,
        maxY = viewBottom,
    ) { orb ->
        if (!orb.isConsumed &&
            orb.x >= viewLeft && orb.x <= viewRight &&
            orb.y >= viewTop && orb.y <= viewBottom
        ) {
            val color = OrbPalette[orb.colorIndex % OrbPalette.size]
            val speed = if (orb.isLootDrop) 6.5f else 4.0f
            val pulse = 1f + (if (orb.isLootDrop) 0.20f else 0.14f) * sin(worldTime * speed + orb.pulsePhase)
            val r = orb.radius * pulse
            val center = Offset(orb.x, orb.y)

            drawCircle(
                color = color.copy(alpha = if (orb.isLootDrop) 0.32f else if (orb.isBoostDrop) 0.18f else 0.24f),
                radius = r * (if (orb.isLootDrop) 2.55f else 2.25f),
                center = center,
            )
            if (orb.isLootDrop) {
                drawCircle(
                    color = SolarGold.copy(alpha = 0.50f),
                    radius = r * 1.45f,
                    center = center,
                    style = Stroke(width = 1.6f),
                )
            }
            drawCircle(
                color = color.copy(alpha = 0.92f),
                radius = r,
                center = center,
            )
            drawCircle(
                color = Color.White.copy(alpha = 0.88f),
                radius = r * 0.44f,
                center = center,
            )
        }
    }
}

private fun DrawScope.drawVisibleParticles(
    particles: List<DeathBurstParticle>,
    viewLeft: Float,
    viewRight: Float,
    viewTop: Float,
    viewBottom: Float,
) {
    for (i in particles.indices) {
        val p = particles[i]
        if (p.x < viewLeft || p.x > viewRight || p.y < viewTop || p.y > viewBottom) continue
        val alpha = (p.lifeSec / p.maxLifeSec).coerceIn(0f, 1f)
        val center = Offset(p.x, p.y)
        drawCircle(
            color = p.color.copy(alpha = alpha * 0.45f),
            radius = p.radius * 2.1f,
            center = center,
        )
        drawCircle(
            color = Color.White.copy(alpha = alpha * 0.85f),
            radius = p.radius * alpha,
            center = center,
        )
    }
}

private fun DrawScope.drawSnake(
    snake: SnakeEntity,
    worldTime: Float,
    labelPaint: Paint,
) {
    val segments = snake.segments
    if (segments.isEmpty()) return

    val skin = snake.skin
    val isShielded = snake.spawnProtectionSec > 0f
    val shieldAlpha = if (isShielded) 0.55f + 0.25f * sin(worldTime * 14f) else 1f
    val boostWave = if (snake.isBoosting) {
        0.75f + 0.25f * sin(worldTime * 18f)
    } else {
        1f
    }

    // Back-to-front segment rendering
    for (i in segments.lastIndex downTo 1) {
        val seg = segments[i]
        val center = Offset(seg.x, seg.y)

        val tailProgress = (segments.lastIndex - i).coerceAtMost(5) / 5f
        val taper = 0.56f + 0.44f * tailProgress
        val radius = snake.bodyRadius * taper

        val isBand = (i % 3 == 0)
        val ringColor = if (isBand) skin.secondaryColor else skin.primaryColor

        drawCircle(
            color = skin.glowColor.copy(
                alpha = (if (snake.isBoosting) 0.30f else 0.15f) * boostWave * shieldAlpha,
            ),
            radius = radius * (if (snake.isBoosting) 1.78f else 1.48f),
            center = center,
        )
        drawCircle(
            color = ringColor.copy(alpha = shieldAlpha),
            radius = radius,
            center = center,
        )
        drawCircle(
            color = skin.coreColor.copy(alpha = (if (snake.isBoosting) 0.72f else 0.40f) * shieldAlpha),
            radius = radius * 0.46f,
            center = center,
        )
    }

    // Snake Head
    val headCenter = Offset(snake.headX, snake.headY)
    val headR = snake.headRadius
    val angle = snake.angleRadians

    drawCircle(
        color = skin.glowColor.copy(alpha = (if (snake.isBoosting) 0.42f else 0.25f) * shieldAlpha),
        radius = headR * 1.95f,
        center = headCenter,
    )
    drawCircle(
        color = skin.primaryColor.copy(alpha = shieldAlpha),
        radius = headR,
        center = headCenter,
    )
    drawCircle(
        color = skin.coreColor.copy(alpha = 0.55f * shieldAlpha),
        radius = headR * 0.62f,
        center = headCenter,
    )

    // Spawn protection dashed neon ring
    if (isShielded) {
        drawCircle(
            color = Color.White.copy(alpha = 0.78f),
            radius = headR * 1.68f,
            center = headCenter,
            style = Stroke(
                width = 2.6f,
                pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 8f), worldTime * 50f),
            ),
        )
    }

    // Eyes tracking movement angle
    val eyeForwardDist = headR * 0.46f
    val eyeSpreadRad = 0.52f
    val eyeRadius = headR * 0.30f
    val pupilRadius = eyeRadius * 0.54f
    val pupilLookDist = eyeRadius * 0.34f

    val lookDx = cos(angle) * pupilLookDist
    val lookDy = sin(angle) * pupilLookDist

    val leftEyePos = Offset(
        x = headCenter.x + cos(angle - eyeSpreadRad) * eyeForwardDist,
        y = headCenter.y + sin(angle - eyeSpreadRad) * eyeForwardDist,
    )
    val rightEyePos = Offset(
        x = headCenter.x + cos(angle + eyeSpreadRad) * eyeForwardDist,
        y = headCenter.y + sin(angle + eyeSpreadRad) * eyeForwardDist,
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

    // #1 Leader Crown above head
    if (snake.isLeader) {
        val crownBaseY = headCenter.y - headR - 14f
        val crownPath = Path().apply {
            moveTo(headCenter.x - 11f, crownBaseY)
            lineTo(headCenter.x - 13f, crownBaseY - 12f)
            lineTo(headCenter.x - 5f, crownBaseY - 5f)
            lineTo(headCenter.x, crownBaseY - 15f)
            lineTo(headCenter.x + 5f, crownBaseY - 5f)
            lineTo(headCenter.x + 13f, crownBaseY - 12f)
            lineTo(headCenter.x + 11f, crownBaseY)
            close()
        }
        drawPath(path = crownPath, color = SolarGold)
    }

    // Floating Bot Nameplate
    if (!snake.isPlayer) {
        val labelY = headCenter.y - headR - (if (snake.isLeader) 34f else 14f)
        drawContext.canvas.nativeCanvas.drawText(
            snake.displayName,
            headCenter.x,
            labelY,
            labelPaint,
        )
    }
}
