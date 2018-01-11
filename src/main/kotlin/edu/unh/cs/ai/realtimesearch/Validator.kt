package edu.unh.cs.ai.realtimesearch

import edu.unh.cs.ai.realtimesearch.experiment.configuration.Configurations
import edu.unh.cs.ai.realtimesearch.experiment.configuration.GeneralExperimentConfiguration
import java.io.File
import java.io.FileWriter
import java.io.InputStream
import java.util.*

class Validator {

    fun validateOptimalSlidingTilePuzzle() {
        val optimalSolutionLengths = intArrayOf(
                58, 55, 59, 56, 56, 52, 52, 50, 46, 59,
                57, 45, 46, 59, 62, 42, 66, 55, 46, 52,
                54, 59, 49, 54, 52, 58, 53, 52, 54, 47,
                50, 59, 60, 52, 55, 52, 58, 53, 49, 54,
                54, 42, 64, 50, 51, 49, 47, 49, 59, 53,
                56, 56, 64, 56, 41, 55, 50, 51, 57, 66,
                45, 57, 56, 51, 47, 61, 50, 51, 53, 52,
                44, 56, 49, 56, 48, 57, 54, 53, 42, 57,
                53, 62, 49, 55, 44, 45, 52, 65, 54, 50,
                57, 57, 46, 53, 50, 49, 44, 54, 57, 54)
        for (i in 1 until 101) {
            val stream = Validator::class.java.classLoader.getResourceAsStream("input/tiles/korf/4/real/$i")

        }

    }
}

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

fun main(args: Array<String>) {

    val optimalSolutionLengths = intArrayOf(
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


}