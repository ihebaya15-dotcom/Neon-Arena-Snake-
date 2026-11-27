package com.example.game.engine

import com.example.game.model.ArenaConfig
import com.example.game.model.ArenaHudSnapshot
import com.example.game.model.BotDifficulty
import com.example.game.model.BotMapBlip
import com.example.game.model.CameraState
import com.example.game.model.ControlInput
import com.example.game.model.DeathBurstParticle
import com.example.game.model.DeathReason
import com.example.game.model.EyeStyle
import com.example.game.model.FoodOrb
import com.example.game.model.GameResult
import com.example.game.model.KillFeedEvent
import com.example.game.model.LeaderboardEntry
import com.example.game.model.MiniMapBlip
import com.example.game.model.PathRingBuffer
import com.example.game.model.SnakeEntity
import com.example.game.model.SnakeSkin
import com.example.game.model.SnakeSkins
import com.example.game.model.TrailStyle
import com.example.game.model.Vec2
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.sin
import kotlin.random.Random

sealed interface EngineStepOutcome {
    data object Running : EngineStepOutcome
    data class Ended(val result: GameResult) : EngineStepOutcome
}

class ArenaEngine(
    private val foodSpawner: FoodSpawner = FoodSpawner(),
    private val cameraController: CameraController = CameraController(),
    val botManager: BotManager = BotManager(),
    private val combatCollisionResolver: CombatCollisionResolver = CombatCollisionResolver(),
    override val spatialGrid: ArenaSpatialGrid = ArenaSpatialGrid(),
) : ArenaWorldView {

    override val arenaWidth: Float = ArenaConfig.ARENA_WIDTH
    override val arenaHeight: Float = ArenaConfig.ARENA_HEIGHT

    private val _foodOrbs = mutableListOf<FoodOrb>()
    override val foodOrbs: List<FoodOrb> get() = _foodOrbs

    private val _snakes = mutableListOf<SnakeEntity>()
    override val snakes: List<SnakeEntity> get() = _snakes

    private val _particles = mutableListOf<DeathBurstParticle>()
    val particles: List<DeathBurstParticle> get() = _particles

    private val controllers = mutableMapOf<String, SnakeController>()
    private val botSlotById = mutableMapOf<String, Int>()
    private val nearbyFoodQueryBuffer = ArrayList<FoodOrb>(32)
    private val eliminationsBuffer = ArrayList<EliminationRecord>(8)
    private val sortedLeaderboardBuffer = ArrayList<SnakeEntity>(24)

    val playerController = HumanJoystickController()
    val camera = CameraState()

    var worldTimeSeconds: Float = 0f
        private set

    private var elapsedPlaySeconds: Float = 0f
    private var peakLength: Int = ArenaConfig.INITIAL_SEGMENT_COUNT
    private var bestRankAchieved: Int = ArenaConfig.TARGET_BOT_COUNT + 1
    private var sessionBestScore: Int = 0
    private var boostDropAccumulatorSec: Float = 0f
    private var boostBlockedWarningTimerSec: Float = 0f

    private var cachedFoodBlips: List<MiniMapBlip> = emptyList()
    private var cachedBotBlips: List<BotMapBlip> = emptyList()
    private var cachedTopThree: List<LeaderboardEntry> = emptyList()
    private var cachedPlayerRankEntry: LeaderboardEntry? = null
    private var foodBlipRefreshCounter: Int = 0
    private var leaderboardRefreshCounter: Int = 0

    private var latestPlayerKill: KillFeedEvent? = null
    private var killBannerTimerSec: Float = 0f
    private var nextKillEventId: Long = 1L
    private var botsEnabled: Boolean = true
    var currentDifficulty: BotDifficulty = BotDifficulty.MEDIUM
        private set

    val playerSnake: SnakeEntity?
        get() = _snakes.firstOrNull { it.isPlayer }

    fun startNewGame(
        playerSkin: SnakeSkin,
        currentBestScore: Int,
        spawnBots: Boolean = true,
        difficulty: BotDifficulty = BotDifficulty.MEDIUM,
        playerTrail: TrailStyle = TrailStyle.CLASSIC_GLOW,
        playerEye: EyeStyle = EyeStyle.CYBER_ROUND,
    ) {
        currentDifficulty = difficulty
        sessionBestScore = currentBestScore
        elapsedPlaySeconds = 0f
        worldTimeSeconds = 0f
        boostDropAccumulatorSec = 0f
        boostBlockedWarningTimerSec = 0f
        latestPlayerKill = null
        killBannerTimerSec = 0f
        playerController.reset()

        _snakes.clear()
        _particles.clear()
        controllers.clear()
        botSlotById.clear()

        val startX = ArenaConfig.ARENA_WIDTH / 2f
        val startY = ArenaConfig.ARENA_HEIGHT / 2f
        val startAngle = (-PI / 2.0).toFloat()

        val initialSegments = MutableList(ArenaConfig.INITIAL_SEGMENT_COUNT) { idx ->
            Vec2(
                x = startX - cos(startAngle) * (idx * ArenaConfig.SEGMENT_SPACING),
                y = startY - sin(startAngle) * (idx * ArenaConfig.SEGMENT_SPACING),
            )
        }

        val totalInitialPathDist = (ArenaConfig.INITIAL_SEGMENT_COUNT + 4) * ArenaConfig.SEGMENT_SPACING
        val sampleCount = (totalInitialPathDist / ArenaConfig.PATH_SAMPLE_STEP).toInt().coerceAtLeast(2)
        val initialPath = PathRingBuffer(initialCapacity = 1024)
        for (stepIdx in 0..sampleCount) {
            val distFromHead = (sampleCount - stepIdx) * ArenaConfig.PATH_SAMPLE_STEP
            initialPath.pushNewest(
                x = startX - cos(startAngle) * distFromHead,
                y = startY - sin(startAngle) * distFromHead,
            )
        }

        val player = SnakeEntity(
            id = PLAYER_ID,
            displayName = "أنت",
            isPlayer = true,
            skin = playerSkin,
            headX = startX,
            headY = startY,
            angleRadians = startAngle,
            currentSpeed = ArenaConfig.BASE_SPEED,
            targetLength = ArenaConfig.INITIAL_SEGMENT_COUNT.toFloat(),
            score = 0,
            foodEatenCount = 0,
            eliminationsCount = 0,
            isBoosting = false,
            spawnProtectionSec = if (spawnBots) ArenaConfig.SPAWN_PROTECTION_SEC else 0f,
            trailStyle = playerTrail,
            eyeStyle = playerEye,
            segments = initialSegments,
            pathHistory = initialPath,
        )
        _snakes.add(player)
        controllers[PLAYER_ID] = playerController
        peakLength = initialSegments.size
        bestRankAchieved = if (spawnBots) ArenaConfig.TARGET_BOT_COUNT + 1 else 1

        botsEnabled = spawnBots
        if (spawnBots) {
            botManager.initializeBots(
                playerSnake = player,
                snakes = _snakes,
                controllers = controllers,
                botSlotById = botSlotById,
                difficulty = difficulty,
            )
        }

        foodSpawner.populateInitialFood(_foodOrbs)
        spatialGrid.rebuildFoodGrid(_foodOrbs)
        spatialGrid.rebuildSnakeGrid(_snakes)
        refreshLeaderboardAndBlips()
        cachedFoodBlips = rebuildMiniMapFoodBlips()
        foodBlipRefreshCounter = 0
        leaderboardRefreshCounter = 0
        cameraController.snapTo(camera, player)
    }

    /**
     * Hook for registering custom bots or Phase 3 remote network players.
     */
    fun registerSnake(snake: SnakeEntity, controller: SnakeController) {
        if (snake.pathHistory.size == 0 && snake.segments.isNotEmpty()) {
            for (i in snake.segments.lastIndex downTo 0) {
                val seg = snake.segments[i]
                snake.pathHistory.pushNewest(seg.x, seg.y)
            }
        }
        _snakes.add(snake)
        controllers[snake.id] = controller
    }

    fun step(dtSeconds: Float): EngineStepOutcome {
        val dt = dtSeconds.coerceIn(0f, ArenaConfig.MAX_DELTA_TIME_SEC)
        if (dt <= 0f) return EngineStepOutcome.Running

        worldTimeSeconds += dt
        elapsedPlaySeconds += dt
        if (boostBlockedWarningTimerSec > 0f) {
            boostBlockedWarningTimerSec = (boostBlockedWarningTimerSec - dt).coerceAtLeast(0f)
        }
        if (killBannerTimerSec > 0f) {
            killBannerTimerSec = (killBannerTimerSec - dt).coerceAtLeast(0f)
            if (killBannerTimerSec == 0f) {
                latestPlayerKill = null
            }
        }

        updateParticles(dt)

        val player = playerSnake ?: return EngineStepOutcome.Running

        // 1. Sample controls and advance all active snakes
        var i = 0
        while (i < _snakes.size) {
            val snake = _snakes[i]
            if (!snake.isAlive) {
                i++
                continue
            }
            if (snake.spawnProtectionSec > 0f) {
                snake.spawnProtectionSec = (snake.spawnProtectionSec - dt).coerceAtLeast(0f)
            }
            val controller = controllers[snake.id]
            if (controller != null) {
                val input = controller.sampleInput(snake, this, dt)
                updateSnakeMovement(snake, input, dt)
            }

            if (isOutsideArena(snake)) {
                if (snake.isPlayer) {
                    snake.isAlive = false
                    spawnDeathParticles(snake)
                    return EngineStepOutcome.Ended(
                        buildGameResult(
                            player = snake,
                            reason = DeathReason.ARENA_BOUNDARY,
                            killerName = null,
                        ),
                    )
                } else {
                    eliminateSnakeAndRespawnBot(
                        victim = snake,
                        killer = null,
                        player = player,
                    )
                }
            } else {
                updateFoodInteractions(snake, dt)
            }
            i++
        }

        // 2. Refresh snake spatial grid before snake-vs-snake combat resolution
        spatialGrid.rebuildSnakeGrid(_snakes)
        combatCollisionResolver.resolveSnakeCollisions(
            snakes = _snakes,
            spatialGrid = spatialGrid,
            outEliminations = eliminationsBuffer,
        )

        for (eIdx in eliminationsBuffer.indices) {
            val record = eliminationsBuffer[eIdx]
            val victim = record.victim
            if (!victim.isAlive) continue

            if (victim.isPlayer) {
                victim.isAlive = false
                convertBodyToLoot(victim)
                spawnDeathParticles(victim)
                return EngineStepOutcome.Ended(
                    buildGameResult(
                        player = victim,
                        reason = record.reason,
                        killerName = record.killer?.displayName,
                    ),
                )
            } else {
                eliminateSnakeAndRespawnBot(
                    victim = victim,
                    killer = record.killer,
                    player = player,
                )
            }
        }

        // 3. Batch prune consumed orbs, replenish natural pool (750), and rebuild spatial buckets
        var writeIdx = 0
        for (idx in _foodOrbs.indices) {
            val orb = _foodOrbs[idx]
            if (!orb.isConsumed) {
                _foodOrbs[writeIdx++] = orb
            }
        }
        if (writeIdx < _foodOrbs.size) {
            _foodOrbs.subList(writeIdx, _foodOrbs.size).clear()
        }

        if (botsEnabled) {
            botManager.ensurePopulationInRange(
                playerSnake = player,
                snakes = _snakes,
                controllers = controllers,
                botSlotById = botSlotById,
            )
        }

        foodSpawner.ensureNaturalFoodCount(_foodOrbs)
        spatialGrid.rebuildFoodGrid(_foodOrbs)
        spatialGrid.rebuildSnakeGrid(_snakes)

        leaderboardRefreshCounter++
        if (leaderboardRefreshCounter >= 6) {
            leaderboardRefreshCounter = 0
            refreshLeaderboardAndBlips()
        }

        cameraController.update(camera, player, dt)
        return EngineStepOutcome.Running
    }

    fun togglePlayerBoost(): Boolean {
        val player = playerSnake ?: return false
        val currentlyOn = playerController.isBoostEnabled()
        if (!currentlyOn) {
            if (player.targetLength <= ArenaConfig.MIN_BOOST_SEGMENT_COUNT + 0.25f) {
                boostBlockedWarningTimerSec = 1.1f
                playerController.setBoostEnabled(false)
                return false
            }
            playerController.setBoostEnabled(true)
            return true
        } else {
            playerController.setBoostEnabled(false)
            return false
        }
    }

    fun buildHudSnapshot(forceRefreshFoodBlips: Boolean = false): ArenaHudSnapshot {
        val player = playerSnake
        if (player == null) {
            return ArenaHudSnapshot(bestScore = sessionBestScore)
        }
        val canBoost = player.targetLength > ArenaConfig.MIN_BOOST_SEGMENT_COUNT + 0.25f

        foodBlipRefreshCounter++
        if (forceRefreshFoodBlips || cachedFoodBlips.isEmpty() || foodBlipRefreshCounter >= 8) {
            foodBlipRefreshCounter = 0
            cachedFoodBlips = rebuildMiniMapFoodBlips()
        }

        if (forceRefreshFoodBlips) {
            refreshLeaderboardAndBlips()
        }

        return ArenaHudSnapshot(
            score = player.score,
            length = player.segments.size,
            bestScore = maxOf(sessionBestScore, player.score),
            eliminationsCount = player.eliminationsCount,
            elapsedSeconds = elapsedPlaySeconds.toInt(),
            isBoostActive = player.isBoosting,
            canBoost = canBoost,
            boostBlockedByMinLength = boostBlockedWarningTimerSec > 0f,
            spawnProtectionRemainingSec = player.spawnProtectionSec,
            playerX = player.headX,
            playerY = player.headY,
            arenaWidth = arenaWidth,
            arenaHeight = arenaHeight,
            playerNormX = (player.headX / arenaWidth).coerceIn(0f, 1f),
            playerNormY = (player.headY / arenaHeight).coerceIn(0f, 1f),
            playerAngleRadians = player.angleRadians,
            foodBlips = cachedFoodBlips,
            botBlips = cachedBotBlips,
            topThree = cachedTopThree,
            playerRankEntry = cachedPlayerRankEntry,
            totalSnakesCount = _snakes.size,
            latestPlayerKill = latestPlayerKill,
        )
    }

    private fun eliminateSnakeAndRespawnBot(
        victim: SnakeEntity,
        killer: SnakeEntity?,
        player: SnakeEntity,
    ) {
        victim.isAlive = false
        convertBodyToLoot(victim)
        spawnDeathParticles(victim)

        if (killer != null && killer.isAlive) {
            killer.eliminationsCount += 1
            killer.score += ArenaConfig.ELIMINATION_BONUS_POINTS
            if (killer.isPlayer) {
                latestPlayerKill = KillFeedEvent(
                    id = nextKillEventId++,
                    victimName = victim.displayName,
                    rewardPoints = ArenaConfig.ELIMINATION_BONUS_POINTS,
                )
                killBannerTimerSec = 2.4f
            }
        }

        botManager.respawnEliminatedBot(
            eliminatedBot = victim,
            playerSnake = player,
            snakes = _snakes,
            controllers = controllers,
            botSlotById = botSlotById,
        )
    }

    private fun convertBodyToLoot(victim: SnakeEntity) {
        val skinColorIdx = SnakeSkins.all.indexOfFirst { it.id == victim.skin.id }
            .coerceAtLeast(0)
        val segments = victim.segments
        var idx = 0
        while (idx < segments.size) {
            val seg = segments[idx]
            _foodOrbs.add(
                foodSpawner.createLootOrb(
                    x = seg.x,
                    y = seg.y,
                    colorIndex = skinColorIdx,
                ),
            )
            idx += 2
        }
    }

    private fun spawnDeathParticles(victim: SnakeEntity) {
        val count = 16
        val color = victim.skin.primaryColor
        for (p in 0 until count) {
            val angle = (p.toFloat() / count) * (2f * PI.toFloat()) + Random.nextFloat() * 0.35f
            val speed = 90f + Random.nextFloat() * 190f
            _particles.add(
                DeathBurstParticle(
                    x = victim.headX,
                    y = victim.headY,
                    vx = cos(angle) * speed,
                    vy = sin(angle) * speed,
                    lifeSec = 0.55f,
                    maxLifeSec = 0.55f,
                    radius = 5f + Random.nextFloat() * 5f,
                    color = color,
                ),
            )
        }
    }

    private fun updateParticles(dt: Float) {
        var idx = _particles.lastIndex
        while (idx >= 0) {
            val p = _particles[idx]
            p.lifeSec -= dt
            if (p.lifeSec <= 0f) {
                _particles.removeAt(idx)
            } else {
                p.x += p.vx * dt
                p.y += p.vy * dt
                p.vx *= 0.94f
                p.vy *= 0.94f
            }
            idx--
        }
    }

    private fun refreshLeaderboardAndBlips() {
        sortedLeaderboardBuffer.clear()
        for (i in _snakes.indices) {
            val s = _snakes[i]
            if (s.isAlive) {
                s.isLeader = false
                sortedLeaderboardBuffer.add(s)
            }
        }
        sortedLeaderboardBuffer.sortByDescending { it.score }

        val leader = sortedLeaderboardBuffer.firstOrNull()
        if (leader != null) {
            leader.isLeader = true
        }

        val top3 = ArrayList<LeaderboardEntry>(3)
        var playerEntry: LeaderboardEntry? = null
        val botMapList = ArrayList<BotMapBlip>(_snakes.size)

        for (rankIdx in sortedLeaderboardBuffer.indices) {
            val snake = sortedLeaderboardBuffer[rankIdx]
            val rank = rankIdx + 1
            if (snake.isPlayer) {
                if (rank < bestRankAchieved) {
                    bestRankAchieved = rank
                }
                if (rank > 3) {
                    playerEntry = LeaderboardEntry(
                        snakeId = snake.id,
                        displayName = snake.displayName,
                        score = snake.score,
                        rank = rank,
                        color = snake.skin.primaryColor,
                        isPlayer = true,
                    )
                }
            } else {
                botMapList.add(
                    BotMapBlip(
                        normX = (snake.headX / arenaWidth).coerceIn(0f, 1f),
                        normY = (snake.headY / arenaHeight).coerceIn(0f, 1f),
                        color = snake.skin.primaryColor,
                        isLeader = snake.isLeader,
                    ),
                )
            }

            if (rank <= 3) {
                top3.add(
                    LeaderboardEntry(
                        snakeId = snake.id,
                        displayName = snake.displayName,
                        score = snake.score,
                        rank = rank,
                        color = snake.skin.primaryColor,
                        isPlayer = snake.isPlayer,
                    ),
                )
            }
        }

        cachedTopThree = top3
        cachedPlayerRankEntry = playerEntry
        cachedBotBlips = botMapList
    }

    private fun buildGameResult(
        player: SnakeEntity,
        reason: DeathReason,
        killerName: String?,
    ): GameResult {
        return GameResult(
            finalScore = player.score,
            peakLength = peakLength,
            survivedSeconds = elapsedPlaySeconds.toInt(),
            foodEaten = player.foodEatenCount,
            eliminationsCount = player.eliminationsCount,
            bestRank = bestRankAchieved,
            isNewBest = player.score > sessionBestScore,
            deathReason = reason,
            killerName = killerName,
        )
    }

    private fun rebuildMiniMapFoodBlips(): List<MiniMapBlip> {
        val blips = ArrayList<MiniMapBlip>(400)
        for (i in _foodOrbs.indices) {
            val orb = _foodOrbs[i]
            if (orb.isLootDrop || orb.points >= 25 || (orb.id and 1) == 0) {
                blips.add(
                    MiniMapBlip(
                        normX = (orb.x / arenaWidth).coerceIn(0f, 1f),
                        normY = (orb.y / arenaHeight).coerceIn(0f, 1f),
                        isLargeOrb = orb.isLootDrop || orb.points >= 50,
                    ),
                )
            }
        }
        return blips
    }

    private fun updateSnakeMovement(
        snake: SnakeEntity,
        input: ControlInput,
        dt: Float,
    ) {
        if (input.hasDirection) {
            val diff = normalizeAngle(input.targetAngleRadians - snake.angleRadians)
            val maxStep = ArenaConfig.MAX_TURN_RATE_RAD_PER_SEC * dt
            snake.angleRadians = if (abs(diff) <= maxStep) {
                input.targetAngleRadians
            } else {
                normalizeAngle(snake.angleRadians + if (diff > 0f) maxStep else -maxStep)
            }
        }

        val analogSpeedScale = if (input.hasDirection) {
            ArenaConfig.MIN_ANALOG_SPEED_FACTOR +
                (1f - ArenaConfig.MIN_ANALOG_SPEED_FACTOR) * input.magnitude
        } else {
            1.0f
        }

        var boostingNow = false
        if (input.isBoostActive) {
            if (snake.targetLength > ArenaConfig.MIN_BOOST_SEGMENT_COUNT.toFloat()) {
                boostingNow = true
                snake.targetLength = (snake.targetLength - ArenaConfig.BOOST_LENGTH_DRAIN_PER_SEC * dt)
                    .coerceAtLeast(ArenaConfig.MIN_BOOST_SEGMENT_COUNT.toFloat())

                boostDropAccumulatorSec += dt
                if (boostDropAccumulatorSec >= ArenaConfig.BOOST_PELLET_DROP_INTERVAL_SEC) {
                    boostDropAccumulatorSec -= ArenaConfig.BOOST_PELLET_DROP_INTERVAL_SEC
                    val tail = snake.segments.lastOrNull()
                    if (tail != null) {
                        val skinColorIdx = SnakeSkins.all.indexOfFirst { it.id == snake.skin.id }
                            .coerceAtLeast(0)
                        _foodOrbs.add(
                            foodSpawner.createBoostDropOrb(tail.x, tail.y, skinColorIdx),
                        )
                    }
                }

                if (snake.targetLength <= ArenaConfig.MIN_BOOST_SEGMENT_COUNT.toFloat()) {
                    boostingNow = false
                    if (snake.isPlayer) {
                        playerController.setBoostEnabled(false)
                        boostBlockedWarningTimerSec = 1.25f
                    }
                }
            } else {
                if (snake.isPlayer) {
                    playerController.setBoostEnabled(false)
                    boostBlockedWarningTimerSec = 1.1f
                }
            }
        }

        snake.isBoosting = boostingNow
        val boostMultiplier = if (boostingNow) ArenaConfig.BOOST_SPEED_MULTIPLIER else 1.0f
        snake.currentSpeed = ArenaConfig.BASE_SPEED * analogSpeedScale * boostMultiplier

        snake.headX += cos(snake.angleRadians) * snake.currentSpeed * dt
        snake.headY += sin(snake.angleRadians) * snake.currentSpeed * dt

        val growthBonus = ((snake.segments.size - ArenaConfig.INITIAL_SEGMENT_COUNT) * 0.06f)
            .coerceIn(0f, 6.5f)
        snake.headRadius = ArenaConfig.BASE_HEAD_RADIUS + growthBonus
        snake.bodyRadius = ArenaConfig.BASE_BODY_RADIUS + growthBonus * 0.9f

        updateSegmentsChain(snake)

        if (snake.isPlayer && snake.segments.size > peakLength) {
            peakLength = snake.segments.size
        }
    }

    private fun updateSegmentsChain(snake: SnakeEntity) {
        val path = snake.pathHistory
        if (path.size == 0) {
            path.pushNewest(snake.headX, snake.headY)
        } else {
            val tipX = path.xFromNewest(0)
            val tipY = path.yFromNewest(0)
            val dx = snake.headX - tipX
            val dy = snake.headY - tipY
            val dist = hypot(dx, dy)
            if (dist >= ArenaConfig.PATH_SAMPLE_STEP) {
                val steps = (dist / ArenaConfig.PATH_SAMPLE_STEP).toInt().coerceAtLeast(1)
                for (s in 1..steps) {
                    val t = s.toFloat() / steps
                    path.pushNewest(
                        x = tipX + dx * t,
                        y = tipY + dy * t,
                    )
                }
            } else if (dist > 0.0001f) {
                path.pushNewest(snake.headX, snake.headY)
            }
        }

        val desiredCount = snake.targetLength.toInt().coerceAtLeast(ArenaConfig.MIN_BOOST_SEGMENT_COUNT)
        val segments = snake.segments
        while (segments.size < desiredCount) {
            val last = segments.lastOrNull()
            segments.add(Vec2(last?.x ?: snake.headX, last?.y ?: snake.headY))
        }
        while (segments.size > desiredCount && segments.size > ArenaConfig.MIN_BOOST_SEGMENT_COUNT) {
            segments.removeAt(segments.lastIndex)
        }

        segments[0].x = snake.headX
        segments[0].y = snake.headY

        val spacing = ArenaConfig.SEGMENT_SPACING
        var reverseIdx = 0
        val maxReverseIdx = path.size - 2
        var accumulatedArc = 0f
        var segIdx = 1
        var oldestUsedReverseIdx = 0

        while (segIdx < segments.size && reverseIdx <= maxReverseIdx) {
            val targetArc = segIdx * spacing
            val newerX = path.xFromNewest(reverseIdx)
            val newerY = path.yFromNewest(reverseIdx)
            val olderX = path.xFromNewest(reverseIdx + 1)
            val olderY = path.yFromNewest(reverseIdx + 1)
            val segLen = hypot(newerX - olderX, newerY - olderY)

            if (accumulatedArc + segLen >= targetArc && segLen > 0.0001f) {
                val remaining = targetArc - accumulatedArc
                val t = (remaining / segLen).coerceIn(0f, 1f)
                segments[segIdx].x = newerX + (olderX - newerX) * t
                segments[segIdx].y = newerY + (olderY - newerY) * t
                oldestUsedReverseIdx = reverseIdx + 1
                segIdx++
            } else {
                accumulatedArc += segLen
                reverseIdx++
                oldestUsedReverseIdx = reverseIdx
            }
        }

        if (segIdx < segments.size && path.size > 0) {
            val oldestX = path.oldestX()
            val oldestY = path.oldestY()
            while (segIdx < segments.size) {
                segments[segIdx].x = oldestX
                segments[segIdx].y = oldestY
                segIdx++
            }
        }

        val samplesUsedFromNewest = oldestUsedReverseIdx + 1
        val excess = path.size - (samplesUsedFromNewest + 32)
        if (excess > 24) {
            path.dropOldest(excess)
        }
    }

    private fun updateFoodInteractions(snake: SnakeEntity, dt: Float) {
        val magnetRadius = ArenaConfig.FOOD_MAGNET_RADIUS + (snake.headRadius - ArenaConfig.BASE_HEAD_RADIUS)
        val eatRadius = ArenaConfig.FOOD_EAT_RADIUS + (snake.headRadius - ArenaConfig.BASE_HEAD_RADIUS)
        val pullSpeed = 420f * dt

        spatialGrid.queryFoodInRadius(
            centerX = snake.headX,
            centerY = snake.headY,
            radius = magnetRadius + 24f,
            outOrbs = nearbyFoodQueryBuffer,
        )

        for (i in nearbyFoodQueryBuffer.indices) {
            val orb = nearbyFoodQueryBuffer[i]
            if (orb.isConsumed) continue

            val dx = snake.headX - orb.x
            val dy = snake.headY - orb.y
            val distSq = dx * dx + dy * dy
            val effectiveEatRadius = eatRadius + orb.radius * 0.35f

            if (distSq <= effectiveEatRadius * effectiveEatRadius) {
                snake.score += orb.points
                snake.targetLength += orb.growthValue
                snake.foodEatenCount += 1
                orb.isConsumed = true
            } else if (distSq <= magnetRadius * magnetRadius) {
                val dist = hypot(dx, dy)
                if (dist > 0.001f) {
                    val step = pullSpeed.coerceAtMost(dist)
                    orb.x += (dx / dist) * step
                    orb.y += (dy / dist) * step
                }
            }
        }
    }

    private fun isOutsideArena(snake: SnakeEntity): Boolean {
        val margin = snake.headRadius * 0.85f
        return snake.headX - margin <= 0f ||
            snake.headX + margin >= arenaWidth ||
            snake.headY - margin <= 0f ||
            snake.headY + margin >= arenaHeight
    }

    private fun normalizeAngle(angle: Float): Float {
        var a = angle
        val twoPi = (2.0 * PI).toFloat()
        while (a > PI) a -= twoPi
        while (a < -PI) a += twoPi
        return a
    }

    companion object {
        const val PLAYER_ID = "local_player"
    }
}
