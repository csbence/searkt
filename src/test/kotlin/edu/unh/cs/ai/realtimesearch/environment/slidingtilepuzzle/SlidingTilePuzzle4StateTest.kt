package edu.unh.cs.ai.realtimesearch.environment.slidingtilepuzzle

import org.junit.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * @author Bence Cserna (bence@cserna.net)
 */
class SlidingTilePuzzle4StateTest {


    @Test
    fun testHeuristic() {
        val slidingTilePuzzle = SlidingTilePuzzle(4, 0)
        val slidingTilePuzzle4State = SlidingTilePuzzle4State(0, 0, 0.0)

        for (i in 0..15) {
            slidingTilePuzzle4State[i] = 15
        }

        assertFalse(slidingTilePuzzle.initialHeuristic(slidingTilePuzzle4State) == 0.0)

        for (i in 1..15) {
            slidingTilePuzzle4State[i] = 15
        }

        assertFalse(slidingTilePuzzle.initialHeuristic(slidingTilePuzzle4State) == 0.0)

        slidingTilePuzzle4State[0] = 0

        assertFalse(slidingTilePuzzle.initialHeuristic(slidingTilePuzzle4State) == 0.0)

        for (i in 0..15) {
            slidingTilePuzzle4State[i] = i.toByte()
        }

        assertTrue(slidingTilePuzzle.initialHeuristic(slidingTilePuzzle4State) == 0.0)

    }
}