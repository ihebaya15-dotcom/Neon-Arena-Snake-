package com.example.game.engine

import com.example.game.model.ArenaConfig
import com.example.game.model.ArenaHudSnapshot
import com.example.game.model.CameraState
import com.example.game.model.ControlInput
import com.example.game.model.DeathReason
import com.example.game.model.FoodOrb
import com.example.game.model.GameResult
import com.example.game.model.MiniMapBlip
import com.example.game.model.SnakeEntity
import com.example.game.model.SnakeSkin
import com.example.game.model.SnakeSkins
import com.example.game.model.Vec2
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.sin

sealed interface EngineStepOutcome {
    data object Running : EngineStepOutcome
    data class Ended(val result: GameResult) : EngineStepOutcome
}

class ArenaEngine(
    private val foodSpawner: FoodSpawner = FoodSpawner(),
    private val cameraController: CameraController = CameraController(),
) : ArenaWorldView {

    override val arenaWidth: Float = ArenaConfig.ARENA_WIDTH
    override val arenaHeight: Float = ArenaConfig.ARENA_HEIGHT

    private val _foodOrbs = mutableListOf<FoodOrb>()
    override val foodOrbs: List<FoodOrb> get() = _foodOrbs

    private val _snakes = mutableListOf<SnakeEntity>()
    override val snakes: List<SnakeEntity> get() = _snakes

    private val controllers = mutableMapOf<String, SnakeController>()
    val playerController = HumanJoystickController()

    val camera = CameraState()

    var worldTimeSeconds: Float = 0f
        private set

    private var elapsedPlaySeconds: Float = 0f
    private var peakLength: Int = ArenaConfig.INITIAL_SEGMENT_COUNT
    private var sessionBestScore: Int = 0
    private var boostDropAccumulatorSec: Float = 0f
    private var boostBlockedWarningTimerSec: Float = 0f

    val playerSnake: SnakeEntity?
        get() = _snakes.firstOrNull { it.isPlayer }

    fun startNewGame(
        playerSkin: SnakeSkin,
        currentBestScore: Int,
    ) {
        sessionBestScore = currentBestScore
        elapsedPlaySeconds = 0f
        worldTimeSeconds = 0f
        boostDropAccumulatorSec = 0f
        boostBlockedWarningTimerSec = 0f
        playerController.reset()

        _snakes.clear()
        controllers.clear()

        val startX = ArenaConfig.ARENA_WIDTH / 2f
        val startY = ArenaConfig.ARENA_HEIGHT / 2f
        val startAngle = (-PI / 2.0).toFloat()

        val initialSegments = MutableList(ArenaConfig.INITIAL_SEGMENT_COUNT) { idx ->
            Vec2(
                x = startX - cos(startAngle) * (idx * ArenaConfig.SEGMENT_SPACING),
                y = startY - sin(startAngle) * (idx * ArenaConfig.SEGMENT_SPACING),
            )
        }

        // Seed pathHistory oldest-to-newest so index 0 is tail-end and last() is current head
        val totalInitialPathDist = (ArenaConfig.INITIAL_SEGMENT_COUNT + 4) * ArenaConfig.SEGMENT_SPACING
        val sampleCount = (totalInitialPathDist / ArenaConfig.PATH_SAMPLE_STEP).toInt().coerceAtLeast(2)
        val initialPath = MutableList(sampleCount + 1) { stepIdx ->
            val distFromHead = (sampleCount - stepIdx) * ArenaConfig.PATH_SAMPLE_STEP
            Vec2(
                x = startX - cos(startAngle) * distFromHead,
                y = startY - sin(startAngle) * distFromHead,
            )
        }

        val player = SnakeEntity(
            id = PLAYER_ID,
            isPlayer = true,
            skin = playerSkin,
            headX = startX,
            headY = startY,
            angleRadians = startAngle,
            currentSpeed = ArenaConfig.BASE_SPEED,
            targetLength = ArenaConfig.INITIAL_SEGMENT_COUNT.toFloat(),
            score = 0,
            foodEatenCount = 0,
            isBoosting = false,
            segments = initialSegments,
            pathHistory = initialPath,
        )
        _snakes.add(player)
        controllers[PLAYER_ID] = playerController
        peakLength = initialSegments.size

        foodSpawner.populateInitialFood(_foodOrbs)
        cameraController.snapTo(camera, player)
    }

    /**
     * Future Phase 2+ hook for registering AI bots or remote network players.
     */
    fun registerSnake(snake: SnakeEntity, controller: SnakeController) {
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

        val player = playerSnake ?: return EngineStepOutcome.Running

        for (i in _snakes.indices) {
            val snake = _snakes[i]
            val controller = controllers[snake.id] ?: continue
            val input = controller.sampleInput(snake, this, dt)
            updateSnakeMovement(snake, input, dt)

            if (snake.isPlayer) {
                val deathReason = when {
                    isOutsideArena(snake) -> DeathReason.ARENA_BOUNDARY
                    hasSelfCollision(snake) -> DeathReason.SELF_COLLISION
                    else -> null
                }
                if (deathReason != null) {
                    val isNewBest = snake.score > sessionBestScore
                    val result = GameResult(
                        finalScore = snake.score,
                        peakLength = peakLength,
                        survivedSeconds = elapsedPlaySeconds.toInt(),
                        foodEaten = snake.foodEatenCount,
                        isNewBest = isNewBest,
                        deathReason = deathReason,
                    )
                    return EngineStepOutcome.Ended(result)
                }
            }

            updateFoodInteractions(snake, dt)
        }

        foodSpawner.ensureNaturalFoodCount(_foodOrbs)
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

    fun buildHudSnapshot(): ArenaHudSnapshot {
        val player = playerSnake
        if (player == null) {
            return ArenaHudSnapshot(bestScore = sessionBestScore)
        }
        val canBoost = player.targetLength > ArenaConfig.MIN_BOOST_SEGMENT_COUNT + 0.25f
        val blips = _foodOrbs.map { orb ->
            MiniMapBlip(
                normX = (orb.x / arenaWidth).coerceIn(0f, 1f),
                normY = (orb.y / arenaHeight).coerceIn(0f, 1f),
                isLargeOrb = orb.points >= 50,
            )
        }
        return ArenaHudSnapshot(
            score = player.score,
            length = player.segments.size,
            bestScore = maxOf(sessionBestScore, player.score),
            elapsedSeconds = elapsedPlaySeconds.toInt(),
            isBoostActive = player.isBoosting,
            canBoost = canBoost,
            boostBlockedByMinLength = boostBlockedWarningTimerSec > 0f,
            playerX = player.headX,
            playerY = player.headY,
            arenaWidth = arenaWidth,
            arenaHeight = arenaHeight,
            playerNormX = (player.headX / arenaWidth).coerceIn(0f, 1f),
            playerNormY = (player.headY / arenaHeight).coerceIn(0f, 1f),
            playerAngleRadians = player.angleRadians,
            foodBlips = blips,
        )
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

        // Update snake girth subtly as length increases
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
        if (path.isEmpty()) {
            path.add(Vec2(snake.headX, snake.headY))
        } else {
            val tip = path.last()
            val dx = snake.headX - tip.x
            val dy = snake.headY - tip.y
            val dist = hypot(dx, dy)
            if (dist >= ArenaConfig.PATH_SAMPLE_STEP) {
                val steps = (dist / ArenaConfig.PATH_SAMPLE_STEP).toInt().coerceAtLeast(1)
                for (s in 1..steps) {
                    val t = s.toFloat() / steps
                    path.add(
                        Vec2(
                            x = tip.x + dx * t,
                            y = tip.y + dy * t,
                        ),
                    )
                }
            } else if (dist > 0.0001f) {
                // Keep exact live head coordinate at the tip for zero-lag visual alignment
                path.add(Vec2(snake.headX, snake.headY))
            }
        }

        val desiredCount = snake.targetLength.toInt().coerceAtLeast(ArenaConfig.MIN_BOOST_SEGMENT_COUNT)
        val segments = snake.segments
        while (segments.size < desiredCount) {
            val last = segments.lastOrNull() ?: Vec2(snake.headX, snake.headY)
            segments.add(Vec2(last.x, last.y))
        }
        while (segments.size > desiredCount && segments.size > ArenaConfig.MIN_BOOST_SEGMENT_COUNT) {
            segments.removeAt(segments.lastIndex)
        }

        // Segment 0 is always the head
        segments[0].x = snake.headX
        segments[0].y = snake.headY

        // Walk backwards along pathHistory from head (lastIndex) to place each segment
        // at exact arc-length intervals (i * SEGMENT_SPACING) with smooth linear interpolation.
        val spacing = ArenaConfig.SEGMENT_SPACING
        var pathIdx = path.lastIndex
        var accumulatedArc = 0f
        var segIdx = 1
        var oldestUsedPathIdx = pathIdx

        while (segIdx < segments.size && pathIdx > 0) {
            val targetArc = segIdx * spacing
            val newer = path[pathIdx]
            val older = path[pathIdx - 1]
            val segLen = hypot(newer.x - older.x, newer.y - older.y)

            if (accumulatedArc + segLen >= targetArc && segLen > 0.0001f) {
                val remaining = targetArc - accumulatedArc
                val t = (remaining / segLen).coerceIn(0f, 1f)
                segments[segIdx].x = newer.x + (older.x - newer.x) * t
                segments[segIdx].y = newer.y + (older.y - newer.y) * t
                oldestUsedPathIdx = pathIdx - 1
                segIdx++
            } else {
                accumulatedArc += segLen
                pathIdx--
                oldestUsedPathIdx = pathIdx
            }
        }

        // If snake grew faster than current path buffer, extend smoothly from oldest point
        val fallback = path.firstOrNull() ?: Vec2(snake.headX, snake.headY)
        while (segIdx < segments.size) {
            segments[segIdx].x = fallback.x
            segments[segIdx].y = fallback.y
            segIdx++
        }

        // Prune stale history beyond the tail (keep a small buffer for growth)
        val pruneCount = oldestUsedPathIdx - 16
        if (pruneCount > 24) {
            path.subList(0, pruneCount).clear()
        }
    }

    private fun hasSelfCollision(snake: SnakeEntity): Boolean {
        val segments = snake.segments
        if (segments.size <= ArenaConfig.SELF_COLLISION_SKIP_SEGMENTS) return false

        val headX = snake.headX
        val headY = snake.headY
        val baseThreshold = (snake.headRadius + snake.bodyRadius) * ArenaConfig.SELF_COLLISION_HITBOX_SCALE

        for (i in ArenaConfig.SELF_COLLISION_SKIP_SEGMENTS until segments.size) {
            val tailProgress = (segments.lastIndex - i).coerceAtMost(5) / 5f
            val taper = 0.56f + 0.44f * tailProgress
            val segRadius = snake.bodyRadius * taper
            val hitRadius = (snake.headRadius + segRadius) * ArenaConfig.SELF_COLLISION_HITBOX_SCALE
            val effectiveRadius = hitRadius.coerceAtMost(baseThreshold)

            val seg = segments[i]
            val dx = headX - seg.x
            val dy = headY - seg.y
            if (dx * dx + dy * dy <= effectiveRadius * effectiveRadius) {
                return true
            }
        }
        return false
    }

    private fun updateFoodInteractions(snake: SnakeEntity, dt: Float) {
        val magnetRadius = ArenaConfig.FOOD_MAGNET_RADIUS + (snake.headRadius - ArenaConfig.BASE_HEAD_RADIUS)
        val eatRadius = ArenaConfig.FOOD_EAT_RADIUS + (snake.headRadius - ArenaConfig.BASE_HEAD_RADIUS)
        val pullSpeed = 420f * dt

        var idx = _foodOrbs.lastIndex
        while (idx >= 0) {
            val orb = _foodOrbs[idx]
            val dx = snake.headX - orb.x
            val dy = snake.headY - orb.y
            val distSq = dx * dx + dy * dy
            val effectiveEatRadius = eatRadius + orb.radius * 0.35f

            if (distSq <= effectiveEatRadius * effectiveEatRadius) {
                snake.score += orb.points
                snake.targetLength += orb.growthValue
                snake.foodEatenCount += 1
                _foodOrbs.removeAt(idx)
            } else if (distSq <= magnetRadius * magnetRadius) {
                val dist = hypot(dx, dy)
                if (dist > 0.001f) {
                    val step = pullSpeed.coerceAtMost(dist)
                    orb.x += (dx / dist) * step
                    orb.y += (dy / dist) * step
                }
            }
            idx--
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
