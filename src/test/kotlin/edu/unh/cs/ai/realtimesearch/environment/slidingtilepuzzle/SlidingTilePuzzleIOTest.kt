package edu.unh.cs.ai.realtimesearch.environment.slidingtilepuzzle

import org.junit.Test
import kotlin.test.assertTrue

class SlidingTilePuzzleIOTest {

    @Test
    fun testParseFromStream() {
        val stream = SlidingTilePuzzleIOTest::class.java.classLoader.getResourceAsStream("input/tiles/korf/5/1")
        val slidingTilePuzzleInstance = SlidingTilePuzzleIO.parseFromStream(stream, 0)
        val slidingTilePuzzle = slidingTilePuzzleInstance.domain
        val startState = slidingTilePuzzleInstance.initialState

        assertTrue(slidingTilePuzzle.size == 5)
        assertTrue(startState.zeroX == 2)
        assertTrue(startState.zeroY == 3)
    }
}