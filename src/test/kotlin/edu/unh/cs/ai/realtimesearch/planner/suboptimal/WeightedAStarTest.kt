package edu.unh.cs.ai.realtimesearch.planner.suboptimal

import edu.unh.cs.ai.realtimesearch.environment.gridworld.GridWorldIO
import edu.unh.cs.ai.realtimesearch.environment.slidingtilepuzzle.SlidingTilePuzzleIO
import edu.unh.cs.ai.realtimesearch.environment.slidingtilepuzzle.SlidingTilePuzzleTest
import org.junit.Test
import java.io.File
import java.io.FileWriter
import java.io.InputStream
import java.util.*

class WeightedAStarTest {
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
        val aStarAgent = WeightedAStar(slidingTilePuzzle.domain, 1.0)
        kotlin.test.assertTrue { aStarAgent.plan(initialState).isEmpty() }
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
        kotlin.test.assertTrue { plan.isNotEmpty() }
        kotlin.test.assertTrue { plan.size == 3 }
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
        kotlin.test.assertTrue { plan.isNotEmpty() }
        kotlin.test.assertTrue { plan.size == 6 }
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
        kotlin.test.assertTrue { plan.isNotEmpty() }
        kotlin.test.assertTrue { plan.size == 12 }
        println("timesReplaced: ${aStarAgent.timesReplaced}")
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
        kotlin.test.assertTrue { plan.isNotEmpty() }
        kotlin.test.assertTrue { plan.size == 12 }
        println("timesReplaced: ${aStarAgent.timesReplaced}")
    }

    @Test
    fun testAStar6() {
        for (i in 12 until 13) {
            val stream = SlidingTilePuzzleTest::class.java.classLoader.getResourceAsStream("input/tiles/korf/4/real/$i")
            val slidingTilePuzzle = SlidingTilePuzzleIO.parseFromStream(stream, 1L)
            val initialState = slidingTilePuzzle.initialState
            val aStarAgent = WeightedAStar(slidingTilePuzzle.domain, 1.0)
            val plan = aStarAgent.plan(initialState)
            var currentState = initialState
            plan.forEach { action ->
                currentState = slidingTilePuzzle.domain.successors(currentState).first { it.action == action }.state
            }
            println(plan)
            println(plan.size)
            println("timesReplaced: ${aStarAgent.timesReplaced}")
        }
    }

    @Test
    fun testAStar7() {
        val tiles = "1 10 4 12 5 3 8 9 6 11 7 2 14 0 13 15"
        val instance = createInstanceFromString(tiles)
        val slidingTilePuzzle = SlidingTilePuzzleIO.parseFromStream(instance, 1L)
        val initialState = slidingTilePuzzle.initialState
        val aStarAgent = WeightedAStar(slidingTilePuzzle.domain, 1.0)
        val plan = aStarAgent.plan(initialState)
        println("plan length: ${plan.size}")
        println(plan)
        println("expandedNodeCount: ${aStarAgent.expandedNodeCount}")
        kotlin.test.assertTrue { plan.isNotEmpty() }
        println("timesReplaced: ${aStarAgent.timesReplaced}")
    }

    @Test
    fun testWeightedAStarGridWorld1() {
        val stream = WeightedAStarTest::class.java.classLoader.getResourceAsStream("input/vacuum/cups.vw")
        val gridWorld = GridWorldIO.parseFromStream(stream, 1L)
        val initialState = gridWorld.initialState
        val aStarAgent = WeightedAStar(gridWorld.domain, 1.0)
        val plan = aStarAgent.plan(initialState)
        var currentState = initialState
        plan.forEach { action ->
            currentState = gridWorld.domain.successors(currentState).first { it.action == action }.state
        }
        println(plan)
        println(plan.size)
        println("timesReplaced: ${aStarAgent.timesReplaced}")
    }


    @Test
    fun testWeightedAStarGridWorld2() {
        val stream = WeightedAStarTest::class.java.classLoader.getResourceAsStream("input/vacuum/empty.vw")
        val gridWorld = GridWorldIO.parseFromStream(stream, 1L)
        val initialState = gridWorld.initialState
        val aStarAgent = WeightedAStar(gridWorld.domain, 1.0)
        val plan = aStarAgent.plan(initialState)
        var currentState = initialState
        plan.forEach { action ->
            currentState = gridWorld.domain.successors(currentState).first { it.action == action }.state
        }
        println(plan)
        println(plan.size)
        println("timesReplaced: ${aStarAgent.timesReplaced}")
    }
}
