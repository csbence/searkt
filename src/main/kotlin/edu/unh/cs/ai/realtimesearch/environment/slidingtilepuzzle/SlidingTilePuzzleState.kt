package edu.unh.cs.ai.realtimesearch.environment.slidingtilepuzzle

import edu.unh.cs.ai.realtimesearch.environment.State

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
class SlidingTilePuzzleState(val zeroLocation: SlidingTilePuzzleState.Location, val tiles: Array<ByteArray>, val heuristic: Double) : State {
    override fun copy(): State {
        throw UnsupportedOperationException()
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
}

operator fun Array<ByteArray>.get(location: SlidingTilePuzzleState.Location): Byte {
    return this[location.x][location.y]
}

operator fun Array<ByteArray>.set(location: SlidingTilePuzzleState.Location, value: Byte) {
    this[location.x][location.y] = value
}
