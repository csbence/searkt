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
import edu.unh.cs.ai.realtimesearch.experiment.result.summary
import edu.unh.cs.ai.realtimesearch.planner.CommitmentStrategy
import edu.unh.cs.ai.realtimesearch.planner.Planners
import edu.unh.cs.ai.realtimesearch.planner.Planners.*
import edu.unh.cs.ai.realtimesearch.planner.realtime.*
import edu.unh.cs.ai.realtimesearch.planner.realtime.SafeRealTimeSearchConfiguration.SAFETY_EXPLORATION_RATIO
import edu.unh.cs.ai.realtimesearch.planner.realtime.SafeRealTimeSearchConfiguration.TARGET_SELECTION
import edu.unh.cs.ai.realtimesearch.planner.realtime.SafeRealTimeSearchTargetSelection.SAFE_TO_BEST
import java.io.File
import java.util.concurrent.TimeUnit.MINUTES
import java.util.concurrent.TimeUnit.NANOSECONDS

fun main(args: Array<String>) {
//    val logger = LoggerFactory.getLogger("Real-time search")

    // We might pull some args in

    val planner: Iterable<Planners>
    val duration: Iterable<Long>
    val instance: Iterable<Pair<Domains, String>>
    val seed: Iterable<Long>

    // 3 planner
    // 2 duration
    // 1 domain target
    // 0 seed

    if (args.size == 3) {
        planner = listOf(Planners.valueOf(args[2]))
        duration = listOf(args[1].toLong())
        instance = listOf(Domains.RACETRACK to args[0])
//        seed = listOf(args[0].toLong())
        seed = 0L..99L
    } else {
        planner = listOf(SIMPLE_SAFE)
        duration = listOf(50L, 100L, 150L, 200L, 400L)
        instance = listOf(
//                      Domains.SLIDING_TILE_PUZZLE_4 to "input/tiles/korf/4/real/12"
//                    Domains.GRID_WORLD to "input/vacuum/empty.vw"
                Domains.RACETRACK to "input/racetrack/hansen-bigger-quad.track",
                Domains.RACETRACK to "input/racetrack/barto-big.track",
                Domains.RACETRACK to "input/racetrack/uniform.track",
                Domains.RACETRACK to "input/racetrack/barto-small.track"
//                    TRAFFIC to "input/traffic/vehicle0.v"
        )
        seed = 0L..99L
    }

    val commitmentStrategy = CommitmentStrategy.MULTIPLE.toString()

    val configurations = generateConfigurations(
            domains = instance,
//            domains = (88..88).map { TRAFFIC to "input/traffic/50/traffic$it" },
            planners = planner,
            actionDurations = duration,//, 800L, 1600L, 3200L, 6400L, 12800L),
            terminationType = EXPANSION,
            lookaheadType = DYNAMIC,
            timeLimit = NANOSECONDS.convert(30, MINUTES),
            expansionLimit = 10000000000,
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
                    Triple(SIMPLE_SAFE, SimpleSafeConfiguration.SAFETY_BACKUP, listOf(SimpleSafeSafetyBackup.PREDECESSOR.toString())),
                    Triple(SIMPLE_SAFE, SimpleSafeConfiguration.SAFETY, listOf(SimpleSafeSafety.PREFERRED.toString())),
                    Triple(SIMPLE_SAFE, TARGET_SELECTION, listOf(SAFE_TO_BEST.toString())),
                    Triple(SIMPLE_SAFE, COMMITMENT_STRATEGY, listOf(commitmentStrategy)),
                    Triple(SIMPLE_SAFE, SimpleSafeConfiguration.VERSION, listOf(SimpleSafeVersion.TWO.toString())),
                    Triple(LSS_LRTA_STAR, COMMITMENT_STRATEGY, listOf(commitmentStrategy)),
                    Triple(WEIGHTED_A_STAR, Configurations.WEIGHT, listOf(1.0))
            ),
            domainExtras = listOf(
                    Triple(RACETRACK, Configurations.DOMAIN_SEED.toString(), seed)
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
//    println("\n$results")
    println("\nResult has been saved to 'output/results.json'.")

    File("output/${planner.first()}_results.json").appendText("\n" + objectMapper.writeValueAsString(results))
    println(results.summary())

    // For some reason the process hangs if we don't kill it below (even if it ran successfully!).
    System.exit(0)

//    runVisualizer(result = results.first())
}

