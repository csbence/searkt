package edu.unh.cs.ai.realtimesearch.environment.slidingtilepuzzle

import java.io.InputStream
import java.util.*

/**
 * @author Bence Cserna (bence@cserna.net)
 */
object SlidingTilePuzzleIO {

    fun parseFromStream(input: InputStream): SlidingTilePuzzleInstance {
        val inputScanner = Scanner(input)
        inputScanner.useDelimiter("\n")

        val dimension: Int

        try {
            val dimensions = inputScanner.nextLine().trim().split(" ").map { it.toInt() }

            if (dimensions.size != 2) {
                throw InvalidSlidingTilePuzzleException("Only two dimensional puzzles are supported.")
            }

            if (dimensions[0] != dimensions[1]) {
                throw InvalidSlidingTilePuzzleException("Only square puzzles are supported.")
            }

            dimension = dimensions[0]
        } catch (e: NoSuchElementException) {
            throw InvalidSlidingTilePuzzleException("SlidingTilePuzzle's first line is missing. The first line should contain the dimensions", e)
        } catch (e: NumberFormatException) {
            throw InvalidSlidingTilePuzzleException("SlidingTilePuzzle's dimensions must be numbers.", e)
        }

        val slidingTilePuzzle = SlidingTilePuzzle(dimension)
        val puzzle = Array(dimension, { ByteArray(dimension) })

        try {
            val tileList = inputScanner.asSequence().drop(1).take(dimension * dimension).map { it.toInt().toByte() }.toList()
            var zeroLocation: SlidingTilePuzzleState.Location? = null
            tileList.forEachIndexed { i, value ->
                val y = i / dimension
                val x = i % dimension

                puzzle[y][x] = value
                if (value == 0.toByte()) {
                    zeroLocation = SlidingTilePuzzleState.Location(x, y)
                }
            }

            return SlidingTilePuzzleInstance(slidingTilePuzzle, SlidingTilePuzzleState(zeroLocation!!, puzzle, slidingTilePuzzle.heuristic(puzzle)))
        } catch (e: NumberFormatException) {
            throw InvalidSlidingTilePuzzleException("Tile must be a number.", e)
        }
    }

}

class InvalidSlidingTilePuzzleException(message: String, e: Exception? = null) : RuntimeException(message, e)
data class SlidingTilePuzzleInstance(val slidingTilePuzzle: SlidingTilePuzzle, val startState: SlidingTilePuzzleState)