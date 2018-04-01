package edu.unh.cs.ai.realtimesearch

import edu.unh.cs.ai.realtimesearch.environment.Domains.*
import edu.unh.cs.ai.realtimesearch.experiment.configuration.*
import edu.unh.cs.ai.realtimesearch.experiment.configuration.Configurations.COMMITMENT_STRATEGY
import edu.unh.cs.ai.realtimesearch.experiment.configuration.realtime.LookaheadType.DYNAMIC
import edu.unh.cs.ai.realtimesearch.experiment.configuration.realtime.TerminationType.EXPANSION
import edu.unh.cs.ai.realtimesearch.planner.realtime.RealTimeComprehensiveSearch.ComprehensiveConfigurations.BACKLOG_RATIO
import edu.unh.cs.ai.realtimesearch.experiment.result.ExperimentResult
import edu.unh.cs.ai.realtimesearch.planner.CommitmentStrategy
import edu.unh.cs.ai.realtimesearch.planner.Planners.*
import kotlinx.serialization.json.JSON
import kotlinx.serialization.list
import java.util.concurrent.TimeUnit.MINUTES
import java.util.concurrent.TimeUnit.NANOSECONDS

fun main(args: Array<String>) {

//    val outputPath = if (args.isNotEmpty()) {
//        args[0]
//    } else {
//        File("output").mkdir()
//        "output/results_${LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)}.json"
//    }
//    val outputFile = File(outputPath)
//    outputFile.createNewFile()
//    if (!outputFile.isFile || !outputFile.canWrite()) throw MetronomeException("Can't write the output file: $outputPath")

    println("Please provide a JSON list of configurations to execute:")
//    val rawConfiguration: String = readLine() ?: throw MetronomeException("Mission configuration on stdin.")
//    if (rawConfiguration.isBlank()) throw MetronomeException("No configurations were provided.")
//    val rawConfiguration = if (rawConfigurations != null && rawConfigurations.isNotBlank()) rawConfigurations else generateConfigurations()
    val rawConfiguration = generateConfigurations()
    println(rawConfiguration)

    val loader = ExperimentConfiguration.serializer().list
    val parsedConfigurations = JSON.parse(loader, rawConfiguration)
    println(parsedConfigurations)

    val results = ConfigurationExecutor.executeConfigurations(parsedConfigurations, dataRootPath = null, parallelCores = 1)

    val rawResults = JSON.Companion.stringify(ExperimentResult.serializer().list, results)
//    PrintWriter(outputPath, "UTF-8").use { it.write(rawResults) }
//    System.err.println("\nResult has been saved to $outputPath")
//    System.err.println(results.summary())

    println('#') // Indicator for the parser
    println(rawResults) // This should be the last printed line

//    System.err.println("Searkt is done!")
//    System.err.flush()

//    runVisualizer(result = results.first())
}

private fun generateConfigurations(): String {
    val commitmentStrategy = CommitmentStrategy.SINGLE.toString()

    val configurations = generateConfigurations(
            domains = listOf(
//                    Domains.SLIDING_TILE_PUZZLE_4 to "input/tiles/korf/4/real/12"
//                    GRID_WORLD to "input/vacuum/empty.vw",
//                    GRID_WORLD to "input/vacuum/h_400.vw",
//                    GRID_WORLD to "input/vacuum/slalom_04.vw",
//                    GRID_WORLD to "input/vacuum/big_minimum.vw"
                    GRID_WORLD to "input/vacuum/minima/minima0.vw",
                    GRID_WORLD to "input/vacuum/minima/minima1.vw",
                    GRID_WORLD to "input/vacuum/minima/minima2.vw",
                    GRID_WORLD to "input/vacuum/minima/minima3.vw"
//                    GRID_WORLD to "input/vacuum/wall.vw"
//                    GRID_WORLD to "input/vacuum/randomNoisy1k.vw",
//                    GRID_WORLD to "input/vacuum/cups.vw",
//                    GRID_WORLD to "input/vacuum/randomShapes1k.vw",
//                    GRID_WORLD to "input/vacuum/openBox_400.vw"
//                    GRID_WORLD to "input/vacuum/maze.vw"
//                    RACETRACK to "input/racetrack/hansen-bigger-quad.track"
//                    RACETRACK to "input/racetrack/barto-big.track",
//                    RACETRACK to "input/racetrack/uniform.track",
//                    RACETRACK to "input/racetrack/barto-small.track"
//                    TRAFFIC to "input/traffic/vehicle0.v"
            ),
//            domains = (88..88).map { TRAFFIC to "input/traffic/50/traffic$it" },
            planners = listOf(CES, LSS_LRTA_STAR),
//            planners = listOf(WEIGHTED_A_STAR),
            actionDurations = listOf(50L, 100L),//150L, 200L, 250L, 3200L, 6400L, 12800L),
            terminationType = EXPANSION,
            lookaheadType = DYNAMIC,
            timeLimit = NANOSECONDS.convert(1999, MINUTES),
            expansionLimit = 10000000,
            stepLimit = 10000000,
            plannerExtras = listOf(
                    Triple(LSS_LRTA_STAR, COMMITMENT_STRATEGY, listOf(commitmentStrategy)),
                    Triple(RTC, COMMITMENT_STRATEGY, listOf(commitmentStrategy)),
                    Triple(RTC, BACKLOG_RATIO, listOf(10.0, Double.POSITIVE_INFINITY)),
                    Triple(CES, COMMITMENT_STRATEGY, listOf(commitmentStrategy)),
                    Triple(CES, BACKLOG_RATIO, listOf(Double.POSITIVE_INFINITY)),
                    Triple(WEIGHTED_A_STAR, Configurations.WEIGHT, listOf(1.0))
            ),
            domainExtras = listOf(
                    Triple(RACETRACK, Configurations.DOMAIN_SEED.toString(), 77L..77L)
            )
    )
    println("${configurations.size} configuration has been generated.")
    return JSON.indented.stringify(SimpleSerializer.list, configurations.toList())
}


