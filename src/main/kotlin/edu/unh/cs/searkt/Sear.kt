package edu.unh.cs.searkt

import edu.unh.cs.searkt.environment.Domains
import edu.unh.cs.searkt.experiment.configuration.*
import edu.unh.cs.searkt.experiment.configuration.Configurations.COMMITMENT_STRATEGY
import edu.unh.cs.searkt.experiment.configuration.realtime.LookaheadType.DYNAMIC
import edu.unh.cs.searkt.experiment.configuration.realtime.TerminationType.EXPANSION
import edu.unh.cs.searkt.experiment.result.ExperimentResult
import edu.unh.cs.searkt.experiment.result.summary
import edu.unh.cs.searkt.planner.CommitmentStrategy
import edu.unh.cs.searkt.planner.Planners
import edu.unh.cs.searkt.planner.Planners.LSS_LRTA_STAR
import edu.unh.cs.searkt.planner.Planners.SAFE_RTS
import edu.unh.cs.searkt.planner.SafeRealTimeSearchConfiguration.*
import edu.unh.cs.searkt.planner.SafeRealTimeSearchTargetSelection.SAFE_TO_BEST
import edu.unh.cs.searkt.planner.SafetyProof
import kotlinx.serialization.json.Json
import kotlinx.serialization.list
import java.util.concurrent.TimeUnit.MINUTES
import java.util.concurrent.TimeUnit.NANOSECONDS

fun main(args: Array<String>) {
    if (args.isEmpty()) {
        println(
                """Please provide one of the following flags to determine the source of configurations:
            | -internalConfiguration    - Generate the hard coded configurations.
            | -stdinConfiguration       - Receive configurations on the standard input.
        """.trimMargin())

        return
    }

    // Get the configurations string from the appropriate source
    val rawConfiguration = when (val configurationSource = args[0]) {
        "-internalConfiguration" -> generateConfigurations()
        "-stdinConfiguration" -> readLine() ?: throw MetronomeException("Missing configuration on stdin.")
        else -> throw MetronomeException("Unknown configuration source: $configurationSource")
    }


    if (rawConfiguration.isBlank()) throw MetronomeException("No configurations were provided.")

    // Convert the json configuration string to experiment configuration instances
    val parsedConfigurations = Json.parse(ExperimentConfiguration.serializer().list, rawConfiguration)

    // Execute the experiments
    val results = ConfigurationExecutor.executeConfigurations(parsedConfigurations, dataRootPath = null, parallelCores = 4)

    // Convert the results to json
    val rawResults = Json.stringify(ExperimentResult.serializer().list, results)

    // Print results
    //    PrintWriter("results/results.json", "UTF-8").use { it.write(rawResults) }
    //    System.err.println("\nResult has been saved to $outputPath")

    System.err.println(results.summary())
    println('#') // Indicator for the parser
    println(rawResults) // This should be the last printed line
}

private fun generateConfigurations(): String {
    // The commitment strategy determines how many actions does the agent commit to in real-time experiments
    val commitmentStrategy = CommitmentStrategy.SINGLE.toString()
//    val commitmentStrategy = CommitmentStrategy.MULTIPLE.toString()

    val domains = listOf(
            Domains.RACETRACK to "input/racetrack/uniform.track",
            Domains.RACETRACK to "input/racetrack/hansen-bigger-quad.track",
            Domains.RACETRACK to "input/racetrack/hansen-bigger-octa.track"
    )

    val actionDurations = LongProgression.fromClosedRange(20, 100, 5)
    val terminationType = EXPANSION
    val lookaheadType = DYNAMIC

    // Maximum time per experiment
    val timeLimit = NANOSECONDS.convert(5, MINUTES)

    // Bounds to avoid potential infinite loops during planning
    val expansionLimit = 10000000000
    val stepLimit: Long = 10000000

    val safetyExplorationRatio = Triple(SAFE_RTS, SAFETY_EXPLORATION_RATIO, listOf(0.5))

    val domainExtras = listOf(
            Triple(Domains.RACETRACK, Configurations.DOMAIN_SEED.toString(), 0..5L)
    )

    val safeRtsConfiguration = generateConfigurations(
            domains = domains,
            planners = listOf(SAFE_RTS, LSS_LRTA_STAR),
            actionDurations = actionDurations,
            terminationType = terminationType,
            lookaheadType = lookaheadType,
            timeLimit = timeLimit,
            expansionLimit = expansionLimit,
            stepLimit = stepLimit,
            plannerExtras = listOf(
                    Triple(LSS_LRTA_STAR, COMMITMENT_STRATEGY, listOf(commitmentStrategy)),
                    Triple(SAFE_RTS, COMMITMENT_STRATEGY, listOf(commitmentStrategy)),
                    Triple(SAFE_RTS, TARGET_SELECTION, listOf(SAFE_TO_BEST.toString())),
                    Triple(SAFE_RTS, SAFETY_PROOF, listOf(SafetyProof.TOP_OF_OPEN)),
                    safetyExplorationRatio
            ),
            domainExtras = domainExtras
    )

    val oracleConfiguration = generateConfigurations(
            domains = domains,
            planners = listOf(Planners.A_STAR),
            actionDurations = listOf(actionDurations.min()!!, actionDurations.max()!!),
            terminationType = terminationType,
            lookaheadType = lookaheadType,
            timeLimit = timeLimit,
            expansionLimit = expansionLimit,
            stepLimit = stepLimit,
            domainExtras = domainExtras
    )

    val configurations = safeRtsConfiguration + oracleConfiguration

    println("${configurations.size} configuration has been generated.")

    // Convert the configurations to raw string
    return Json.indented.stringify(SimpleSerializer.list, configurations.toList())
}
