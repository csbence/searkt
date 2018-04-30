package edu.unh.cs.ai.realtimesearch

import edu.unh.cs.ai.realtimesearch.environment.Domains
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
import edu.unh.cs.ai.realtimesearch.planner.realtime.TBAStarConfiguration.TBA_OPTIMIZATION
import kotlinx.serialization.json.JSON
import kotlinx.serialization.list
import java.util.concurrent.TimeUnit.MINUTES
import java.util.concurrent.TimeUnit.NANOSECONDS

fun main(args: Array<String>) {

    var outputPath: String?
    var basePath: String?
    if (args.isNotEmpty()) {
        outputPath = args[0]

        val fileNameIndex = outputPath.lastIndexOf("\\")
    }

    println("Please provide a JSON list of configurations to execute:")
    var rawConfiguration: String = readLine() ?: throw MetronomeException("Mission configuration on stdin.")
    if (rawConfiguration.isBlank()) throw MetronomeException("No configurations were provided.")
//    val rawConfiguration = if (rawConfigurations != null && rawConfigurations.isNotBlank()) rawConfigurations else generateConfigurations()
//    println(rawConfiguration)

    // Manually override
    rawConfiguration = generateConfigurations()

    val loader = ExperimentConfiguration.serializer().list
    val parsedConfigurations = JSON.parse(loader, rawConfiguration)
    println(parsedConfigurations)

    val results = ConfigurationExecutor.executeConfigurations(parsedConfigurations, dataRootPath = null, parallelCores = 1)

    val rawResults = JSON.Companion.stringify(ExperimentResult.serializer().list, results)
//    PrintWriter(outputPath, "UTF-8").use { it.write(rawResults) }
//    System.err.println("\nResult has been saved to $outputPath")
    System.err.println(results.summary())

    println('#') // Indicator for the parser
    println(rawResults) // This should be the last printed line

//    runVisualizer(result = results.first())
}

private fun generateConfigurations(): String {
    val commitmentStrategy = CommitmentStrategy.SINGLE.toString()

    val configurations = generateConfigurations(
            domains = listOf(
//                    Domains.SLIDING_TILE_PUZZLE_4 to "input/tiles/korf/4/real/12"
                    Domains.GRID_WORLD to "input/vacuum/cups.vw"
//                    RACETRACK to "input/racetrack/hansen-bigger-quad.track",
//                    RACETRACK to "input/racetrack/barto-big.track",
//                    RACETRACK to "input/racetrack/uniform.track",
//                    RACETRACK to "input/racetrack/barto-small.track"
//                    TRAFFIC to "input/traffic/vehicle0.v"
            ),
//            domains = (88..88).map { TRAFFIC to "input/traffic/50/traffic$it" },
            planners = listOf(TIME_BOUNDED_A_STAR),
            actionDurations = listOf(5),// 250L, 400L, 800L, 1600L, 3200L, 6400L, 12800L),
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
                    Triple(TIME_BOUNDED_A_STAR, COMMITMENT_STRATEGY, listOf(commitmentStrategy)),
                    Triple(TIME_BOUNDED_A_STAR, TBA_OPTIMIZATION, listOf(TBAOptimization.NONE)),
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


