package edu.unh.cs.ai.realtimesearch.environment.acrobot

import edu.unh.cs.ai.realtimesearch.agent.ClassicalAgent
import edu.unh.cs.ai.realtimesearch.agent.RTSAgent
import edu.unh.cs.ai.realtimesearch.environment.location.Location
import edu.unh.cs.ai.realtimesearch.experiment.ClassicalExperiment
import edu.unh.cs.ai.realtimesearch.experiment.RTSExperiment
import edu.unh.cs.ai.realtimesearch.experiment.configuration.EmptyConfiguration
import edu.unh.cs.ai.realtimesearch.experiment.terminationCheckers.CallsTerminationChecker
import edu.unh.cs.ai.realtimesearch.planner.classical.closedlist.heuristic.AStarPlanner
import edu.unh.cs.ai.realtimesearch.planner.realtime_.LssLrtaStarPlanner
import org.junit.Test
import java.io.File
import java.io.FileInputStream
import kotlin.test.assertTrue

fun doubleNearEquals(a: Double, b: Double): Boolean {
    return a == b || Math.abs(a - b) < 0.00001
}

class AcrobotTest {

    @Test
    fun testGoalHeuristic() {
        val acrobot = Acrobot()
        val heuristic1 = acrobot.heuristic(Acrobot.goal.lowerBound)
        val heuristic2 = acrobot.heuristic(Acrobot.goal.upperBound)
        val heuristic3 = acrobot.heuristic(AcrobotState(Acrobot.goal.verticalLinkPosition1, Acrobot.goal.verticalLinkPosition2, 0.0, 0.0))

        assertTrue { doubleNearEquals(heuristic1, 0.0) }
        assertTrue { doubleNearEquals(heuristic2, 0.0) }
        assertTrue { doubleNearEquals(heuristic3, 0.0) }
    }

    @Test
    fun testHeuristic1() {
        val acrobot = Acrobot()
        val heuristic = acrobot.heuristic(Acrobot.goal.lowerBound - AcrobotState(0.1, 0.0, 0.0, 0.0))
        assertTrue { heuristic > 0.0 }
    }

    @Test
    fun testHeuristic2() {
        val acrobot = Acrobot()
        val heuristic = acrobot.heuristic(Acrobot.goal.upperBound + AcrobotState(0.1, 0.0, 0.0, 0.0))
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
    fun testAStar1() {
        val acrobot = Acrobot()
        val initialState = initialAcrobotState

        val aStarAgent = ClassicalAgent(AStarPlanner(acrobot))
        val aStarExperiment = ClassicalExperiment(EmptyConfiguration, aStarAgent, acrobot, initialState, 1)

        aStarExperiment.run()

//        val aStarAgent = ClassicalAgent(AStarPlanner(slidingTilePuzzle))
//        val aStarExperiment = ClassicalExperiment(EmptyConfiguration, aStarAgent, slidingTilePuzzle, initialState, 1)

//        aStarExperiment.run()
    }

//    @Test
//    fun testAStar2() {
//        val tiles = tiles(3) {
//            row (1, 0, 2)
//            row (3, 4, 5)
//            row (6, 7, 8)
//        }
//
//        val slidingTilePuzzle = SlidingTilePuzzle(3)
//        val initialState = SlidingTilePuzzleState(Location(1, 0), tiles, slidingTilePuzzle.heuristic(tiles))
//
//        val aStarAgent = ClassicalAgent(AStarPlanner(slidingTilePuzzle))
//        val aStarExperiment = ClassicalExperiment(EmptyConfiguration, aStarAgent, slidingTilePuzzle, initialState, 1)
//
//        aStarExperiment.run()
//    }
//
//    @Test
//    fun testAStar3() {
//        val tiles = tiles(3) {
//            row (1, 2, 0)
//            row (3, 4, 5)
//            row (6, 7, 8)
//        }
//
//        val slidingTilePuzzle = SlidingTilePuzzle(3)
//        val initialState = SlidingTilePuzzleState(Location(2, 0), tiles, slidingTilePuzzle.heuristic(tiles))
//
//        val aStarAgent = ClassicalAgent(AStarPlanner(slidingTilePuzzle))
//        val aStarExperiment = ClassicalExperiment(EmptyConfiguration, aStarAgent, slidingTilePuzzle, initialState, 1)
//
//        aStarExperiment.run()
//    }
//
//    @Test
//    fun testLssLrtaStar1() {
//        val tiles = tiles(3) {
//            row (1, 2, 0)
//            row (3, 4, 5)
//            row (6, 7, 8)
//        }
//
//        val slidingTilePuzzle = SlidingTilePuzzle(3)
//        val initialState = SlidingTilePuzzleState(Location(2, 0), tiles, slidingTilePuzzle.heuristic(tiles))
//
//        runLssLrtaStart(initialState, slidingTilePuzzle)
//    }
//
//    @Test
//    fun testLssLrtaStar2() {
//        val tiles = tiles(3) {
//            row (5, 6, 8)
//            row (4, 3, 1)
//            row (2, 7, 0)
//        }
//
//        val slidingTilePuzzle = SlidingTilePuzzle(3)
//        val initialState = SlidingTilePuzzleState(Location(2, 2), tiles, slidingTilePuzzle.heuristic(tiles))
//
//        runLssLrtaStart(initialState, slidingTilePuzzle)
//    }
//
//    @Test
//    fun testLssLrtaStarOnFileInput() {
//        val fileName = "input/tiles/korf/4/1"
//        val slidingTilePuzzleInstance = SlidingTilePuzzleIO.parseFromStream(FileInputStream(File(fileName)))
//        val slidingTilePuzzle = slidingTilePuzzleInstance.domain
//        val initialState = slidingTilePuzzleInstance.initialState
//
//        runLssLrtaStart(initialState, slidingTilePuzzle)
//    }
//
//    private fun runLssLrtaStart(initialState: SlidingTilePuzzleState, slidingTilePuzzle: SlidingTilePuzzle) {
//        val environment = SlidingTilePuzzleEnvironment(slidingTilePuzzle, initialState)
//        val terminalCondition = CallsTerminationChecker(10)
//
//        val lsslrtaStarPlanner = LssLrtaStarPlanner(slidingTilePuzzle)
//
//        val lssRTAAgent = RTSAgent(lsslrtaStarPlanner)
//        val lssRTAExperiment = RTSExperiment<SlidingTilePuzzleState>(EmptyConfiguration, lssRTAAgent, environment, terminalCondition)
//
//        lssRTAExperiment.run()
//    }
//
//    @Test
//    fun testAStarFromFileEasy1() {
//        val fileName = "input/tiles/korf/test/easy0"
//        runAStarOnSlidingTilePuzzleFileInput(fileName)
//    }
//
//    @Test
//    fun testAStarFromFileEasy2() {
//        val fileName = "input/tiles/korf/test/easy1"
//        runAStarOnSlidingTilePuzzleFileInput(fileName)
//    }
//
//    @Test
//    fun testAStarFromFileEasy3() {
//        val fileName = "input/tiles/korf/test/easy2"
//        runAStarOnSlidingTilePuzzleFileInput(fileName)
//    }

}