package com.example

import com.example.game.engine.ArenaEngine
import com.example.game.engine.EngineStepOutcome
import com.example.game.engine.SnakeController
import com.example.game.model.ArenaConfig
import com.example.game.model.BotDifficulty
import com.example.game.model.ControlInput
import com.example.game.model.DeathReason
import com.example.game.model.EyeStyle
import com.example.game.model.FoodOrb
import com.example.game.model.GameResult
import com.example.game.model.PathRingBuffer
import com.example.game.model.PlayerProgress
import com.example.game.model.SnakeEntity
import com.example.game.model.SnakeSkins
import com.example.game.model.SparksEconomy
import com.example.game.model.TrailStyle
import com.example.game.model.UiAccentPreset
import com.example.game.model.Vec2
import com.example.ui.viewmodel.GameViewModel
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

    @Test
    fun botAiController_evaluatesAngleSafetyUsingCurrentSnakeHeadingBeforeFallback() {
        val grid = com.example.game.engine.ArenaSpatialGrid()
        val westAngle = kotlin.math.PI.toFloat()
        val bot = SnakeEntity(
            id = "bot_heading_test",
            displayName = "TEST",
            isPlayer = false,
            skin = SnakeSkins.CYBER_CYAN,
            headX = 2000f,
            headY = 2000f,
            angleRadians = westAngle,
            currentSpeed = ArenaConfig.BASE_SPEED,
            targetLength = 20f,
            score = 100,
            foodEatenCount = 0,
            spawnProtectionSec = 0f,
            segments = MutableList(20) { idx -> Vec2(2000f + idx * ArenaConfig.SEGMENT_SPACING, 2000f) },
        )

        // Obstacle sits 110 units away at westAngle - 1.15 rad: blocks the -0.92 rad ray (~107 units)
        // while staying > 73 units clear of the inner -0.42 rad ray.
        val blockerOffsetAngle = westAngle - 1.15f
        val blockerSegs = MutableList(10) { idx ->
            val dist = 110f + idx * 10f
            Vec2(
                2000f + kotlin.math.cos(blockerOffsetAngle) * dist,
                2000f + kotlin.math.sin(blockerOffsetAngle) * dist,
            )
        }
        val blocker = SnakeEntity(
            id = "enemy_blocker",
            displayName = "BLOCKER",
            isPlayer = false,
            skin = SnakeSkins.PLASMA_MAGENTA,
            headX = blockerSegs.first().x,
            headY = blockerSegs.first().y,
            angleRadians = blockerOffsetAngle,
            currentSpeed = ArenaConfig.BASE_SPEED,
            targetLength = 10f,
            score = 100,
            foodEatenCount = 0,
            spawnProtectionSec = 0f,
            segments = blockerSegs,
        )

        // High-value orb on the blocked -0.92 rad flank (South-West: sin > 0),
        // moderate orb on the safe +0.92 rad flank (North-West: sin < 0).
        val blockedAngle = westAngle - 0.92f
        val blockedLootOrb = FoodOrb(
            id = 1,
            x = 2000f + kotlin.math.cos(blockedAngle) * 210f,
            y = 2000f + kotlin.math.sin(blockedAngle) * 210f,
            radius = 12f,
            points = 120,
            growthValue = 1.5f,
            colorIndex = 0,
            pulsePhase = 0f,
            isLootDrop = true,
        )
        val safeAngle = westAngle + 0.92f
        val safeOrb = FoodOrb(
            id = 2,
            x = 2000f + kotlin.math.cos(safeAngle) * 220f,
            y = 2000f + kotlin.math.sin(safeAngle) * 220f,
            radius = 8f,
            points = 20,
            growthValue = 0.5f,
            colorIndex = 1,
            pulsePhase = 0f,
        )

        val world = object : com.example.game.engine.ArenaWorldView {
            override val arenaWidth = ArenaConfig.ARENA_WIDTH
            override val arenaHeight = ArenaConfig.ARENA_HEIGHT
            override val foodOrbs = listOf(blockedLootOrb, safeOrb)
            override val snakes = listOf(bot, blocker)
            override val spatialGrid = grid
        }
        grid.rebuildFoodGrid(world.foodOrbs)
        grid.rebuildSnakeGrid(world.snakes)

        val controller = com.example.game.engine.BotAiController(
            personality = com.example.game.model.BotPersonality.SCAVENGER,
            botSlot = 1, // frameCounter becomes 2 -> evaluates on very first sampleInput call
            random = kotlin.random.Random(42),
        )
        val input = controller.sampleInput(bot, world, 0.016f)

        // Must reject the blocked -0.92 rad loot orb (vectorY > 0) and steer toward the safe +0.92 rad orb (vectorY < -0.25)
        assertTrue(
            "Bot heading West must reject blocked -0.92 rad orb and steer North-West (vectorY=${input.vectorY})",
            input.vectorY < -0.25f,
        )
    }

    @Test
    fun botManager_initializesAtLeast75PercentAggressivePersonalities() {
        val engine = ArenaEngine()
        engine.startNewGame(SnakeSkins.CYBER_CYAN, currentBestScore = 0, spawnBots = true)

        val bots = engine.snakes.filter { !it.isPlayer }
        val aggressiveCount = bots.count {
            it.personality == com.example.game.model.BotPersonality.HUNTER ||
                it.personality == com.example.game.model.BotPersonality.TACTICIAN
        }
        assertTrue(
            "Expected >= 75% aggressive bots (15/20), got $aggressiveCount/${bots.size}",
            aggressiveCount * 4 >= bots.size * 3,
        )
    }

    @Test
    fun hunterBot_prioritizesPlayerWithin900UnitsAndBoostsAtMinPlus4() {
        val grid = com.example.game.engine.ArenaSpatialGrid()
        val hunterSegmentsCount = ArenaConfig.MIN_BOOST_SEGMENT_COUNT + 4 // 16 segments
        val hunter = SnakeEntity(
            id = "bot_hunter",
            displayName = "HUNTER-1",
            isPlayer = false,
            skin = SnakeSkins.PLASMA_MAGENTA,
            headX = 3000f,
            headY = 3500f,
            angleRadians = 0f,
            currentSpeed = ArenaConfig.BASE_SPEED,
            targetLength = hunterSegmentsCount.toFloat(),
            score = 160,
            foodEatenCount = 0,
            spawnProtectionSec = 0f,
            personality = com.example.game.model.BotPersonality.HUNTER,
            segments = MutableList(hunterSegmentsCount) { idx ->
                Vec2(3000f - idx * ArenaConfig.SEGMENT_SPACING, 3500f)
            },
        )

        // Rival bot sits 440 units away (below-left), while player sits 510 units ahead-right.
        // Because player gets 0.5x effective distance (255 < 440), hunter must choose the player.
        val rivalBot = SnakeEntity(
            id = "bot_rival",
            displayName = "RIVAL",
            isPlayer = false,
            skin = SnakeSkins.TOXIC_LIME,
            headX = 3000f,
            headY = 3940f,
            angleRadians = (kotlin.math.PI / 2.0).toFloat(),
            currentSpeed = ArenaConfig.BASE_SPEED,
            targetLength = 20f,
            score = 150,
            foodEatenCount = 0,
            spawnProtectionSec = 0f,
            segments = MutableList(20) { idx ->
                Vec2(3000f, 3940f - idx * ArenaConfig.SEGMENT_SPACING)
            },
        )
        val player = SnakeEntity(
            id = "local_player",
            displayName = "أنت",
            isPlayer = true,
            skin = SnakeSkins.CYBER_CYAN,
            headX = 3510f,
            headY = 3500f,
            angleRadians = (-kotlin.math.PI / 2.0).toFloat(), // moving North
            currentSpeed = ArenaConfig.BASE_SPEED,
            targetLength = 24f,
            score = 300,
            foodEatenCount = 5,
            spawnProtectionSec = 0f,
            segments = MutableList(24) { idx ->
                Vec2(3510f, 3500f + idx * ArenaConfig.SEGMENT_SPACING)
            },
        )

        val world = object : com.example.game.engine.ArenaWorldView {
            override val arenaWidth = ArenaConfig.ARENA_WIDTH
            override val arenaHeight = ArenaConfig.ARENA_HEIGHT
            override val foodOrbs = emptyList<FoodOrb>()
            override val snakes = listOf(hunter, rivalBot, player)
            override val spatialGrid = grid
        }
        grid.rebuildFoodGrid(world.foodOrbs)
        grid.rebuildSnakeGrid(world.snakes)

        val controller = com.example.game.engine.BotAiController(
            personality = com.example.game.model.BotPersonality.HUNTER,
            botSlot = 1,
            random = kotlin.random.Random(7),
        )
        val input = controller.sampleInput(hunter, world, 0.016f)

        // Should steer East & North (vectorX > 0, vectorY < 0) ahead of the North-moving player, with boost active
        assertTrue("Hunter should steer toward player cutoff (vectorX=${input.vectorX})", input.vectorX > 0.6f)
        assertTrue("Hunter should lead North ahead of player nose (vectorY=${input.vectorY})", input.vectorY < -0.15f)
        assertTrue("Hunter at MIN+4 segments should activate intercept boost", input.isBoostActive)
    }

    @Test
    fun tacticianBot_opportunisticallyInterceptsNearbyPlayer() {
        val grid = com.example.game.engine.ArenaSpatialGrid()
        val tactician = SnakeEntity(
            id = "bot_tactician",
            displayName = "TACTIC",
            isPlayer = false,
            skin = SnakeSkins.ULTRAVIOLET,
            headX = 3000f,
            headY = 3000f,
            angleRadians = 0f,
            currentSpeed = ArenaConfig.BASE_SPEED,
            targetLength = 22f,
            score = 220,
            foodEatenCount = 0,
            spawnProtectionSec = 0f,
            personality = com.example.game.model.BotPersonality.TACTICIAN,
            segments = MutableList(22) { idx ->
                Vec2(3000f - idx * ArenaConfig.SEGMENT_SPACING, 3000f)
            },
        )
        val player = SnakeEntity(
            id = "local_player",
            displayName = "أنت",
            isPlayer = true,
            skin = SnakeSkins.CYBER_CYAN,
            headX = 3360f,
            headY = 3020f,
            angleRadians = (kotlin.math.PI / 2.0).toFloat(), // moving South
            currentSpeed = ArenaConfig.BASE_SPEED,
            targetLength = 22f,
            score = 200,
            foodEatenCount = 2,
            spawnProtectionSec = 0f,
            segments = MutableList(22) { idx ->
                Vec2(3360f, 3020f - idx * ArenaConfig.SEGMENT_SPACING)
            },
        )

        val world = object : com.example.game.engine.ArenaWorldView {
            override val arenaWidth = ArenaConfig.ARENA_WIDTH
            override val arenaHeight = ArenaConfig.ARENA_HEIGHT
            override val foodOrbs = emptyList<FoodOrb>()
            override val snakes = listOf(tactician, player)
            override val spatialGrid = grid
        }
        grid.rebuildFoodGrid(world.foodOrbs)
        grid.rebuildSnakeGrid(world.snakes)

        val controller = com.example.game.engine.BotAiController(
            personality = com.example.game.model.BotPersonality.TACTICIAN,
            botSlot = 1,
            random = kotlin.random.Random(11),
        )
        val input = controller.sampleInput(tactician, world, 0.016f)

        assertTrue("Tactician should intercept ahead of South-moving player (vectorX=${input.vectorX})", input.vectorX > 0.6f)
        assertTrue("Tactician should lead South ahead of player nose (vectorY=${input.vectorY})", input.vectorY > 0.15f)
        assertTrue("Tactician should boost into opportunistic cutoff", input.isBoostActive)
    }

    @Test
    fun difficultyLevels_scaleAggressiveBotMixAcrossEasyMediumHard() {
        val engine = ArenaEngine()

        engine.startNewGame(
            SnakeSkins.CYBER_CYAN,
            currentBestScore = 0,
            spawnBots = true,
            difficulty = com.example.game.model.BotDifficulty.EASY,
        )
        val easyAggressive = engine.snakes.count {
            !it.isPlayer && (
                it.personality == com.example.game.model.BotPersonality.HUNTER ||
                    it.personality == com.example.game.model.BotPersonality.TACTICIAN
                )
        }
        assertEquals(9, easyAggressive) // 45% of 20

        engine.startNewGame(
            SnakeSkins.CYBER_CYAN,
            currentBestScore = 0,
            spawnBots = true,
            difficulty = com.example.game.model.BotDifficulty.MEDIUM,
        )
        val mediumAggressive = engine.snakes.count {
            !it.isPlayer && (
                it.personality == com.example.game.model.BotPersonality.HUNTER ||
                    it.personality == com.example.game.model.BotPersonality.TACTICIAN
                )
        }
        assertEquals(15, mediumAggressive) // 75% of 20

        engine.startNewGame(
            SnakeSkins.CYBER_CYAN,
            currentBestScore = 0,
            spawnBots = true,
            difficulty = com.example.game.model.BotDifficulty.HARD,
        )
        val hardAggressive = engine.snakes.count {
            !it.isPlayer && (
                it.personality == com.example.game.model.BotPersonality.HUNTER ||
                    it.personality == com.example.game.model.BotPersonality.TACTICIAN
                )
        }
        assertEquals(18, hardAggressive) // 90% of 20
    }

    @Test
    fun difficultyLevels_adjustReactionCadenceSenseRangeAndBoostDuration() {
        val grid = com.example.game.engine.ArenaSpatialGrid()
        val segCount = 26

        fun createHunter(id: String) = SnakeEntity(
            id = id,
            displayName = id,
            isPlayer = false,
            skin = SnakeSkins.PLASMA_MAGENTA,
            headX = 3000f,
            headY = 3500f,
            angleRadians = 0f,
            currentSpeed = ArenaConfig.BASE_SPEED,
            targetLength = segCount.toFloat(),
            score = 260,
            foodEatenCount = 0,
            spawnProtectionSec = 0f,
            personality = com.example.game.model.BotPersonality.HUNTER,
            segments = MutableList(segCount) { idx ->
                Vec2(3000f - idx * ArenaConfig.SEGMENT_SPACING, 3500f)
            },
        )

        // Place player 320 units ahead (inside both Easy 388f and Hard 675f boost engage ranges)
        val player = SnakeEntity(
            id = "local_player",
            displayName = "أنت",
            isPlayer = true,
            skin = SnakeSkins.CYBER_CYAN,
            headX = 3320f,
            headY = 3500f,
            angleRadians = (-kotlin.math.PI / 2.0).toFloat(),
            currentSpeed = ArenaConfig.BASE_SPEED,
            targetLength = 24f,
            score = 300,
            foodEatenCount = 0,
            spawnProtectionSec = 0f,
            segments = MutableList(24) { idx ->
                Vec2(3320f, 3500f + idx * ArenaConfig.SEGMENT_SPACING)
            },
        )

        val easyBot = createHunter("easy_hunter")
        val hardBot = createHunter("hard_hunter")
        val easyGrid = com.example.game.engine.ArenaSpatialGrid()
        val hardGrid = com.example.game.engine.ArenaSpatialGrid()

        val easyWorld = object : com.example.game.engine.ArenaWorldView {
            override val arenaWidth = ArenaConfig.ARENA_WIDTH
            override val arenaHeight = ArenaConfig.ARENA_HEIGHT
            override val foodOrbs = emptyList<FoodOrb>()
            override val snakes = listOf(easyBot, player)
            override val spatialGrid = easyGrid
        }
        val hardWorld = object : com.example.game.engine.ArenaWorldView {
            override val arenaWidth = ArenaConfig.ARENA_WIDTH
            override val arenaHeight = ArenaConfig.ARENA_HEIGHT
            override val foodOrbs = emptyList<FoodOrb>()
            override val snakes = listOf(hardBot, player)
            override val spatialGrid = hardGrid
        }
        easyGrid.rebuildFoodGrid(easyWorld.foodOrbs)
        easyGrid.rebuildSnakeGrid(easyWorld.snakes)
        hardGrid.rebuildFoodGrid(hardWorld.foodOrbs)
        hardGrid.rebuildSnakeGrid(hardWorld.snakes)

        val easyController = com.example.game.engine.BotAiController(
            personality = com.example.game.model.BotPersonality.HUNTER,
            botSlot = 0,
            difficulty = com.example.game.model.BotDifficulty.EASY,
            random = kotlin.random.Random(1),
        )
        val hardController = com.example.game.engine.BotAiController(
            personality = com.example.game.model.BotPersonality.HUNTER,
            botSlot = 0,
            difficulty = com.example.game.model.BotDifficulty.HARD,
            random = kotlin.random.Random(1),
        )

        // Frame 1 triggers initial boost on both bots because cachedInput starts as IDLE.
        val easyStart = easyController.sampleInput(easyBot, easyWorld, 0.016f)
        val hardStart = hardController.sampleInput(hardBot, hardWorld, 0.016f)
        assertTrue(easyStart.isBoostActive)
        assertTrue(hardStart.isBoostActive)

        // Advance time by 0.75s (beyond Easy max 0.65s, well inside Hard min 0.95s)
        // Use a far player so neither bot re-triggers a fresh boost after initial burst.
        player.headX = 6200f
        easyGrid.rebuildSnakeGrid(easyWorld.snakes)
        hardGrid.rebuildSnakeGrid(hardWorld.snakes)

        val easyAfter750ms = easyController.sampleInput(easyBot, easyWorld, 0.75f)
        val hardAfter750ms = hardController.sampleInput(hardBot, hardWorld, 0.75f)

        assertFalse("Easy boost should expire within <= 0.65s", easyAfter750ms.isBoostActive)
        assertTrue("Hard boost should stay active beyond 0.75s (>= 0.95s)", hardAfter750ms.isBoostActive)

        // Verify Hard re-evaluates every single frame (interval=1) while Easy waits 4 frames
        assertTrue(
            com.example.game.model.BotDifficulty.HARD.reactionIntervalFrames <
                com.example.game.model.BotDifficulty.MEDIUM.reactionIntervalFrames &&
                com.example.game.model.BotDifficulty.MEDIUM.reactionIntervalFrames <
                com.example.game.model.BotDifficulty.EASY.reactionIntervalFrames,
        )
    }

    @Test
    fun sparksEconomy_scalesWithDurationEliminationsAndDifficulty() {
        val baseResult = GameResult(
            finalScore = 500,
            peakLength = 40,
            survivedSeconds = 40,
            foodEaten = 12,
            eliminationsCount = 2,
            bestRank = 3,
            isNewBest = false,
            deathReason = DeathReason.ARENA_BOUNDARY,
        )
        val easySparks = SparksEconomy.calculateMatchReward(baseResult, BotDifficulty.EASY)
        val mediumSparks = SparksEconomy.calculateMatchReward(baseResult, BotDifficulty.MEDIUM)
        val hardSparks = SparksEconomy.calculateMatchReward(baseResult, BotDifficulty.HARD)

        // (40s * 0.5 = 20) + (2 * 15 = 30) = 50 -> Easy=50, Medium=63, Hard=75
        assertEquals(50, easySparks)
        assertEquals(63, mediumSparks)
        assertEquals(75, hardSparks)

        val shortSurvival = baseResult.copy(finalScore = 0, eliminationsCount = 0, survivedSeconds = 10, foodEaten = 0)
        assertEquals(5, SparksEconomy.calculateMatchReward(shortSurvival, BotDifficulty.EASY))

        val instantQuit = baseResult.copy(finalScore = 0, eliminationsCount = 0, survivedSeconds = 0, foodEaten = 0)
        assertEquals(0, SparksEconomy.calculateMatchReward(instantQuit, BotDifficulty.EASY))
    }

    @Test
    fun playerProgress_coordinatedAccentTracksSkinOrExplicitPreset() {
        val synced = PlayerProgress(
            equippedSkinId = SnakeSkins.PLASMA_MAGENTA.id,
            accentPresetId = UiAccentPreset.SYNC_WITH_SKIN.id,
        )
        assertEquals(SnakeSkins.PLASMA_MAGENTA.primaryColor, synced.coordinatedAccentColor)

        val customPreset = synced.copy(accentPresetId = UiAccentPreset.SOLAR_GOLD.id)
        assertEquals(UiAccentPreset.SOLAR_GOLD.color, customPreset.coordinatedAccentColor)
    }

    @Test
    fun gameViewModel_storePurchasesAndCustomizationPropagateToEngine() {
        val viewModel = GameViewModel(persistenceManager = null)
        val initialBalance = viewModel.uiState.value.progress.sparksBalance
        assertEquals(SparksEconomy.INITIAL_STARTER_SPARKS, initialBalance)

        // Purchase Plasma Magenta (120 sparks) -> balance becomes 60, skin equipped
        viewModel.purchaseOrEquipSkin(SnakeSkins.PLASMA_MAGENTA)
        assertEquals(60, viewModel.uiState.value.progress.sparksBalance)
        assertTrue(SnakeSkins.PLASMA_MAGENTA.id in viewModel.uiState.value.progress.unlockedSkinIds)
        assertEquals(SnakeSkins.PLASMA_MAGENTA, viewModel.uiState.value.selectedSkin)
        assertEquals(SnakeSkins.PLASMA_MAGENTA.primaryColor, viewModel.uiState.value.coordinatedAccentColor)

        // Attempting to buy an expensive trail (190 sparks) with only 60 sparks must be rejected
        viewModel.purchaseOrEquipTrail(TrailStyle.PULSE_RINGS)
        assertEquals(60, viewModel.uiState.value.progress.sparksBalance)
        assertFalse(TrailStyle.PULSE_RINGS.id in viewModel.uiState.value.progress.unlockedTrailIds)

        // Start a game and verify selected skin/trail/eye propagate to ArenaEngine playerSnake
        viewModel.purchaseOrEquipEye(EyeStyle.CYBER_ROUND)
        viewModel.startGame(BotDifficulty.MEDIUM)
        val player = viewModel.engine.playerSnake!!
        assertEquals(SnakeSkins.PLASMA_MAGENTA, player.skin)
        assertEquals(TrailStyle.CLASSIC_GLOW, player.trailStyle)
        assertEquals(EyeStyle.CYBER_ROUND, player.eyeStyle)
    }
}
