package edu.unh.cs.ai.realtimesearch.environment.acrobot

import edu.unh.cs.ai.realtimesearch.agent.ClassicalAgent
import edu.unh.cs.ai.realtimesearch.environment.DiscretizedDomain
import edu.unh.cs.ai.realtimesearch.environment.DiscretizedEnvironment
import edu.unh.cs.ai.realtimesearch.environment.DiscretizedState
import edu.unh.cs.ai.realtimesearch.experiment.ClassicalExperiment
import edu.unh.cs.ai.realtimesearch.experiment.configuration.GeneralExperimentConfiguration
import edu.unh.cs.ai.realtimesearch.experiment.result.ExperimentResult
import edu.unh.cs.ai.realtimesearch.logging.debug
import edu.unh.cs.ai.realtimesearch.planner.classical.closedlist.heuristic.AStarPlanner
import edu.unh.cs.ai.realtimesearch.util.doubleNearEquals
import org.junit.Test
import org.slf4j.LoggerFactory
import kotlin.test.assertTrue


class AcrobotTest {

    private val logger = LoggerFactory.getLogger(AcrobotTest::class.java)

    private fun printResults(result: ExperimentResult) {
        val domain = DiscretizedDomain(Acrobot())
        val environment = DiscretizedEnvironment(domain)
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
        val heuristic1 = acrobot.heuristic(acrobot.endStateLowerBound)
        val heuristic2 = acrobot.heuristic(acrobot.endStateUpperBound)
        val heuristic3 = acrobot.heuristic(AcrobotState(acrobot.configuration.endState.link1.position, acrobot.configuration.endState.link2.position, 0.0, 0.0))

        assertTrue { doubleNearEquals(heuristic1, 0.0) }
        assertTrue { doubleNearEquals(heuristic2, 0.0) }
        assertTrue { doubleNearEquals(heuristic3, 0.0) }
    }

    @Test
    fun testHeuristic1() {
        val acrobot = Acrobot()
        val heuristic = acrobot.heuristic(acrobot.endStateLowerBound - AcrobotState(acrobot.configuration.stateConfiguration.positionGranularity1, 0.0, 0.0, 0.0))

        assertTrue { heuristic > 0.0 }
    }

    @Test
    fun testHeuristic2() {
        val acrobot = Acrobot()
        val heuristic = acrobot.heuristic(acrobot.endStateUpperBound + AcrobotState(acrobot.configuration.stateConfiguration.positionGranularity1, 0.0, 0.0, 0.0))
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
        val domain = DiscretizedDomain(Acrobot())
        val initialState = DiscretizedState(defaultInitialAcrobotState)

        val aStarAgent = ClassicalAgent(AStarPlanner(domain))
        val aStarExperiment = ClassicalExperiment(GeneralExperimentConfiguration(), aStarAgent, domain, initialState)

//        aStarExperiment.run()
        printResults(aStarExperiment.run())
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