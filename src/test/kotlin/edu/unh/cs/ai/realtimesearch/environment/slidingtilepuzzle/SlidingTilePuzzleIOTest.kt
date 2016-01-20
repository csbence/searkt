package edu.unh.cs.ai.realtimesearch.environment.slidingtilepuzzle

import org.junit.Test
import java.io.File
import java.io.FileInputStream
import kotlin.test.assertTrue

class SlidingTilePuzzleIOTest {

    @Test
    fun testParseFromStream() {
        val file = File("input/tiles/korf/5/1")
        val slidingTilePuzzleInstance = SlidingTilePuzzleIO.parseFromStream(FileInputStream(file))
        val slidingTilePuzzle = slidingTilePuzzleInstance.domain
        val startState = slidingTilePuzzleInstance.initialState

        assertTrue(slidingTilePuzzle.size == 5)
        assertTrue(startState.zeroLocation.x == 2)
        assertTrue(startState.zeroLocation.y == 3)
    }
}