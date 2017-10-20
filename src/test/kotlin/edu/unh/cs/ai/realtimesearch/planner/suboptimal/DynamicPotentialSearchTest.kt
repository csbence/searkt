package edu.unh.cs.ai.realtimesearch.planner.suboptimal

import edu.unh.cs.ai.realtimesearch.environment.slidingtilepuzzle.SlidingTilePuzzleIO
import org.junit.Test
import java.io.File
import java.io.FileWriter
import java.io.InputStream
import java.util.*

class DynamicPotentialSearchTest {
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
        val dynamicPotentialSearchAgent = DynamicPotentialSearch(slidingTilePuzzle.domain, 1.0)
        kotlin.test.assertTrue { dynamicPotentialSearchAgent.plan(initialState).isEmpty() }
    }

    @Test
    fun dpsSimpleThreePlan() {
        val tiles = "1 2 3 0 4 5 6 7 8 9 10 11 12 13 14 15"
        val instance = createInstanceFromString(tiles)
        val slidingTilePuzzle = SlidingTilePuzzleIO.parseFromStream(instance, 1L)
        val initialState = slidingTilePuzzle.initialState
        val dynamicPotentialSearchAgent= DynamicPotentialSearch(slidingTilePuzzle.domain, 1.0)
        val plan = dynamicPotentialSearchAgent.plan(initialState)
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
        val dynamicPotentialSearchAgent=DynamicPotentialSearch(slidingTilePuzzle.domain, 1.0)
        val plan = dynamicPotentialSearchAgent.plan(initialState)
        println(plan)
        kotlin.test.assertTrue { plan.isNotEmpty() }
        kotlin.test.assertTrue { plan.size == 6 }
    }
}
