package edu.unh.cs.searkt

import edu.unh.cs.searkt.environment.Domains
import edu.unh.cs.searkt.environment.Domains.RACETRACK
import edu.unh.cs.searkt.experiment.configuration.*
import edu.unh.cs.searkt.experiment.configuration.Configurations.COMMITMENT_STRATEGY
import edu.unh.cs.searkt.experiment.configuration.realtime.LookaheadType.DYNAMIC
import edu.unh.cs.searkt.experiment.configuration.realtime.TerminationType.EXPANSION
import edu.unh.cs.searkt.experiment.result.ExperimentResult
import edu.unh.cs.searkt.planner.CommitmentStrategy
import edu.unh.cs.searkt.planner.Planners.LSS_LRTA_STAR
import edu.unh.cs.searkt.planner.Planners.SAFE_RTS
import edu.unh.cs.searkt.planner.SafeRealTimeSearchConfiguration.*
import edu.unh.cs.searkt.planner.SafeRealTimeSearchTargetSelection.SAFE_TO_BEST
import edu.unh.cs.searkt.planner.SafetyProof
import kotlinx.serialization.json.Json
import kotlinx.serialization.list
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit.MINUTES
import java.util.concurrent.TimeUnit.NANOSECONDS

fun main(args: Array<String>) {
    val rawConfiguration: String = readLine() ?: throw MetronomeException("Missing configuration on stdin.")
//    val rawConfiguration = generateConfigurations()

    if (rawConfiguration.isBlank()) throw MetronomeException("No configurations were provided.")

    val parsedConfigurations = Json.parse(ExperimentConfiguration.serializer().list, rawConfiguration)

    val results = ConfigurationExecutor.executeConfigurations(parsedConfigurations, dataRootPath = null, parallelCores = 4)

    val rawResults = Json.stringify(ExperimentResult.serializer().list, results)
//    PrintWriter("results/d_filter_unsafe_2.json", "UTF-8").use { it.write(rawResults) }
//    System.err.println("\nResult has been saved to $outputPath")

//    System.err.println(results.summary())
    println('#') // Indicator for the parser
    println(rawResults) // This should be the last printed line
}

private fun generateConfigurations(): String {
    val commitmentStrategy = CommitmentStrategy.SINGLE.toString()
//    val commitmentStrategy = CommitmentStrategy.MULTIPLE.toString()

    val configurations = generateConfigurations(
//            domains = (88..89).map { Domains.TRAFFIC to "input/traffic/50/traffic$it" },
            domains =
//            (88..98).map { Domains.TRAFFIC to "input/traffic/50/traffic$it" } +
            listOf(
//                    Domains.TRAFFIC to "input/traffic/10/traffic0"
//                    Domains.RACETRACK to "input/racetrack/uniform.track"
//                    Domains.RACETRACK to "input/racetrack/hansen-bigger-d-wide3.track",
                    Domains.RACETRACK to "input/racetrack/hansen-bigger-quad.track"
//                    Domains.RACETRACK to "input/racetrack/hansen-bigger-octa.track"
            ),
//            planners = listOf(LSS_LRTA_STAR),
            planners = listOf(SAFE_RTS),
//            actionDurations = LongProgression.fromClosedRange(10, 1000, 50),
//            actionDurations = listOf(50L, 100L, 150L, 200L, 250L, 400L, 800L, 1600L, 3200L, 6400L, 12800L),
            actionDurations = listOf(3200L, 6400L, 12800L),
//            actionDurations = listOf(20, 100, 150, 200, 300, 400, 600, 800, 1600),
            terminationType = EXPANSION,
            lookaheadType = DYNAMIC,
            timeLimit = NANOSECONDS.convert(1999, MINUTES),
            expansionLimit = 10000000000,
            stepLimit = 10000000,
            plannerExtras = listOf(
                    Triple(LSS_LRTA_STAR, COMMITMENT_STRATEGY, listOf(commitmentStrategy)),
                    Triple(SAFE_RTS, COMMITMENT_STRATEGY, listOf(commitmentStrategy)),
                    Triple(SAFE_RTS, TARGET_SELECTION, listOf(SAFE_TO_BEST.toString())),
                    Triple(SAFE_RTS, FILTER_UNSAFE, listOf(false, true)),
                    Triple(SAFE_RTS, SAFETY_PROOF, listOf(SafetyProof.TOP_OF_OPEN)), Triple(SAFE_RTS, SAFETY_EXPLORATION_RATIO, listOf(1))
//                    Triple(SAFE_RTS, SAFETY_PROOF, listOf(SafetyProof.A_STAR_FIRST)), Triple(SAFE_RTS, SAFETY_EXPLORATION_RATIO, listOf(0.8))
            ),
            domainExtras = listOf(
                    Triple(RACETRACK, Configurations.DOMAIN_SEED.toString(), 0..10L)
            )
    )

    println("${configurations.size} configuration has been generated.")
    return Json.indented.stringify(SimpleSerializer.list, configurations.toList())
}

val visualizerLatch = CountDownLatch(1)
