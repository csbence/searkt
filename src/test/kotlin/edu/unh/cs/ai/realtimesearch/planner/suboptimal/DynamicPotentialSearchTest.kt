package edu.unh.cs.ai.realtimesearch.planner.suboptimal

import edu.unh.cs.ai.realtimesearch.environment.Domains
import edu.unh.cs.ai.realtimesearch.environment.gridworld.GridWorldIO
import edu.unh.cs.ai.realtimesearch.environment.slidingtilepuzzle.SlidingTilePuzzleIO
import edu.unh.cs.ai.realtimesearch.environment.slidingtilepuzzle.SlidingTilePuzzleTest
import edu.unh.cs.ai.realtimesearch.experiment.configuration.Configurations
import edu.unh.cs.ai.realtimesearch.experiment.configuration.DomainPath
import edu.unh.cs.ai.realtimesearch.experiment.configuration.GeneralExperimentConfiguration
import edu.unh.cs.ai.realtimesearch.experiment.configuration.generateConfigurations
import edu.unh.cs.ai.realtimesearch.experiment.configuration.realtime.LookaheadType
import edu.unh.cs.ai.realtimesearch.experiment.configuration.realtime.TerminationType
import edu.unh.cs.ai.realtimesearch.experiment.terminationCheckers.StaticExpansionTerminationChecker
import edu.unh.cs.ai.realtimesearch.planner.Planners
import org.junit.Test
import java.io.File
import java.io.FileWriter
import java.io.InputStream
import java.util.*
import java.util.concurrent.TimeUnit
import kotlin.test.assertEquals
import kotlin.test.assertTrue


class DynamicPotentialSearchTest {

    private fun makeTestConfiguration(domain: Pair<Domains, DomainPath>, weight: Double) = generateConfigurations(
            domains = listOf(domain),
            planners = listOf(Planners.DYNAMIC_POTENTIAL_SEARCH),
            actionDurations = listOf(1L),//50L, 100L, 150L, 200L, 250L, 400L, 800L, 1600L, 3200L, 6400L, 12800L),
            terminationType = TerminationType.EXPANSION,
            lookaheadType = LookaheadType.DYNAMIC,
            timeLimit = TimeUnit.NANOSECONDS.convert(15, TimeUnit.MINUTES),
            expansionLimit = 300000000,
            stepLimit = 300000000,
            plannerExtras = listOf(
                    Triple(Planners.DYNAMIC_POTENTIAL_SEARCH, Configurations.WEIGHT, listOf(weight))
            ),
            domainExtras = listOf()

    )

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
    fun dpsPlanFromGoal() {
        val tiles = "0 1 2 3 4 5 6 7 8 9 10 11 12 13 14 15"
        val instance = createInstanceFromString(tiles)
        val slidingTilePuzzle = SlidingTilePuzzleIO.parseFromStream(instance, 1L)
        val initialState = slidingTilePuzzle.initialState
        val domainPair = Pair(Domains.SLIDING_TILE_PUZZLE_4, "input/tiles/korf/test/easy0")
        val config = makeTestConfiguration(domainPair, 1.0).first()
        val dynamicPotentialSearchAgent = DynamicPotentialSearch(slidingTilePuzzle.domain, config)
        kotlin.test.assertTrue { dynamicPotentialSearchAgent.plan(initialState, StaticExpansionTerminationChecker(1000)).isEmpty() }
    }

    @Test
    fun dpsSimpleThreePlan() {
        val tiles = "1 2 3 0 4 5 6 7 8 9 10 11 12 13 14 15"
        val instance = createInstanceFromString(tiles)
        val slidingTilePuzzle = SlidingTilePuzzleIO.parseFromStream(instance, 1L)
        val initialState = slidingTilePuzzle.initialState
        val domainPair = Pair(Domains.SLIDING_TILE_PUZZLE_4, "input/tiles/korf/test/simple1")
        val config = makeTestConfiguration(domainPair, 1.0).first()
        val dynamicPotentialSearchAgent = DynamicPotentialSearch(slidingTilePuzzle.domain, config)
        val plan = dynamicPotentialSearchAgent.plan(initialState, StaticExpansionTerminationChecker(1000))
        println(plan)
        kotlin.test.assertTrue { plan.isNotEmpty() }
        kotlin.test.assertTrue { plan.size == 3 }
    }

    @Test
    fun dpsSimpleSixPlan() {
        val tiles = "4 1 2 3 8 5 6 7 12 9 10 11 13 14 15 0"
        val instance = createInstanceFromString(tiles)
        val slidingTilePuzzle = SlidingTilePuzzleIO.parseFromStream(instance, 1L)
        val initialState = slidingTilePuzzle.initialState
        val domainPair = Pair(Domains.SLIDING_TILE_PUZZLE_4, "input/tiles/korf/test/simple2")
        val config = makeTestConfiguration(domainPair, 1.0).first()
        val dynamicPotentialSearchAgent = DynamicPotentialSearch(slidingTilePuzzle.domain, config)
        val plan = dynamicPotentialSearchAgent.plan(initialState, StaticExpansionTerminationChecker(1000))
        println(plan)
        kotlin.test.assertTrue { plan.isNotEmpty() }
        kotlin.test.assertTrue { plan.size == 6 }
    }

    @Test
    fun testSimpleTwelvePlan() {
        val tiles = "0 4 1 2 8 5 6 3 12 9 10 7 13 14 15 11"
        val instance = createInstanceFromString(tiles)
        val slidingTilePuzzle = SlidingTilePuzzleIO.parseFromStream(instance, 1L)
        val initialState = slidingTilePuzzle.initialState
        val domainPair = Pair(Domains.SLIDING_TILE_PUZZLE_4, "input/tiles/korf/test/simple3")
        val config = makeTestConfiguration(domainPair, 1.0).first()
        val dynamicPotentialSearchAgent = DynamicPotentialSearch(slidingTilePuzzle.domain, config)
        val plan = dynamicPotentialSearchAgent.plan(initialState, StaticExpansionTerminationChecker(1000))
        println(plan)
        kotlin.test.assertTrue { plan.isNotEmpty() }
        kotlin.test.assertTrue { plan.size == 12 }
    }

    @Test
    fun testSimpleTwelvePlan2() {
        val tiles = "4 1 2 3 8 0 10 6 12 5 9 7 13 14 15 11"
        val instance = createInstanceFromString(tiles)
        val slidingTilePuzzle = SlidingTilePuzzleIO.parseFromStream(instance, 1L)
        val initialState = slidingTilePuzzle.initialState
        val domainPair = Pair(Domains.SLIDING_TILE_PUZZLE_4, "input/tiles/korf/test/simple4")
        val config = makeTestConfiguration(domainPair, 1.0).first()
        val dynamicPotentialSearchAgent = DynamicPotentialSearch(slidingTilePuzzle.domain, config)
        val plan = dynamicPotentialSearchAgent.plan(initialState, StaticExpansionTerminationChecker(1000))
        println(plan)
        kotlin.test.assertTrue { plan.isNotEmpty() }
        kotlin.test.assertTrue { plan.size == 12 }
        println(dynamicPotentialSearchAgent.executionNanoTime)
    }

    @Test
    fun testSimpleWeightedTwelvePlan() {
        val tiles = "4 1 2 3 8 0 10 6 12 5 9 7 13 14 15 11"
        val instance = createInstanceFromString(tiles)
        val slidingTilePuzzle = SlidingTilePuzzleIO.parseFromStream(instance, 1L)
        val initialState = slidingTilePuzzle.initialState
        val domainPair = Pair(Domains.SLIDING_TILE_PUZZLE_4, "input/tiles/korf/test/simple4")
        val config = makeTestConfiguration(domainPair, 1.35).first()
        val aStarAgent = DynamicPotentialSearch(slidingTilePuzzle.domain, config)
        val plan = aStarAgent.plan(initialState, StaticExpansionTerminationChecker(1000))
        println("" + plan + "\nlength ${plan.size}")
        kotlin.test.assertTrue { plan.isNotEmpty() }
        kotlin.test.assertTrue { plan.size * 1.35 >= 12 }
    }

    @Test
    fun testKorfInstance12() {
        val tiles = "14 1 9 6 4 8 12 5 7 2 3 0 10 11 13 15"
        val instance = createInstanceFromString(tiles)
        val slidingTilePuzzle = SlidingTilePuzzleIO.parseFromStream(instance, 1L)
        val initialState = slidingTilePuzzle.initialState
        val domainPair = Pair(Domains.SLIDING_TILE_PUZZLE_4, "input/tiles/korf/4/real/12")
        val config = makeTestConfiguration(domainPair, 1.0).first()
        val aStarAgent = DynamicPotentialSearch(slidingTilePuzzle.domain, config)
        val plan = aStarAgent.plan(initialState, StaticExpansionTerminationChecker(1000))
        println("" + plan + "\nlength ${plan.size}")
        kotlin.test.assertTrue { plan.isNotEmpty() }
        kotlin.test.assertTrue { plan.size <= 45 * 1.0 }
    }


    @Test
    fun testRandomKorfInstancesWeightedOptimal() {
        val optimalSolutionLengths = intArrayOf(57, 55, 59, 56, 56, 52, 52, 50, 46, 59, 57, 45)
        for (i in 12 until 13) {
            val stream = SlidingTilePuzzleTest::class.java.classLoader.getResourceAsStream("input/tiles/korf/4/real/$i")
            val slidingTilePuzzle = SlidingTilePuzzleIO.parseFromStream(stream, 1L)
            val initialState = slidingTilePuzzle.initialState
            val domainPair = Pair(Domains.SLIDING_TILE_PUZZLE_4, "input/tiles/korf/4/real/$i")
            val config = makeTestConfiguration(domainPair, 1.0).first()
            val dynamicPotentialSearch = DynamicPotentialSearch(slidingTilePuzzle.domain, config)
            val plan = dynamicPotentialSearch.plan(initialState, StaticExpansionTerminationChecker(1000))
            var currentState = initialState
            plan.forEach { action ->
                currentState = slidingTilePuzzle.domain.successors(currentState).first { it.action == action }.state
            }
            println("execution Time: ${dynamicPotentialSearch.executionNanoTime}")
            assertTrue { slidingTilePuzzle.domain.heuristic(currentState) == 0.0 }
            assertEquals(optimalSolutionLengths[i - 1], plan.size, "instance $i")
        }
    }


    @Test
    fun testKorfInstance1() {
        val weight = 1.7
        val tiles = "14 13 15 7 11 12 9 5 6 0 2 1 4 8 10 3"
        val instance = createInstanceFromString(tiles)
        val slidingTilePuzzle = SlidingTilePuzzleIO.parseFromStream(instance, 1L)
        val initialState = slidingTilePuzzle.initialState
        val domainPair = Pair(Domains.SLIDING_TILE_PUZZLE_4, "input/tiles/korf/4/real/1")
        val config = makeTestConfiguration(domainPair, weight).first()
        val aStarAgent = DynamicPotentialSearch(slidingTilePuzzle.domain, config)
        val plan = aStarAgent.plan(initialState, StaticExpansionTerminationChecker(1000))
        println("" + plan + "\nlength ${plan.size}")
        kotlin.test.assertTrue { plan.isNotEmpty() }
        kotlin.test.assertTrue { plan.size <= 57 * weight }
    }

    @Test
    fun testDynamicPotentialSearchPuzzle() {
        val weight = 1.5
        val instanceNumbers = intArrayOf(1, 3)
        val optimalSolutionLengths = intArrayOf(57, 59)
        for ((experimentNumber, i) in instanceNumbers.withIndex()) {
            print("Executing $i...")
            val stream = SlidingTilePuzzleTest::class.java.classLoader.getResourceAsStream("input/tiles/korf/4/real/$i")
            val slidingTilePuzzle = SlidingTilePuzzleIO.parseFromStream(stream, 1L)
            val initialState = slidingTilePuzzle.initialState
            val domainPair = Pair(Domains.SLIDING_TILE_PUZZLE_4, "input/tiles/korf/4/real/$i")
            val config = makeTestConfiguration(domainPair, weight).first()
            val dynamicPotentialSearchAgent = DynamicPotentialSearch(slidingTilePuzzle.domain, config)
            val plan = dynamicPotentialSearchAgent.plan(initialState, StaticExpansionTerminationChecker(1000000))
            var currentState = initialState
            plan.forEach { action ->
                currentState = slidingTilePuzzle.domain.successors(currentState).first { it.action == action }.state
            }
            assertTrue { slidingTilePuzzle.domain.heuristic(currentState) == 0.0 }
            print("...plan size: ${plan.size}...")
            assertTrue { optimalSolutionLengths[experimentNumber] * weight >= plan.size }
            print("nodes expanded ${dynamicPotentialSearchAgent.expandedNodeCount}...")
            print("nodes generated: ${dynamicPotentialSearchAgent.generatedNodeCount}...")
            println("total time: ${dynamicPotentialSearchAgent.executionNanoTime}")
        }
    }


    @Test
    fun testDynamicPotentialSearchGridWorld1() {
        val stream = WeightedAStarTest::class.java.classLoader.getResourceAsStream("input/vacuum/cups.vw")
        val gridWorld = GridWorldIO.parseFromStream(stream, 1L)
        val initialState = gridWorld.initialState
        val domainPair = Pair(Domains.SLIDING_TILE_PUZZLE_4, "input/vacuum/cups.vw")
        val config = makeTestConfiguration(domainPair, 1.0).first()
        val dynamicPotentialSearchAgent = DynamicPotentialSearch(gridWorld.domain, config)
        val plan = dynamicPotentialSearchAgent.plan(initialState, StaticExpansionTerminationChecker(1000))
        var currentState = initialState
        plan.forEach { action ->
            currentState = gridWorld.domain.successors(currentState).first { it.action == action }.state
        }
        println(plan)
        println(plan.size)
    }


    @Test
    fun testDynamicPotentialSearchGridWorld2() {
        val stream = WeightedAStarTest::class.java.classLoader.getResourceAsStream("input/vacuum/maze.vw")
        val gridWorld = GridWorldIO.parseFromStream(stream, 1L)
        val initialState = gridWorld.initialState
        val domainPair = Pair(Domains.SLIDING_TILE_PUZZLE_4, "input/vacuum/maze.vw")
        val config = makeTestConfiguration(domainPair, 1.0).first()
        val dynamicPotentialSearchAgent = DynamicPotentialSearch(gridWorld.domain, config)
        val plan = dynamicPotentialSearchAgent.plan(initialState, StaticExpansionTerminationChecker(1000))
        var currentState = initialState
        plan.forEach { action ->
            currentState = gridWorld.domain.successors(currentState).first { it.action == action }.state
        }
        println(plan)
        println(plan.size)
    }
}
