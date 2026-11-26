package com.example.game.model

import kotlin.math.atan2
import kotlin.math.hypot

/**
 * Normalized 2D control input produced by either the Virtual Joystick (human player),
 * an AI Bot controller (Phase 2), or a remote network player stream (Phase 3).
 *
 * [vectorX] and [vectorY] are normalized within the unit circle ([-1f, 1f], magnitude <= 1f).
 */
data class ControlInput(
    val vectorX: Float = 0f,
    val vectorY: Float = 0f,
    val isBoostActive: Boolean = false,
) {
    val magnitude: Float
        get() = hypot(vectorX, vectorY).coerceIn(0f, 1f)

    val hasDirection: Boolean
        get() = magnitude > DEADZONE

    val targetAngleRadians: Float
        get() = atan2(vectorY, vectorX)

    companion object {
        const val DEADZONE = 0.08f
        val IDLE = ControlInput(0f, 0f, false)
    }
}
