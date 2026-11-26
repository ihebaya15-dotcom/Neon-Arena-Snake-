package com.example.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.game.model.ArenaConfig
import com.example.game.model.MiniMapBlip
import com.example.ui.theme.DangerCrimson
import com.example.ui.theme.GridLineBlue
import com.example.ui.theme.GridMajorBlue
import com.example.ui.theme.HudGlassSurface
import com.example.ui.theme.NeonBlue
import com.example.ui.theme.NeonCyan
import com.example.ui.theme.SolarGold
import kotlin.math.cos
import kotlin.math.sin

/**
 * Corner radar Mini-map displaying the player's live position and heading
 * relative to the 7000x7000 arena boundaries, alongside food cluster blips.
 */
@Composable
fun MiniMap(
    playerX: Float,
    playerY: Float,
    playerAngleRadians: Float,
    accentColor: Color,
    foodBlips: List<MiniMapBlip>,
    modifier: Modifier = Modifier,
    arenaWidth: Float = ArenaConfig.ARENA_WIDTH,
    arenaHeight: Float = ArenaConfig.ARENA_HEIGHT,
    mapSize: Dp = 112.dp,
) {
    val miniMapDescription = stringResource(R.string.hud_minimap_cd)
    val normX = (playerX / arenaWidth.coerceAtLeast(1f)).coerceIn(0f, 1f)
    val normY = (playerY / arenaHeight.coerceAtLeast(1f)).coerceIn(0f, 1f)

    Box(
        modifier = modifier
            .size(mapSize)
            .clip(RoundedCornerShape(14.dp))
            .background(HudGlassSurface)
            .border(1.5.dp, NeonBlue.copy(alpha = 0.85f), RoundedCornerShape(14.dp))
            .padding(6.dp)
            .semantics { contentDescription = miniMapDescription }
            .testTag("minimap"),
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height

            // 7000x7000 sector grid (7x7 sectors of 1000 world units each)
            val sectors = 7
            val cellW = w / sectors
            val cellH = h / sectors
            for (i in 1 until sectors) {
                drawLine(
                    color = GridLineBlue.copy(alpha = 0.65f),
                    start = Offset(cellW * i, 0f),
                    end = Offset(cellW * i, h),
                    strokeWidth = 1f,
                )
                drawLine(
                    color = GridLineBlue.copy(alpha = 0.65f),
                    start = Offset(0f, cellH * i),
                    end = Offset(w, cellH * i),
                    strokeWidth = 1f,
                )
            }

            // Center crosshairs
            drawLine(
                color = GridMajorBlue.copy(alpha = 0.7f),
                start = Offset(w * 0.5f, 0f),
                end = Offset(w * 0.5f, h),
                strokeWidth = 1f,
            )
            drawLine(
                color = GridMajorBlue.copy(alpha = 0.7f),
                start = Offset(0f, h * 0.5f),
                end = Offset(w, h * 0.5f),
                strokeWidth = 1f,
            )

            // Neon blue inner boundary frame
            drawRect(
                color = NeonBlue.copy(alpha = 0.45f),
                topLeft = Offset.Zero,
                size = Size(w, h),
                style = Stroke(width = 1.5f),
            )

            // Food energy blips
            for (i in foodBlips.indices) {
                val blip = foodBlips[i]
                val bx = blip.normX * w
                val by = blip.normY * h
                drawCircle(
                    color = if (blip.isLargeOrb) {
                        SolarGold.copy(alpha = 0.88f)
                    } else {
                        NeonCyan.copy(alpha = 0.34f)
                    },
                    radius = if (blip.isLargeOrb) 2.3f else 1.2f,
                    center = Offset(bx, by),
                )
            }

            // Player position relative to 7000x7000 bounds
            val px = normX * w
            val py = normY * h

            // Proximity warning halo when near arena wall
            val nearWall = normX < 0.06f || normX > 0.94f || normY < 0.06f || normY > 0.94f
            drawCircle(
                color = (if (nearWall) DangerCrimson else accentColor).copy(alpha = 0.34f),
                radius = if (nearWall) 11f else 8.5f,
                center = Offset(px, py),
            )

            // Directional heading chevron
            val tipX = px + cos(playerAngleRadians) * 7.0f
            val tipY = py + sin(playerAngleRadians) * 7.0f
            val leftX = px + cos(playerAngleRadians + 2.45f) * 5.2f
            val leftY = py + sin(playerAngleRadians + 2.45f) * 5.2f
            val rightX = px + cos(playerAngleRadians - 2.45f) * 5.2f
            val rightY = py + sin(playerAngleRadians - 2.45f) * 5.2f

            val pointerPath = Path().apply {
                moveTo(tipX, tipY)
                lineTo(leftX, leftY)
                lineTo(rightX, rightY)
                close()
            }
            drawPath(path = pointerPath, color = Color.White)
            drawCircle(
                color = accentColor,
                radius = 2.6f,
                center = Offset(px, py),
            )
        }
    }
}
