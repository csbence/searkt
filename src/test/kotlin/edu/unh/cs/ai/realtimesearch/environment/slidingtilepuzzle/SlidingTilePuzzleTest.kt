package edu.unh.cs.ai.realtimesearch.environment.slidingtilepuzzle

import edu.unh.cs.ai.realtimesearch.agent.ClassicalAgent
import edu.unh.cs.ai.realtimesearch.experiment.ClassicalExperiment
import edu.unh.cs.ai.realtimesearch.planner.classical.closedlist.heuristic.AStarPlanner
import org.junit.Test
import java.io.File
import java.io.FileInputStream
import kotlin.test.assertTrue

/**
 * @author Bence Cserna (bence@cserna.net)
 */
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

        val state = SlidingTilePuzzleState(SlidingTilePuzzleState.Location(2, 0), tiles, heuristic)

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
        val initialState = SlidingTilePuzzleState(SlidingTilePuzzleState.Location(2, 2), tiles, slidingTilePuzzle.heuristic(tiles))

        val aStarAgent = ClassicalAgent(AStarPlanner(slidingTilePuzzle))
        val aStarExperiment = ClassicalExperiment(aStarAgent, slidingTilePuzzle, initialState, 1)

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
        val initialState = SlidingTilePuzzleState(SlidingTilePuzzleState.Location(1, 0), tiles, slidingTilePuzzle.heuristic(tiles))

        val aStarAgent = ClassicalAgent(AStarPlanner(slidingTilePuzzle))
        val aStarExperiment = ClassicalExperiment(aStarAgent, slidingTilePuzzle, initialState, 1)

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
        val initialState = SlidingTilePuzzleState(SlidingTilePuzzleState.Location(2, 0), tiles, slidingTilePuzzle.heuristic(tiles))

        val aStarAgent = ClassicalAgent(AStarPlanner(slidingTilePuzzle))
        val aStarExperiment = ClassicalExperiment(aStarAgent, slidingTilePuzzle, initialState, 1)

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
        val initialState = SlidingTilePuzzleState(SlidingTilePuzzleState.Location(2, 0), tiles, slidingTilePuzzle.heuristic(tiles))
        //
        println("hashcode: ${initialState.hashCode()}")

        //        val environment = SlidingTilePuzzleEnvironment(slidingTilePuzzle, initialState)
        //        val terminalCondition = CallsTerminationChecker(10)
        //
        //        val lsslrtaStarPlanner = LssLrtaStarPlanner(slidingTilePuzzle)
        //        LssLrtaStarPlanner

        //        val lssRTAAgent = RTSAgent(null!!)
        //        val lssRTAExperiment = RTSExperiment(lssRTAAgent, environment, terminalCondition)
        //
        //        lssRTAExperiment.run()
    }

    @Test
    fun testAStarFromFile() {
        val slidingTilePuzzleInstance = SlidingTilePuzzleIO.parseFromStream(FileInputStream(File("input/tiles/korf/4/2")))
        val slidingTilePuzzle = slidingTilePuzzleInstance.slidingTilePuzzle
        val initialState = slidingTilePuzzleInstance.startState

        val aStarAgent = ClassicalAgent(AStarPlanner(slidingTilePuzzle))
        val aStarExperiment = ClassicalExperiment(aStarAgent, slidingTilePuzzle, initialState, 1)

        aStarExperiment.run()
    }


}