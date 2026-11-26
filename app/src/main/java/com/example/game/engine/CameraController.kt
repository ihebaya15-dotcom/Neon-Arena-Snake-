package com.example.game.engine

import com.example.game.model.ArenaConfig
import com.example.game.model.CameraState
import com.example.game.model.SnakeEntity
import kotlin.math.cos
import kotlin.math.exp
import kotlin.math.sin

class CameraController {
    fun snapTo(camera: CameraState, target: SnakeEntity) {
        camera.x = target.headX
        camera.y = target.headY
        camera.zoom = computeTargetZoom(target)
    }

    fun update(camera: CameraState, target: SnakeEntity, dtSeconds: Float) {
        val lookAheadDist = if (target.isBoosting) 42f else 22f
        val desiredX = target.headX + cos(target.angleRadians) * lookAheadDist
        val desiredY = target.headY + sin(target.angleRadians) * lookAheadDist

        val positionAlpha = 1f - exp(-7.5f * dtSeconds)
        camera.x += (desiredX - camera.x) * positionAlpha
        camera.y += (desiredY - camera.y) * positionAlpha

        val targetZoom = computeTargetZoom(target)
        val zoomAlpha = 1f - exp(-3.5f * dtSeconds)
        camera.zoom += (targetZoom - camera.zoom) * zoomAlpha
    }

    private fun computeTargetZoom(target: SnakeEntity): Float {
        val extraSegments = (target.segments.size - ArenaConfig.INITIAL_SEGMENT_COUNT).coerceAtLeast(0)
        val lengthZoomOut = (extraSegments * 0.0014f).coerceAtMost(0.22f)
        val boostZoomOut = if (target.isBoosting) 0.045f else 0f
        return (1.0f - lengthZoomOut - boostZoomOut).coerceIn(0.74f, 1.0f)
    }
}
