package com.example.game.engine

import com.example.game.model.ControlInput
import com.example.game.model.FoodOrb
import com.example.game.model.SnakeEntity

/**
 * Read-only view of the arena world passed to any [SnakeController].
 * Enables Phase 2 AI Bots and Phase 3 network adapters to read arena state
 * without mutating engine internals.
 */
interface ArenaWorldView {
    val arenaWidth: Float
    val arenaHeight: Float
    val foodOrbs: List<FoodOrb>
    val snakes: List<SnakeEntity>
}

/**
 * Pluggable steering & boost controller port.
 * Phase 1: [HumanJoystickController]
 * Phase 2+: BotAiController, RemotePlayerController
 */
interface SnakeController {
    fun sampleInput(
        snake: SnakeEntity,
        world: ArenaWorldView,
        dtSeconds: Float,
    ): ControlInput
}

class HumanJoystickController : SnakeController {
    @Volatile
    private var vectorX: Float = 0f

    @Volatile
    private var vectorY: Float = 0f

    @Volatile
    private var boostToggleEnabled: Boolean = false

    fun updateJoystickVector(x: Float, y: Float) {
        vectorX = x.coerceIn(-1f, 1f)
        vectorY = y.coerceIn(-1f, 1f)
    }

    fun setBoostEnabled(enabled: Boolean) {
        boostToggleEnabled = enabled
    }

    fun isBoostEnabled(): Boolean = boostToggleEnabled

    fun reset() {
        vectorX = 0f
        vectorY = 0f
        boostToggleEnabled = false
    }

    override fun sampleInput(
        snake: SnakeEntity,
        world: ArenaWorldView,
        dtSeconds: Float,
    ): ControlInput {
        return ControlInput(
            vectorX = vectorX,
            vectorY = vectorY,
            isBoostActive = boostToggleEnabled,
        )
    }
}
