package edu.unh.cs.ai.realtimesearch

import edu.unh.cs.ai.realtimesearch.environment.slidingtilepuzzle.SlidingTilePuzzleIO
import edu.unh.cs.ai.realtimesearch.experiment.configuration.Configurations
import edu.unh.cs.ai.realtimesearch.experiment.configuration.GeneralExperimentConfiguration
import edu.unh.cs.ai.realtimesearch.experiment.terminationCheckers.StaticExpansionTerminationChecker
import edu.unh.cs.ai.realtimesearch.planner.suboptimal.WeightedAStar
import java.io.File
import java.io.FileWriter
import java.io.InputStream
import java.util.*

class Validator {

    private class ValidatorException(message: String?) : Exception(message)

    private val optimalSolutionLengths = intArrayOf(
            57, 55, 59, 56, 56, 52, 52, 50, 46, 59,
            57, 45, 46, 59, 62, 42, 66, 55, 46, 52,
            54, 59, 49, 54, 52, 58, 53, 52, 54, 47,
            50, 59, 60, 52, 55, 52, 58, 53, 49, 54,
            54, 42, 64, 50, 51, 49, 47, 49, 59, 53,
            56, 56, 64, 56, 41, 55, 50, 51, 57, 66,
            45, 57, 56, 51, 47, 61, 50, 51, 53, 52,
            44, 56, 49, 56, 48, 57, 54, 53, 42, 57,
            53, 62, 49, 55, 44, 45, 52, 65, 54, 50,
            57, 57, 46, 53, 50, 49, 44, 54, 57, 54)

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

    fun validateOptimalSlidingTilePuzzle(puzzleNumber: Int) {
        val configuration = GeneralExperimentConfiguration(mutableMapOf(Configurations.WEIGHT.toString() to 1.0))
        print("Executing $puzzleNumber...")
        try {
            val stream = Validator::class.java.classLoader.getResourceAsStream("input/tiles/korf/4/real/$puzzleNumber")
            val slidingTilePuzzle = SlidingTilePuzzleIO.parseFromStream(stream, 1L)
            val initialState = slidingTilePuzzle.initialState

            val aStarAgent = WeightedAStar(slidingTilePuzzle.domain, configuration)
            val plan = aStarAgent.plan(initialState, StaticExpansionTerminationChecker(1000000000))
            var currentState = initialState
            plan.forEach { action ->
                currentState = slidingTilePuzzle.domain.successors(currentState).first { it.action == action }.state
            }
            if (slidingTilePuzzle.domain.heuristic(currentState) != 0.0) throw ValidatorException("Did not find goal!")
            print("...plan size : ${plan.size}...")
            if (optimalSolutionLengths[puzzleNumber - 1] != plan.size) throw ValidatorException("Plan was not optimal!")
            print("nodes expanded: ${aStarAgent.expandedNodeCount}...")
            print("nodes generated: ${aStarAgent.generatedNodeCount}...")
            println("total time: ${aStarAgent.executionNanoTime}")
        } catch (e: ValidatorException) {
            println(e.message + "\n")
        } catch (e: Exception) {
            println(e.message + "\n")
        }
    }
}


fun main(args: Array<String>) {
    val validator = Validator()
    validator.validateOptimalSlidingTilePuzzle(args[0].toInt())
}