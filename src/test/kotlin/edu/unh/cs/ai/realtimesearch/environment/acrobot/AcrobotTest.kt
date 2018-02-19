package edu.unh.cs.ai.realtimesearch.environment.acrobot

import edu.unh.cs.ai.realtimesearch.environment.DiscretizedDomain
import edu.unh.cs.ai.realtimesearch.environment.DiscretizedState
import edu.unh.cs.ai.realtimesearch.environment.Domains
import edu.unh.cs.ai.realtimesearch.environment.acrobot.configuration.AcrobotStateConfiguration
import edu.unh.cs.ai.realtimesearch.experiment.ClassicalExperiment
import edu.unh.cs.ai.realtimesearch.experiment.configuration.Configurations
import edu.unh.cs.ai.realtimesearch.experiment.configuration.GeneralExperimentConfiguration
import edu.unh.cs.ai.realtimesearch.planner.Planners
import edu.unh.cs.ai.realtimesearch.planner.classical.closedlist.heuristic.AStarPlanner
import edu.unh.cs.ai.realtimesearch.util.doubleNearEquals
//import groovy.json.JsonOutput
import org.junit.Test
import org.slf4j.LoggerFactory
import java.util.concurrent.TimeUnit
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * @author Mike Bogochow (mgp36@unh.edu)
 */
class AcrobotTest {

    private val logger = LoggerFactory.getLogger(AcrobotTest::class.java)

    @Test
    fun testGoalHeuristic() {
        val acrobot = Acrobot()
        val (endStateLowerBound, endStateUpperBound) = Acrobot.getBoundStates(acrobot.configuration.goalState, acrobot.configuration)
        val heuristic1 = acrobot.heuristic(endStateLowerBound)
        val heuristic2 = acrobot.heuristic(endStateUpperBound)
        val heuristic3 = acrobot.heuristic(AcrobotState(acrobot.configuration.goalState.link1.position, acrobot.configuration.goalState.link2.position, 0.0, 0.0))

        assertTrue { doubleNearEquals(heuristic1, 0.0) }
        assertTrue { doubleNearEquals(heuristic2, 0.0) }
        assertTrue { doubleNearEquals(heuristic3, 0.0) }
    }

    @Test
    fun testHeuristic1() {
        val acrobot = Acrobot()
        val endStateBounds = Acrobot.getBoundStates(acrobot.configuration.goalState, acrobot.configuration)
        val heuristic = acrobot.heuristic(endStateBounds.lowerBound - AcrobotState(acrobot.configuration.stateConfiguration.positionGranularity1, 0.0, 0.0, 0.0))

        assertTrue { heuristic > 0.0 }
    }

    @Test
    fun testHeuristic2() {
        val acrobot = Acrobot()
        val endStateBounds = Acrobot.getBoundStates(acrobot.configuration.goalState, acrobot.configuration)
        val heuristic = acrobot.heuristic(endStateBounds.upperBound + AcrobotState(acrobot.configuration.stateConfiguration.positionGranularity1, 0.0, 0.0, 0.0))
        assertTrue { heuristic > 0.0 }
    }

    @Test
    fun testSuccessors1() {
        val acrobot = Acrobot()
        val state = AcrobotState(0.0, 0.0, 0.0, 0.0)
        val successors = acrobot.successors(state)

        assertTrue { successors.size == AcrobotAction.values().size }
    }

    @Test
    fun testGoal1() {
        val acrobot = Acrobot()
        val endStateBounds = Acrobot.getBoundStates(acrobot.configuration)
        val state1 = endStateBounds.upperBound + AcrobotState(0.1, 0.1, 0.1, 0.1)
        val state2 = endStateBounds.upperBound - AcrobotState(AcrobotLink(acrobot.configuration.goalLink1UpperBound.position / 2, acrobot.configuration.goalLink1UpperBound.velocity), acrobot.configuration.goalState.link2)

        assertTrue { acrobot.isGoal(acrobot.configuration.goalState) }
        assertTrue { acrobot.isGoal(acrobot.getGoals().first()) }
        assertTrue { acrobot.isGoal(endStateBounds.upperBound) }
        assertTrue { acrobot.isGoal(endStateBounds.lowerBound) }
        assertFalse { acrobot.isGoal(acrobot.configuration.initialState) }
        assertFalse { acrobot.isGoal(state1) }
        assertTrue { acrobot.isGoal(state2) }
    }

    @Test
    fun testAStarDiscretized1() {
//        val domain = DiscretizedDomain(Acrobot())
//        val initialState = DiscretizedState(AcrobotState.Companion.defaultInitialState)
//        val experimentConfiguration = GeneralExperimentConfiguration(Domains.ACROBOT.toString(), JsonOutput.toJson(domain.domain.configuration), Planners.A_STAR.toString(), "time")
//        experimentConfiguration[Configurations.ACTION_DURATION.toString()] = AcrobotStateConfiguration.defaultActionDuration
//        experimentConfiguration[Configurations.TIME_LIMIT.toString()] = TimeUnit.NANOSECONDS.convert(5, TimeUnit.MINUTES)
//
//        val aStarAgent = AStarPlanner(domain)
//        val aStarExperiment = ClassicalExperiment(experimentConfiguration, aStarAgent, domain, initialState)
//
//        aStarExperiment.run()
    }

//        @Test
//        fun testLssLrtaStarDiscretized1() {
//            val domain = DiscretizedDomain(Acrobot())
//            val initialState = DiscretizedState(defaultInitialAcrobotState)
//
//            runLssLrtaStarDiscretized(initialState, domain)
//        }
//
//        private fun runLssLrtaStarDiscretized(initialState: DiscretizedState<AcrobotState>, acrobot: DiscretizedDomain<AcrobotState, Acrobot>) {
//            val environment = DiscretizedEnvironment(acrobot, initialState)
//            val terminalCondition = CallsTerminationChecker(10)
//
//            val lsslrtaStarPlanner = LssLrtaStarPlanner(acrobot)
//
//            val lsslrtaStarAgent = RTSAgent(lsslrtaStarPlanner)
//            val lsslrtaStarExperiment = RTSExperiment(EmptyConfiguration, lsslrtaStarAgent, environment, terminalCondition)
//
//            lsslrtaStarExperiment.run()
//        }
}