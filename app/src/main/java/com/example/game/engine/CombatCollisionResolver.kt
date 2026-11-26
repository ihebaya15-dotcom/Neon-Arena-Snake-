package com.example.game.engine

import com.example.game.model.ArenaConfig
import com.example.game.model.DeathReason
import com.example.game.model.SnakeEntity

data class EliminationRecord(
    val victim: SnakeEntity,
    val killer: SnakeEntity?,
    val reason: DeathReason,
)

/**
 * Resolves lethal collisions between snake heads and enemy snake bodies (or head-on ties)
 * using [ArenaSpatialGrid] 500x500 cells for O(1) local neighbor checks.
 */
class CombatCollisionResolver {
    private val nearbyEnemiesBuffer = ArrayList<SnakeEntity>(12)

    fun resolveSnakeCollisions(
        snakes: List<SnakeEntity>,
        spatialGrid: ArenaSpatialGrid,
        outEliminations: MutableList<EliminationRecord>,
    ) {
        outEliminations.clear()

        for (i in snakes.indices) {
            val attacker = snakes[i]
            if (!attacker.isAlive || attacker.spawnProtectionSec > 0f) continue

            val queryRadius = attacker.headRadius + 46f
            nearbyEnemiesBuffer.clear()
            spatialGrid.forEachSnakeInBounds(
                minX = attacker.headX - queryRadius,
                minY = attacker.headY - queryRadius,
                maxX = attacker.headX + queryRadius,
                maxY = attacker.headY + queryRadius,
            ) { candidate ->
                if (candidate.id != attacker.id &&
                    candidate.isAlive &&
                    candidate.spawnProtectionSec <= 0f
                ) {
                    nearbyEnemiesBuffer.add(candidate)
                }
            }

            var elimination: EliminationRecord? = null
            for (eIdx in nearbyEnemiesBuffer.indices) {
                val enemy = nearbyEnemiesBuffer[eIdx]
                val enemySegments = enemy.segments
                if (enemySegments.isEmpty()) continue

                // Head-to-head check (index 0): shorter snake loses (both on exact tie)
                val headDx = attacker.headX - enemy.headX
                val headDy = attacker.headY - enemy.headY
                val headThreshold = (attacker.headRadius + enemy.headRadius) * ArenaConfig.COMBAT_HITBOX_SCALE
                if (headDx * headDx + headDy * headDy <= headThreshold * headThreshold) {
                    if (attacker.segments.size <= enemy.segments.size) {
                        elimination = EliminationRecord(
                            victim = attacker,
                            killer = enemy,
                            reason = DeathReason.HEAD_ON_COLLISION,
                        )
                        break
                    }
                }

                // Head-to-enemy-body check (index 1..last)
                for (segIdx in 1 until enemySegments.size) {
                    val seg = enemySegments[segIdx]
                    val tailProgress = (enemySegments.lastIndex - segIdx).coerceAtMost(5) / 5f
                    val taper = 0.56f + 0.44f * tailProgress
                    val segRadius = enemy.bodyRadius * taper
                    val threshold = (attacker.headRadius + segRadius) * ArenaConfig.COMBAT_HITBOX_SCALE

                    val dx = attacker.headX - seg.x
                    val dy = attacker.headY - seg.y
                    if (dx * dx + dy * dy <= threshold * threshold) {
                        elimination = EliminationRecord(
                            victim = attacker,
                            killer = enemy,
                            reason = DeathReason.ENEMY_BODY_COLLISION,
                        )
                        break
                    }
                }
                if (elimination != null) break
            }

            if (elimination != null) {
                outEliminations.add(elimination)
            }
        }
    }
}
