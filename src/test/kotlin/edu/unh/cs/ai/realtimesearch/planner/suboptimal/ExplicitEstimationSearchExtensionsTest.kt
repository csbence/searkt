package edu.unh.cs.ai.realtimesearch.planner.suboptimal

import edu.unh.cs.ai.realtimesearch.environment.slidingtilepuzzle.SlidingTilePuzzleIO
import edu.unh.cs.ai.realtimesearch.environment.slidingtilepuzzle.SlidingTilePuzzleTest
import edu.unh.cs.ai.realtimesearch.experiment.configuration.ExperimentConfiguration
import edu.unh.cs.ai.realtimesearch.experiment.configuration.realtime.TerminationType
import edu.unh.cs.ai.realtimesearch.experiment.terminationCheckers.StaticExpansionTerminationChecker
import org.junit.Test
import java.io.File
import java.io.FileWriter
import java.io.InputStream
import java.util.*
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ExplicitEstimationSearchExtensionsTest {

    private val configuration = ExperimentConfiguration("SLIDING_TILE_PUZZLE_4", null, "EES", TerminationType.EXPANSION,
            null, 1L, 1000L, 1000000L, null, 1.0, null, null, null, null,
            null, null, null, null, null, null)

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
    fun testEEETS1() {
        val tiles = "0 1 2 3 4 5 6 7 8 9 10 11 12 13 14 15"
        val instance = createInstanceFromString(tiles)
        val slidingTilePuzzle = SlidingTilePuzzleIO.parseFromStream(instance, 1L)
        val initialState = slidingTilePuzzle.initialState
        val eesAgent = ExplicitEstimationTildeSearch(slidingTilePuzzle.domain, configuration)
        kotlin.test.assertTrue { eesAgent.plan(initialState, StaticExpansionTerminationChecker(1000)).isEmpty() }
    }

    @Test
    fun testEETS2() {
        val tiles = "1 2 3 0 4 5 6 7 8 9 10 11 12 13 14 15"
        val instance = createInstanceFromString(tiles)
        val slidingTilePuzzle = SlidingTilePuzzleIO.parseFromStream(instance, 1L)
        val initialState = slidingTilePuzzle.initialState
        val eesAgent = ExplicitEstimationTildeSearch(slidingTilePuzzle.domain, configuration)
        val plan = eesAgent.plan(initialState, StaticExpansionTerminationChecker(1000))
        println(plan)
        kotlin.test.assertTrue { plan.isNotEmpty() }
        kotlin.test.assertTrue { plan.size == 3 }
    }

    @Test
    fun testEETS3() {
        val tiles = "4 1 2 3 8 5 6 7 12 9 10 11 13 14 15 0"
        val instance = createInstanceFromString(tiles)
        val slidingTilePuzzle = SlidingTilePuzzleIO.parseFromStream(instance, 1L)
        val initialState = slidingTilePuzzle.initialState
        val eesAgent= ExplicitEstimationTildeSearch(slidingTilePuzzle.domain, configuration)
        val plan = eesAgent.plan(initialState, StaticExpansionTerminationChecker(1000))
        println(plan)
        kotlin.test.assertTrue { plan.isNotEmpty() }
        kotlin.test.assertTrue { plan.size == 6 }
    }

    @Test
    fun testEETS4() {
        val tiles = "0 4 1 2 8 5 6 3 12 9 10 7 13 14 15 11"
        val instance = createInstanceFromString(tiles)
        val slidingTilePuzzle = SlidingTilePuzzleIO.parseFromStream(instance, 1L)
        val initialState = slidingTilePuzzle.initialState
        val eesAgent = ExplicitEstimationTildeSearch(slidingTilePuzzle.domain, configuration)
        val plan = eesAgent.plan(initialState, StaticExpansionTerminationChecker(1000))
        println(plan)
        kotlin.test.assertTrue { plan.isNotEmpty() }
        kotlin.test.assertTrue { plan.size == 12 }
    }


    @Test
    fun testEETS5() {
        val tiles = "4 1 2 3 8 0 10 6 12 5 9 7 13 14 15 11"
        val instance = createInstanceFromString(tiles)
        val slidingTilePuzzle = SlidingTilePuzzleIO.parseFromStream(instance, 1L)
        val initialState = slidingTilePuzzle.initialState
        val eesAgent = ExplicitEstimationTildeSearch(slidingTilePuzzle.domain, configuration)
        val plan = eesAgent.plan(initialState, StaticExpansionTerminationChecker(1000))
        println(plan)
        kotlin.test.assertTrue { plan.isNotEmpty() }
        kotlin.test.assertTrue { plan.size == 12 }
    }

    @Test
    fun testEETS6() {
        val optimalSolutionLengths = intArrayOf(57, 55, 59, 56, 56, 52, 52, 50, 46, 59, 57, 45)
        for (i in 12 until 13) {
            val stream = SlidingTilePuzzleTest::class.java.classLoader.getResourceAsStream("input/tiles/korf/4/real/$i")
            val slidingTilePuzzle = SlidingTilePuzzleIO.parseFromStream(stream, 1L)
            val initialState = slidingTilePuzzle.initialState
            val eesAgent= ExplicitEstimationTildeSearch(slidingTilePuzzle.domain, configuration)
            val plan = eesAgent.plan(initialState, StaticExpansionTerminationChecker(1000000000))
            var currentState = initialState
            plan.forEach { action ->
                currentState = slidingTilePuzzle.domain.successors(currentState).first { it.action == action }.state
            }
            assertTrue { slidingTilePuzzle.domain.heuristic(currentState) == 0.0 }
            assertEquals(optimalSolutionLengths[i - 1], plan.size, "instance $i")
        }
    }

    @Test
    fun testEETS() {
        val tiles = "4 1 2 3 8 0 10 6 12 5 9 7 13 14 15 11"
        configuration.weight = 3.0
        val instance = createInstanceFromString(tiles)
        val slidingTilePuzzle = SlidingTilePuzzleIO.parseFromStream(instance, 1L)
        val initialState = slidingTilePuzzle.initialState
        val eesAgent = ExplicitEstimationTildeSearch(slidingTilePuzzle.domain, configuration)
        val plan = eesAgent.plan(initialState, StaticExpansionTerminationChecker(1000))
        println(plan)
        kotlin.test.assertTrue { plan.isNotEmpty() }
        kotlin.test.assertTrue { plan.size <= 3.0 * 12 }
    }

    @Test
    fun testEETSHardPuzzle() {
        val weight = 1.5
        val configuration = ExperimentConfiguration("SLIDING_TILE_PUZZLE_4", null, "EES", TerminationType.EXPANSION,
                null, 1L, 1000L, 1000000L, null, weight, null, null, null, null,
                null, null, null, null, null, null)

        val instanceNumbers = intArrayOf(1, 3)
        val optimalSolutionLengths = intArrayOf(57, 59)

        for ((experimentNumber, i) in instanceNumbers.withIndex()) {
            print("Executing $i...")
            val stream = SlidingTilePuzzleTest::class.java.classLoader.getResourceAsStream("input/tiles/korf/4/real/$i")
            val slidingTilePuzzle = SlidingTilePuzzleIO.parseFromStream(stream, 1L)
            val initialState = slidingTilePuzzle.initialState

            val eesAgent = ExplicitEstimationTildeSearch(slidingTilePuzzle.domain, configuration)
            val plan = eesAgent.plan(initialState, StaticExpansionTerminationChecker(1000000000))
            var currentState = initialState
            plan.forEach { action ->
                currentState = slidingTilePuzzle.domain.successors(currentState).first { it.action == action }.state
            }
            assertTrue { slidingTilePuzzle.domain.heuristic(currentState) == 0.0 }
            print("...plan size: ${plan.size}...")
            assertTrue { optimalSolutionLengths[experimentNumber] * weight >= plan.size }
            print("nodes expanded: ${eesAgent.expandedNodeCount}...")
            print("nodes generated: ${eesAgent.generatedNodeCount}...")
            println("total time: ${eesAgent.executionNanoTime}")
        }
    }

}
