package edu.unh.cs.ai.realtimesearch

import com.fasterxml.jackson.databind.ObjectMapper
import edu.unh.cs.ai.realtimesearch.environment.Domains.RACETRACK
import edu.unh.cs.ai.realtimesearch.environment.Domains.TRAFFIC
import edu.unh.cs.ai.realtimesearch.experiment.configuration.ConfigurationExecutor
import edu.unh.cs.ai.realtimesearch.experiment.configuration.Configurations
import edu.unh.cs.ai.realtimesearch.experiment.configuration.GeneralExperimentConfiguration
import edu.unh.cs.ai.realtimesearch.experiment.configuration.generateConfigurations
import edu.unh.cs.ai.realtimesearch.experiment.configuration.json.toIndentedJson
import edu.unh.cs.ai.realtimesearch.experiment.configuration.realtime.LookaheadType.DYNAMIC
import edu.unh.cs.ai.realtimesearch.experiment.configuration.realtime.TerminationType.EXPANSION
import edu.unh.cs.ai.realtimesearch.planner.CommitmentStrategy.SINGLE
import edu.unh.cs.ai.realtimesearch.planner.Planners.*
import edu.unh.cs.ai.realtimesearch.planner.realtime.SafeRealTimeSearchConfiguration.SAFETY_EXPLORATION_RATIO
import edu.unh.cs.ai.realtimesearch.planner.realtime.SafeRealTimeSearchConfiguration.TARGET_SELECTION
import edu.unh.cs.ai.realtimesearch.planner.realtime.SafeRealTimeSearchTargetSelection.BEST_SAFE
import edu.unh.cs.ai.realtimesearch.planner.realtime.SafeRealTimeSearchTargetSelection.SAFE_TO_BEST
import org.slf4j.LoggerFactory
import java.io.PrintWriter
import java.util.*
import java.util.concurrent.TimeUnit.MINUTES
import java.util.concurrent.TimeUnit.NANOSECONDS

class Input

private var manualConfiguration: GeneralExperimentConfiguration = GeneralExperimentConfiguration()
private var outFile: String = ""
private val visualizerParameters = mutableListOf<String>()

fun main(args: Array<String>) {
    val logger = LoggerFactory.getLogger("Real-time search")

    val configurations = generateConfigurations(
            domains = listOf(
                    //                      RACETRACK to "input/racetrack/uniform.track",
//                      RACETRACK to "input/racetrack/barto-big.track",
//                      RACETRACK to "input/racetrack/barto-small.track",
//                      RACETRACK to "input/racetrack/hansen-bigger-doubled.track"
                    TRAFFIC to ""
            ),
            planners = listOf(A_STAR, LSS_LRTA_STAR, SAFE_RTS, S_ONE, S_ZERO),
            //            planners = listOf(SAFE_RTS),
            //            planners = listOf(SAFE_RTS),
            commitmentStrategy = listOf(SINGLE),
            //            actionDurations = listOf(1000L, 2000L, 3000L, 4000L, 5000L, 6000L, 7000L, 8000L, 9000L),
            actionDurations = listOf(6000),
            terminationType = EXPANSION,
            lookaheadType = DYNAMIC,
            timeLimit = NANOSECONDS.convert(10, MINUTES),
            plannerExtras = listOf(
                    Triple(SAFE_RTS, TARGET_SELECTION.toString(), listOf(BEST_SAFE.toString(), SAFE_TO_BEST.toString())),
                    Triple(SAFE_RTS, SAFETY_EXPLORATION_RATIO.toString(), listOf(0.3))
            ),
            domainExtras = listOf(
                    Triple(RACETRACK, Configurations.DOMAIN_SEED.toString(), 0L..3L)
            )
    )

    configurations.forEach {
        println(it.toIndentedJson())
        val instanceFileName = it.domainPath
        val input = Input::class.java.classLoader.getResourceAsStream(instanceFileName) ?: throw RuntimeException("Resource not found")
        val rawDomain = Scanner(input).useDelimiter("\\Z").next()
        it["rawDomain"] = rawDomain
    }
    val results = ConfigurationExecutor.executeConfigurations(configurations, dataRootPath = null, parallel = true)


    val objectMapper = ObjectMapper()
    PrintWriter("output/results.json", "UTF-8").use { it.write(objectMapper.writeValueAsString(results)) }
//    runVisualizer(result = results.first())
}


