package edu.unh.cs.ai.realtimesearch.environment.acrobot

import edu.unh.cs.ai.realtimesearch.util.doubleNearEquals
import groovy.json.JsonOutput
import org.junit.Test
import org.slf4j.LoggerFactory
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class AcrobotStateTest {

    private val logger = LoggerFactory.getLogger(AcrobotStateTest::class.java)

//    @Test
//    fun testEnergy1() {
//        val state = AcrobotState(0.0, 0.0, 0.0, 0.0)
//        assertTrue { doubleNearEquals(state.kineticEnergy, 0.0) }
//        assertTrue { doubleNearEquals(state.potentialEnergy, 0.0) }
//        assertTrue { doubleNearEquals(state.totalEnergy, 0.0) }
//    }
//
//    @Test
//    fun testEnergy2() {
//        val state = AcrobotState(2.0, 1.0, 0.0, 0.0)
//        assertTrue { doubleNearEquals(state.kineticEnergy, 0.0) }
//    }

    @Test
    fun testAcceleration() {
        val state = AcrobotState.defaultInitialState

        val (accelerationNone1, accelerationNone2) = state.calculateLinkAccelerations(AcrobotAction.NONE)
        assertTrue { doubleNearEquals(accelerationNone1, 0.0) }
        assertTrue { doubleNearEquals(accelerationNone2, 0.0) }

        val (accelerationPositive1, accelerationPositive2) = state.calculateLinkAccelerations(AcrobotAction.POSITIVE)
        assertTrue { accelerationPositive1 < 0.0 }
        assertTrue { accelerationPositive2 > 0.0 }

        val (accelerationNegative1, accelerationNegative2) = state.calculateLinkAccelerations(AcrobotAction.NEGATIVE)
        assertTrue { accelerationNegative1 > 0.0 }
        assertTrue { accelerationNegative2 < 0.0 }
    }

    @Test
    fun testBounds() {
        val lowerBound = AcrobotState(0.0, Math.PI / 2, -1.0, 1.0)
        val upperBound = lowerBound + AcrobotState(Math.PI, Math.PI, Math.PI, Math.PI)
        val state1 = AcrobotState(0.1, 0.1, 0.1, 0.1)
        val state2 = lowerBound + state1
        val state3 = upperBound - state1

        assertTrue  { state2.inBounds(lowerBound, upperBound) }
        assertTrue  { state3.inBounds(lowerBound, upperBound) }
        assertFalse { state1.inBounds(lowerBound, upperBound) }
        assertTrue  { lowerBound.inBounds(lowerBound, upperBound) }
        assertTrue  { upperBound.inBounds(lowerBound, upperBound) }
    }

    @Test
    fun testJSON1() {
        assertTrue { AcrobotState.Companion.defaultInitialState.equals(AcrobotState.fromString(JsonOutput.toJson(AcrobotState.defaultInitialState))) }
    }
}
