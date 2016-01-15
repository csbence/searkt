package edu.unh.cs.ai.realtimesearch.environment.slidingtilepuzzle

import org.junit.Test
import java.io.File
import java.io.FileInputStream
import kotlin.test.assertTrue

/**
 * @author Bence Cserna (bence@cserna.net)
 */
class SlidingTilePuzzleIOTest {

    @Test
    fun testParseFromStream() {
        val file = File("input/tiles/korf/5/5")
        val slidingTilePuzzleInstance = SlidingTilePuzzleIO.parseFromStream(FileInputStream(file))
        val slidingTilePuzzle = slidingTilePuzzleInstance.slidingTilePuzzle
        val startState = slidingTilePuzzleInstance.startState

        assertTrue(slidingTilePuzzle.size == 5)
        assertTrue(startState.zeroLocation.x == 2)
        assertTrue(startState.zeroLocation.y == 3)
    }
}