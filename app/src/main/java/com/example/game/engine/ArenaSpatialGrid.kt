package com.example.game.engine

import com.example.game.model.ArenaConfig
import com.example.game.model.FoodOrb
import com.example.game.model.SnakeEntity
import kotlin.math.ceil
import kotlin.math.floor

/**
 * Fixed-size 2D spatial hash grid (500x500 world-unit cells) over the 7000x7000 arena.
 * Pre-allocates all bucket lists to avoid per-frame GC allocations at 60 FPS,
 * accelerating both physics queries (food magnet/collision, future snake collisions)
 * and viewport-culled rendering in [ArenaCanvas].
 */
class ArenaSpatialGrid(
    val arenaWidth: Float = ArenaConfig.ARENA_WIDTH,
    val arenaHeight: Float = ArenaConfig.ARENA_HEIGHT,
    val cellSize: Float = ArenaConfig.SPATIAL_CELL_SIZE,
) {
    val cols: Int = ceil(arenaWidth / cellSize).toInt().coerceAtLeast(1)
    val rows: Int = ceil(arenaHeight / cellSize).toInt().coerceAtLeast(1)
    val totalCells: Int = cols * rows

    @PublishedApi
    internal val foodBuckets: Array<ArrayList<FoodOrb>> = Array(totalCells) { ArrayList(12) }

    @PublishedApi
    internal val snakeBuckets: Array<ArrayList<SnakeEntity>> = Array(totalCells) { ArrayList(4) }

    // Deduplication marks so a snake spanning multiple cells is yielded only once per query
    private val snakeStampMap = HashMap<String, Int>(16)
    private var queryStamp = 0

    fun rebuildFoodGrid(orbs: List<FoodOrb>) {
        for (i in 0 until totalCells) {
            foodBuckets[i].clear()
        }
        for (i in orbs.indices) {
            val orb = orbs[i]
            if (!orb.isConsumed) {
                foodBuckets[cellIndex(orb.x, orb.y)].add(orb)
            }
        }
    }

    fun rebuildSnakeGrid(snakes: List<SnakeEntity>) {
        for (i in 0 until totalCells) {
            snakeBuckets[i].clear()
        }
        for (sIdx in snakes.indices) {
            val snake = snakes[sIdx]
            val segments = snake.segments
            if (segments.isEmpty()) {
                snakeBuckets[cellIndex(snake.headX, snake.headY)].add(snake)
                continue
            }
            var prevCell = -1
            for (i in segments.indices) {
                val seg = segments[i]
                val cIdx = cellIndex(seg.x, seg.y)
                if (cIdx != prevCell) {
                    val bucket = snakeBuckets[cIdx]
                    if (!bucket.contains(snake)) {
                        bucket.add(snake)
                    }
                    prevCell = cIdx
                }
            }
        }
    }

    fun queryFoodInRadius(
        centerX: Float,
        centerY: Float,
        radius: Float,
        outOrbs: MutableList<FoodOrb>,
    ) {
        outOrbs.clear()
        forEachFoodInBounds(
            minX = centerX - radius,
            minY = centerY - radius,
            maxX = centerX + radius,
            maxY = centerY + radius,
        ) { orb ->
            outOrbs.add(orb)
        }
    }

    inline fun forEachFoodInBounds(
        minX: Float,
        minY: Float,
        maxX: Float,
        maxY: Float,
        action: (FoodOrb) -> Unit,
    ) {
        val minCol = colOf(minX)
        val maxCol = colOf(maxX)
        val minRow = rowOf(minY)
        val maxRow = rowOf(maxY)

        for (row in minRow..maxRow) {
            val rowOffset = row * cols
            for (col in minCol..maxCol) {
                val bucket = foodBuckets[rowOffset + col]
                for (i in bucket.indices) {
                    action(bucket[i])
                }
            }
        }
    }

    inline fun forEachSnakeInBounds(
        minX: Float,
        minY: Float,
        maxX: Float,
        maxY: Float,
        action: (SnakeEntity) -> Unit,
    ) {
        val stamp = nextQueryStamp()
        val minCol = colOf(minX)
        val maxCol = colOf(maxX)
        val minRow = rowOf(minY)
        val maxRow = rowOf(maxY)

        for (row in minRow..maxRow) {
            val rowOffset = row * cols
            for (col in minCol..maxCol) {
                val bucket = snakeBuckets[rowOffset + col]
                for (i in bucket.indices) {
                    val snake = bucket[i]
                    if (markSnakeVisited(snake.id, stamp)) {
                        action(snake)
                    }
                }
            }
        }
    }

    @PublishedApi
    internal fun nextQueryStamp(): Int {
        queryStamp += 1
        if (queryStamp == Int.MAX_VALUE) {
            snakeStampMap.clear()
            queryStamp = 1
        }
        return queryStamp
    }

    @PublishedApi
    internal fun markSnakeVisited(snakeId: String, stamp: Int): Boolean {
        val prev = snakeStampMap[snakeId]
        if (prev == stamp) return false
        snakeStampMap[snakeId] = stamp
        return true
    }

    @PublishedApi
    internal fun colOf(x: Float): Int =
        floor(x / cellSize).toInt().coerceIn(0, cols - 1)

    @PublishedApi
    internal fun rowOf(y: Float): Int =
        floor(y / cellSize).toInt().coerceIn(0, rows - 1)

    private fun cellIndex(x: Float, y: Float): Int =
        rowOf(y) * cols + colOf(x)

    @PublishedApi
    internal fun foodBucketAt(row: Int, col: Int): List<FoodOrb> =
        foodBuckets[row * cols + col]

    @PublishedApi
    internal fun snakeBucketAt(row: Int, col: Int): List<SnakeEntity> =
        snakeBuckets[row * cols + col]
}
