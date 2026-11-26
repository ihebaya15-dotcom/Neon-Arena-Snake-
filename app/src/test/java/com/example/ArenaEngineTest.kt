package com.example

import com.example.game.engine.ArenaEngine
import com.example.game.engine.EngineStepOutcome
import com.example.game.engine.SnakeController
import com.example.game.model.ArenaConfig
import com.example.game.model.ControlInput
import com.example.game.model.DeathReason
import com.example.game.model.FoodOrb
import com.example.game.model.PathRingBuffer
import com.example.game.model.SnakeEntity
import com.example.game.model.SnakeSkins
import com.example.game.model.Vec2
import kotlin.math.hypot
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test

class ArenaEngineTest {
    @Test
    fun startNewGame_spawnsPlayer20BotsAnd750FoodOrbsInSpatialGrid() {
        val engine = ArenaEngine()
        engine.startNewGame(SnakeSkins.CYBER_CYAN, currentBestScore = 0)

        val player = engine.playerSnake
        assertNotNull(player)
        assertEquals(ArenaConfig.ARENA_WIDTH / 2f, player!!.headX, 0.01f)
        assertEquals(ArenaConfig.ARENA_HEIGHT / 2f, player.headY, 0.01f)
        assertEquals(1 + ArenaConfig.TARGET_BOT_COUNT, engine.snakes.size)
        assertEquals(ArenaConfig.TARGET_FOOD_COUNT, engine.foodOrbs.size)

        // Verify all 20 bots spawn safely away from the player
        engine.snakes.filter { !it.isPlayer }.forEach { bot ->
            val d = hypot(bot.headX - player.headX, bot.headY - player.headY)
            assertTrue("Bot spawned too close to player: $d", d >= ArenaConfig.BOT_MIN_DIST_FROM_PLAYER - 1f)
        }

        val hud = engine.buildHudSnapshot(forceRefreshFoodBlips = true)
        assertEquals(3, hud.topThree.size)
        assertEquals(ArenaConfig.TARGET_BOT_COUNT, hud.botBlips.size)
        assertEquals(1, hud.botBlips.count { it.isLeader })
    }

    @Test
    fun botElimination_convertsBodyToLootAwardsPlayerAndRespawnsReplacementBot() {
        val engine = ArenaEngine()
        engine.startNewGame(SnakeSkins.CYBER_CYAN, currentBestScore = 0, spawnBots = false)

        val player = engine.playerSnake!!
        player.spawnProtectionSec = 0f

        // Register an enemy bot heading straight into the player's body segment at x=3500, y=3620
        val enemySegments = MutableList(16) { idx -> Vec2(3420f - idx * 13.5f, 3620f) }
        val enemy = SnakeEntity(
            id = "bot_test_victim",
            displayName = "TEST-BOT",
            isPlayer = false,
            skin = SnakeSkins.PLASMA_MAGENTA,
            headX = 3488f,
            headY = 3620f,
            angleRadians = 0f,
            currentSpeed = ArenaConfig.BASE_SPEED,
            targetLength = 16f,
            score = 120,
            foodEatenCount = 0,
            spawnProtectionSec = 0f,
            segments = enemySegments,
        )
        engine.registerSnake(
            enemy,
            object : SnakeController {
                override fun sampleInput(
                    snake: SnakeEntity,
                    world: com.example.game.engine.ArenaWorldView,
                    dtSeconds: Float,
                ): ControlInput = ControlInput(1f, 0f, false)
            },
        )

        // Step a couple frames until enemy head hits player body
        repeat(4) {
            val res = engine.step(0.03f)
            assertTrue(res is EngineStepOutcome.Running)
        }

        assertTrue("Player should score elimination", player.eliminationsCount >= 1)
        assertTrue("Player should receive elimination bonus", player.score >= ArenaConfig.ELIMINATION_BONUS_POINTS)
        assertTrue("Defeated snake should drop high-value loot orbs", engine.foodOrbs.any { it.isLootDrop })
        // Replacement bot should keep snake count intact
        assertEquals(2, engine.snakes.size)
        assertTrue(engine.snakes.none { it.id == "bot_test_victim" })
    }

    @Test
    fun playerHittingEnemyBody_endsRoundWithKillerName() {
        val engine = ArenaEngine()
        engine.startNewGame(SnakeSkins.SOLAR_GOLD, currentBestScore = 0, spawnBots = false)

        val player = engine.playerSnake!!
        player.spawnProtectionSec = 0f

        // Place an enemy bot horizontally right across the player's upward path (y = 3470)
        val enemySegments = MutableList(18) { idx -> Vec2(3560f - idx * 13.5f, 3470f) }
        val enemy = SnakeEntity(
            id = "bot_killer",
            displayName = "APEX-9",
            isPlayer = false,
            skin = SnakeSkins.ULTRAVIOLET,
            headX = 3650f,
            headY = 3470f,
            angleRadians = 0f,
            currentSpeed = ArenaConfig.BASE_SPEED,
            targetLength = 18f,
            score = 500,
            foodEatenCount = 0,
            spawnProtectionSec = 0f,
            segments = enemySegments,
        )
        engine.registerSnake(
            enemy,
            object : SnakeController {
                override fun sampleInput(
                    snake: SnakeEntity,
                    world: com.example.game.engine.ArenaWorldView,
                    dtSeconds: Float,
                ): ControlInput = ControlInput(1f, 0f, false)
            },
        )

        var outcome: EngineStepOutcome = EngineStepOutcome.Running
        repeat(10) {
            if (outcome is EngineStepOutcome.Running) {
                outcome = engine.step(0.03f)
            }
        }

        assertTrue(outcome is EngineStepOutcome.Ended)
        val ended = outcome as EngineStepOutcome.Ended
        assertEquals(DeathReason.ENEMY_BODY_COLLISION, ended.result.deathReason)
        assertEquals("APEX-9", ended.result.killerName)
    }

    @Test
    fun simulationWith20SmartBots_maintainsFullBotRosterAcross120Frames() {
        val engine = ArenaEngine()
        engine.startNewGame(SnakeSkins.CYBER_CYAN, currentBestScore = 0)

        repeat(120) {
            val outcome = engine.step(0.03f)
            if (outcome is EngineStepOutcome.Ended) return@repeat
        }

        assertEquals(1 + ArenaConfig.TARGET_BOT_COUNT, engine.snakes.size)
    }

    @Test
    fun pathRingBuffer_handlesWrapAroundAndGrowCleanly() {
        val buffer = PathRingBuffer(initialCapacity = 16)
        for (i in 0 until 24) {
            buffer.pushNewest(i.toFloat(), i * 2f)
            if (buffer.size > 10) {
                buffer.dropOldest(1)
            }
        }
        assertEquals(10, buffer.size)
        assertEquals(23f, buffer.xFromNewest(0), 0.01f)
        assertEquals(46f, buffer.yFromNewest(0), 0.01f)
        assertEquals(14f, buffer.oldestX(), 0.01f)

        for (i in 24 until 44) {
            buffer.pushNewest(i.toFloat(), i * 2f)
        }
        assertEquals(30, buffer.size)
        assertEquals(43f, buffer.xFromNewest(0), 0.01f)
        assertEquals(14f, buffer.oldestX(), 0.01f)
    }

    @Test
    fun spatialGrid_queriesOnlyLocal500x500Buckets() {
        val engine = ArenaEngine()
        engine.startNewGame(SnakeSkins.CYBER_CYAN, currentBestScore = 0, spawnBots = false)

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
    fun buildHudSnapshot_reusesCachedFoodBlipsAcrossIntermediateFrames() {
        val engine = ArenaEngine()
        engine.startNewGame(SnakeSkins.CYBER_CYAN, currentBestScore = 0, spawnBots = false)

        val first = engine.buildHudSnapshot()
        val second = engine.buildHudSnapshot()
        assertSame(first.foodBlips, second.foodBlips)
    }

    @Test
    fun joystickVector_rotatesSnakeAndBoostStopsAtMinimumLength() {
        val engine = ArenaEngine()
        engine.startNewGame(SnakeSkins.PLASMA_MAGENTA, currentBestScore = 0, spawnBots = false)

        engine.playerController.updateJoystickVector(1f, 0f)
        val toggledOn = engine.togglePlayerBoost()
        assertTrue(toggledOn)

        repeat(160) {
            engine.step(0.03f)
        }

        val player = engine.playerSnake!!
        assertFalse(player.isBoosting)
        assertFalse(engine.playerController.isBoostEnabled())
        assertTrue(player.segments.size >= ArenaConfig.MIN_BOOST_SEGMENT_COUNT)
        assertTrue(engine.foodOrbs.count { !it.isBoostDrop && !it.isLootDrop } >= ArenaConfig.TARGET_FOOD_COUNT)
    }

    @Test
    fun hittingArenaBoundary_endsRoundWithGameOver() {
        val engine = ArenaEngine()
        engine.startNewGame(SnakeSkins.TOXIC_LIME, currentBestScore = 0, spawnBots = false)

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

    @Test
    fun botManager_instantlyRespawnsWallCollidingBotAndKeeps18To22Bots() {
        val engine = ArenaEngine()
        engine.startNewGame(SnakeSkins.CYBER_CYAN, currentBestScore = 0, spawnBots = true)

        val initialBots = engine.snakes.filter { !it.isPlayer }
        assertTrue(initialBots.size in ArenaConfig.MIN_BOT_COUNT..ArenaConfig.MAX_BOT_COUNT)

        // Drive one bot outside the arena wall so it collides with the boundary
        val doomedBot = initialBots.first()
        val doomedId = doomedBot.id
        doomedBot.headX = 2f
        doomedBot.headY = 3500f
        doomedBot.angleRadians = kotlin.math.PI.toFloat()

        val outcome = engine.step(0.03f)
        assertTrue(outcome is EngineStepOutcome.Running)

        val botsAfterWallHit = engine.snakes.filter { !it.isPlayer && it.isAlive }
        assertTrue(
            "Active bot count (${botsAfterWallHit.size}) must stay in 18..22",
            botsAfterWallHit.size in ArenaConfig.MIN_BOT_COUNT..ArenaConfig.MAX_BOT_COUNT,
        )
        assertTrue("Wall-colliding bot should be replaced immediately", botsAfterWallHit.none { it.id == doomedId })

        val player = engine.playerSnake!!
        botsAfterWallHit.forEach { bot ->
            assertTrue(bot.headX in ArenaConfig.BOT_SPAWN_MARGIN..(ArenaConfig.ARENA_WIDTH - ArenaConfig.BOT_SPAWN_MARGIN))
            assertTrue(bot.headY in ArenaConfig.BOT_SPAWN_MARGIN..(ArenaConfig.ARENA_HEIGHT - ArenaConfig.BOT_SPAWN_MARGIN))
            assertTrue(hypot(bot.headX - player.headX, bot.headY - player.headY) >= 0f)
        }
    }
}
