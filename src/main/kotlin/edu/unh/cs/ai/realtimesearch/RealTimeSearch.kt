package edu.unh.cs.ai.realtimesearch

import com.fasterxml.jackson.databind.ObjectMapper
import edu.unh.cs.ai.realtimesearch.environment.Domains
import edu.unh.cs.ai.realtimesearch.environment.Domains.RACETRACK
import edu.unh.cs.ai.realtimesearch.experiment.configuration.ConfigurationExecutor
import edu.unh.cs.ai.realtimesearch.experiment.configuration.Configurations
import edu.unh.cs.ai.realtimesearch.experiment.configuration.Configurations.COMMITMENT_STRATEGY
import edu.unh.cs.ai.realtimesearch.experiment.configuration.generateConfigurations
import edu.unh.cs.ai.realtimesearch.experiment.configuration.realtime.LookaheadType.DYNAMIC
import edu.unh.cs.ai.realtimesearch.experiment.configuration.realtime.TerminationType.EXPANSION
import edu.unh.cs.ai.realtimesearch.experiment.configuration.realtime.TerminationType.TIME
import edu.unh.cs.ai.realtimesearch.experiment.result.summary
import edu.unh.cs.ai.realtimesearch.planner.CommitmentStrategy
import edu.unh.cs.ai.realtimesearch.planner.Planners.*
import edu.unh.cs.ai.realtimesearch.planner.realtime.*
import edu.unh.cs.ai.realtimesearch.planner.realtime.SafeRealTimeSearchConfiguration.SAFETY_EXPLORATION_RATIO
import edu.unh.cs.ai.realtimesearch.planner.realtime.SafeRealTimeSearchConfiguration.TARGET_SELECTION
import edu.unh.cs.ai.realtimesearch.planner.realtime.SafeRealTimeSearchTargetSelection.SAFE_TO_BEST
import java.io.File
import java.io.PrintWriter
import java.util.concurrent.TimeUnit.MINUTES
import java.util.concurrent.TimeUnit.NANOSECONDS
import javax.security.auth.login.Configuration

class Input

fun main(args: Array<String>) {
//    val logger = LoggerFactory.getLogger("Real-time search")

    val weight = args[0].toDouble()
    val alg = args[1]
    val domain = args[2]

    val plannerToRun =
            when (alg) {
                "wa*" -> WEIGHTED_A_STAR
                "dps" -> DYNAMIC_POTENTIAL_SEARCH
                else -> A_STAR
            }

    val domainToRun =
            when (domain) {
                "stp" -> Domains.SLIDING_TILE_PUZZLE_4
                "istp" -> Domains.SLIDING_TILE_PUZZLE_INVERSE
                "hstp" -> Domains.SLIDING_TILE_PUZZLE_HEAVY
                else -> Domains.SLIDING_TILE_PUZZLE_4
            }

    val commitmentStrategy = CommitmentStrategy.SINGLE.toString()

    val korfDomains = mutableListOf<Pair<Domains, String>>()

    (1 until 101).forEach { korfDomains.add(domainToRun to "input/tiles/korf/4/real/$it") }

    val configurations = generateConfigurations(
            domains = korfDomains, //listOf(
//                      domainToRun to "input/tiles/korf/4/real/12"
//                    Domains.GRID_WORLD to "input/vacuum/empty.vw"
//                    Domains.RACETRACK to "input/racetrack/hansen-bigger-quad.track"
//                    Domains.RACETRACK to "input/racetrack/barto-big.track"
//                    Domains.RACETRACK to "input/racetrack/uniform.track",
//                    Domains.RACETRACK to "input/racetrack/barto-small.track"
//                    TRAFFIC to "input/traffic/vehicle0.v"
//            ),
//            domains = (88..88).map { TRAFFIC to "input/traffic/50/traffic$it" },
            planners = listOf(plannerToRun),
            actionDurations = listOf(1L),//50L, 100L, 150L, 200L, 250L, 400L, 800L, 1600L, 3200L, 6400L, 12800L),
            terminationType = EXPANSION,
            lookaheadType = DYNAMIC,
            timeLimit = NANOSECONDS.convert(15, MINUTES),
            expansionLimit = 100000000,
            stepLimit = 100000000,
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
                    Triple(SIMPLE_SAFE, SimpleSafeConfiguration.SAFETY_BACKUP, listOf(SimpleSafeSafetyBackup.PREDECESSOR.toString())),
                    Triple(SIMPLE_SAFE, SimpleSafeConfiguration.SAFETY, listOf(SimpleSafeSafety.PREFERRED.toString())),
                    Triple(SIMPLE_SAFE, TARGET_SELECTION, listOf(SAFE_TO_BEST.toString())),
                    Triple(SIMPLE_SAFE, COMMITMENT_STRATEGY, listOf(commitmentStrategy)),
                    Triple(SIMPLE_SAFE, SimpleSafeConfiguration.VERSION, listOf(SimpleSafeVersion.TWO.toString())),
                    Triple(LSS_LRTA_STAR, COMMITMENT_STRATEGY, listOf(commitmentStrategy)),
                    Triple(WEIGHTED_A_STAR, Configurations.WEIGHT, listOf(weight)),
                    Triple(DYNAMIC_POTENTIAL_SEARCH, Configurations.WEIGHT, listOf(weight))
            ),
            domainExtras = listOf(
                    Triple(RACETRACK, Configurations.DOMAIN_SEED.toString(), 77L..77L)
            )
    )

    configurations.forEach(::println)

//    configurations.forEach {
//        println(it.toIndentedJson())
//        val instanceFileName = it.domainPath
//        val input = Input::class.java.classLoader.getResourceAsStream(instanceFileName) ?: throw RuntimeException("Resource not found")
//        val rawDomain = Scanner(input).useDelimiter("\\Z").next()
//        it["rawDomain"] = rawDomain
//    }

    println("${configurations.size} configuration has been generated.")

    val results = ConfigurationExecutor.executeConfigurations(configurations, dataRootPath = null, parallelCores = 1)

    val objectMapper = ObjectMapper()

    File("output").mkdir()
    PrintWriter("output/results.json", "UTF-8").use { it.write(objectMapper.writeValueAsString(results)) }
    println("\n$results")
    println("\nResult has been saved to 'output/results.json'.")

    println(results.summary())

//    runVisualizer(result = results.first())
}

