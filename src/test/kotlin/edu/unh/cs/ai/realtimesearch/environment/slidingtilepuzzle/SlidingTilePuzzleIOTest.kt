package edu.unh.cs.ai.realtimesearch.environment.slidingtilepuzzle

import org.junit.Test
import kotlin.test.assertTrue

class SlidingTilePuzzleIOTest {

    @Test
    fun testParseFromStream() {
        val stream = SlidingTilePuzzleIOTest::class.java.classLoader.getResourceAsStream("input/tiles/korf/4/real/1")
        val slidingTilePuzzleInstance = SlidingTilePuzzleIO.parseFromStream(stream, 0)
        val slidingTilePuzzle = slidingTilePuzzleInstance.domain
        val startState = slidingTilePuzzleInstance.initialState

        println(startState)
        assertTrue(slidingTilePuzzle.size == 4)
        assertTrue(startState.zeroIndex == 9)
    }

    @Test
    fun testParseSolutionFromStream() {
        val stream = SlidingTilePuzzleIOTest::class.java.classLoader.getResourceAsStream("input/tiles/korf/4/real/solution")
        val slidingTilePuzzleInstacne = SlidingTilePuzzleIO.parseFromStream(stream, 1L)
        val slidingTilePuzzle = slidingTilePuzzleInstacne.domain
        val startState = slidingTilePuzzleInstacne.initialState

        println(startState)
        assertTrue(slidingTilePuzzle.size == 4)
        assertTrue { slidingTilePuzzle.heuristic(startState) == 0.0 }
        assertTrue(startState.zeroIndex == 0)
    }
}