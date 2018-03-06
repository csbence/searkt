package edu.unh.cs.ai.realtimesearch.planner.suboptimal

import edu.unh.cs.ai.realtimesearch.environment.Action
import edu.unh.cs.ai.realtimesearch.environment.gridworld.GridWorldIO
import edu.unh.cs.ai.realtimesearch.environment.slidingtilepuzzle.SlidingTilePuzzleIO
import edu.unh.cs.ai.realtimesearch.environment.slidingtilepuzzle.SlidingTilePuzzleTest
import edu.unh.cs.ai.realtimesearch.experiment.configuration.ConfigurationExecutor.executeConfiguration
import edu.unh.cs.ai.realtimesearch.experiment.configuration.ExperimentConfiguration
import edu.unh.cs.ai.realtimesearch.experiment.configuration.realtime.LookaheadType
import edu.unh.cs.ai.realtimesearch.experiment.configuration.realtime.TerminationType
import edu.unh.cs.ai.realtimesearch.experiment.terminationCheckers.StaticExpansionTerminationChecker
import edu.unh.cs.ai.realtimesearch.planner.CommitmentStrategy
import org.junit.Test
import java.io.File
import java.io.FileWriter
import java.io.InputStream
import java.util.*
import java.util.concurrent.TimeUnit
import kotlin.system.measureNanoTime
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class WeightedAStarTest {
    private val configuration = ExperimentConfiguration("SLIDING_TILE_PUZZLE_4", null, "WEIGHTED_A_STAR", TerminationType.EXPANSION, "input/tiles/korf/4/real/12", 1000000L, 1000L, 1000000L,
            null, 3.0, LookaheadType.STATIC, CommitmentStrategy.SINGLE, null, null, null, null, null, null, null, null)

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
    fun testRunTime() {
        var plan: List<*> = emptyList<Action>()
        val tiles = "14 1 9 6 4 8 12 5 7 2 3 0 10 11 13 15"
        val instance = createInstanceFromString(tiles)
        val slidingTilePuzzle = SlidingTilePuzzleIO.parseFromStream(instance, 1L)
        val initialState = slidingTilePuzzle.initialState
        val aStarAgent = WeightedAStar(slidingTilePuzzle.domain, configuration)
        val runTime = measureNanoTime {
            plan = aStarAgent.plan(initialState, StaticExpansionTerminationChecker(10L))
        }
        val result = executeConfiguration(configuration)
        println("$runTime >= ${result.experimentRunTime}")
        println("diff ${result.experimentRunTime - runTime}")
        println(plan)
        kotlin.test.assertTrue { plan.isNotEmpty() }
        // kotlin.test.assertTrue { plan.size == 12 }

    }

    @Test
    fun testAStar1() {
        val tiles = "0 1 2 3 4 5 6 7 8 9 10 11 12 13 14 15"
        val instance = createInstanceFromString(tiles)
        val slidingTilePuzzle = SlidingTilePuzzleIO.parseFromStream(instance, 1L)
        val initialState = slidingTilePuzzle.initialState
        val aStarAgent = WeightedAStar(slidingTilePuzzle.domain, configuration)
        kotlin.test.assertTrue { aStarAgent.plan(initialState, StaticExpansionTerminationChecker(1000000L)).isEmpty() }
    }

    @Test
    fun testAStar2() {
        val tiles = "1 2 3 0 4 5 6 7 8 9 10 11 12 13 14 15"
        val instance = createInstanceFromString(tiles)
        val slidingTilePuzzle = SlidingTilePuzzleIO.parseFromStream(instance, 1L)
        val initialState = slidingTilePuzzle.initialState
        val aStarAgent = WeightedAStar(slidingTilePuzzle.domain, configuration)
        val plan = aStarAgent.plan(initialState, StaticExpansionTerminationChecker(1000000L))
        println(plan)
        kotlin.test.assertTrue { plan.isNotEmpty() }
        kotlin.test.assertTrue { plan.size == 3 }
    }

    @Test
    fun testAStar3() {
        val tiles = "4 1 2 3 8 5 6 7 12 9 10 11 13 14 15 0"
        val instance = createInstanceFromString(tiles)
        val slidingTilePuzzle = SlidingTilePuzzleIO.parseFromStream(instance, 1L)
        val initialState = slidingTilePuzzle.initialState
        val aStarAgent = WeightedAStar(slidingTilePuzzle.domain, configuration)
        val plan = aStarAgent.plan(initialState, StaticExpansionTerminationChecker(1000000L))
        println(plan)
        kotlin.test.assertTrue { plan.isNotEmpty() }
        kotlin.test.assertTrue { plan.size == 6 }
    }

    @Test
    fun testAStar4() {
        val tiles = "0 4 1 2 8 5 6 3 12 9 10 7 13 14 15 11"
        val instance = createInstanceFromString(tiles)
        val slidingTilePuzzle = SlidingTilePuzzleIO.parseFromStream(instance, 1L)
        val initialState = slidingTilePuzzle.initialState
        val aStarAgent = WeightedAStar(slidingTilePuzzle.domain, configuration)
        val plan = aStarAgent.plan(initialState, StaticExpansionTerminationChecker(1000000L))
        println(plan)
        kotlin.test.assertTrue { plan.isNotEmpty() }
        kotlin.test.assertTrue { plan.size == 12 }
    }

    @Test
    fun testAStar5() {
        val tiles = "4 1 2 3 8 0 10 6 12 5 9 7 13 14 15 11"
        val instance = createInstanceFromString(tiles)
        val slidingTilePuzzle = SlidingTilePuzzleIO.parseFromStream(instance, 1L)
        val initialState = slidingTilePuzzle.initialState
        val aStarAgent = WeightedAStar(slidingTilePuzzle.domain, configuration)
        val plan = aStarAgent.plan(initialState, StaticExpansionTerminationChecker(1000000L))
        println(plan)
        kotlin.test.assertTrue { plan.isNotEmpty() }
        kotlin.test.assertTrue { plan.size == 12 }
    }

    @Test
    fun testAStar6() {
        val weight = 1.0
        val configuration = ExperimentConfiguration("SLIDING_TILE_PUZZLE_4", null, "EES", TerminationType.EXPANSION,
                null, 1L, 1000L, 1000000L, null, weight, null, null, null, null,
                null, null, null, null, null, null)

        val optimalSolutionLengths = intArrayOf(57, 55, 59, 56, 56, 52, 52, 50, 46, 59, 57, 45)
        for (i in 12 until 13) {
            val stream = SlidingTilePuzzleTest::class.java.classLoader.getResourceAsStream("input/tiles/korf/4/real/$i")
            val slidingTilePuzzle = SlidingTilePuzzleIO.parseFromStream(stream, 1L)
            val initialState = slidingTilePuzzle.initialState
            val aStarAgent = WeightedAStar(slidingTilePuzzle.domain, configuration)
            val plan = aStarAgent.plan(initialState, StaticExpansionTerminationChecker(1000000L))
            var currentState = initialState
            plan.forEach { action ->
                currentState = slidingTilePuzzle.domain.successors(currentState).first { it.action == action }.state
            }
            assertTrue { slidingTilePuzzle.domain.heuristic(currentState) == 0.0 }
            assertEquals(optimalSolutionLengths[i - 1], plan.size, "instance $i")
        }
    }

//    @Test
//    fun testAStar7() {
//        val weight = 1.0
//        val configuration = ExperimentConfiguration("SLIDING_TILE_PUZZLE_4", null, "WEIGHED_A_STAR", TerminationType.EXPANSION,
//            null, 1L, 1000L, 1000000L, null, weight, null, null, null, null,
//            null, null, null, null, null, null)
//
//        var experimentNumber = 0
//        val instanceNumbers = intArrayOf(42, 47, 55)
//        val optimalSolutionLengths = intArrayOf(42, 47, 41)
//        for (i in instanceNumbers) {
//            print("Executing $i...")
//            val stream = SlidingTilePuzzleTest::class.java.classLoader.getResourceAsStream("input/tiles/korf/4/real/$i")
//            val slidingTilePuzzle = SlidingTilePuzzleIO.parseFromStream(stream, 1L)
//            val initialState = slidingTilePuzzle.initialState
//            val aStarAgent = WeightedAStar(slidingTilePuzzle.domain, configuration)
//            val plan = aStarAgent.plan(initialState, StaticExpansionTerminationChecker(1000000L))
//            var currentState = initialState
//            plan.forEach { action ->
//                currentState = slidingTilePuzzle.domain.successors(currentState).first { it.action == action }.state
//            }
//            assertTrue { slidingTilePuzzle.domain.heuristic(currentState) == 0.0 }
//            assertEquals(optimalSolutionLengths[experimentNumber], plan.size, "instance $i")
//            println("total time: ${aStarAgent.executionNanoTime}")
//            experimentNumber++
//        }
//    }

    @Test
    fun testAStarHardPuzzle() {
        val weight = 1.5
        val configuration = ExperimentConfiguration("SLIDING_TILE_PUZZLE_4", null, "WEIGHED_A_STAR", TerminationType.EXPANSION,
                null, 1L, 1000L, 1000000L, null, weight, null, null, null, null,
                null, null, null, null, null, null)

        //val configuration = GeneralExperimentConfiguration(mutableMapOf(Configurations.WEIGHT.toString() to weight))

        val instanceNumbers = intArrayOf(1, 3)
        val optimalSolutionLengths = intArrayOf(57, 59)

        for ((experimentNumber, i) in instanceNumbers.withIndex()) {
            print("Executing $i...")
            val stream = SlidingTilePuzzleTest::class.java.classLoader.getResourceAsStream("input/tiles/korf/4/real/$i")
            val slidingTilePuzzle = SlidingTilePuzzleIO.parseFromStream(stream, 1L)
            val initialState = slidingTilePuzzle.initialState

            val aStarAgent = WeightedAStar(slidingTilePuzzle.domain, configuration)
            val plan = aStarAgent.plan(initialState, StaticExpansionTerminationChecker(1000000L))
            var currentState = initialState
            plan.forEach { action ->
                currentState = slidingTilePuzzle.domain.successors(currentState).first { it.action == action }.state
            }
            assertTrue { slidingTilePuzzle.domain.heuristic(currentState) == 0.0 }
            print("...plan size: ${plan.size}...")
            assertTrue { optimalSolutionLengths[experimentNumber] * weight >= plan.size }
            print("nodes expanded: ${aStarAgent.expandedNodeCount}...")
            print("nodes generated: ${aStarAgent.generatedNodeCount}...")
            println("total time: ${aStarAgent.executionNanoTime}")
        }
    }


//    @Test
//    fun testWeightedAStarGridWorld1() {
//        val stream = WeightedAStarTest::class.java.classLoader.getResourceAsStream("input/vacuum/cups.vw")
//        val gridWorld = GridWorldIO.parseFromStream(stream, 1L)
//        val initialState = gridWorld.initialState
//        val aStarAgent = WeightedAStar(gridWorld.domain, configuration)
//        val plan = aStarAgent.plan(initialState, StaticExpansionTerminationChecker(1000000L))
//        var currentState = initialState
//        plan.forEach { action ->
//            currentState = gridWorld.domain.successors(currentState).first { it.action == action }.state
//        }
//        println(plan)
//        println(plan.size)
//    }
//
//
//    @Test
//    fun testWeightedAStarGridWorld2() {
//        val stream = WeightedAStarTest::class.java.classLoader.getResourceAsStream("input/vacuum/maze.vw")
//        val gridWorld = GridWorldIO.parseFromStream(stream, 1L)
//        val initialState = gridWorld.initialState
//        val aStarAgent = WeightedAStar(gridWorld.domain, configuration)
//        val plan = aStarAgent.plan(initialState, StaticExpansionTerminationChecker(1000000L))
//        var currentState = initialState
//        plan.forEach { action ->
//            currentState = gridWorld.domain.successors(currentState).first { it.action == action }.state
//        }
//        println(plan)
//        println(plan.size)
//    }

}
