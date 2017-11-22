package edu.unh.cs.ai.realtimesearch.environment.inversetiles

import edu.unh.cs.ai.realtimesearch.environment.Domains
import edu.unh.cs.ai.realtimesearch.experiment.configuration.Configurations
import edu.unh.cs.ai.realtimesearch.experiment.configuration.DomainPath
import edu.unh.cs.ai.realtimesearch.experiment.configuration.generateConfigurations
import edu.unh.cs.ai.realtimesearch.experiment.configuration.realtime.LookaheadType
import edu.unh.cs.ai.realtimesearch.experiment.configuration.realtime.TerminationType
import edu.unh.cs.ai.realtimesearch.planner.Planners
import org.junit.Test
import java.io.File
import java.io.FileWriter
import java.io.InputStream
import java.util.*
import java.util.concurrent.TimeUnit
import kotlin.test.assertTrue

class InverseTilePuzzleTest {

    private fun makeTestConfiguration(domain: Pair<Domains, DomainPath>, planner: Planners, weight: Double) = generateConfigurations(
            domains = listOf(domain),
            planners = listOf(planner),
            actionDurations = listOf(1L),//50L, 100L, 150L, 200L, 250L, 400L, 800L, 1600L, 3200L, 6400L, 12800L),
            terminationType = TerminationType.EXPANSION,
            lookaheadType = LookaheadType.DYNAMIC,
            timeLimit = TimeUnit.NANOSECONDS.convert(15, TimeUnit.MINUTES),
            expansionLimit = 300000000,
            stepLimit = 300000000,
            plannerExtras = listOf(
                    Triple(Planners.WEIGHTED_A_STAR, Configurations.WEIGHT, listOf(weight))
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
    fun testZeroHeuristic() {
        val tiles = "0 1 2 3 4 5 6 7 8 9 10 11 12 13 14 15"
        val instance = createInstanceFromString(tiles)
        val slidingTilePuzzle = InverseTilePuzzleIO.parseFromStream(instance, 1L)
        val heuristic = slidingTilePuzzle.domain.heuristic(slidingTilePuzzle.initialState)
        assertTrue { heuristic == 0.0 }
    }

}
