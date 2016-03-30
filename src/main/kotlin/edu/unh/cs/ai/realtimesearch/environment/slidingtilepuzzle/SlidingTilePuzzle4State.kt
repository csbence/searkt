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
 */
data class SlidingTilePuzzle4State(val zeroIndex: Int, var tiles: Long, val heuristic: Double) : State<SlidingTilePuzzle4State> {

    override fun copy(): SlidingTilePuzzle4State {
        return SlidingTilePuzzle4State(zeroIndex, tiles, heuristic)
    }

    fun getIndex(x: Int, y: Int): Int {
        return 4 * y + x
    }

    operator fun get(index: Int): Byte {
        return ((tiles shr (index * 4)).toInt() and 0xF).toByte()
    }

    operator fun set(index: Int, value: Byte) {
        tiles = tiles and (0xFL shl (index * 4)).inv() or (value.toLong() shl (index * 4))
    }

    override fun hashCode(): Int {
        return (tiles shr 32 xor tiles).toInt()
    }

    override fun equals(other: Any?): Boolean {
        return when {
            other == null -> false
            other === this -> true
            other !is SlidingTilePuzzle4State -> false
            else -> tiles == other.tiles
        }
    }
}



