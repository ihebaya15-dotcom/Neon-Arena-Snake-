package com.example.game.engine

import com.example.game.model.ArenaConfig
import com.example.game.model.FoodOrb
import kotlin.math.PI
import kotlin.random.Random

class FoodSpawner(
    private val random: Random = Random.Default,
) {
    private var nextOrbId = 1

    fun populateInitialFood(orbs: MutableList<FoodOrb>) {
        orbs.clear()
        repeat(ArenaConfig.TARGET_FOOD_COUNT) {
            orbs.add(createRandomNaturalOrb())
        }
    }

    fun ensureNaturalFoodCount(orbs: MutableList<FoodOrb>) {
        var naturalCount = 0
        var lootCount = 0
        for (i in orbs.indices) {
            val orb = orbs[i]
            if (!orb.isBoostDrop && !orb.isLootDrop) {
                naturalCount++
            } else if (orb.isLootDrop) {
                lootCount++
            }
        }
        // Trim oldest loot orbs if total loot exceeds cap after massive battles
        if (lootCount > ArenaConfig.MAX_ACTIVE_LOOT_ORBS) {
            var toRemove = lootCount - ArenaConfig.MAX_ACTIVE_LOOT_ORBS
            var idx = 0
            while (idx < orbs.size && toRemove > 0) {
                if (orbs[idx].isLootDrop) {
                    orbs[idx].isConsumed = true
                    toRemove--
                }
                idx++
            }
        }
        while (naturalCount < ArenaConfig.TARGET_FOOD_COUNT) {
            orbs.add(createRandomNaturalOrb())
            naturalCount++
        }
    }

    fun createBoostDropOrb(x: Float, y: Float, colorIndex: Int): FoodOrb {
        val jitterX = (random.nextFloat() - 0.5f) * 14f
        val jitterY = (random.nextFloat() - 0.5f) * 14f
        val clampedX = (x + jitterX).coerceIn(
            ArenaConfig.FOOD_SPAWN_MARGIN * 0.5f,
            ArenaConfig.ARENA_WIDTH - ArenaConfig.FOOD_SPAWN_MARGIN * 0.5f,
        )
        val clampedY = (y + jitterY).coerceIn(
            ArenaConfig.FOOD_SPAWN_MARGIN * 0.5f,
            ArenaConfig.ARENA_HEIGHT - ArenaConfig.FOOD_SPAWN_MARGIN * 0.5f,
        )
        return FoodOrb(
            id = nextOrbId++,
            x = clampedX,
            y = clampedY,
            radius = 5.5f,
            points = 6,
            growthValue = 0.25f,
            colorIndex = colorIndex,
            pulsePhase = random.nextFloat() * (2f * PI.toFloat()),
            isBoostDrop = true,
        )
    }

    fun createLootOrb(x: Float, y: Float, colorIndex: Int): FoodOrb {
        val jitterX = (random.nextFloat() - 0.5f) * 22f
        val jitterY = (random.nextFloat() - 0.5f) * 22f
        val clampedX = (x + jitterX).coerceIn(
            ArenaConfig.FOOD_SPAWN_MARGIN * 0.5f,
            ArenaConfig.ARENA_WIDTH - ArenaConfig.FOOD_SPAWN_MARGIN * 0.5f,
        )
        val clampedY = (y + jitterY).coerceIn(
            ArenaConfig.FOOD_SPAWN_MARGIN * 0.5f,
            ArenaConfig.ARENA_HEIGHT - ArenaConfig.FOOD_SPAWN_MARGIN * 0.5f,
        )
        return FoodOrb(
            id = nextOrbId++,
            x = clampedX,
            y = clampedY,
            radius = ArenaConfig.LOOT_ORB_RADIUS,
            points = ArenaConfig.LOOT_ORB_POINTS,
            growthValue = ArenaConfig.LOOT_ORB_GROWTH,
            colorIndex = colorIndex,
            pulsePhase = random.nextFloat() * (2f * PI.toFloat()),
            isLootDrop = true,
        )
    }

    private fun createRandomNaturalOrb(): FoodOrb {
        val roll = random.nextFloat()
        val (radius, points, growth) = when {
            roll < 0.65f -> Triple(6.5f, 10, 0.35f)
            roll < 0.90f -> Triple(10.0f, 25, 0.75f)
            else -> Triple(14.0f, 50, 1.35f)
        }
        val margin = ArenaConfig.FOOD_SPAWN_MARGIN
        val x = margin + random.nextFloat() * (ArenaConfig.ARENA_WIDTH - margin * 2f)
        val y = margin + random.nextFloat() * (ArenaConfig.ARENA_HEIGHT - margin * 2f)

        return FoodOrb(
            id = nextOrbId++,
            x = x,
            y = y,
            radius = radius,
            points = points,
            growthValue = growth,
            colorIndex = random.nextInt(NEON_ORB_COLOR_COUNT),
            pulsePhase = random.nextFloat() * (2f * PI.toFloat()),
            isBoostDrop = false,
        )
    }

    companion object {
        const val NEON_ORB_COLOR_COUNT = 6
    }
}
