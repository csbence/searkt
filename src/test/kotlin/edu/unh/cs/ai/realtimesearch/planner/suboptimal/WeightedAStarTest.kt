package edu.unh.cs.ai.realtimesearch.planner.suboptimal

import edu.unh.cs.ai.realtimesearch.environment.gridworld.GridWorldIO
import edu.unh.cs.ai.realtimesearch.environment.slidingtilepuzzle.SlidingTilePuzzleIO
import edu.unh.cs.ai.realtimesearch.environment.slidingtilepuzzle.SlidingTilePuzzleTest
import edu.unh.cs.ai.realtimesearch.experiment.configuration.Configurations
import edu.unh.cs.ai.realtimesearch.experiment.configuration.GeneralExperimentConfiguration
import edu.unh.cs.ai.realtimesearch.experiment.terminationCheckers.StaticExpansionTerminationChecker
import org.junit.Test
import java.io.File
import java.io.FileWriter
import java.io.InputStream
import java.util.*
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class WeightedAStarTest {
    private val configuration = GeneralExperimentConfiguration(mutableMapOf(Configurations.WEIGHT.toString() to 1.0))

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
    fun testAStar1() {
        val tiles = "0 1 2 3 4 5 6 7 8 9 10 11 12 13 14 15"
        val instance = createInstanceFromString(tiles)
        val slidingTilePuzzle = SlidingTilePuzzleIO.parseFromStream(instance, 1L)
        val initialState = slidingTilePuzzle.initialState
        val aStarAgent = WeightedAStar(slidingTilePuzzle.domain, configuration)
        kotlin.test.assertTrue { aStarAgent.plan(initialState, StaticExpansionTerminationChecker(1000)).isEmpty() }
    }

    @Test
    fun testAStar2() {
        val tiles = "1 2 3 0 4 5 6 7 8 9 10 11 12 13 14 15"
        val instance = createInstanceFromString(tiles)
        val slidingTilePuzzle = SlidingTilePuzzleIO.parseFromStream(instance, 1L)
        val initialState = slidingTilePuzzle.initialState
        val aStarAgent = WeightedAStar(slidingTilePuzzle.domain, configuration)
        val plan = aStarAgent.plan(initialState, StaticExpansionTerminationChecker(1000))
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
        val plan = aStarAgent.plan(initialState, StaticExpansionTerminationChecker(1000))
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
        val plan = aStarAgent.plan(initialState, StaticExpansionTerminationChecker(1000))
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
        val plan = aStarAgent.plan(initialState, StaticExpansionTerminationChecker(1000))
        println(plan)
        kotlin.test.assertTrue { plan.isNotEmpty() }
        kotlin.test.assertTrue { plan.size == 12 }
    }

    @Test
    fun testAStar6() {
        val optimalSolutionLengths = intArrayOf(57, 55, 59, 56, 56, 52, 52, 50, 46, 59, 57, 45)
        for (i in 12 until 13) {
            val stream = SlidingTilePuzzleTest::class.java.classLoader.getResourceAsStream("input/tiles/korf/4/real/$i")
            val slidingTilePuzzle = SlidingTilePuzzleIO.parseFromStream(stream, 1L)
            val initialState = slidingTilePuzzle.initialState
            val aStarAgent = WeightedAStar(slidingTilePuzzle.domain, configuration)
            val plan = aStarAgent.plan(initialState, StaticExpansionTerminationChecker(1000000000))
            var currentState = initialState
            plan.forEach { action ->
                currentState = slidingTilePuzzle.domain.successors(currentState).first { it.action == action }.state
            }
            assertTrue { slidingTilePuzzle.domain.heuristic(currentState) == 0.0 }
            assertEquals(optimalSolutionLengths[i - 1], plan.size, "instance $i")
        }
    }

    @Test
    fun testAStar7() {
        val tiles = "1 10 4 12 5 3 8 9 6 11 7 2 14 0 13 15"
        val instance = createInstanceFromString(tiles)
        val slidingTilePuzzle = SlidingTilePuzzleIO.parseFromStream(instance, 1L)
        val initialState = slidingTilePuzzle.initialState
        val aStarAgent = WeightedAStar(slidingTilePuzzle.domain, configuration)
        val plan = aStarAgent.plan(initialState, StaticExpansionTerminationChecker(1000000000))
        println("plan length: ${plan.size}")
        println(plan)
        println("expandedNodeCount: ${aStarAgent.expandedNodeCount}")
        kotlin.test.assertTrue { plan.isNotEmpty() }
    }

    @Test
    fun testAStar8() {
        val instanceNumbers = intArrayOf(42, 47, 55)
        val optimalSolutionLengths = intArrayOf(42, 47, 41)
        for ((experimentNumber, i) in instanceNumbers.withIndex()) {
            print("Executing $i...")
            val stream = SlidingTilePuzzleTest::class.java.classLoader.getResourceAsStream("input/tiles/korf/4/real/$i")
            val slidingTilePuzzle = SlidingTilePuzzleIO.parseFromStream(stream, 1L)
            val initialState = slidingTilePuzzle.initialState
            val aStarAgent = WeightedAStar(slidingTilePuzzle.domain, configuration)
            val plan = aStarAgent.plan(initialState, StaticExpansionTerminationChecker(1000))
            var currentState = initialState
            plan.forEach { action ->
                currentState = slidingTilePuzzle.domain.successors(currentState).first { it.action == action }.state
            }
            assertTrue { slidingTilePuzzle.domain.heuristic(currentState) == 0.0 }
            assertEquals(optimalSolutionLengths[experimentNumber], plan.size, "instance $i")
            println("total time: ${aStarAgent.executionNanoTime}")
        }
    }

    @Test
    fun testAStarHardPuzzle() {
        val weight = 1.5
        val configuration = GeneralExperimentConfiguration(mutableMapOf(Configurations.WEIGHT.toString() to weight))

        val instanceNumbers = intArrayOf(1, 3)
        val optimalSolutionLengths = intArrayOf(57, 59)

        for ((experimentNumber, i) in instanceNumbers.withIndex()) {
            print("Executing $i...")
            val stream = SlidingTilePuzzleTest::class.java.classLoader.getResourceAsStream("input/tiles/korf/4/real/$i")
            val slidingTilePuzzle = SlidingTilePuzzleIO.parseFromStream(stream, 1L)
            val initialState = slidingTilePuzzle.initialState

            val aStarAgent = WeightedAStar(slidingTilePuzzle.domain, configuration)
            val plan = aStarAgent.plan(initialState, StaticExpansionTerminationChecker(1000000000))
            var currentState = initialState
            plan.forEach { action ->
                currentState = slidingTilePuzzle.domain.successors(currentState).first { it.action == action }.state
            }
            assertTrue { slidingTilePuzzle.domain.heuristic(currentState) == 0.0 }
            print("...plan size: ${plan.size}...")
            assertTrue { optimalSolutionLengths[experimentNumber] * weight >= plan.size }
            print("nodes expanded ${aStarAgent.expandedNodeCount}...")
            print("nodes generated: ${aStarAgent.generatedNodeCount}...")
            println("total time: ${aStarAgent.executionNanoTime}")

        }
    }


    @Test
    fun testWeightedAStarGridWorld1() {
        val stream = WeightedAStarTest::class.java.classLoader.getResourceAsStream("input/vacuum/cups.vw")
        val gridWorld = GridWorldIO.parseFromStream(stream, 1L)
        val initialState = gridWorld.initialState
        val aStarAgent = WeightedAStar(gridWorld.domain, configuration)
        val plan = aStarAgent.plan(initialState, StaticExpansionTerminationChecker(1000))
        var currentState = initialState
        plan.forEach { action ->
            currentState = gridWorld.domain.successors(currentState).first { it.action == action }.state
        }
        println(plan)
        println(plan.size)
    }


    @Test
    fun testWeightedAStarGridWorld2() {
        val stream = WeightedAStarTest::class.java.classLoader.getResourceAsStream("input/vacuum/maze.vw")
        val gridWorld = GridWorldIO.parseFromStream(stream, 1L)
        val initialState = gridWorld.initialState
        val aStarAgent = WeightedAStar(gridWorld.domain, configuration)
        val plan = aStarAgent.plan(initialState, StaticExpansionTerminationChecker(1000))
        var currentState = initialState
        plan.forEach { action ->
            currentState = gridWorld.domain.successors(currentState).first { it.action == action }.state
        }
        println(plan)
        println(plan.size)
    }

}
