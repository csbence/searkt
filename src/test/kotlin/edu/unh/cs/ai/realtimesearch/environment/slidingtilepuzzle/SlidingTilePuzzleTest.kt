package edu.unh.cs.ai.realtimesearch.environment.slidingtilepuzzle

import org.junit.Test
import kotlin.test.assertTrue

/**
 * @author Bence Cserna (bence@cserna.net)
 */
class SlidingTilePuzzleTest {

    @Test
    fun testGoalHeuristic() {
        val tiles = tiles(8) {
            row (0, 1, 2)
            row (3, 4, 5)
            row (6, 7, 8)
        }

        val slidingTilePuzzle = SlidingTilePuzzle(3)
        val heuristic = slidingTilePuzzle.heuristic(tiles)
        assertTrue { heuristic == 0.0 }
    }

    @Test
    fun testHeuristic1() {
        val tiles = tiles(8) {
            row (1, 0, 2)
            row (3, 4, 5)
            row (6, 7, 8)
        }

        val slidingTilePuzzle = SlidingTilePuzzle(3)
        val heuristic = slidingTilePuzzle.heuristic(tiles)
        assertTrue { heuristic == 1.0 }
    }

    @Test
    fun testHeuristic2() {
        val tiles = tiles(8) {
            row (3, 1, 2)
            row (0, 4, 5)
            row (6, 7, 8)
        }

        val slidingTilePuzzle = SlidingTilePuzzle(3)
        val heuristic = slidingTilePuzzle.heuristic(tiles)
        assertTrue { heuristic == 1.0 }
    }

    @Test
    fun testHeuristic3() {
        val tiles = tiles(8) {
            row (1, 2, 0)
            row (4, 3, 8)
            row (7, 6, 5)
        }

        val slidingTilePuzzle = SlidingTilePuzzle(3)
        val heuristic = slidingTilePuzzle.heuristic(tiles)
        assertTrue { heuristic == 8.0 }
    }

    private fun tiles(size: Int, init: Array<ByteArray>.() -> Unit): Array<ByteArray> {
        val tiles = Array(size, { ByteArray(0) })
        tiles.init()
        return tiles
    }

    private fun Array<ByteArray>.row(vararg args: Int) {
        val index = indexOfFirst { it.isEmpty() }
        this[index] = args.map { it.toByte() }.toByteArray()
    }
}