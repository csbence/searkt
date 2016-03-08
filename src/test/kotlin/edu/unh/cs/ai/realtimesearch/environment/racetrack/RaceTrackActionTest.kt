package edu.unh.cs.ai.realtimesearch.environment.racetrack

import edu.unh.cs.ai.realtimesearch.environment.location.Location
import org.junit.Test

import org.junit.Assert.*

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

        /*action = RaceTrackAction.NOOP
        assertEquals(action.getAcceleration(), Location(0, 0))*/

        action = RaceTrackAction.LEFT
        assertEquals(action.getAcceleration(), Location(-1, 0))

        action = RaceTrackAction.RIGHT
        assertEquals(action.getAcceleration(), Location(1, 0))

        action = RaceTrackAction.LEFTUP
        assertEquals(action.getAcceleration(), Location(-1, 1))

        action = RaceTrackAction.LEFTDOWN
        assertEquals(action.getAcceleration(), Location(-1, -1))

        action = RaceTrackAction.RIGHTDOWN
        assertEquals(action.getAcceleration(), Location(1, -1))

        action = RaceTrackAction.RIGHTUP
        assertEquals(action.getAcceleration(), Location(1, 1))
    }

}