package edu.unh.cs.ai.realtimesearch.environment.acrobot

import edu.unh.cs.ai.realtimesearch.agent.ClassicalAgent
import edu.unh.cs.ai.realtimesearch.agent.RTSAgent
import edu.unh.cs.ai.realtimesearch.environment.DiscretizedState
import edu.unh.cs.ai.realtimesearch.experiment.ClassicalExperiment
import edu.unh.cs.ai.realtimesearch.experiment.ExperimentResult
import edu.unh.cs.ai.realtimesearch.experiment.RTSExperiment
import edu.unh.cs.ai.realtimesearch.experiment.configuration.EmptyConfiguration
import edu.unh.cs.ai.realtimesearch.experiment.terminationCheckers.CallsTerminationChecker
import edu.unh.cs.ai.realtimesearch.planner.classical.closedlist.heuristic.AStarPlanner
import edu.unh.cs.ai.realtimesearch.planner.realtime.LssLrtaStarPlanner
import edu.unh.cs.ai.realtimesearch.environment.State
import edu.unh.cs.ai.realtimesearch.logging.debug
import edu.unh.cs.ai.realtimesearch.logging.trace
import org.junit.Test
import org.slf4j.LoggerFactory
import kotlin.test.assertTrue

fun doubleNearEquals(a: Double, b: Double): Boolean {
    return a == b || Math.abs(a - b) < 0.00001
}

class AcrobotTest {

    private val logger = LoggerFactory.getLogger(AcrobotTest::class.java)

    private fun printResults(result: ExperimentResult, initialState: DiscretizedState<AcrobotState>) {
        var environment = DiscretizedAcrobotEnvironment(DiscretizedAcrobot(), initialState)
//        for (action in result.actions) {
////            logger.debug { "Accelerations: ${environment.getState().state.calculateLinkAccelerations(action as AcrobotAction)}" }
//            environment.step(action)
//            logger.debug { "$action: ${environment.getState()}" }
//        }

        logger.debug { "Final state: ${environment.getState()} (goal:${environment.isGoal()})" }

        logger.debug { "Expanded nodes: ${result.expandedNodes}" }
        logger.debug { "Generated nodes: ${result.generatedNodes}" }
        logger.debug { "Path length: ${result.pathLength} (alt: ${result.actions.size})" }
        logger.debug { "Runtime (ms): ${result.timeInMillis}" }
    }

    @Test
    fun testGoalHeuristic() {
        val acrobot = Acrobot()
        val heuristic1 = acrobot.heuristic(acrobot.configuration.endStateLowerBound)
        val heuristic2 = acrobot.heuristic(acrobot.configuration.endStateUpperBound)
        val heuristic3 = acrobot.heuristic(AcrobotState(acrobot.configuration.endState.linkPosition1, acrobot.configuration.endState.linkPosition2, 0.0, 0.0))

        assertTrue { doubleNearEquals(heuristic1, 0.0) }
        assertTrue { doubleNearEquals(heuristic2, 0.0) }
        assertTrue { doubleNearEquals(heuristic3, 0.0) }
    }

    @Test
    fun testHeuristic1() {
        val acrobot = Acrobot()
        val heuristic = acrobot.heuristic(acrobot.configuration.endStateLowerBound - AcrobotState(acrobot.configuration.stateConfiguration.positionGranularity1, 0.0, 0.0, 0.0))

        assertTrue { heuristic > 0.0 }
    }

    @Test
    fun testHeuristic2() {
        val acrobot = Acrobot()
        val heuristic = acrobot.heuristic(acrobot.configuration.endStateUpperBound + AcrobotState(acrobot.configuration.stateConfiguration.positionGranularity1, 0.0, 0.0, 0.0))
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
    fun testAStarDiscretized1() {
        val acrobot = DiscretizedAcrobot()
        val initialState = DiscretizedState(defaultInitialAcrobotState)

        val aStarAgent = ClassicalAgent(AStarPlanner(acrobot))
        val aStarExperiment = ClassicalExperiment(EmptyConfiguration, aStarAgent, acrobot, initialState, 1)

//        aStarExperiment.run()
        printResults(aStarExperiment.run().first(), initialState)
    }

//    @Test
//    fun testAStar1() {
//        val acrobot = Acrobot()
//        val initialState = initialAcrobotState
//
//        val aStarAgent = ClassicalAgent(AStarPlanner(acrobot))
//        val aStarExperiment = ClassicalExperiment(EmptyConfiguration, aStarAgent, acrobot, initialState, 1)
//
//        aStarExperiment.run()
//    }



//    @Test
//    fun testLssLrtaStar1() {
//        val acrobot = Acrobot()
//        val initialState = initialAcrobotState
//
//        runLssLrtaStart(initialState, acrobot)
//    }
//
//    private fun runLssLrtaStart(initialState: AcrobotState, acrobot: Acrobot) {
//        val environment = AcrobotEnvironment(acrobot, initialState)
//        val terminalCondition = CallsTerminationChecker(10)
//
//        val lsslrtaStarPlanner = LssLrtaStarPlanner(acrobot)
//
//        val lsslrtaStarAgent = RTSAgent(lsslrtaStarPlanner)
//        val lsslrtaStarExperiment = RTSExperiment(EmptyConfiguration, lsslrtaStarAgent, environment, terminalCondition)
//
//        lsslrtaStarExperiment.run()
//    }
}