package com.example.game.model

object ArenaConfig {
    const val ARENA_WIDTH = 7000f
    const val ARENA_HEIGHT = 7000f
    const val GRID_CELL_SIZE = 140f
    const val SPATIAL_CELL_SIZE = 500f

    const val TARGET_FOOD_COUNT = 750
    const val MAX_ACTIVE_LOOT_ORBS = 400
    const val FOOD_SPAWN_MARGIN = 90f
    const val FOOD_MAGNET_RADIUS = 58f
    const val FOOD_EAT_RADIUS = 26f

    const val LOOT_ORB_RADIUS = 13.0f
    const val LOOT_ORB_POINTS = 55
    const val LOOT_ORB_GROWTH = 1.35f

    const val BASE_SPEED = 250f
    const val MIN_ANALOG_SPEED_FACTOR = 0.72f
    const val BOOST_SPEED_MULTIPLIER = 2.0f
    const val MAX_TURN_RATE_RAD_PER_SEC = 5.6f

    const val INITIAL_SEGMENT_COUNT = 18
    const val MIN_BOOST_SEGMENT_COUNT = 12
    const val SEGMENT_SPACING = 13.5f
    const val PATH_SAMPLE_STEP = 4.0f
    const val BASE_HEAD_RADIUS = 17f
    const val BASE_BODY_RADIUS = 14.5f

    const val BOOST_LENGTH_DRAIN_PER_SEC = 4.2f
    const val BOOST_PELLET_DROP_INTERVAL_SEC = 0.16f
    const val MAX_DELTA_TIME_SEC = 0.05f

    // Phase 2: Bots & Combat
    const val MIN_BOT_COUNT = 18
    const val TARGET_BOT_COUNT = 20
    const val MAX_BOT_COUNT = 22
    const val SPAWN_PROTECTION_SEC = 1.8f
    const val BOT_SPAWN_MARGIN = 450f
    const val BOT_MIN_DIST_FROM_PLAYER = 950f
    const val BOT_MIN_DIST_FROM_OTHER_HEAD = 360f
    const val COMBAT_HITBOX_SCALE = 0.80f
    const val ELIMINATION_BONUS_POINTS = 100
}
