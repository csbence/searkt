package edu.unh.cs.ai.realtimesearch.environment.racetrack

import edu.unh.cs.ai.realtimesearch.environment.location.Location
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Tests the RaceTrackAction class
 */
class RaceTrackActionTest {

    @Test
    fun testGetIndex() {
        var action = RaceTrackAction.DOWN

        assertTrue(action.index == 7)
    }

    @Test
    fun testGetRelativeLocation() {
        var action = RaceTrackAction.DOWN
        assertEquals(action.getAcceleration(), Location(0, -1))

        action = RaceTrackAction.UP
        assertEquals(action.getAcceleration(), Location(0, 1))

        action = RaceTrackAction.NO_OP
        assertEquals(action.getAcceleration(), Location(0, 0))

        action = RaceTrackAction.LEFT
        assertEquals(action.getAcceleration(), Location(-1, 0))

        action = RaceTrackAction.RIGHT
        assertEquals(action.getAcceleration(), Location(1, 0))

        action = RaceTrackAction.LEFT_UP
        assertEquals(action.getAcceleration(), Location(-1, 1))

        action = RaceTrackAction.LEFT_DOWN
        assertEquals(action.getAcceleration(), Location(-1, -1))

        action = RaceTrackAction.RIGHT_DOWN
        assertEquals(action.getAcceleration(), Location(1, -1))

        action = RaceTrackAction.RIGHT_UP
        assertEquals(action.getAcceleration(), Location(1, 1))
    }

}