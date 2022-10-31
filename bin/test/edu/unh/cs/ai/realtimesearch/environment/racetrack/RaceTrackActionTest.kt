package edu.unh.cs.searkt.environment.racetrack

import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Tests the RaceTrackAction class
 */
class RaceTrackActionTest {

    @Test
    fun testGetRelativeLocation() {
        var action = RaceTrackAction.DOWN
        assertEquals(action.aX, 0)
        assertEquals(action.aY, -1)

        action = RaceTrackAction.UP
        assertEquals(action.aX, 0)
        assertEquals(action.aY, 1)

        action = RaceTrackAction.NO_OP
        assertEquals(action.aX, 0)
        assertEquals(action.aY, 0)

        action = RaceTrackAction.LEFT
        assertEquals(action.aX, -1)
        assertEquals(action.aY, 0)

        action = RaceTrackAction.RIGHT
        assertEquals(action.aX, 1)
        assertEquals(action.aY, 0)

        action = RaceTrackAction.LEFT_UP
        assertEquals(action.aX, -1)
        assertEquals(action.aY, 1)

        action = RaceTrackAction.LEFT_DOWN
        assertEquals(action.aX, -1)
        assertEquals(action.aY, -1)

        action = RaceTrackAction.RIGHT_DOWN
        assertEquals(action.aX, 1)
        assertEquals(action.aY, -1)

        action = RaceTrackAction.RIGHT_UP
        assertEquals(action.aX, 1)
        assertEquals(action.aY, 1)
    }

}