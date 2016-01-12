package edu.unh.cs.ai.realtimesearch.environment.slidingtilepuzzle

import org.junit.Test
import kotlin.test.assertTrue

/**
 * @author Bence Cserna (bence@cserna.net)
 */
class SlidingTilePuzzleStateTest {

    @Test
    fun testHashCode() {
        val tiles1 = tiles(3) {
            row (1, 2, 0)
            row (3, 4, 5)
            row (6, 7, 8)
        }

        val tiles2 = tiles(3) {
            row (1, 2, 0)
            row (3, 4, 5)
            row (6, 7, 8)
        }

        val slidingTilePuzzle = SlidingTilePuzzle(3)

        val location1 = SlidingTilePuzzleState.Location(2, 0)
        val location2 = SlidingTilePuzzleState.Location(2, 0)

        val state1 = SlidingTilePuzzleState(location1, tiles1, slidingTilePuzzle.heuristic(tiles1))
        val state2 = SlidingTilePuzzleState(location2, tiles2, slidingTilePuzzle.heuristic(tiles2))

        assertTrue(location1 == location2)
        println(location1.hashCode())
        println(location2.hashCode())

        var byte: Byte = 0

        println(tiles1.hashCode())
        println(tiles2.hashCode())

        println(state1.hashCode())
        println(state2.hashCode())
        assertTrue(state1 == state2)
        assertTrue(state1.hashCode() == state2.hashCode())
    }
}