package edu.unh.cs.ai.realtimesearch.environment.acrobot

import edu.unh.cs.ai.realtimesearch.environment.DiscretizedState
import edu.unh.cs.ai.realtimesearch.environment.acrobot.configuration.AcrobotConfiguration
import edu.unh.cs.ai.realtimesearch.util.doubleNearEquals
import org.junit.Test
import org.slf4j.LoggerFactory
import kotlin.test.assertTrue

class AcrobotDiscretizedStateTest {

    private val logger = LoggerFactory.getLogger(AcrobotDiscretizedStateTest::class.java)

    private val acrobotConfiguration = AcrobotConfiguration()

    private val positionIncrement1 = getDiscretizationIncrement(acrobotConfiguration.stateConfiguration.positionGranularity1)
    private val positionIncrement2 = getDiscretizationIncrement(acrobotConfiguration.stateConfiguration.positionGranularity2)
    private val velocityIncrement1 = getDiscretizationIncrement(acrobotConfiguration.stateConfiguration.velocityGranularity1)
    private val velocityIncrement2 = getDiscretizationIncrement(acrobotConfiguration.stateConfiguration.velocityGranularity2)

    @Test
    fun testDiscretization1() {
        val state = DiscretizedState(AcrobotState(
                acrobotConfiguration.stateConfiguration.positionGranularity1,
                acrobotConfiguration.stateConfiguration.positionGranularity2 + acrobotConfiguration.stateConfiguration.positionGranularity2,
                acrobotConfiguration.stateConfiguration.velocityGranularity1 + acrobotConfiguration.stateConfiguration.velocityGranularity1,
                acrobotConfiguration.stateConfiguration.velocityGranularity2 + acrobotConfiguration.stateConfiguration.velocityGranularity2))

        assertTrue { doubleNearEquals(state.state.link1.position, state.discretizedState.link1.position) }
        assertTrue { doubleNearEquals(state.state.link2.position, state.discretizedState.link2.position) }
        assertTrue { doubleNearEquals(state.state.link1.velocity, state.discretizedState.link1.velocity) }
        assertTrue { doubleNearEquals(state.state.link2.velocity, state.discretizedState.link2.velocity) }
    }

    @Test
    fun testDiscretization2() {
        val state = DiscretizedState(AcrobotState(
                acrobotConfiguration.stateConfiguration.positionGranularity1 + positionIncrement1,
                acrobotConfiguration.stateConfiguration.positionGranularity2 * 2 + positionIncrement2,
                acrobotConfiguration.stateConfiguration.velocityGranularity1 * 3 + velocityIncrement1,
                acrobotConfiguration.stateConfiguration.velocityGranularity2 * 4 + velocityIncrement2))

        assertTrue { doubleNearEquals(state.discretizedState.link1.position, acrobotConfiguration.stateConfiguration.positionGranularity1) }
        assertTrue { doubleNearEquals(state.discretizedState.link2.position, acrobotConfiguration.stateConfiguration.positionGranularity2 * 2) }
        assertTrue { doubleNearEquals(state.discretizedState.link1.velocity, acrobotConfiguration.stateConfiguration.velocityGranularity1 * 3) }
        assertTrue { doubleNearEquals(state.discretizedState.link2.velocity, acrobotConfiguration.stateConfiguration.velocityGranularity2 * 4) }
        assertTrue { state.state != state.discretizedState }
    }

    @Test
    fun testDiscretization3() {
        val state = DiscretizedState(AcrobotState(
                acrobotConfiguration.stateConfiguration.positionGranularity1 * 2 - positionIncrement1,
                acrobotConfiguration.stateConfiguration.positionGranularity2 * 2 - positionIncrement2,
                acrobotConfiguration.stateConfiguration.velocityGranularity1 * 2 - velocityIncrement1,
                acrobotConfiguration.stateConfiguration.velocityGranularity2 * 2 - velocityIncrement2))

        assertTrue { doubleNearEquals(state.discretizedState.link1.position, acrobotConfiguration.stateConfiguration.positionGranularity1) }
        assertTrue { doubleNearEquals(state.discretizedState.link2.position, acrobotConfiguration.stateConfiguration.positionGranularity2) }
        assertTrue { doubleNearEquals(state.discretizedState.link1.velocity, acrobotConfiguration.stateConfiguration.velocityGranularity1) }
        assertTrue { doubleNearEquals(state.discretizedState.link2.velocity, acrobotConfiguration.stateConfiguration.velocityGranularity2) }
        assertTrue { state.state != state.discretizedState }
    }

    @Test
    fun testStateEquality() {
        val state1 = DiscretizedState(AcrobotState(
                acrobotConfiguration.stateConfiguration.positionGranularity1,
                acrobotConfiguration.stateConfiguration.positionGranularity2,
                acrobotConfiguration.stateConfiguration.velocityGranularity1,
                acrobotConfiguration.stateConfiguration.velocityGranularity2))
        val state2 = DiscretizedState(AcrobotState(
                acrobotConfiguration.stateConfiguration.positionGranularity1 + positionIncrement1,
                acrobotConfiguration.stateConfiguration.positionGranularity2 + positionIncrement2,
                acrobotConfiguration.stateConfiguration.velocityGranularity1 + velocityIncrement1,
                acrobotConfiguration.stateConfiguration.velocityGranularity2 + velocityIncrement2))

        assertTrue { state1 == state2 }
        assertTrue { state1.state != state2.state }
    }

    @Test
    fun testStateHashcode() {
        val state1 = DiscretizedState(AcrobotState(
                acrobotConfiguration.stateConfiguration.positionGranularity1,
                acrobotConfiguration.stateConfiguration.positionGranularity2,
                acrobotConfiguration.stateConfiguration.velocityGranularity1,
                acrobotConfiguration.stateConfiguration.velocityGranularity2))
        val state2 = DiscretizedState(AcrobotState(
                acrobotConfiguration.stateConfiguration.positionGranularity1 + positionIncrement1,
                acrobotConfiguration.stateConfiguration.positionGranularity2 + positionIncrement2,
                acrobotConfiguration.stateConfiguration.velocityGranularity1 + velocityIncrement1,
                acrobotConfiguration.stateConfiguration.velocityGranularity2 + velocityIncrement2))

        assertTrue { state1.hashCode() == state2.hashCode() }
    }

    private fun getDiscretizationIncrement(granularity: Double): Double {
        var increment = 0.1
        while (increment >= granularity)
            increment /= 10.0
        return increment
    }
}


