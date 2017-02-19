package edu.unh.cs.ai.realtimesearch

import com.fasterxml.jackson.databind.ObjectMapper
import edu.unh.cs.ai.realtimesearch.environment.Domains
import edu.unh.cs.ai.realtimesearch.environment.Domains.TRAFFIC
import edu.unh.cs.ai.realtimesearch.experiment.configuration.ConfigurationExecutor
import edu.unh.cs.ai.realtimesearch.experiment.configuration.Configurations
import edu.unh.cs.ai.realtimesearch.experiment.configuration.Configurations.COMMITMENT_STRATEGY
import edu.unh.cs.ai.realtimesearch.experiment.configuration.generateConfigurations
import edu.unh.cs.ai.realtimesearch.experiment.configuration.json.toIndentedJson
import edu.unh.cs.ai.realtimesearch.experiment.configuration.realtime.LookaheadType.DYNAMIC
import edu.unh.cs.ai.realtimesearch.experiment.configuration.realtime.TerminationType.EXPANSION
import edu.unh.cs.ai.realtimesearch.experiment.result.summary
import edu.unh.cs.ai.realtimesearch.planner.CommitmentStrategy
import edu.unh.cs.ai.realtimesearch.planner.Planners.*
import edu.unh.cs.ai.realtimesearch.planner.realtime.SafeRealTimeSearchConfiguration.SAFETY_EXPLORATION_RATIO
import edu.unh.cs.ai.realtimesearch.planner.realtime.SafeRealTimeSearchConfiguration.TARGET_SELECTION
import edu.unh.cs.ai.realtimesearch.planner.realtime.SafeRealTimeSearchTargetSelection.BEST_SAFE
import edu.unh.cs.ai.realtimesearch.planner.realtime.SafeRealTimeSearchTargetSelection.SAFE_TO_BEST
import edu.unh.cs.ai.realtimesearch.planner.realtime.SafeZeroConfiguration
import edu.unh.cs.ai.realtimesearch.planner.realtime.SafeZeroSafety
import edu.unh.cs.ai.realtimesearch.planner.realtime.SafeZeroSafetyBackup
import org.slf4j.LoggerFactory
import java.io.File
import java.io.PrintWriter
import java.util.concurrent.TimeUnit.MINUTES
import java.util.concurrent.TimeUnit.NANOSECONDS

class Input

fun main(args: Array<String>) {
    val logger = LoggerFactory.getLogger("Real-time search")

    val configurations = generateConfigurations(
//            domains = listOf(
//                    Domains.RACETRACK to "input/racetrack/hansen-bigger-quad.track",
//                    Domains.RACETRACK to "input/racetrack/barto-big.track",
//                    Domains.RACETRACK to "input/racetrack/uniform.track"
//            ),
            domains = (0..99).map { TRAFFIC to "input/traffic/50/traffic$it" },
//            domains = listOf( TRAFFIC to "input/traffic/vehicle1.v" ),
            planners = listOf(S_ZERO),
            actionDurations = listOf(50L, 100L, 150L, 200L, 250L, 400L, 800L, 1600L, 3200L, 6400L, 12800L),
            terminationType = EXPANSION,
            lookaheadType = DYNAMIC,
            timeLimit = NANOSECONDS.convert(10, MINUTES),
            plannerExtras = listOf(
                    Triple(SAFE_RTS, TARGET_SELECTION.toString(), listOf(SAFE_TO_BEST.toString())),
                    Triple(SAFE_RTS, SAFETY_EXPLORATION_RATIO.toString(), listOf(1.0)),
                    Triple(SAFE_RTS, COMMITMENT_STRATEGY.toString(), listOf(CommitmentStrategy.SINGLE.toString())),
                    Triple(S_ZERO, COMMITMENT_STRATEGY.toString(), listOf(CommitmentStrategy.SINGLE.toString())),
                    Triple(S_ZERO, SafeZeroConfiguration.SAFETY_BACKUP.toString(), listOf(SafeZeroSafetyBackup.PREDECESSOR.toString())),
                    Triple(S_ZERO, SafeZeroConfiguration.SAFETY.toString(), listOf(SafeZeroSafety.PREFERRED.toString())),
                    Triple(S_ZERO, TARGET_SELECTION.toString(), listOf(SAFE_TO_BEST.toString())),
                    Triple(LSS_LRTA_STAR, COMMITMENT_STRATEGY.toString(), listOf(CommitmentStrategy.SINGLE.toString()))
            ),
            domainExtras = listOf(
                    Triple(Domains.RACETRACK, Configurations.DOMAIN_SEED.toString(), 0L..25L)
            )
    )

    val reruns = listOf("input/traffic/50/traffic2" to 100, "input/traffic/50/traffic2" to 150, "input/traffic/50/traffic2" to 200, "input/traffic/50/traffic2" to 250, "input/traffic/50/traffic2" to 400, "input/traffic/50/traffic2" to 800, "input/traffic/50/traffic2" to 1600, "input/traffic/50/traffic2" to 3200, "input/traffic/50/traffic2" to 6400, "input/traffic/50/traffic2" to 12800, "input/traffic/50/traffic3" to 400, "input/traffic/50/traffic33" to 12800, "input/traffic/50/traffic35" to 50, "input/traffic/50/traffic35" to 100, "input/traffic/50/traffic35" to 3200, "input/traffic/50/traffic35" to 12800, "input/traffic/50/traffic36" to 50, "input/traffic/50/traffic36" to 100, "input/traffic/50/traffic36" to 200, "input/traffic/50/traffic36" to 250, "input/traffic/50/traffic36" to 800, "input/traffic/50/traffic36" to 1600, "input/traffic/50/traffic36" to 3200, "input/traffic/50/traffic36" to 6400, "input/traffic/50/traffic36" to 12800, "input/traffic/50/traffic38" to 100, "input/traffic/50/traffic38" to 150, "input/traffic/50/traffic38" to 200, "input/traffic/50/traffic38" to 400, "input/traffic/50/traffic38" to 800, "input/traffic/50/traffic38" to 1600, "input/traffic/50/traffic38" to 3200, "input/traffic/50/traffic50" to 800, "input/traffic/50/traffic51" to 50, "input/traffic/50/traffic51" to 100, "input/traffic/50/traffic51" to 150, "input/traffic/50/traffic51" to 200, "input/traffic/50/traffic51" to 250, "input/traffic/50/traffic51" to 400, "input/traffic/50/traffic51" to 800, "input/traffic/50/traffic51" to 1600, "input/traffic/50/traffic51" to 3200, "input/traffic/50/traffic51" to 6400, "input/traffic/50/traffic51" to 12800, "input/traffic/50/traffic66" to 50, "input/traffic/50/traffic66" to 100, "input/traffic/50/traffic66" to 150, "input/traffic/50/traffic66" to 200, "input/traffic/50/traffic66" to 250, "input/traffic/50/traffic66" to 400, "input/traffic/50/traffic66" to 800, "input/traffic/50/traffic66" to 1600, "input/traffic/50/traffic66" to 3200, "input/traffic/50/traffic66" to 6400, "input/traffic/50/traffic66" to 12800, "input/traffic/50/traffic67" to 400, "input/traffic/50/traffic67" to 12800, "input/traffic/50/traffic85" to 800, "input/traffic/50/traffic93" to 50, "input/traffic/50/traffic93" to 100, "input/traffic/50/traffic93" to 150, "input/traffic/50/traffic93" to 200, "input/traffic/50/traffic93" to 250, "input/traffic/50/traffic93" to 400, "input/traffic/50/traffic93" to 800, "input/traffic/50/traffic93" to 1600, "input/traffic/50/traffic93" to 3200, "input/traffic/50/traffic93" to 6400, "input/traffic/50/traffic93" to 12800, "input/traffic/50/traffic98" to 12800)
    val filtered = configurations.filter { i -> reruns.any { i.domainPath == it.first && i.actionDuration == it.second.toLong() } }

//    configurations.forEach {
//        println(it.toIndentedJson())
//        val instanceFileName = it.domainPath
//        val input = Input::class.java.classLoader.getResourceAsStream(instanceFileName) ?: throw RuntimeException("Resource not found")
//        val rawDomain = Scanner(input).useDelimiter("\\Z").next()
//        it["rawDomain"] = rawDomain
//    }

    println("${configurations.size} configuration has been generated.")

    val results = ConfigurationExecutor.executeConfigurations(filtered, dataRootPath = null, parallelCores = 1)

    val objectMapper = ObjectMapper()

    File("output").mkdir()
    PrintWriter("output/results.json", "UTF-8").use { it.write(objectMapper.writeValueAsString(results)) }
    println("\nResult has been saved to 'output/results.json'.")

    println(results.summary())

//    runVisualizer(result = results.first())
}

