package edu.unh.cs.ai.realtimesearch.environment.inversetiles

import edu.unh.cs.ai.realtimesearch.environment.slidingtilepuzzle.SlidingTilePuzzleIO
import edu.unh.cs.ai.realtimesearch.experiment.configuration.ExperimentConfiguration
import edu.unh.cs.ai.realtimesearch.experiment.configuration.realtime.TerminationType
import edu.unh.cs.ai.realtimesearch.experiment.terminationCheckers.StaticExpansionTerminationChecker
import edu.unh.cs.ai.realtimesearch.planner.suboptimal.ExplicitEstimationSearch
import edu.unh.cs.ai.realtimesearch.planner.suboptimal.WeightedAStar
import org.junit.Test
import java.io.File
import java.io.FileWriter
import java.io.InputStream
import java.util.*
import kotlin.test.assertTrue

class InverseTilePuzzleTest {

    private val configuration = ExperimentConfiguration("SLIDING_TILE_PUZZLE_4", null, "EES", TerminationType.EXPANSION,
            null, 1L, 1000L, 1000000L, null, 1.0, null, null, null, null,
            null, null, null, null, null, null, errorModel = "path")


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
    fun testDistance() {
        var tiles = "0 1 2 3 4 5 6 7 8 9 10 11 12 13 14 15"
        var instance = createInstanceFromString(tiles)
        var inverseTilePuzzle = InverseTilePuzzleIO.parseFromStream(instance, 1L)
        var initialState = inverseTilePuzzle.initialState
        assertTrue { initialState.distance == 0.0 }

        tiles = "1 2 3 0 4 5 6 7 8 9 10 11 12 13 14 15"
        instance = createInstanceFromString(tiles)
        inverseTilePuzzle = InverseTilePuzzleIO.parseFromStream(instance, 1L)
        initialState = inverseTilePuzzle.initialState
        assertTrue { initialState.distance == 3.0 }
        assertTrue { initialState.distance != initialState.heuristic }

    }

    @Test
    fun testSuccessorActionCost() {
        val tiles = "0 4 1 2 8 5 6 3 12 9 10 7 13 14 15 11"
        val instance = createInstanceFromString(tiles)
        val inverseTilePuzzle = InverseTilePuzzleIO.parseFromStream(instance, 1L)
        val initialState = inverseTilePuzzle.initialState
        println(initialState)
        val successorsLevel1 = inverseTilePuzzle.domain.successors(initialState)
        successorsLevel1.forEach(::println)

    }

    @Test
    fun testSolving() {
        val tiles = "4 1 2 3 8 0 10 6 12 5 9 7 13 14 15 11"
        val instance = createInstanceFromString(tiles)
        val slidingTilePuzzle = InverseTilePuzzleIO.parseFromStream(instance, 1L)
        val initialState = slidingTilePuzzle.initialState
        val aStarAgent = WeightedAStar(slidingTilePuzzle.domain, configuration)
        val plan = aStarAgent.plan(initialState, StaticExpansionTerminationChecker(1000))
        println(plan)
        kotlin.test.assertTrue { plan.isNotEmpty() }
        kotlin.test.assertTrue { plan.size == 12 }
        var transitionState = initialState
        plan.forEach { transitionState = slidingTilePuzzle.domain.transition(transitionState, it)!! }
        assertTrue { slidingTilePuzzle.domain.heuristic(transitionState) == 0.0 }
    }
}
