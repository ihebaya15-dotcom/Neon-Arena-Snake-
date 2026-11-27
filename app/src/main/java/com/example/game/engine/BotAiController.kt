package com.example.game.engine

import com.example.game.model.ArenaConfig
import com.example.game.model.BotPersonality
import com.example.game.model.ControlInput
import com.example.game.model.FoodOrb
import com.example.game.model.SnakeEntity
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin
import kotlin.random.Random

/**
 * Multi-layer AI Bot controller implementing:
 * 1) 5-ray forward danger sensing against 7000x7000 walls and enemy snake bodies
 * 2) Personality-driven tactical combat (HUNTER cutoff, SCAVENGER loot sprint, TACTICIAN safe growth)
 * 3) Weighted spatial food seeking via [ArenaSpatialGrid]
 */
class BotAiController(
    private val personality: BotPersonality,
    private val botSlot: Int,
    private val random: Random = Random.Default,
) : SnakeController {

    private var frameCounter = botSlot
    private var cachedInput = ControlInput.IDLE
    private var boostHoldTimerSec = 0f
    private var wanderAngleOffset = 0f
    private var wanderTimerSec = 0f

    private val rayClearances = FloatArray(RAY_OFFSETS_RAD.size)
    private val foodQueryBuffer = ArrayList<FoodOrb>(48)
    private val nearbySnakesBuffer = ArrayList<SnakeEntity>(12)

    override fun sampleInput(
        snake: SnakeEntity,
        world: ArenaWorldView,
        dtSeconds: Float,
    ): ControlInput {
        if (!snake.isAlive) return ControlInput.IDLE

        if (boostHoldTimerSec > 0f) {
            boostHoldTimerSec = (boostHoldTimerSec - dtSeconds).coerceAtLeast(0f)
        }
        wanderTimerSec -= dtSeconds
        if (wanderTimerSec <= 0f) {
            wanderTimerSec = 1.1f + random.nextFloat() * 1.4f
            wanderAngleOffset = (random.nextFloat() - 0.5f) * 0.55f
        }

        // Stagger heavy spatial sensing every 2nd frame per bot while keeping immediate boundary safety
        frameCounter++
        val shouldReevaluate = (frameCounter and 1) == 0 ||
            cachedInput === ControlInput.IDLE ||
            isNearBoundary(snake, world)

        if (!shouldReevaluate) {
            return cachedInput.copy(isBoostActive = boostHoldTimerSec > 0f)
        }

        val lookAhead = if (snake.isBoosting) 355f else 245f
        evaluateForwardDangerRays(snake, world, lookAhead)

        // Priority 1: Immediate Danger Avoidance (walls & enemy bodies)
        val centerClear = rayClearances[0]
        val innerMinClear = min(rayClearances[1], rayClearances[2])
        if (centerClear < 0.78f || innerMinClear < 0.52f) {
            var bestRayIdx = 0
            var bestScore = -1f
            for (r in RAY_OFFSETS_RAD.indices) {
                val anglePenalty = abs(RAY_OFFSETS_RAD[r]) * 0.14f
                val score = rayClearances[r] - anglePenalty
                if (score > bestScore) {
                    bestScore = score
                    bestRayIdx = r
                }
            }
            val escapeAngle = snake.angleRadians + RAY_OFFSETS_RAD[bestRayIdx]
            if (centerClear < 0.44f && canAffordBoost(snake, reserveSegments = 6)) {
                boostHoldTimerSec = max(boostHoldTimerSec, 0.25f)
            }
            cachedInput = ControlInput(
                vectorX = cos(escapeAngle),
                vectorY = sin(escapeAngle),
                isBoostActive = boostHoldTimerSec > 0f,
            )
            return cachedInput
        }

        // Priority 2: Personality Tactical Combat / High-Value Loot
        queryNearbySnakes(snake, world, radius = 580f)
        val tacticalInput = when (personality) {
            BotPersonality.HUNTER -> tryHunterIntercept(snake)
            BotPersonality.SCAVENGER -> tryScavengerLootRush(snake, world)
            BotPersonality.TACTICIAN -> tryTacticianManeuver(snake, world)
        }
        if (tacticalInput != null) {
            cachedInput = tacticalInput
            return cachedInput
        }

        // Priority 3: Weighted Food & Loot Seeking
        val foodInput = seekBestFood(snake, world)
        if (foodInput != null) {
            cachedInput = foodInput
            return cachedInput
        }

        // Fallback: Smooth inward-biased cruise
        val centerBiasAngle = computeInwardCruiseAngle(snake, world) + wanderAngleOffset
        cachedInput = ControlInput(
            vectorX = cos(centerBiasAngle),
            vectorY = sin(centerBiasAngle),
            isBoostActive = boostHoldTimerSec > 0f,
        )
        return cachedInput
    }

    private fun evaluateForwardDangerRays(
        snake: SnakeEntity,
        world: ArenaWorldView,
        lookAhead: Float,
    ) {
        val headX = snake.headX
        val headY = snake.headY
        val margin = 45f

        val minX = headX - lookAhead
        val minY = headY - lookAhead
        val maxX = headX + lookAhead
        val maxY = headY + lookAhead

        nearbySnakesBuffer.clear()
        world.spatialGrid.forEachSnakeInBounds(minX, minY, maxX, maxY) { other ->
            if (other.id != snake.id && other.isAlive && other.spawnProtectionSec <= 0f) {
                nearbySnakesBuffer.add(other)
            }
        }

        for (r in RAY_OFFSETS_RAD.indices) {
            val rayAngle = snake.angleRadians + RAY_OFFSETS_RAD[r]
            val rx = cos(rayAngle)
            val ry = sin(rayAngle)
            var minDist = lookAhead

            // Wall ray-cast
            if (rx > 0.001f) {
                val d = (world.arenaWidth - margin - headX) / rx
                if (d in 0f..minDist) minDist = d
            } else if (rx < -0.001f) {
                val d = (margin - headX) / rx
                if (d in 0f..minDist) minDist = d
            }
            if (ry > 0.001f) {
                val d = (world.arenaHeight - margin - headY) / ry
                if (d in 0f..minDist) minDist = d
            } else if (ry < -0.001f) {
                val d = (margin - headY) / ry
                if (d in 0f..minDist) minDist = d
            }

            // Enemy snake body segment ray-cast
            for (sIdx in nearbySnakesBuffer.indices) {
                val other = nearbySnakesBuffer[sIdx]
                val hitBuffer = snake.headRadius + other.bodyRadius + 30f
                val hitBufferSq = hitBuffer * hitBuffer
                val segments = other.segments
                val step = if (segments.size > 40) 2 else 1

                var segIdx = 0
                while (segIdx < segments.size) {
                    val seg = segments[segIdx]
                    val toSegX = seg.x - headX
                    val toSegY = seg.y - headY
                    val proj = toSegX * rx + toSegY * ry
                    if (proj > 0f && proj < minDist) {
                        val perpX = toSegX - rx * proj
                        val perpY = toSegY - ry * proj
                        if (perpX * perpX + perpY * perpY <= hitBufferSq) {
                            minDist = proj
                        }
                    }
                    segIdx += step
                }
            }

            rayClearances[r] = (minDist / lookAhead).coerceIn(0f, 1f)
        }
    }

    private fun tryHunterIntercept(snake: SnakeEntity): ControlInput? {
        if (snake.segments.size < ArenaConfig.MIN_BOOST_SEGMENT_COUNT + 8) return null
        var bestTarget: SnakeEntity? = null
        var bestDist = 520f

        for (i in nearbySnakesBuffer.indices) {
            val other = nearbySnakesBuffer[i]
            val d = hypot(other.headX - snake.headX, other.headY - snake.headY)
            if (d in 120f..bestDist) {
                bestDist = d
                bestTarget = other
            }
        }
        val target = bestTarget ?: return null

        // Predict point ahead of target's nose to cut across its trajectory
        val leadDist = (bestDist * 0.45f).coerceIn(75f, 185f)
        val interceptX = (target.headX + cos(target.angleRadians) * leadDist)
            .coerceIn(220f, ArenaConfig.ARENA_WIDTH - 220f)
        val interceptY = (target.headY + sin(target.angleRadians) * leadDist)
            .coerceIn(220f, ArenaConfig.ARENA_HEIGHT - 220f)

        val desiredAngle = atan2(interceptY - snake.headY, interceptX - snake.headX)
        if (!isAngleSafe(snake, desiredAngle)) return null

        if (bestDist < 310f && canAffordBoost(snake, reserveSegments = 10)) {
            boostHoldTimerSec = max(boostHoldTimerSec, 0.35f)
        }

        return ControlInput(
            vectorX = cos(desiredAngle),
            vectorY = sin(desiredAngle),
            isBoostActive = boostHoldTimerSec > 0f,
        )
    }

    private fun tryScavengerLootRush(snake: SnakeEntity, world: ArenaWorldView): ControlInput? {
        world.spatialGrid.queryFoodInRadius(snake.headX, snake.headY, 520f, foodQueryBuffer)
        var bestOrb: FoodOrb? = null
        var bestScore = 0f

        for (i in foodQueryBuffer.indices) {
            val orb = foodQueryBuffer[i]
            if (orb.isConsumed || (!orb.isLootDrop && orb.points < 45)) continue
            val dist = hypot(orb.x - snake.headX, orb.y - snake.headY)
            val score = (orb.points * 3.2f) / (dist + 50f)
            if (score > bestScore) {
                val angleToOrb = atan2(orb.y - snake.headY, orb.x - snake.headX)
                if (isAngleSafe(snake, angleToOrb)) {
                    bestScore = score
                    bestOrb = orb
                }
            }
        }
        val targetOrb = bestOrb ?: return null
        val angle = atan2(targetOrb.y - snake.headY, targetOrb.x - snake.headX)
        val dist = hypot(targetOrb.x - snake.headX, targetOrb.y - snake.headY)

        if (dist < 350f && canAffordBoost(snake, reserveSegments = 8)) {
            boostHoldTimerSec = max(boostHoldTimerSec, 0.30f)
        }

        return ControlInput(
            vectorX = cos(angle),
            vectorY = sin(angle),
            isBoostActive = boostHoldTimerSec > 0f,
        )
    }

    private fun tryTacticianManeuver(snake: SnakeEntity, world: ArenaWorldView): ControlInput? {
        // If an enemy head is charging closely behind/beside us, break sharply toward clear space
        for (i in nearbySnakesBuffer.indices) {
            val other = nearbySnakesBuffer[i]
            val dist = hypot(other.headX - snake.headX, other.headY - snake.headY)
            if (dist < 220f && other.isBoosting) {
                val awayAngle = atan2(snake.headY - other.headY, snake.headX - other.headX)
                if (isAngleSafe(snake, awayAngle)) {
                    if (canAffordBoost(snake, reserveSegments = 6)) {
                        boostHoldTimerSec = max(boostHoldTimerSec, 0.25f)
                    }
                    return ControlInput(
                        vectorX = cos(awayAngle),
                        vectorY = sin(awayAngle),
                        isBoostActive = boostHoldTimerSec > 0f,
                    )
                }
            }
        }
        return seekBestFood(snake, world)
    }

    private fun seekBestFood(snake: SnakeEntity, world: ArenaWorldView): ControlInput? {
        world.spatialGrid.queryFoodInRadius(snake.headX, snake.headY, 390f, foodQueryBuffer)
        var bestOrb: FoodOrb? = null
        var bestScore = -1f

        for (i in foodQueryBuffer.indices) {
            val orb = foodQueryBuffer[i]
            if (orb.isConsumed) continue
            val dx = orb.x - snake.headX
            val dy = orb.y - snake.headY
            val dist = hypot(dx, dy)
            val angleToOrb = atan2(dy, dx)
            if (!isAngleSafe(snake, angleToOrb)) continue

            val angleDiff = abs(normalizeAngle(angleToOrb - snake.angleRadians))
            val headingBonus = 1.25f - (angleDiff / PI.toFloat()) * 0.45f
            val lootWeight = if (orb.isLootDrop) 2.6f else 1.0f
            val score = (orb.points * lootWeight * headingBonus) / (dist + 45f)

            if (score > bestScore) {
                bestScore = score
                bestOrb = orb
            }
        }

        val target = bestOrb ?: return null
        val angle = atan2(target.y - snake.headY, target.x - snake.headX)
        return ControlInput(
            vectorX = cos(angle),
            vectorY = sin(angle),
            isBoostActive = boostHoldTimerSec > 0f,
        )
    }

    private fun queryNearbySnakes(snake: SnakeEntity, world: ArenaWorldView, radius: Float) {
        nearbySnakesBuffer.clear()
        world.spatialGrid.forEachSnakeInBounds(
            minX = snake.headX - radius,
            minY = snake.headY - radius,
            maxX = snake.headX + radius,
            maxY = snake.headY + radius,
        ) { other ->
            if (other.id != snake.id && other.isAlive && other.spawnProtectionSec <= 0f) {
                nearbySnakesBuffer.add(other)
            }
        }
    }

    private fun isNearBoundary(snake: SnakeEntity, world: ArenaWorldView): Boolean {
        val margin = 290f
        return snake.headX < margin ||
            snake.headX > world.arenaWidth - margin ||
            snake.headY < margin ||
            snake.headY > world.arenaHeight - margin
    }

    private fun isAngleSafe(snake: SnakeEntity, desiredAngle: Float): Boolean {
        val diff = normalizeAngle(desiredAngle - snake.angleRadians)
        // Map desired angle difference to nearest forward ray clearance check
        var closestIdx = 0
        var closestDiff = Float.MAX_VALUE
        for (i in RAY_OFFSETS_RAD.indices) {
            val d = abs(diff - RAY_OFFSETS_RAD[i])
            if (d < closestDiff) {
                closestDiff = d
                closestIdx = i
            }
        }
        if (rayClearances[closestIdx] <= SAFE_RAY_CLEARANCE_THRESHOLD) {
            return false
        }

        // Sharp or rear turns (|diff| > 0.92 rad) sweep across the turning side's inner and outer
        // rays, so require extra clearance on both before committing.
        if (abs(diff) > OUTER_RAY_OFFSET_RAD) {
            val innerSideIdx = if (diff < 0f) 1 else 2
            val outerSideIdx = if (diff < 0f) 3 else 4
            return rayClearances[innerSideIdx] > SHARP_TURN_INNER_CLEARANCE_THRESHOLD &&
                rayClearances[outerSideIdx] > SAFE_RAY_CLEARANCE_THRESHOLD
        }
        return true
    }

    private fun computeInwardCruiseAngle(snake: SnakeEntity, world: ArenaWorldView): Float {
        val margin = 520f
        return if (
            snake.headX < margin ||
            snake.headX > world.arenaWidth - margin ||
            snake.headY < margin ||
            snake.headY > world.arenaHeight - margin
        ) {
            atan2(world.arenaHeight * 0.5f - snake.headY, world.arenaWidth * 0.5f - snake.headX)
        } else {
            snake.angleRadians
        }
    }

    private fun canAffordBoost(snake: SnakeEntity, reserveSegments: Int): Boolean {
        return snake.targetLength >= ArenaConfig.MIN_BOOST_SEGMENT_COUNT + reserveSegments
    }

    private fun normalizeAngle(angle: Float): Float {
        var a = angle
        val twoPi = (2.0 * PI).toFloat()
        while (a > PI) a -= twoPi
        while (a < -PI) a += twoPi
        return a
    }

    companion object {
        private const val OUTER_RAY_OFFSET_RAD = 0.92f
        private const val SAFE_RAY_CLEARANCE_THRESHOLD = 0.48f
        private const val SHARP_TURN_INNER_CLEARANCE_THRESHOLD = 0.58f
        private val RAY_OFFSETS_RAD = floatArrayOf(0f, -0.42f, 0.42f, -0.92f, 0.92f)
    }
}
