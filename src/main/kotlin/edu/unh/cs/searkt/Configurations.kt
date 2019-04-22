package edu.unh.cs.searkt

import edu.unh.cs.searkt.environment.Domains
import edu.unh.cs.searkt.experiment.configuration.Configurations
import edu.unh.cs.searkt.experiment.configuration.SimpleSerializer
import edu.unh.cs.searkt.experiment.configuration.realtime.LookaheadType
import edu.unh.cs.searkt.experiment.configuration.realtime.TerminationType
import edu.unh.cs.searkt.planner.*
import kotlinx.serialization.json.Json
import kotlinx.serialization.list
import java.util.concurrent.TimeUnit

/**
 * Generate experiment configurations.
 *
 * Modify the content of this function to create custom experiments.
 */
fun generateConfigurations(): String {
    // The commitment strategy determines how many actions does the agent commit to in real-time experiments
    val commitmentStrategy = CommitmentStrategy.SINGLE.toString()
//    val commitmentStrategy = CommitmentStrategy.MULTIPLE.toString()

    val domains = listOf(
            Domains.RACETRACK to "input/racetrack/uniform.track",
            Domains.RACETRACK to "input/racetrack/hansen-bigger-quad.track",
            Domains.RACETRACK to "input/racetrack/hansen-bigger-octa.track"
    )

    val actionDurations = LongProgression.fromClosedRange(20, 100, 5)
    val terminationType = TerminationType.EXPANSION
    val lookaheadType = LookaheadType.DYNAMIC

    // Maximum time per experiment
    val timeLimit = TimeUnit.NANOSECONDS.convert(5, TimeUnit.MINUTES)

    // Bounds to avoid potential infinite loops during planning
    val expansionLimit = 10000000000
    val stepLimit: Long = 10000000

    val safetyExplorationRatio = Triple(Planners.SAFE_RTS, SafeRealTimeSearchConfiguration.SAFETY_EXPLORATION_RATIO, listOf(0.5))

    val domainExtras = listOf(
            Triple(Domains.RACETRACK, Configurations.DOMAIN_SEED.toString(), 0..5L)
    )

    val safeRtsConfiguration = edu.unh.cs.searkt.experiment.configuration.generateConfigurations(
            domains = domains,
            planners = listOf(Planners.SAFE_RTS, Planners.LSS_LRTA_STAR),
            actionDurations = actionDurations,
            terminationType = terminationType,
            lookaheadType = lookaheadType,
            timeLimit = timeLimit,
            expansionLimit = expansionLimit,
            stepLimit = stepLimit,
            plannerExtras = listOf(
                    Triple(Planners.LSS_LRTA_STAR, Configurations.COMMITMENT_STRATEGY, listOf(commitmentStrategy)),
                    Triple(Planners.SAFE_RTS, Configurations.COMMITMENT_STRATEGY, listOf(commitmentStrategy)),
                    Triple(Planners.SAFE_RTS, SafeRealTimeSearchConfiguration.TARGET_SELECTION, listOf(SafeRealTimeSearchTargetSelection.SAFE_TO_BEST.toString())),
                    Triple(Planners.SAFE_RTS, SafeRealTimeSearchConfiguration.SAFETY_PROOF, listOf(SafetyProof.TOP_OF_OPEN)),
                    safetyExplorationRatio
            ),
            domainExtras = domainExtras
    )

    val oracleConfiguration = edu.unh.cs.searkt.experiment.configuration.generateConfigurations(
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