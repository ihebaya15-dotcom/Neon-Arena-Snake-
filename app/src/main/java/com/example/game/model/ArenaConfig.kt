package com.example.game.model

object ArenaConfig {
    const val ARENA_WIDTH = 7000f
    const val ARENA_HEIGHT = 7000f
    const val GRID_CELL_SIZE = 140f

    const val TARGET_FOOD_COUNT = 150
    const val FOOD_SPAWN_MARGIN = 90f
    const val FOOD_MAGNET_RADIUS = 58f
    const val FOOD_EAT_RADIUS = 26f

    const val BASE_SPEED = 250f
    const val MIN_ANALOG_SPEED_FACTOR = 0.72f
    const val BOOST_SPEED_MULTIPLIER = 2.0f
    const val MAX_TURN_RATE_RAD_PER_SEC = 5.6f

    const val INITIAL_SEGMENT_COUNT = 18
    const val MIN_BOOST_SEGMENT_COUNT = 12
    const val SEGMENT_SPACING = 13.5f
    const val PATH_SAMPLE_STEP = 4.0f
    const val SELF_COLLISION_SKIP_SEGMENTS = 7
    const val SELF_COLLISION_HITBOX_SCALE = 0.78f
    const val BASE_HEAD_RADIUS = 17f
    const val BASE_BODY_RADIUS = 14.5f

    const val BOOST_LENGTH_DRAIN_PER_SEC = 4.2f
    const val BOOST_PELLET_DROP_INTERVAL_SEC = 0.16f
    const val MAX_DELTA_TIME_SEC = 0.05f
}
