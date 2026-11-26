package com.example.game.model

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
)

data class SnakeEntity(
    val id: String,
    val isPlayer: Boolean,
    val skin: SnakeSkin,
    var headX: Float,
    var headY: Float,
    var angleRadians: Float,
    var currentSpeed: Float,
    var targetLength: Float,
    var score: Int,
    var foodEatenCount: Int,
    var isBoosting: Boolean,
    var headRadius: Float = ArenaConfig.BASE_HEAD_RADIUS,
    var bodyRadius: Float = ArenaConfig.BASE_BODY_RADIUS,
    val segments: MutableList<Vec2> = mutableListOf(),
    val pathHistory: MutableList<Vec2> = mutableListOf(),
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

data class ArenaHudSnapshot(
    val score: Int = 0,
    val length: Int = ArenaConfig.INITIAL_SEGMENT_COUNT,
    val bestScore: Int = 0,
    val elapsedSeconds: Int = 0,
    val isBoostActive: Boolean = false,
    val canBoost: Boolean = true,
    val boostBlockedByMinLength: Boolean = false,
    val playerX: Float = ArenaConfig.ARENA_WIDTH / 2f,
    val playerY: Float = ArenaConfig.ARENA_HEIGHT / 2f,
    val arenaWidth: Float = ArenaConfig.ARENA_WIDTH,
    val arenaHeight: Float = ArenaConfig.ARENA_HEIGHT,
    val playerNormX: Float = 0.5f,
    val playerNormY: Float = 0.5f,
    val playerAngleRadians: Float = 0f,
    val foodBlips: List<MiniMapBlip> = emptyList(),
)

enum class DeathReason {
    ARENA_BOUNDARY,
    SELF_COLLISION,
}

data class GameResult(
    val finalScore: Int,
    val peakLength: Int,
    val survivedSeconds: Int,
    val foodEaten: Int,
    val isNewBest: Boolean,
    val deathReason: DeathReason = DeathReason.ARENA_BOUNDARY,
)

sealed interface GamePhase {
    data object Lobby : GamePhase
    data object Playing : GamePhase
    data class GameOver(val result: GameResult) : GamePhase
}
