package edu.unh.cs.ai.realtimesearch.environment.pointrobot

import edu.unh.cs.ai.realtimesearch.environment.pointrobotlost.PointRobotLOSTState
import org.junit.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * @author Bence Cserna (bence@cserna.net)
 */
class PointRobotStateTest {

    @Test
    fun hashAndEquals() {
        val state1 = PointRobotLOSTState(1.0, 2.0)
        val state2 = PointRobotLOSTState(1.0, 2.0)

        val state3 = PointRobotLOSTState(2.0, 2.0)
        val state4 = PointRobotLOSTState(2.23423, 2.1)

        assertTrue(state1.hashCode() == state1.hashCode())
        assertTrue(state1.hashCode() == state2.hashCode())
        assertTrue(state1 == state2)
        assertFalse(state1.hashCode() == state3.hashCode())

        // Rounding test
        assertTrue(state3.hashCode() == state4.hashCode())
        assertTrue(state3 == state4)
    }

}