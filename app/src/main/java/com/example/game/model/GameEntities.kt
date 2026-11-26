package com.example.game.model

import androidx.compose.ui.graphics.Color

/**
 * Primitive FloatArray-backed circular ring buffer storing 2D (x, y) path history
 * with zero per-frame object allocations.
 */
class PathRingBuffer(initialCapacity: Int = 1024) {
    private var xs = FloatArray(initialCapacity.coerceAtLeast(16))
    private var ys = FloatArray(initialCapacity.coerceAtLeast(16))
    private var headIdx = 0

    var size: Int = 0
        private set

    fun clear() {
        headIdx = 0
        size = 0
    }

    fun pushNewest(x: Float, y: Float) {
        if (size == xs.size) {
            grow()
        }
        val writeIdx = (headIdx + size) % xs.size
        xs[writeIdx] = x
        ys[writeIdx] = y
        size++
    }

    fun dropOldest(count: Int) {
        val toDrop = count.coerceIn(0, size)
        if (toDrop == 0) return
        headIdx = (headIdx + toDrop) % xs.size
        size -= toDrop
    }

    fun xFromNewest(reverseOffset: Int): Float {
        val logicalIdx = size - 1 - reverseOffset
        return xs[(headIdx + logicalIdx) % xs.size]
    }

    fun yFromNewest(reverseOffset: Int): Float {
        val logicalIdx = size - 1 - reverseOffset
        return ys[(headIdx + logicalIdx) % ys.size]
    }

    fun oldestX(): Float = xs[headIdx]

    fun oldestY(): Float = ys[headIdx]

    private fun grow() {
        val oldCap = xs.size
        val newCap = oldCap * 2
        val newXs = FloatArray(newCap)
        val newYs = FloatArray(newCap)
        val firstPart = oldCap - headIdx
        System.arraycopy(xs, headIdx, newXs, 0, firstPart)
        System.arraycopy(ys, headIdx, newYs, 0, firstPart)
        if (headIdx > 0) {
            System.arraycopy(xs, 0, newXs, firstPart, headIdx)
            System.arraycopy(ys, 0, newYs, firstPart, headIdx)
        }
        xs = newXs
        ys = newYs
        headIdx = 0
    }
}

enum class BotPersonality {
    HUNTER,
    SCAVENGER,
    TACTICIAN,
}

data class Vec2(
    var x: Float,
    var y: Float,
)

data class FoodOrb(
    val id: Int,
    var x: Float,
    var y: Float,
    val radius: Float,
    val points: Int,
    val growthValue: Float,
    val colorIndex: Int,
    val pulsePhase: Float,
    val isBoostDrop: Boolean = false,
    val isLootDrop: Boolean = false,
    var isConsumed: Boolean = false,
)

data class DeathBurstParticle(
    var x: Float,
    var y: Float,
    var vx: Float,
    var vy: Float,
    var lifeSec: Float,
    val maxLifeSec: Float,
    val radius: Float,
    val color: Color,
)

data class SnakeEntity(
    val id: String,
    val displayName: String,
    val isPlayer: Boolean,
    val skin: SnakeSkin,
    var headX: Float,
    var headY: Float,
    var angleRadians: Float,
    var currentSpeed: Float,
    var targetLength: Float,
    var score: Int,
    var foodEatenCount: Int,
    var eliminationsCount: Int = 0,
    var isBoosting: Boolean = false,
    var spawnProtectionSec: Float = 0f,
    var isLeader: Boolean = false,
    var isAlive: Boolean = true,
    val personality: BotPersonality? = null,
    var headRadius: Float = ArenaConfig.BASE_HEAD_RADIUS,
    var bodyRadius: Float = ArenaConfig.BASE_BODY_RADIUS,
    val segments: MutableList<Vec2> = mutableListOf(),
    val pathHistory: PathRingBuffer = PathRingBuffer(),
)

data class CameraState(
    var x: Float = ArenaConfig.ARENA_WIDTH / 2f,
    var y: Float = ArenaConfig.ARENA_HEIGHT / 2f,
    var zoom: Float = 1.0f,
)

data class MiniMapBlip(
    val normX: Float,
    val normY: Float,
    val isLargeOrb: Boolean,
)

data class BotMapBlip(
    val normX: Float,
    val normY: Float,
    val color: Color,
    val isLeader: Boolean,
)

data class LeaderboardEntry(
    val snakeId: String,
    val displayName: String,
    val score: Int,
    val rank: Int,
    val color: Color,
    val isPlayer: Boolean,
)

data class KillFeedEvent(
    val id: Long,
    val victimName: String,
    val rewardPoints: Int,
)

data class ArenaHudSnapshot(
    val score: Int = 0,
    val length: Int = ArenaConfig.INITIAL_SEGMENT_COUNT,
    val bestScore: Int = 0,
    val eliminationsCount: Int = 0,
    val elapsedSeconds: Int = 0,
    val isBoostActive: Boolean = false,
    val canBoost: Boolean = true,
    val boostBlockedByMinLength: Boolean = false,
    val spawnProtectionRemainingSec: Float = 0f,
    val playerX: Float = ArenaConfig.ARENA_WIDTH / 2f,
    val playerY: Float = ArenaConfig.ARENA_HEIGHT / 2f,
    val arenaWidth: Float = ArenaConfig.ARENA_WIDTH,
    val arenaHeight: Float = ArenaConfig.ARENA_HEIGHT,
    val playerNormX: Float = 0.5f,
    val playerNormY: Float = 0.5f,
    val playerAngleRadians: Float = 0f,
    val foodBlips: List<MiniMapBlip> = emptyList(),
    val botBlips: List<BotMapBlip> = emptyList(),
    val topThree: List<LeaderboardEntry> = emptyList(),
    val playerRankEntry: LeaderboardEntry? = null,
    val totalSnakesCount: Int = 1,
    val latestPlayerKill: KillFeedEvent? = null,
)

enum class DeathReason {
    ARENA_BOUNDARY,
    ENEMY_BODY_COLLISION,
    HEAD_ON_COLLISION,
}

data class GameResult(
    val finalScore: Int,
    val peakLength: Int,
    val survivedSeconds: Int,
    val foodEaten: Int,
    val eliminationsCount: Int,
    val bestRank: Int,
    val isNewBest: Boolean,
    val deathReason: DeathReason = DeathReason.ARENA_BOUNDARY,
    val killerName: String? = null,
)

sealed interface GamePhase {
    data object Lobby : GamePhase
    data object Playing : GamePhase
    data class GameOver(val result: GameResult) : GamePhase
}
