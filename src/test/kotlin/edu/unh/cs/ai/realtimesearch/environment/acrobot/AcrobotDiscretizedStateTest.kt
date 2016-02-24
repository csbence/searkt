package edu.unh.cs.ai.realtimesearch.environment.acrobot

import org.junit.Test
import edu.unh.cs.ai.realtimesearch.environment.DiscretizedState
import edu.unh.cs.ai.realtimesearch.logging.debug
import org.slf4j.LoggerFactory
import kotlin.test.assertTrue

class AcrobotDiscretizedStateTest {

    private val logger = LoggerFactory.getLogger(AcrobotDiscretizedStateTest::class.java)

    private val positionIncrement1 = getDiscretizationIncrement(AcrobotState.limits.positionGranularity1)
    private val positionIncrement2 = getDiscretizationIncrement(AcrobotState.limits.velocityGranularity2)
    private val velocityIncrement1 = getDiscretizationIncrement(AcrobotState.limits.positionGranularity1)
    private val velocityIncrement2 = getDiscretizationIncrement(AcrobotState.limits.velocityGranularity2)

    @Test
    fun testDiscretization1() {
        val state = DiscretizedState(AcrobotState(
                AcrobotState.limits.positionGranularity1,
                AcrobotState.limits.positionGranularity2 * 2,
                AcrobotState.limits.velocityGranularity1 * 3,
                AcrobotState.limits.velocityGranularity2 * 4))

        assertTrue { doubleNearEquals(state.state.linkPosition1, state.discretizedState.linkPosition1) }
        assertTrue { doubleNearEquals(state.state.linkPosition2, state.discretizedState.linkPosition2) }
        assertTrue { doubleNearEquals(state.state.linkVelocity1, state.discretizedState.linkVelocity1) }
        assertTrue { doubleNearEquals(state.state.linkVelocity2, state.discretizedState.linkVelocity2) }
    }

    @Test
    fun testDiscretization2() {
        val state = DiscretizedState(AcrobotState(
                AcrobotState.limits.positionGranularity1 + positionIncrement1,
                AcrobotState.limits.positionGranularity2 * 2 + positionIncrement2,
                AcrobotState.limits.velocityGranularity1 * 3 + velocityIncrement1,
                AcrobotState.limits.velocityGranularity2 * 4 + velocityIncrement2))

        assertTrue { doubleNearEquals(state.discretizedState.linkPosition1, AcrobotState.limits.positionGranularity1) }
        assertTrue { doubleNearEquals(state.discretizedState.linkPosition2, AcrobotState.limits.positionGranularity2 * 2) }
        assertTrue { doubleNearEquals(state.discretizedState.linkVelocity1, AcrobotState.limits.velocityGranularity1 * 3) }
        assertTrue { doubleNearEquals(state.discretizedState.linkVelocity2, AcrobotState.limits.velocityGranularity2 * 4) }
        assertTrue { state.state != state.discretizedState }
    }

    @Test
    fun testDiscretization3() {
        val state = DiscretizedState(AcrobotState(
                AcrobotState.limits.positionGranularity1 * 2 - positionIncrement1,
                AcrobotState.limits.positionGranularity2 * 2 - positionIncrement2,
                AcrobotState.limits.velocityGranularity1 * 2 - velocityIncrement1,
                AcrobotState.limits.velocityGranularity2 * 2 - velocityIncrement2))

        assertTrue { doubleNearEquals(state.discretizedState.linkPosition1, AcrobotState.limits.positionGranularity1) }
        assertTrue { doubleNearEquals(state.discretizedState.linkPosition2, AcrobotState.limits.positionGranularity2) }
        assertTrue { doubleNearEquals(state.discretizedState.linkVelocity1, AcrobotState.limits.velocityGranularity1) }
        assertTrue { doubleNearEquals(state.discretizedState.linkVelocity2, AcrobotState.limits.velocityGranularity2) }
        assertTrue { state.state != state.discretizedState }
    }

    private fun getDiscretizationIncrement(granularity: Double): Double {
        var increment = 0.1
        while (increment > granularity)
            increment / 10.0
        return increment
    }
}

