package edu.unh.cs.ai.realtimesearch

import edu.unh.cs.ai.realtimesearch.environment.Domains
import edu.unh.cs.ai.realtimesearch.environment.Domains.RACETRACK
import edu.unh.cs.ai.realtimesearch.experiment.configuration.*
import edu.unh.cs.ai.realtimesearch.experiment.configuration.Configurations.COMMITMENT_STRATEGY
import edu.unh.cs.ai.realtimesearch.experiment.configuration.realtime.LookaheadType.DYNAMIC
import edu.unh.cs.ai.realtimesearch.experiment.configuration.realtime.TerminationType.EXPANSION
import edu.unh.cs.ai.realtimesearch.experiment.configuration.realtime.TerminationType.TIME
import edu.unh.cs.ai.realtimesearch.experiment.result.ExperimentResult
import edu.unh.cs.ai.realtimesearch.experiment.result.summary
import edu.unh.cs.ai.realtimesearch.planner.CommitmentStrategy
import edu.unh.cs.ai.realtimesearch.planner.Planners.*
import edu.unh.cs.ai.realtimesearch.planner.realtime.*
import edu.unh.cs.ai.realtimesearch.planner.realtime.TBAStarConfiguration.TBA_OPTIMIZATION
import kotlinx.serialization.json.JSON
import kotlinx.serialization.list
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit.MINUTES
import java.util.concurrent.TimeUnit.NANOSECONDS
import edu.unh.cs.ai.realtimesearch.util.BinomialHeapPriorityQueue
import edu.unh.cs.ai.realtimesearch.visualizer.thrift.ThriftVisualizerClient

fun main(args: Array<String>) {
//    val client = ThriftVisualizerClient.clientFactory()
//
//    if (client != null) {
//        println("""Visualizer ping: ${client.ping()}""")
//        client.close()
//    }


    var outputPath: String?
    var basePath: String?
    if (args.isNotEmpty()) {
        outputPath = args[0]

        val fileNameIndex = outputPath.lastIndexOf("\\")
    }

//    println("Please provide a JSON list of configurations to execute:")
//    var rawConfiguration: String = readLine() ?: throw MetronomeException("Mission configuration on stdin.")
//    if (rawConfiguration.isBlank()) throw MetronomeException("No configurations were provided.")
//    val rawConfiguration = if (rawConfigurations != null && rawConfigurations.isNotBlank()) rawConfigurations else generateConfigurations()
//    println(rawConfiguration)

    // Manually override
    val rawConfiguration = generateConfigurations()

    val loader = ExperimentConfiguration.serializer().list
    val parsedConfigurations = JSON.parse(loader, rawConfiguration)
    println(parsedConfigurations)

    val results = ConfigurationExecutor.executeConfigurations(parsedConfigurations, dataRootPath = null, parallelCores = 1)

    val rawResults = JSON.Companion.stringify(ExperimentResult.serializer().list, results)
//    PrintWriter(outputPath, "UTF-8").use { it.write(rawResults) }
//    System.err.println("\nResult has been saved to $outputPath")
    System.err.println(results.summary())

    results.forEach { println(it.goalAchievementTime) }

    println('#') // Indicator for the parser
    println(rawResults) // This should be the last printed line

//    runVisualizer(result = results.first())
}

private fun generateConfigurations(): String {
    val commitmentStrategy = CommitmentStrategy.SINGLE.toString()

    val configurations = generateConfigurations(
            domains = listOf(
//                    Domains.GRID_WORLD to "input/vacuum/maze.vw"
//                    Domains.GRID_WORLD to "input/vacuum/minima2000/minima2000_2000-0.vw"
//                    Domains.GRID_WORLD to "input/vacuum/minima2000/minima2000_2000-1.vw",
//                    Domains.GRID_WORLD to "input/vacuum/minima2000/minima2000_2000-2.vw",
//                    Domains.GRID_WORLD to "input/vacuum/cups.vw"
                    Domains.GRID_WORLD to "input/vacuum/minima1500/minima1500_1500-0.vw",
                    Domains.GRID_WORLD to "input/vacuum/minima1500/minima1500_1500-1.vw",
                    Domains.GRID_WORLD to "input/vacuum/minima1500/minima1500_1500-2.vw",
                    Domains.GRID_WORLD to "input/vacuum/minima1500/minima1500_1500-3.vw"
//                    Domains.GRID_WORLD to "input/vacuum/minima1500/minima1500_1500-4.vw",
//                    Domains.GRID_WORLD to "input/vacuum/minima1500/minima1500_1500-5.vw",
//                    Domains.GRID_WORLD to "input/vacuum/minima1500/minima1500_1500-6.vw",
//                    Domains.GRID_WORLD to "input/vacuum/minima1500/minima1500_1500-7.vw",
//                    Domains.GRID_WORLD to "input/vacuum/minima1500/minima1500_1500-8.vw",
//                    Domains.GRID_WORLD to "input/vacuum/temp/minima500_700-2.vw"
//                    Domains.GRID_WORLD to "input/vacuum/temp/minima500_700-3.vw",
//                    Domains.GRID_WORLD to "input/vacuum/temp/minima500_700-4.vw",
//                    Domains.GRID_WORLD to "input/vacuum/temp/minima500_700-5.vw",
//                    Domains.GRID_WORLD to "input/vacuum/temp/minima500_700-6.vw",
//                    Domains.GRID_WORLD to "input/vacuum/temp/minima500_700-7.vw",
//                    Domains.GRID_WORLD to "input/vacuum/temp/minima500_700-8.vw",
//                    Domains.GRID_WORLD to "input/vacuum/temp/minima500_700-9.vw",
//                    Domains.GRID_WORLD to "input/vacuum/temp/minima500_700-10.vw",
//                    Domains.GRID_WORLD to "input/vacuum/temp/minima500_700-11.vw",
//                    Domains.GRID_WORLD to "input/vacuum/temp/minima500_700-12.vw",
//                    Domains.GRID_WORLD to "input/vacuum/temp/minima500_700-13.vw",
//                    Domains.GRID_WORLD to "input/vacuum/temp/minima500_700-14.vw"
//                    Domains.GRID_WORLD to "input/vacuum/minima100_100-0.vw"
//                    Domains.GRID_WORLD to "input/vacuum/random5k.vw"
            ),
            planners = listOf(TIME_BOUNDED_A_STAR),
            actionDurations = listOf(5L),// 250L, 400L, 800L, 1600L, 3200L, 6400L, 12800L),100_000_000L
            terminationType = EXPANSION,
            lookaheadType = DYNAMIC,
            timeLimit = NANOSECONDS.convert(1999, MINUTES),
            expansionLimit = 10000000000,
            stepLimit = 10000000,
            plannerExtras = listOf(
                    Triple(LSS_LRTA_STAR, COMMITMENT_STRATEGY, listOf(commitmentStrategy)),
                    Triple(TIME_BOUNDED_A_STAR, COMMITMENT_STRATEGY, listOf(commitmentStrategy)),
                    Triple(TIME_BOUNDED_A_STAR, TBA_OPTIMIZATION, listOf(TBAOptimization.THRESHOLD)),
                    Triple(TIME_BOUNDED_A_STAR, Configurations.TERMINATION_EPSILON, listOf(4_000_000)),
                    Triple(TIME_BOUNDED_A_STAR, Configurations.WEIGHT, listOf(1.0, 1.4, 1.8)),
                    Triple(TIME_BOUNDED_A_STAR, TBAStarConfiguration.TB_STRATEGY, listOf(TBStrategy.A_STAR)),
                    Triple(LSS_LRTA_STAR, Configurations.TERMINATION_EPSILON, listOf(4_000_000)),
                    Triple(ES, Configurations.TERMINATION_EPSILON, listOf(4_000_000)),
                    Triple(ES, COMMITMENT_STRATEGY, listOf(commitmentStrategy)),
                    Triple(TIME_BOUNDED_A_STAR, TBAStarConfiguration.BACKLOG_RATIO, listOf(1.0)),
                    Triple(WEIGHTED_A_STAR, Configurations.WEIGHT, listOf(1.0)),
//                    Triple(ES, "expansionDelay", listOf(50000)),
                    Triple(TIME_BOUNDED_A_STAR, "expansionDelay", listOf(50000))
            ),
            domainExtras = listOf(
                    Triple(RACETRACK, Configurations.DOMAIN_SEED.toString(), 77L..77L)
            )
    )
    println("${configurations.size} configuration has been generated.")
    return JSON.indented.stringify(SimpleSerializer.list, configurations.toList())
}

val visualizerLatch = CountDownLatch(1)
