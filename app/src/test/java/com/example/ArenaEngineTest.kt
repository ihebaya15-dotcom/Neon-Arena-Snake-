package com.example

import com.example.game.engine.ArenaEngine
import com.example.game.engine.EngineStepOutcome
import com.example.game.model.ArenaConfig
import com.example.game.model.DeathReason
import com.example.game.model.FoodOrb
import com.example.game.model.SnakeSkins
import kotlin.math.hypot
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ArenaEngineTest {
    @Test
    fun startNewGame_spawnsPlayerCenteredAnd750FoodOrbsInSpatialGrid() {
        val engine = ArenaEngine()
        engine.startNewGame(SnakeSkins.CYBER_CYAN, currentBestScore = 0)

        val player = engine.playerSnake
        assertNotNull(player)
        assertEquals(ArenaConfig.ARENA_WIDTH / 2f, player!!.headX, 0.01f)
        assertEquals(ArenaConfig.ARENA_HEIGHT / 2f, player.headY, 0.01f)
        assertEquals(ArenaConfig.INITIAL_SEGMENT_COUNT, player.segments.size)
        assertTrue(player.pathHistory.size > ArenaConfig.INITIAL_SEGMENT_COUNT)
        assertEquals(750, ArenaConfig.TARGET_FOOD_COUNT)
        assertEquals(ArenaConfig.TARGET_FOOD_COUNT, engine.foodOrbs.size)

        // Verify 7000x7000 partitioned into 500x500 cells (14x14 = 196 buckets)
        assertEquals(14, engine.spatialGrid.cols)
        assertEquals(14, engine.spatialGrid.rows)
        assertEquals(196, engine.spatialGrid.totalCells)

        val queriedSnakes = mutableListOf<String>()
        engine.spatialGrid.forEachSnakeInBounds(3000f, 3000f, 4000f, 4000f) { snake ->
            queriedSnakes.add(snake.id)
        }
        assertEquals(listOf(ArenaEngine.PLAYER_ID), queriedSnakes)
    }

    @Test
    fun spatialGrid_queriesOnlyLocal500x500Buckets() {
        val engine = ArenaEngine()
        engine.startNewGame(SnakeSkins.CYBER_CYAN, currentBestScore = 0)

        val nearOrb = FoodOrb(
            id = 9001,
            x = 260f,
            y = 260f,
            radius = 8f,
            points = 10,
            growthValue = 0.5f,
            colorIndex = 0,
            pulsePhase = 0f,
        )
        val farOrb = FoodOrb(
            id = 9002,
            x = 6400f,
            y = 6400f,
            radius = 8f,
            points = 10,
            growthValue = 0.5f,
            colorIndex = 0,
            pulsePhase = 0f,
        )
        engine.spatialGrid.rebuildFoodGrid(listOf(nearOrb, farOrb))

        val queried = mutableListOf<FoodOrb>()
        engine.spatialGrid.queryFoodInRadius(250f, 250f, 100f, queried)
        assertEquals(1, queried.size)
        assertEquals(9001, queried.first().id)
    }

    @Test
    fun joystickVector_rotatesSnakeAndBoostStopsAtMinimumLength() {
        val engine = ArenaEngine()
        engine.startNewGame(SnakeSkins.PLASMA_MAGENTA, currentBestScore = 0)

        engine.playerController.updateJoystickVector(1f, 0f)
        val toggledOn = engine.togglePlayerBoost()
        assertTrue(toggledOn)

        // Step repeatedly until boost drains snake to MIN_BOOST_SEGMENT_COUNT
        repeat(160) {
            engine.step(0.03f)
        }

        val player = engine.playerSnake!!
        assertFalse(player.isBoosting)
        assertFalse(engine.playerController.isBoostEnabled())
        assertTrue(player.segments.size >= ArenaConfig.MIN_BOOST_SEGMENT_COUNT)
        assertTrue(engine.foodOrbs.count { !it.isBoostDrop } >= ArenaConfig.TARGET_FOOD_COUNT)
    }

    @Test
    fun pathHistory_keepsConsistentSegmentSpacingOnSharpTurnWithoutFalseSelfCollision() {
        val engine = ArenaEngine()
        engine.startNewGame(SnakeSkins.SOLAR_GOLD, currentBestScore = 0)

        // Sharp right turn should curve smoothly without false-triggering self-collision on neck
        engine.playerController.updateJoystickVector(1f, 0f)
        var outcome: EngineStepOutcome = EngineStepOutcome.Running
        repeat(25) {
            outcome = engine.step(0.03f)
            assertTrue(outcome is EngineStepOutcome.Running)
        }

        val segments = engine.playerSnake!!.segments
        for (i in 1 until segments.size) {
            val prev = segments[i - 1]
            val curr = segments[i]
            val dist = hypot(curr.x - prev.x, curr.y - prev.y)
            assertTrue("Segment spacing $dist out of expected bounds", dist in 10f..14.5f)
        }
    }

    @Test
    fun loopingBackIntoBody_triggersSelfCollisionGameOver() {
        val engine = ArenaEngine()
        engine.startNewGame(SnakeSkins.ULTRAVIOLET, currentBestScore = 0)

        // Grow snake long enough to cross back over its own body trail
        val player = engine.playerSnake!!
        player.targetLength = 90f

        // Move straight up first to build a long vertical body trail
        engine.playerController.updateJoystickVector(0f, -1f)
        repeat(50) {
            val res = engine.step(0.03f)
            assertTrue(res is EngineStepOutcome.Running)
        }

        // Turn right, then down, then left sharply across the vertical body
        val directions = listOf(
            Triple(1f, 0f, 18),
            Triple(0f, 1f, 18),
            Triple(-1f, 0f, 35),
        )
        var outcome: EngineStepOutcome = EngineStepOutcome.Running
        for ((vx, vy, steps) in directions) {
            engine.playerController.updateJoystickVector(vx, vy)
            repeat(steps) {
                if (outcome is EngineStepOutcome.Running) {
                    outcome = engine.step(0.03f)
                }
            }
        }

        assertTrue("Expected game over from self-collision", outcome is EngineStepOutcome.Ended)
        val ended = outcome as EngineStepOutcome.Ended
        assertEquals(DeathReason.SELF_COLLISION, ended.result.deathReason)
    }

    @Test
    fun hittingArenaBoundary_endsRoundWithGameOver() {
        val engine = ArenaEngine()
        engine.startNewGame(SnakeSkins.TOXIC_LIME, currentBestScore = 0)
        assertEquals(7000f, engine.arenaWidth, 0.01f)
        assertEquals(7000f, engine.arenaHeight, 0.01f)

        // Move straight up (-PI/2) until hitting top wall (y <= 0)
        var outcome: EngineStepOutcome = EngineStepOutcome.Running
        repeat(600) {
            if (outcome is EngineStepOutcome.Running) {
                outcome = engine.step(0.03f)
            }
        }

        assertTrue(outcome is EngineStepOutcome.Ended)
        val ended = outcome as EngineStepOutcome.Ended
        assertEquals(DeathReason.ARENA_BOUNDARY, ended.result.deathReason)
    }
}
