package edu.unh.cs.ai.realtimesearch.environment.slidingtilepuzzle

import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * @author Bence Cserna (bence@cserna.net)
 */
class SlidingTilePuzzle4StateTest {

    @Test
    fun testGetOperator() {
        val slidingTilePuzzle = SlidingTilePuzzle(4, 1)
        val validTiles = byteArrayOf(1,2,0,3,5,4,6,7,8,9,10,11,12,13,14,15)
        val state = SlidingTilePuzzle4State(2, validTiles, 0.0,
                slidingTilePuzzle.calculateHashCode(validTiles))
        validTiles.forEachIndexed{ index, byte ->
            assertEquals(state[index],byte,"index $index")
        }
    }

    @Test
    fun testGetAndSetAreConsistent() {
        val slidingTilePuzzle = SlidingTilePuzzle(4, 1)
        val validTiles = byteArrayOf(1,2,0,3,5,4,6,7,8,9,10,11,12,13,14,15)
        val state = SlidingTilePuzzle4State(2, kotlin.ByteArray(16, {0.toByte()}),0.0,
                slidingTilePuzzle.calculateHashCode(validTiles))
        validTiles.forEachIndexed{index, byte ->
            assertEquals(0, state[index], "index $index")
            state[index] = byte
            assertEquals(state[index], byte, "index $index")
        }
    }

    @Test
    fun testHeuristic() {
        val slidingTilePuzzle = SlidingTilePuzzle(4, 0)
        val tiles = ByteArray(16, {0.toByte()})
        val slidingTilePuzzle4State = SlidingTilePuzzle4State(0, tiles, 0.0,
                slidingTilePuzzle.calculateHashCode(tiles))

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


    @Test
    fun testTwoStateHeuristic() {
        val tiles = ByteArray(16, {0.toByte()})
        val slidingTilePuzzle = SlidingTilePuzzle(4, 0)
        val stateA = SlidingTilePuzzle4State(0, tiles, 0.0,
                slidingTilePuzzle.calculateHashCode(tiles))
        val stateB = SlidingTilePuzzle4State(0, tiles, 0.0,
                slidingTilePuzzle.calculateHashCode(tiles))

        for (i in 0..15) {
            stateA[i] = i.toByte()
        }

        assertTrue(slidingTilePuzzle.heuristic(stateA, stateA) == 0.0)

        for (i in 15 downTo 0) {
            stateB[i] = i.toByte()
        }

        assertTrue(slidingTilePuzzle.heuristic(stateA, stateB) == slidingTilePuzzle.heuristic(stateB, stateA))

        val stateC = SlidingTilePuzzle4State(0, tiles, 0.0,
                slidingTilePuzzle.calculateHashCode(tiles))

        for (i in 15 downTo 0) {
            stateC[i] = 10
        }

        assertFalse(slidingTilePuzzle.heuristic(stateC, stateB) == slidingTilePuzzle.heuristic(stateB, stateC))
    }
}