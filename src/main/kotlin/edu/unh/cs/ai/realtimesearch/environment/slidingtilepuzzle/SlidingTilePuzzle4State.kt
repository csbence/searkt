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
 */
data class SlidingTilePuzzle4State(val zeroIndex: Int, var tiles: ByteArray, val heuristic: Double) : State<SlidingTilePuzzle4State> {

    override fun copy(): SlidingTilePuzzle4State = SlidingTilePuzzle4State(zeroIndex, ByteArray(16, {tiles[it]}), heuristic)

    fun getIndex(x: Int, y: Int): Int = 4 * y + x

    operator fun get(index: Int): Byte = tiles[index]

    operator fun set(index: Int, value: Byte) {
        tiles[index] = value
    }

    override fun hashCode(): Int {
        var hashCode = 0
        tiles.forEach { byte -> hashCode = (hashCode shl 1) xor byte.toInt() }
        return hashCode
    }

    override fun equals(other: Any?): Boolean = when {
        other == null -> false
        other === this -> true
        other !is SlidingTilePuzzle4State -> false
        else -> tiles contentEquals other.tiles
    }
}



