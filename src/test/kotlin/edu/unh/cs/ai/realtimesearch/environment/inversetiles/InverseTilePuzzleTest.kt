package edu.unh.cs.ai.realtimesearch.environment.inversetiles

import edu.unh.cs.ai.realtimesearch.environment.Action
import edu.unh.cs.ai.realtimesearch.environment.slidingtilepuzzle.SlidingTilePuzzleIO
import edu.unh.cs.ai.realtimesearch.environment.slidingtilepuzzle.SlidingTilePuzzleTest
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
import kotlin.math.roundToInt
import kotlin.test.assertTrue

class InverseTilePuzzleTest {

    private val configuration = ExperimentConfiguration(domainName = "SLIDING_TILE_PUZZLE_4_INVERSE",
            algorithmName = "WEIGHTED_A_STAR", terminationType = TerminationType.EXPANSION, actionDuration = 1L, timeLimit = 1000L,
            errorModel = "global", expansionLimit = 1000000L, weight = 1.0)


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
    fun testHeuristicSimple() {
        val tiles = "1 2 3 0 4 5 6 7 8 9 10 11 12 13 14 15"
        val instance = createInstanceFromString(tiles)
        val inverseTilePuzzle = InverseTilePuzzleIO.parseFromStream(instance, 1L)
        val initialState = inverseTilePuzzle.initialState
        val successors = inverseTilePuzzle.domain.successors(initialState)
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

    @Test
    fun testSuboptimalBound() {
        val startingWeight = 15
        val stepSize = 0.0
        for (w in 2..2) {
            val currentWeight = startingWeight + (stepSize * w)
            println("running sub-optimality validation on weight: $currentWeight")
            configuration.weight = currentWeight
            for (i in 1..100) {
                println(i.toString())
                val stream = SlidingTilePuzzleTest::class.java.classLoader.getResourceAsStream("input/tiles/korf/4/real/$i")
                val slidingTilePuzzle = InverseTilePuzzleIO.parseFromStream(stream, 1L)
                val initialState = slidingTilePuzzle.initialState
                val eesAgent = WeightedAStar(slidingTilePuzzle.domain, configuration)
                val plan: List<Action>
                try {
                    plan = eesAgent.plan(initialState, StaticExpansionTerminationChecker(5000000))
                    println(plan)
                    val pathLength  = plan.size.toLong()
                    var currentState = initialState
                    // valid plan check
                    plan.forEach { action ->
                        currentState = slidingTilePuzzle.domain.successors(currentState).first { it.action == action }.state
                    }
                    assertTrue { slidingTilePuzzle.domain.heuristic(currentState) == 0.0 }
                } catch (e: Exception) {
                    e.printStackTrace()
                    System.err.println(e.message + " on instance $i with weight $currentWeight")
                }
//                 assertTrue { (optimalSolutionLengths[i - 1] * currentWeight).roundToInt() >= plan.size }
//                 assertEquals((optimalSolutionLengths[i - 1 ] * 1.25).roundToInt(), plan.size, "instance $i")
            }
        }
    }
}
