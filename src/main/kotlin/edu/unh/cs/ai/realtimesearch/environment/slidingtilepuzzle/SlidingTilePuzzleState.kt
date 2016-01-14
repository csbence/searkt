package edu.unh.cs.ai.realtimesearch.environment.slidingtilepuzzle

import edu.unh.cs.ai.realtimesearch.environment.State
import java.util.*

/**
 * State of a sliding tile puzzle.
 *
 *  width (x)
 * -------
 * |0|1|2|
 * |3|4|5| height(y)
 * |6|7|8|
 * -------
 *
 * (0, 0) == 0
 * (1, 0) == 1
 * (0, 1) == 3
 *
 * @author Bence Cserna (bence@cserna.net)
 */
data class SlidingTilePuzzleState(val zeroLocation: SlidingTilePuzzleState.Location, val tiles: SlidingTilePuzzleState.Tiles, val heuristic: Double) : State<SlidingTilePuzzleState> {
    private val hashCode: Int = calculateHashCode()

    private fun calculateHashCode(): Int {
        var hashCode: Int = tiles.hashCode()
        return hashCode xor zeroLocation.hashCode()
    }

    override fun copy(): SlidingTilePuzzleState {
        return SlidingTilePuzzleState(zeroLocation.copy(), tiles.copy(), heuristic)
    }

    data class Location(val x: Int, val y: Int) {
        operator fun plus(rhs: Location): Location {
            return Location(x + rhs.x, y + rhs.y)
        }

        /**
         * Check if location is inside the boundaries.
         * The lower bound is inclusive the upper bound is exclusive.
         */
        fun inBounds(upperBound: Int, lowerBound: Int = 0): Boolean {
            return x >= lowerBound && y >= lowerBound && x < upperBound && y < upperBound
        }
    }

    class Tiles(val dimension: Int, tiles: ByteArray? = null) {
        val tiles: ByteArray

        init {
            this.tiles = tiles ?: ByteArray(dimension * dimension)
        }

        public fun copy(): Tiles {
            return Tiles(dimension, tiles.clone())
        }

        override fun hashCode(): Int {
            var hashCode = 0

            tiles.forEach {
                hashCode = Integer.rotateLeft(hashCode, 1) xor it.toInt()
            }

            return hashCode xor dimension
        }

        override fun equals(other: Any?): Boolean {
            return when {
                other == null -> false
                other === this -> true
                other !is Tiles -> false
                else -> dimension == other.dimension && Arrays.equals(this.tiles, other.tiles)
            }
        }

        public inline fun getIndex(x: Int, y: Int): Int {
            return dimension * y + x
        }

        public operator fun get(index: Int): Byte {
            return tiles[index]
        }

        public operator fun set(index: Int, value: Byte) {
            tiles[index] = value
        }

        public operator fun get(location: SlidingTilePuzzleState.Location): Byte {
            return tiles[location.y * dimension + location.x]
        }

        public operator fun set(location: SlidingTilePuzzleState.Location, value: Byte) {
            tiles[location.y * dimension + location.x] = value
        }
    }

    override fun hashCode(): Int {
        return hashCode
    }

    override fun equals(other: Any?): Boolean {
        return when {
            other == null -> false
            other === this -> true
            other !is SlidingTilePuzzleState -> false
            else -> zeroLocation == other.zeroLocation && tiles == other.tiles
        }
    }

}

fun tiles(size: Int, init: SlidingTilePuzzleState.Tiles.() -> Unit): SlidingTilePuzzleState.Tiles {
    val internalTiles = ByteArray(size * size)
    internalTiles.forEachIndexed { i, byte -> internalTiles[i] = -1 }
    val tiles = SlidingTilePuzzleState.Tiles(size, internalTiles)

    tiles.init()
    return tiles
}

fun SlidingTilePuzzleState.Tiles.row(vararg args: Int) {
    val minusOne: Byte = -1
    val index = tiles.indexOfFirst { it == minusOne }
    val row = args.map { it.toByte() }.toByteArray()

    System.arraycopy(row, 0, this.tiles, index, this.dimension)
}




