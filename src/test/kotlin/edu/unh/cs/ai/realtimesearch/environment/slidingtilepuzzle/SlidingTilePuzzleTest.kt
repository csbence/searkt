package edu.unh.cs.ai.realtimesearch.environment.slidingtilepuzzle

import edu.unh.cs.ai.realtimesearch.agent.ClassicalAgent
import edu.unh.cs.ai.realtimesearch.agent.RTSAgent
import edu.unh.cs.ai.realtimesearch.environment.location.Location
import edu.unh.cs.ai.realtimesearch.experiment.ClassicalExperiment
import edu.unh.cs.ai.realtimesearch.experiment.RTSExperiment
import edu.unh.cs.ai.realtimesearch.experiment.configuration.GeneralExperimentConfiguration
import edu.unh.cs.ai.realtimesearch.experiment.terminationCheckers.StaticTimeTerminationChecker
import edu.unh.cs.ai.realtimesearch.planner.classical.closedlist.heuristic.AStarPlanner
import edu.unh.cs.ai.realtimesearch.planner.realtime.LssLrtaStarPlanner
import org.junit.Test
import java.io.InputStream
import kotlin.test.assertTrue

class SlidingTilePuzzleTest {

    @Test
    fun testGoalHeuristic() {
        val tiles = tiles(3) {
            row (0, 1, 2)
            row (3, 4, 5)
            row (6, 7, 8)
        }

        val slidingTilePuzzle = SlidingTilePuzzle(3)
        val heuristic = slidingTilePuzzle.heuristic(tiles)
        assertTrue { heuristic == 0.0 }
    }

    @Test
    fun testHeuristic1() {
        val tiles = tiles(3) {
            row (1, 0, 2)
            row (3, 4, 5)
            row (6, 7, 8)
        }

        val slidingTilePuzzle = SlidingTilePuzzle(3)
        val heuristic = slidingTilePuzzle.heuristic(tiles)
        assertTrue { heuristic == 1.0 }
    }

    @Test
    fun testHeuristic2() {
        val tiles = tiles(3) {
            row (3, 1, 2)
            row (0, 4, 5)
            row (6, 7, 8)
        }

        val slidingTilePuzzle = SlidingTilePuzzle(3)
        val heuristic = slidingTilePuzzle.heuristic(tiles)
        assertTrue { heuristic == 1.0 }
    }

    @Test
    fun testHeuristic3() {
        val tiles = tiles(3) {
            row (1, 2, 0)
            row (4, 3, 8)
            row (7, 6, 5)
        }

        val slidingTilePuzzle = SlidingTilePuzzle(3)
        val heuristic = slidingTilePuzzle.heuristic(tiles)
        assertTrue { heuristic == 8.0 }
    }

    @Test
    fun testSuccessors1() {
        val tiles = tiles(3) {
            row (1, 2, 0)
            row (4, 3, 8)
            row (7, 6, 5)
        }

        val slidingTilePuzzle = SlidingTilePuzzle(3)
        val heuristic = slidingTilePuzzle.heuristic(tiles)

        val state = SlidingTilePuzzleState(Location(2, 0), tiles, heuristic)

        val successors = slidingTilePuzzle.successors(state)

        assertTrue(successors.size == 2)
    }

    @Test
    fun testAStar1() {
        val tiles = tiles(3) {
            row (5, 6, 8)
            row (4, 3, 1)
            row (2, 7, 0)
        }

        val slidingTilePuzzle = SlidingTilePuzzle(3)
        val initialState = SlidingTilePuzzleState(Location(2, 2), tiles, slidingTilePuzzle.heuristic(tiles))

        val aStarAgent = ClassicalAgent(AStarPlanner(slidingTilePuzzle))
        val aStarExperiment = ClassicalExperiment(GeneralExperimentConfiguration(), aStarAgent, slidingTilePuzzle, initialState)

        aStarExperiment.run()
    }

    @Test
    fun testAStar2() {
        val tiles = tiles(3) {
            row (1, 0, 2)
            row (3, 4, 5)
            row (6, 7, 8)
        }

        val slidingTilePuzzle = SlidingTilePuzzle(3)
        val initialState = SlidingTilePuzzleState(Location(1, 0), tiles, slidingTilePuzzle.heuristic(tiles))

        val aStarAgent = ClassicalAgent(AStarPlanner(slidingTilePuzzle))
        val aStarExperiment = ClassicalExperiment(GeneralExperimentConfiguration(), aStarAgent, slidingTilePuzzle, initialState)

        aStarExperiment.run()
    }

    @Test
    fun testAStar3() {
        val tiles = tiles(3) {
            row (1, 2, 0)
            row (3, 4, 5)
            row (6, 7, 8)
        }

        val slidingTilePuzzle = SlidingTilePuzzle(3)
        val initialState = SlidingTilePuzzleState(Location(2, 0), tiles, slidingTilePuzzle.heuristic(tiles))

        val aStarAgent = ClassicalAgent(AStarPlanner(slidingTilePuzzle))
        val aStarExperiment = ClassicalExperiment(GeneralExperimentConfiguration(), aStarAgent, slidingTilePuzzle, initialState)

        aStarExperiment.run()
    }

    @Test
    fun testLssLrtaStar1() {
        val tiles = tiles(3) {
            row (1, 2, 0)
            row (3, 4, 5)
            row (6, 7, 8)
        }

        val slidingTilePuzzle = SlidingTilePuzzle(3)
        val initialState = SlidingTilePuzzleState(Location(2, 0), tiles, slidingTilePuzzle.heuristic(tiles))

        runLssLrtaStart(initialState, slidingTilePuzzle)
    }

    @Test
    fun testLssLrtaStar2() {
        val tiles = tiles(3) {
            row (5, 6, 8)
            row (4, 3, 1)
            row (2, 7, 0)
        }

        val slidingTilePuzzle = SlidingTilePuzzle(3)
        val initialState = SlidingTilePuzzleState(Location(2, 2), tiles, slidingTilePuzzle.heuristic(tiles))

        runLssLrtaStart(initialState, slidingTilePuzzle)
    }

    @Test
    fun testLssLrtaStarOnFileInput() {
        val stream = SlidingTilePuzzleTest::class.java.classLoader.getResourceAsStream("input/tiles/korf/4/1")
        val slidingTilePuzzleInstance = SlidingTilePuzzleIO.parseFromStream(stream)
        val slidingTilePuzzle = slidingTilePuzzleInstance.domain
        val initialState = slidingTilePuzzleInstance.initialState

        runLssLrtaStart(initialState, slidingTilePuzzle)
    }

    private fun runLssLrtaStart(initialState: SlidingTilePuzzleState, slidingTilePuzzle: SlidingTilePuzzle) {
        val environment = SlidingTilePuzzleEnvironment(slidingTilePuzzle, initialState)
        val terminalCondition = StaticTimeTerminationChecker(50)

        val lsslrtaStarPlanner = LssLrtaStarPlanner(slidingTilePuzzle)

        val lssRTAAgent = RTSAgent(lsslrtaStarPlanner)
        val lssConfiguration = GeneralExperimentConfiguration()
        lssConfiguration["singleStepCommitment"] = false
        val lssRTAExperiment = RTSExperiment(lssConfiguration, lssRTAAgent, environment, terminalCondition)

        lssRTAExperiment.run()
    }

    @Test
    fun testAStarFromFileEasy1() {
        val stream = SlidingTilePuzzleTest::class.java.classLoader.getResourceAsStream("input/tiles/korf/test/easy0")
        runAStarOnSlidingTilePuzzleFileInput(stream)
    }

    @Test
    fun testAStarFromFileEasy2() {
        val stream = SlidingTilePuzzleTest::class.java.classLoader.getResourceAsStream("input/tiles/korf/test/easy1")
        runAStarOnSlidingTilePuzzleFileInput(stream)
    }

    @Test
    fun testAStarFromFileEasy3() {
        val stream = SlidingTilePuzzleTest::class.java.classLoader.getResourceAsStream("input/tiles/korf/test/easy2")
        runAStarOnSlidingTilePuzzleFileInput(stream)
    }

    //    @Test
    //    fun testAStarFromFileMedium1() {
    //        val fileName = "input/tiles/korf/test/medium1"
    //        runAStarOnSlidingTilePuzzleFileInput(fileName)
    //    }

    //    @Test
    //    fun testAStarFromFileHard() {
    //        val fileName = "input/tiles/korf/4/1"
    //
    //        runAStarOnSlidingTilePuzzleFileInput(fileName)
    //    }

    private fun runAStarOnSlidingTilePuzzleFileInput(stream: InputStream) {
        val slidingTilePuzzleInstance = SlidingTilePuzzleIO.parseFromStream(stream)
        val slidingTilePuzzle = slidingTilePuzzleInstance.domain
        val initialState = slidingTilePuzzleInstance.initialState

        val aStarAgent = ClassicalAgent(AStarPlanner(slidingTilePuzzle))
        val aStarExperiment = ClassicalExperiment(GeneralExperimentConfiguration(), aStarAgent, slidingTilePuzzle, initialState)

        aStarExperiment.run()
    }


}