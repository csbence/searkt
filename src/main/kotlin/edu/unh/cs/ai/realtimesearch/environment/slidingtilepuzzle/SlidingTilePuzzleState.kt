package edu.unh.cs.ai.realtimesearch.environment.slidingtilepuzzle

import edu.unh.cs.ai.realtimesearch.environment.State
import java.lang.Integer.rotateLeft

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
data class SlidingTilePuzzleState(val zeroLocation: SlidingTilePuzzleState.Location, val tiles: Array<ByteArray>, val heuristic: Double) : State {
    private val hashCode: Int = calculateHashCode()

    private fun calculateHashCode(): Int {
        var hashCode: Int = 0
        for (column in tiles) {
            for (tile in column) {
                hashCode = rotateLeft(hashCode, 1) xor tile.toInt()
            }
        }

        return hashCode xor zeroLocation.hashCode()
    }

    override fun copy(): State {
        return SlidingTilePuzzleState(zeroLocation.copy(), copyTiles(), heuristic)
    }

    fun copyTiles() = Array(tiles.size, { tiles[it].copyOf() })

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

    override fun hashCode(): Int {
        return hashCode
    }

    override fun equals(other: Any?): Boolean {
        return super.equals(other)
    }

}

fun tiles(size: Int, init: Array<ByteArray>.() -> Unit): Array<ByteArray> {
    val tiles = Array(size, { ByteArray(0) })
    tiles.init()
    return tiles
}

fun Array<ByteArray>.row(vararg args: Int) {
    val index = indexOfFirst { it.isEmpty() }
    this[index] = args.map { it.toByte() }.toByteArray()
}

operator fun Array<ByteArray>.get(location: SlidingTilePuzzleState.Location): Byte {
    return this[location.y][location.x]
}

operator fun Array<ByteArray>.set(location: SlidingTilePuzzleState.Location, value: Byte) {
    this[location.y][location.x] = value
}
