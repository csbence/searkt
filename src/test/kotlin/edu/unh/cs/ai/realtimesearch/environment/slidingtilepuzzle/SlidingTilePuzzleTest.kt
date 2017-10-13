package edu.unh.cs.ai.realtimesearch.environment.slidingtilepuzzle

import edu.unh.cs.ai.realtimesearch.environment.SuccessorBundle
import edu.unh.cs.ai.realtimesearch.experiment.ClassicalExperiment
import edu.unh.cs.ai.realtimesearch.experiment.configuration.GeneralExperimentConfiguration
import edu.unh.cs.ai.realtimesearch.experiment.terminationCheckers.StaticTimeTerminationChecker
import edu.unh.cs.ai.realtimesearch.planner.classical.closedlist.heuristic.AStarPlanner
import edu.unh.cs.ai.realtimesearch.planner.realtime.LssLrtaStarPlanner
import edu.unh.cs.ai.realtimesearch.planner.suboptimal.WeightedAStar
import org.junit.Test
import java.io.File
import java.io.FileWriter
import java.io.InputStream
import java.io.OutputStream
import java.util.*
import kotlin.test.assertTrue

class SlidingTilePuzzleTest {

    private fun createInstanceFromString(puzzle: String): InputStream {
        val temp = File.createTempFile("tile", ".puzzle")
        temp.deleteOnExit()
        val tileReader = Scanner(puzzle)

        val fileWriter = FileWriter(temp)
        fileWriter.write("4 4\nstarting:\n")
        while (tileReader.hasNext()) {
            val num = tileReader.nextInt().toString()
            fileWriter.write(num + "\n")
        }
        fileWriter.close()
        return temp.inputStream()
    }

    @Test
    fun testGoalHeuristic() {
        val tiles = "0 1 2 3 4 5 6 7 8 9 10 11 12 13 14 15"
        val instance = createInstanceFromString(tiles)
        val slidingTilePuzzle = SlidingTilePuzzleIO.parseFromStream(instance, 1L)
        val heuristic = slidingTilePuzzle.domain.heuristic(slidingTilePuzzle.initialState)
        assertTrue { heuristic == 0.0 }
    }

    @Test
    fun testHeuristic1() {
        val tiles = "1 0 2 3 4 5 6 7 8 9 10 11 12 13 14 15"
        val instance = createInstanceFromString(tiles)
        val slidingTilePuzzle = SlidingTilePuzzleIO.parseFromStream(instance, 1L)
        val heuristic = slidingTilePuzzle.domain.heuristic(slidingTilePuzzle.initialState)
        assertTrue { heuristic == 1.0 }
    }

    @Test
    fun testHeuristic2() {
        val tiles = "4 1 2 3 0 5 6 7 8 9 10 11 12 13 14 15"
        val instance = createInstanceFromString(tiles)
        val slidingTilePuzzle = SlidingTilePuzzleIO.parseFromStream(instance, 1L)
        val heuristic = slidingTilePuzzle.domain.heuristic(slidingTilePuzzle.initialState)
        assertTrue { heuristic == 1.0 }
    }

    @Test
    fun testHeuristic3() {
        val tiles = "1 2 0 3 5 4 6 7 8 9 10 11 12 13 14 15"
        val instance = createInstanceFromString(tiles)
        val slidingTilePuzzle = SlidingTilePuzzleIO.parseFromStream(instance, 1L)
        val heuristic = slidingTilePuzzle.domain.heuristic(slidingTilePuzzle.initialState)
        assertTrue { heuristic == 4.0 }
    }

    @Test
    fun testHeuristic4() {
        val tiles = "14 13 15 7 11 12 9 5 6 0 2 1 4 8 10 3"
        val instance = createInstanceFromString(tiles)
        val slidingTilePuzzle = SlidingTilePuzzleIO.parseFromStream(instance, 1L)
        val heuristic = slidingTilePuzzle.domain.heuristic(slidingTilePuzzle.initialState)
        assertTrue { heuristic == 41.0 }
    }

    @Test
    fun testHeuristic5() {
        val tiles = "14 9 12 13 15 4 8 10 0 2 1 7 3 11 5 6"
        val instance = createInstanceFromString(tiles)
        val slidingTilePuzzle = SlidingTilePuzzleIO.parseFromStream(instance, 1L)
        val heuristic = slidingTilePuzzle.domain.heuristic(slidingTilePuzzle.initialState)
        assertTrue { heuristic == 50.0 }

    }

    @Test
    fun testSuccessors1() {
        val tiles = "1 2 0 3 5 4 6 7 8 9 10 11 12 13 14 15"
        val instance = createInstanceFromString(tiles)
        val slidingTilePuzzle = SlidingTilePuzzleIO.parseFromStream(instance, 1L)
        val successors = slidingTilePuzzle.domain.successors(slidingTilePuzzle.initialState)
        assertTrue(successors.size == 3)
    }

    @Test
    fun testSuccessors2() {
        val initialState = "1 2 0 3 5 4 6 7 8 9 10 11 12 13 14 15"
        val successor1 = "1 0 2 3 5 4 6 7 8 9 10 11 12 13 14 15"
        val successor2 = "1 2 3 0 5 4 6 7 8 9 10 11 12 13 14 15"
        val successor3 = "1 2 6 3 5 4 0 7 8 9 10 11 12 13 14 15"
        val initInstance = createInstanceFromString(initialState)
        val s1Instance = createInstanceFromString(successor1)
        val s2Instance = createInstanceFromString(successor2)
        val s3Instance = createInstanceFromString(successor3)
        val stp = SlidingTilePuzzleIO.parseFromStream(initInstance, 1L)
        val s1 = SlidingTilePuzzleIO.parseFromStream(s1Instance, 1L)
        val s2 = SlidingTilePuzzleIO.parseFromStream(s2Instance, 1L)
        val s3 = SlidingTilePuzzleIO.parseFromStream(s3Instance, 1L)
        val successors = stp.domain.successors(stp.initialState)
        assertTrue { successors.contains(SuccessorBundle(s1.initialState, SlidingTilePuzzleAction.WEST, 1L)) }
        assertTrue { successors.contains(SuccessorBundle(s2.initialState, SlidingTilePuzzleAction.EAST, 1L)) }
        assertTrue { successors.contains(SuccessorBundle(s3.initialState, SlidingTilePuzzleAction.SOUTH, 1L)) }
    }

    @Test
    fun testSuccessors3() {
        val initialState = "10 2 8 4 15 0 1 14 11 13 3 6 9 7 5 12"
        val successor1 = "10 2 8 4 0 15 1 14 11 13 3 6 9 7 5 12"
        val successor2 = "10 0 8 4 15 2 1 14 11 13 3 6 9 7 5 12"
        val successor3 = "10 2 8 4 15 1 0 14 11 13 3 6 9 7 5 12"
        val successor4 = "10 2 8 4 15 13 1 14 11 0 3 6 9 7 5 12"
        val initInstance = createInstanceFromString(initialState)
        val s1Instance = createInstanceFromString(successor1)
        val s2Instance = createInstanceFromString(successor2)
        val s3Instance = createInstanceFromString(successor3)
        val s4Instance = createInstanceFromString(successor4)
        val stp = SlidingTilePuzzleIO.parseFromStream(initInstance, 1L)
        val s1 = SlidingTilePuzzleIO.parseFromStream(s1Instance, 1L)
        val s2 = SlidingTilePuzzleIO.parseFromStream(s2Instance, 1L)
        val s3 = SlidingTilePuzzleIO.parseFromStream(s3Instance, 1L)
        val s4 = SlidingTilePuzzleIO.parseFromStream(s4Instance, 1L)
        val successors = stp.domain.successors(stp.initialState)
        assertTrue { stp.domain.heuristic(stp.initialState) == 44.0 }
        assertTrue { successors.contains(SuccessorBundle(s1.initialState, SlidingTilePuzzleAction.WEST, 1L)) }
        assertTrue { successors.contains(SuccessorBundle(s2.initialState, SlidingTilePuzzleAction.NORTH, 1L)) }
        assertTrue { successors.contains(SuccessorBundle(s3.initialState, SlidingTilePuzzleAction.EAST, 1L)) }
        assertTrue { successors.contains(SuccessorBundle(s4.initialState, SlidingTilePuzzleAction.SOUTH, 1L)) }
    }


    @Test
    fun testAStar1() {
        val tiles = "0 1 2 3 4 5 6 7 8 9 10 11 12 13 14 15"
        val instance = createInstanceFromString(tiles)
        val slidingTilePuzzle = SlidingTilePuzzleIO.parseFromStream(instance, 1L)
        val initialState = slidingTilePuzzle.initialState
        val aStarAgent = WeightedAStar(slidingTilePuzzle.domain, 1.0)
        assertTrue { aStarAgent.plan(initialState).isEmpty() }
    }

    @Test
    fun testAStar2() {
        val tiles = "1 2 3 0 4 5 6 7 8 9 10 11 12 13 14 15"
        val instance = createInstanceFromString(tiles)
        val slidingTilePuzzle = SlidingTilePuzzleIO.parseFromStream(instance, 1L)
        val initialState = slidingTilePuzzle.initialState
        val aStarAgent = WeightedAStar(slidingTilePuzzle.domain, 1.0)
        val plan = aStarAgent.plan(initialState)
        println(plan)
        assertTrue { plan.isNotEmpty() }
        assertTrue { plan.size == 3 }
    }

    @Test
    fun testAStar3() {
        val tiles = "4 1 2 3 8 5 6 7 12 9 10 11 13 14 15 0"
        val instance = createInstanceFromString(tiles)
        val slidingTilePuzzle = SlidingTilePuzzleIO.parseFromStream(instance, 1L)
        val initialState = slidingTilePuzzle.initialState
        val aStarAgent = WeightedAStar(slidingTilePuzzle.domain, 1.0)
        val plan = aStarAgent.plan(initialState)
        println(plan)
        assertTrue { plan.isNotEmpty() }
        assertTrue { plan.size == 6 }
    }

    @Test
    fun testAStar4() {
        val tiles = "0 4 1 2 8 5 6 3 12 9 10 7 13 14 15 11"
        val instance = createInstanceFromString(tiles)
        val slidingTilePuzzle = SlidingTilePuzzleIO.parseFromStream(instance, 1L)
        val initialState = slidingTilePuzzle.initialState
        val aStarAgent = WeightedAStar(slidingTilePuzzle.domain, 1.0)
        val plan = aStarAgent.plan(initialState)
        println(plan)
        assertTrue { plan.isNotEmpty() }
        assertTrue { plan.size == 12 }
    }

    @Test
    fun testAStar5() {
        val tiles = "4 1 2 3 8 0 10 6 12 5 9 7 13 14 15 11"
        val instance = createInstanceFromString(tiles)
        val slidingTilePuzzle = SlidingTilePuzzleIO.parseFromStream(instance, 1L)
        val initialState = slidingTilePuzzle.initialState
        val aStarAgent = WeightedAStar(slidingTilePuzzle.domain, 1.0)
        val plan = aStarAgent.plan(initialState)
        println(plan)
        assertTrue { plan.isNotEmpty() }
        assertTrue { plan.size == 12 }
    }
//
//    @Test
//    fun testAStar2() {
//        val tiles = tiles(3) {
//            row (1, 0, 2)
//            row (3, 4, 5)
//            row (6, 7, 8)
//        }
//
//        val slidingTilePuzzle = SlidingTilePuzzle(3, 0)
//        val initialState = SlidingTilePuzzleDynamicState(1, 0, tiles, slidingTilePuzzle.heuristic(tiles))
//
//        val aStarAgent = ClassicalAgent(AStarPlanner(slidingTilePuzzle))
//        val aStarExperiment = ClassicalExperiment(GeneralExperimentConfiguration(), aStarAgent, slidingTilePuzzle, initialState)
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
//        val slidingTilePuzzle = SlidingTilePuzzle(3, 0)
//        val initialState = SlidingTilePuzzleDynamicState(2, 0, tiles, slidingTilePuzzle.heuristic(tiles))
//
//        val aStarAgent = ClassicalAgent(AStarPlanner(slidingTilePuzzle))
//        val aStarExperiment = ClassicalExperiment(GeneralExperimentConfiguration(), aStarAgent, slidingTilePuzzle, initialState)
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
//        val slidingTilePuzzle = SlidingTilePuzzle(3, 0)
//        val initialState = SlidingTilePuzzleDynamicState(2, 0, tiles, slidingTilePuzzle.heuristic(tiles))
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
//        val slidingTilePuzzle = SlidingTilePuzzle(3, 0)
//        val initialState = SlidingTilePuzzleDynamicState(2, 2, tiles, slidingTilePuzzle.heuristic(tiles))
//
//        runLssLrtaStart(initialState, slidingTilePuzzle)
//    }
//
//    @Test
//    fun testLssLrtaStarOnFileInput() {
//        val stream = SlidingTilePuzzleTest::class.java.classLoader.getResourceAsStream("input/tiles/korf/4/1")
//        val slidingTilePuzzleInstance = SlidingTilePuzzleIO.parseFromStream(stream, 10000)
//        val slidingTilePuzzle = slidingTilePuzzleInstance.domain
//        val initialState = slidingTilePuzzleInstance.initialState
//
//        runLssLrtaStart(initialState, slidingTilePuzzle)
//    }
//
//    private fun runLssLrtaStart(initialState: SlidingTilePuzzleDynamicState, slidingTilePuzzle: SlidingTilePuzzle) {
//        val environment = SlidingTilePuzzleEnvironment(slidingTilePuzzle, initialState)
//        val terminalCondition = StaticTimeTerminationChecker(50)
//
//        val lsslrtaStarPlanner = LssLrtaStarPlanner(slidingTilePuzzle)
//
//        val lssRTAAgent = RTSAgent(lsslrtaStarPlanner)
//        val lssConfiguration = GeneralExperimentConfiguration()
//        lssConfiguration["singleStepCommitment"] = false
//        val lssRTAExperiment = RTSExperiment(lssConfiguration, lssRTAAgent, environment, terminalCondition)
//
//        lssRTAExperiment.run()
//    }
//
//    @Test
//    fun testAStarFromFileEasy1() {
//        val stream = SlidingTilePuzzleTest::class.java.classLoader.getResourceAsStream("input/tiles/korf/test/easy0")
//        runAStarOnSlidingTilePuzzleFileInput(stream)
//    }
//
//    @Test
//    fun testAStarFromFileEasy2() {
//        val stream = SlidingTilePuzzleTest::class.java.classLoader.getResourceAsStream("input/tiles/korf/test/easy1")
//        runAStarOnSlidingTilePuzzleFileInput(stream)
//    }
//
//    @Test
//    fun testAStarFromFileEasy3() {
//        val stream = SlidingTilePuzzleTest::class.java.classLoader.getResourceAsStream("input/tiles/korf/test/easy2")
//        runAStarOnSlidingTilePuzzleFileInput(stream)
//    }
//
//        @Test
//        fun testAStarFromFileMedium1() {
//            val fileName = "input/tiles/korf/test/medium1"
//            runAStarOnSlidingTilePuzzleFileInput(fileName)
//        }
//
//        @Test
//        fun testAStarFromFileHard() {
//            val fileName = "input/tiles/korf/4/1"
//
//            runAStarOnSlidingTilePuzzleFileInput(fileName)
//        }
//
//    private fun runAStarOnSlidingTilePuzzleFileInput(stream: InputStream) {
//        val slidingTilePuzzleInstance = SlidingTilePuzzleIO.parseFromStream(stream, 10000)
//        val slidingTilePuzzle = slidingTilePuzzleInstance.domain
//        val initialState = slidingTilePuzzleInstance.initialState
//
//        val aStarAgent = ClassicalAgent(AStarPlanner(slidingTilePuzzle))
//        val aStarExperiment = ClassicalExperiment(GeneralExperimentConfiguration(), aStarAgent, slidingTilePuzzle, initialState)
//
//        aStarExperiment.run()
//    }
//
//
}