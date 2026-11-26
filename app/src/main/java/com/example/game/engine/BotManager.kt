package com.example.game.engine

import com.example.game.model.ArenaConfig
import com.example.game.model.BotPersonality
import com.example.game.model.PathRingBuffer
import com.example.game.model.SnakeEntity
import com.example.game.model.SnakeSkins
import com.example.game.model.Vec2
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.sin
import kotlin.random.Random

/**
 * Maintains 18–22 active AI bots inside the 7000x7000 arena, spawning them at safe
 * random positions and instantly respawning replacements whenever a bot collides with
 * arena walls or another snake.
 */
class BotManager(
    private val minBots: Int = ArenaConfig.MIN_BOT_COUNT,
    private val targetBots: Int = ArenaConfig.TARGET_BOT_COUNT,
    private val maxBots: Int = ArenaConfig.MAX_BOT_COUNT,
    private val random: Random = Random.Default,
) {
    private var nextBotSerial = 1

    init {
        require(minBots in 1..targetBots && targetBots <= maxBots) {
            "Bot bounds must satisfy 1 <= minBots ($minBots) <= targetBots ($targetBots) <= maxBots ($maxBots)"
        }
    }

    fun initializeBots(
        playerSnake: SnakeEntity,
        snakes: MutableList<SnakeEntity>,
        controllers: MutableMap<String, SnakeController>,
        botSlotById: MutableMap<String, Int>,
    ) {
        nextBotSerial = 1
        for (slot in 0 until targetBots) {
            val (initialSegs, initialScore) = when {
                slot < 3 -> (78 + random.nextInt(42)) to (950 + random.nextInt(650))
                slot < 10 -> (34 + random.nextInt(24)) to (260 + random.nextInt(380))
                else -> (16 + random.nextInt(12)) to random.nextInt(140)
            }
            val (bot, controller) = createBot(
                slot = slot,
                playerSnake = playerSnake,
                occupiedSnakes = snakes,
                initialSegmentsCount = initialSegs,
                initialScore = initialScore,
                withSpawnShield = false,
            )
            snakes.add(bot)
            controllers[bot.id] = controller
            botSlotById[bot.id] = slot
        }
    }

    /**
     * Instantly replaces an eliminated bot (from wall or snake collision) with a freshly
     * spawned bot at a random safe arena position, keeping active count within [minBots, maxBots].
     */
    fun respawnEliminatedBot(
        eliminatedBot: SnakeEntity,
        playerSnake: SnakeEntity?,
        snakes: MutableList<SnakeEntity>,
        controllers: MutableMap<String, SnakeController>,
        botSlotById: MutableMap<String, Int>,
    ) {
        val slot = botSlotById.remove(eliminatedBot.id) ?: 0
        controllers.remove(eliminatedBot.id)

        val currentActiveBots = activeBotCount(snakes)
        val idx = snakes.indexOfFirst { it.id == eliminatedBot.id }
        if (currentActiveBots >= maxBots) {
            if (idx >= 0) snakes.removeAt(idx)
            return
        }

        val (replacement, controller) = createReplacementBot(
            slot = slot,
            playerSnake = playerSnake,
            occupiedSnakes = snakes,
        )
        if (idx >= 0) {
            snakes[idx] = replacement
        } else {
            snakes.add(replacement)
        }
        controllers[replacement.id] = controller
        botSlotById[replacement.id] = slot
    }

    /**
     * Guarantees active bot population remains between [minBots] (18) and [maxBots] (22).
     */
    fun ensurePopulationInRange(
        playerSnake: SnakeEntity?,
        snakes: MutableList<SnakeEntity>,
        controllers: MutableMap<String, SnakeController>,
        botSlotById: MutableMap<String, Int>,
    ) {
        var active = activeBotCount(snakes)
        var slot = active
        while (active < targetBots && active < maxBots) {
            val (bot, controller) = createReplacementBot(
                slot = slot % targetBots,
                playerSnake = playerSnake,
                occupiedSnakes = snakes,
            )
            snakes.add(bot)
            controllers[bot.id] = controller
            botSlotById[bot.id] = slot % targetBots
            active++
            slot++
        }
        if (active < minBots) {
            while (active < minBots) {
                val (bot, controller) = createReplacementBot(
                    slot = active % targetBots,
                    playerSnake = playerSnake,
                    occupiedSnakes = snakes,
                )
                snakes.add(bot)
                controllers[bot.id] = controller
                botSlotById[bot.id] = active % targetBots
                active++
            }
        }
    }

    fun activeBotCount(snakes: List<SnakeEntity>): Int {
        var count = 0
        for (i in snakes.indices) {
            val s = snakes[i]
            if (!s.isPlayer && s.isAlive) count++
        }
        return count
    }

    private fun createReplacementBot(
        slot: Int,
        playerSnake: SnakeEntity?,
        occupiedSnakes: List<SnakeEntity>,
    ): Pair<SnakeEntity, BotAiController> {
        val initialSegs = 18 + random.nextInt(22)
        val initialScore = initialSegs * 8 + random.nextInt(90)
        return createBot(
            slot = slot,
            playerSnake = playerSnake,
            occupiedSnakes = occupiedSnakes,
            initialSegmentsCount = initialSegs,
            initialScore = initialScore,
            withSpawnShield = true,
        )
    }

    private fun createBot(
        slot: Int,
        playerSnake: SnakeEntity?,
        occupiedSnakes: List<SnakeEntity>,
        initialSegmentsCount: Int,
        initialScore: Int,
        withSpawnShield: Boolean,
    ): Pair<SnakeEntity, BotAiController> {
        val (spawnX, spawnY) = findRandomSafeSpawnPoint(playerSnake, occupiedSnakes)
        val centerAngle = atan2(
            ArenaConfig.ARENA_HEIGHT * 0.5f - spawnY,
            ArenaConfig.ARENA_WIDTH * 0.5f - spawnX,
        )
        val startAngle = centerAngle + (random.nextFloat() - 0.5f) * 1.4f

        val segments = MutableList(initialSegmentsCount) { idx ->
            Vec2(
                x = spawnX - cos(startAngle) * (idx * ArenaConfig.SEGMENT_SPACING),
                y = spawnY - sin(startAngle) * (idx * ArenaConfig.SEGMENT_SPACING),
            )
        }

        val totalPathDist = (initialSegmentsCount + 4) * ArenaConfig.SEGMENT_SPACING
        val sampleCount = (totalPathDist / ArenaConfig.PATH_SAMPLE_STEP).toInt().coerceAtLeast(2)
        val pathBuffer = PathRingBuffer(initialCapacity = maxOf(1024, sampleCount * 2))
        for (stepIdx in 0..sampleCount) {
            val distFromHead = (sampleCount - stepIdx) * ArenaConfig.PATH_SAMPLE_STEP
            pathBuffer.pushNewest(
                x = spawnX - cos(startAngle) * distFromHead,
                y = spawnY - sin(startAngle) * distFromHead,
            )
        }

        val personality = PERSONALITIES[slot % PERSONALITIES.size]
        val skin = SnakeSkins.all[(slot + random.nextInt(SnakeSkins.all.size)) % SnakeSkins.all.size]
        val serial = nextBotSerial++
        val callsign = BOT_CALLSIGNS[(serial - 1) % BOT_CALLSIGNS.size]

        val growthBonus = ((initialSegmentsCount - ArenaConfig.INITIAL_SEGMENT_COUNT) * 0.06f)
            .coerceIn(0f, 6.5f)

        val bot = SnakeEntity(
            id = "bot_${slot}_$serial",
            displayName = callsign,
            isPlayer = false,
            skin = skin,
            headX = spawnX,
            headY = spawnY,
            angleRadians = startAngle,
            currentSpeed = ArenaConfig.BASE_SPEED,
            targetLength = initialSegmentsCount.toFloat(),
            score = initialScore,
            foodEatenCount = 0,
            eliminationsCount = 0,
            isBoosting = false,
            spawnProtectionSec = if (withSpawnShield) ArenaConfig.SPAWN_PROTECTION_SEC else 0f,
            personality = personality,
            headRadius = ArenaConfig.BASE_HEAD_RADIUS + growthBonus,
            bodyRadius = ArenaConfig.BASE_BODY_RADIUS + growthBonus * 0.9f,
            segments = segments,
            pathHistory = pathBuffer,
        )

        val controller = BotAiController(
            personality = personality,
            botSlot = slot,
            random = random,
        )
        return bot to controller
    }

    private fun findRandomSafeSpawnPoint(
        playerSnake: SnakeEntity?,
        occupiedSnakes: List<SnakeEntity>,
    ): Pair<Float, Float> {
        val margin = ArenaConfig.BOT_SPAWN_MARGIN
        val spanX = ArenaConfig.ARENA_WIDTH - margin * 2f
        val spanY = ArenaConfig.ARENA_HEIGHT - margin * 2f

        var bestX = margin + random.nextFloat() * spanX
        var bestY = margin + random.nextFloat() * spanY
        var bestClearance = -1f

        repeat(16) {
            val cx = margin + random.nextFloat() * spanX
            val cy = margin + random.nextFloat() * spanY

            var minDist = Float.MAX_VALUE
            if (playerSnake != null) {
                val dPlayer = hypot(cx - playerSnake.headX, cy - playerSnake.headY)
                minDist = if (dPlayer < ArenaConfig.BOT_MIN_DIST_FROM_PLAYER) {
                    dPlayer * 0.4f
                } else {
                    minOf(minDist, dPlayer)
                }
            }

            for (i in occupiedSnakes.indices) {
                val other = occupiedSnakes[i]
                if (!other.isAlive) continue
                val dHead = hypot(cx - other.headX, cy - other.headY)
                if (dHead < minDist) minDist = dHead
            }

            if (minDist >= ArenaConfig.BOT_MIN_DIST_FROM_PLAYER) {
                return cx to cy
            }
            if (minDist > bestClearance) {
                bestClearance = minDist
                bestX = cx
                bestY = cy
            }
        }
        return bestX to bestY
    }

    companion object {
        private val PERSONALITIES = listOf(
            BotPersonality.HUNTER,
            BotPersonality.SCAVENGER,
            BotPersonality.TACTICIAN,
        )

        private val BOT_CALLSIGNS = listOf(
            "VIPER-X",
            "NOVA-7",
            "KRAKEN",
            "SPECTRE",
            "APEX-9",
            "COBALT",
            "VOLT",
            "PHANTOM",
            "HYDRA",
            "ORION",
            "BLITZ",
            "VECTOR",
            "ZEPHYR",
            "PULSE",
            "TITAN",
            "RONIN",
            "MIRAGE",
            "NEXUS",
            "PRISM",
            "VORTEX",
            "LYNX",
            "EMBER",
        )
    }
}
