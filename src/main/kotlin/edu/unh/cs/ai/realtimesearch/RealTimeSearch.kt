package edu.unh.cs.ai.realtimesearch

import edu.unh.cs.ai.realtimesearch.environment.Domains.RACETRACK
import edu.unh.cs.ai.realtimesearch.experiment.configuration.*
import edu.unh.cs.ai.realtimesearch.experiment.configuration.Configurations.COMMITMENT_STRATEGY
import edu.unh.cs.ai.realtimesearch.experiment.configuration.realtime.LookaheadType.DYNAMIC
import edu.unh.cs.ai.realtimesearch.experiment.configuration.realtime.TerminationType.EXPANSION
import edu.unh.cs.ai.realtimesearch.experiment.result.ExperimentResult
import edu.unh.cs.ai.realtimesearch.experiment.result.summary
import edu.unh.cs.ai.realtimesearch.planner.CommitmentStrategy
import edu.unh.cs.ai.realtimesearch.planner.Planners.*
import edu.unh.cs.ai.realtimesearch.planner.SafeRealTimeSearchConfiguration.SAFETY_EXPLORATION_RATIO
import edu.unh.cs.ai.realtimesearch.planner.SafeRealTimeSearchConfiguration.TARGET_SELECTION
import edu.unh.cs.ai.realtimesearch.planner.SafeRealTimeSearchTargetSelection.SAFE_TO_BEST
import edu.unh.cs.ai.realtimesearch.planner.SafetyBackup
import edu.unh.cs.ai.realtimesearch.planner.realtime.*
import kotlinx.io.PrintWriter
import kotlinx.serialization.json.JSON
import kotlinx.serialization.list
import java.io.File
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.concurrent.TimeUnit.MINUTES
import java.util.concurrent.TimeUnit.NANOSECONDS

fun main(args: Array<String>) {
    generateConfigurations()

//    val outputPath = if (args.isNotEmpty()) {
//        args[0]
//    } else {
//        File("output").mkdir()
//        "output/results_${LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)}.json"
//    }
//    val outputFile = File(outputPath)
//    outputFile.createNewFile()
//    if (!outputFile.isFile || !outputFile.canWrite()) throw MetronomeException("Can't write the output file: $outputPath")

//    println("Please provide a JSON list of configurations to execute:")
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
//                    Domains.GRID_WORLD to "input/vacuum/empty.vw"
                    RACETRACK to "input/racetrack/hansen-bigger-quad.track",
                    RACETRACK to "input/racetrack/barto-big.track",
                    RACETRACK to "input/racetrack/uniform.track",
                    RACETRACK to "input/racetrack/barto-small.track"
//                    TRAFFIC to "input/traffic/vehicle0.v"
            ),
//            domains = (88..88).map { TRAFFIC to "input/traffic/50/traffic$it" },
            planners = listOf(SAFE_RTS),
            actionDurations = listOf(50L, 100L, 150L, 200L),// 250L, 400L, 800L, 1600L, 3200L, 6400L, 12800L),
            terminationType = EXPANSION,
            lookaheadType = DYNAMIC,
            timeLimit = NANOSECONDS.convert(1999, MINUTES),
            expansionLimit = 10000000,
            stepLimit = 10000000,
            plannerExtras = listOf(
                    Triple(SAFE_RTS, TARGET_SELECTION, listOf(SAFE_TO_BEST.toString())),
                    Triple(SAFE_RTS, SAFETY_EXPLORATION_RATIO, listOf(1.0)),
                    Triple(SAFE_RTS, COMMITMENT_STRATEGY, listOf(commitmentStrategy)),
                    Triple(S_ZERO, TARGET_SELECTION, listOf(SAFE_TO_BEST.toString())),
                    Triple(S_ZERO, COMMITMENT_STRATEGY, listOf(commitmentStrategy)),
                    Triple(S_ZERO, SafeZeroConfiguration.SAFETY_BACKUP, listOf(SafeZeroSafetyBackup.PREDECESSOR.toString())),
                    Triple(S_ZERO, SafeZeroConfiguration.SAFETY, listOf(SafeZeroSafety.PREFERRED.toString())),
                    Triple(LSS_LRTA_STAR, COMMITMENT_STRATEGY, listOf(commitmentStrategy)),
                    Triple(SIMPLE_SAFE, Configurations.LOOKAHEAD_DEPTH_LIMIT, listOf(10)),
                    Triple(SIMPLE_SAFE, SimpleSafeConfiguration.SAFETY_BACKUP, listOf(SafetyBackup.PREDECESSOR.toString())),
                    Triple(SIMPLE_SAFE, SimpleSafeConfiguration.SAFETY, listOf(SimpleSafeSafety.PREFERRED.toString())),
                    Triple(SIMPLE_SAFE, TARGET_SELECTION, listOf(SAFE_TO_BEST.toString())),
                    Triple(SIMPLE_SAFE, COMMITMENT_STRATEGY, listOf(commitmentStrategy)),
                    Triple(SIMPLE_SAFE, SimpleSafeConfiguration.VERSION, listOf(SimpleSafeVersion.TWO.toString())),
                    Triple(LSS_LRTA_STAR, COMMITMENT_STRATEGY, listOf(commitmentStrategy)),
                    Triple(WEIGHTED_A_STAR, Configurations.WEIGHT, listOf(1.0))
            ),
            domainExtras = listOf(
                    Triple(RACETRACK, Configurations.DOMAIN_SEED.toString(), 77L..77L)
            )
    )
    println("${configurations.size} configuration has been generated.")
    return JSON.indented.stringify(SimpleSerializer.list, configurations.toList())
}


