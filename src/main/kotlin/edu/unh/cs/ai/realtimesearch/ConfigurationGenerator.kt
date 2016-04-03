package edu.unh.cs.ai.realtimesearch

import edu.unh.cs.ai.realtimesearch.environment.Domains
import edu.unh.cs.ai.realtimesearch.environment.Domains.*
import edu.unh.cs.ai.realtimesearch.environment.acrobot.AcrobotLink
import edu.unh.cs.ai.realtimesearch.environment.acrobot.configuration.AcrobotConfiguration
import edu.unh.cs.ai.realtimesearch.environment.acrobot.configuration.AcrobotStateConfiguration
import edu.unh.cs.ai.realtimesearch.experiment.configuration.realtime.TimeBoundType
import edu.unh.cs.ai.realtimesearch.experiment.configuration.realtime.TimeBoundType.DYNAMIC
import edu.unh.cs.ai.realtimesearch.experiment.configuration.realtime.TimeBoundType.STATIC
import edu.unh.cs.ai.realtimesearch.planner.CommitmentStrategy
import edu.unh.cs.ai.realtimesearch.planner.CommitmentStrategy.SINGLE
import edu.unh.cs.ai.realtimesearch.planner.Planners
import edu.unh.cs.ai.realtimesearch.planner.Planners.*
import groovy.json.JsonOutput
import org.springframework.http.HttpEntity
import org.springframework.http.HttpMethod
import org.springframework.http.HttpStatus
import org.springframework.web.client.RestTemplate
import java.util.*
import java.util.concurrent.TimeUnit

val terminationType = "time"
val timeLimit = TimeUnit.NANOSECONDS.convert(300, TimeUnit.SECONDS)
val actionDurationRange = 1..5
val actionDurations = actionDurationRange.map { Math.pow(10.0, it.toDouble()).toLong() * 100 }
val lookaheadLimits = 1..6

fun main(args: Array<String>) {
    val configurations = mutableListOf<MutableMap<String, Any?>>()

    for (domain in Domains.values()) {
        for (planner in Planners.values()) {
            for (actionDuration in actionDurations) {
                val partialConfiguration = mutableMapOf<String, Any?>(
                        "domainName" to domain,
                        "algorithmName" to planner,
                        "actionDuration" to actionDuration,
                        "timeLimit" to timeLimit,
                        "terminationType" to terminationType
                )

                val realTimePlannerConfigurations = getPlannerConfigurations(planner)
                val domainConfigurations = getDomainConfigurations(domain)

                for (realTimePlannerConfiguration in realTimePlannerConfigurations) {
                    for (domainConfiguration in domainConfigurations) {

                        val completeConfiguration = mutableMapOf<String, Any?>()

                        completeConfiguration.putAll(partialConfiguration)
                        completeConfiguration.putAll(realTimePlannerConfiguration)
                        completeConfiguration.putAll(domainConfiguration)

                        configurations.add(completeConfiguration)
                    }
                }
            }
        }
    }

//    for (configuration in configurations) {
//        println(ExperimentData(configuration).toIndentedJson())
//    }

    println("${configurations.size} configurations were generated.")
    uploadConfigurations(configurations)
}

fun getDomainConfigurations(domain: Domains): MutableList<MutableMap<String, Any?>> {
    val configurations = mutableListOf<MutableMap<String, Any?>>()

    val gridMaps = listOf(
            "input/vacuum/dylan/cups.vw",
            "input/vacuum/dylan/slalom.vw",
            "input/vacuum/dylan/uniform.vw",
            "input/vacuum/dylan/wall.vw"
    )

    val racetrackMaps = listOf(
            "input/racetrack/barto-big.track",
            "input/racetrack/barto-small.track"
    )

    val pointRobotMaps = listOf(
            "input/pointrobot/empty.pr",
            "input/pointrobot/smallmaze.pr",
            "input/pointrobot/uniform.pr",
            "input/pointrobot/wall.pr",
            "input/pointrobot/wall2.pr"
    )

    val slidingTile4MapRoot = "input/tiles/korf/4/all/"
    val slidingTile25EasyMapRoot = "input/tiles/korf/4/25_easy/"
    val slidingTileEasyMapRoot = "input/tiles/korf/4/easy"

    val slidingTile25EasyMapNames = listOf(9, 12, 19, 28, 30, 31, 42, 45, 47, 48, 55, 57, 61, 71, 73, 74, 79, 81, 85, 86, 90, 93, 94, 95, 97)
    val slidingTIleEasyMapNames = listOf (5, 6, 8, 9, 12, 13, 19, 20, 23, 28, 30, 31, 34, 38, 39, 42, 45, 46, 47, 48, 51, 55, 57, 58, 61, 62, 65, 71, 73, 74, 77, 78, 79, 81, 85, 86, 90, 93, 94, 95, 96, 97)

    when (domain) {
        ACROBOT -> {
            val bounds = listOf(
                    0.3,
                    0.1,
                    0.09,
                    0.08,
                    0.07
            )
            val stateConfiguration = AcrobotStateConfiguration()

            for (lowerBound in bounds) {
                for (upperBound in bounds) {
                    val acrobotConfiguration = AcrobotConfiguration(
                            endLink1LowerBound = AcrobotLink(lowerBound, lowerBound),
                            endLink2LowerBound = AcrobotLink(lowerBound, lowerBound),
                            endLink1UpperBound = AcrobotLink(upperBound, upperBound),
                            endLink2UpperBound = AcrobotLink(upperBound, upperBound),
                            stateConfiguration = stateConfiguration
                    )
                    configurations.add(mutableMapOf(
                            "rawDomain" to "${JsonOutput.toJson(acrobotConfiguration).replace("\"", "\\\"")}",
                            "domainInstanceName" to "$lowerBound-$upperBound"
                    ))
                }
            }
        }
        GRID_WORLD, VACUUM_WORLD, POINT_ROBOT, POINT_ROBOT_WITH_INERTIA -> {
            for (map in gridMaps) {
                val input = GRID_WORLD.javaClass.classLoader.getResourceAsStream(map)
                configurations.add(mutableMapOf(
                        "rawDomain" to Scanner(input).useDelimiter("\\Z").next(),
                        "domainInstanceName" to map
                ))
            }
        }
        RACETRACK -> {
            for (map in racetrackMaps) {
                val input = GRID_WORLD.javaClass.classLoader.getResourceAsStream(map)
                configurations.add(mutableMapOf(
                        "rawDomain" to Scanner(input).useDelimiter("\\Z").next(),
                        "domainInstanceName" to map
                ))
            }
        }
        SLIDING_TILE_PUZZLE_4 -> {
            for (instanceName in slidingTile25EasyMapNames) {
                val map = "$slidingTile4MapRoot$instanceName"
                val input = GRID_WORLD.javaClass.classLoader.getResourceAsStream(map)
                configurations.add(mutableMapOf(
                        "rawDomain" to Scanner(input).useDelimiter("\\Z").next(),
                        "domainInstanceName" to map
                ))
            }
        }
        POINT_ROBOT, POINT_ROBOT_WITH_INERTIA -> {
            for (map in pointRobotMaps) {
                val input = GRID_WORLD.javaClass.classLoader.getResourceAsStream(map)
                configurations.add(mutableMapOf(
                        "rawDomain" to Scanner(input).useDelimiter("\\Z").next(),
                        "domainInstanceName" to map
                ))
            }
        }
    }

    return configurations
}

fun getPlannerConfigurations(planner: Planners): MutableList<MutableMap<String, Any?>> {
    val configurations = mutableListOf<MutableMap<String, Any?>>()

    val weights = listOf(
            2.0,
            3.0
    )

    when (planner) {
        DYNAMIC_F_HAT, LSS_LRTA_STAR -> {
            for (timeBoundType in TimeBoundType.values()) {

                when (timeBoundType) {
                    STATIC -> {
                        configurations.add(mutableMapOf<String, Any?>(
                                "timeBoundType" to timeBoundType,
                                "commitmentStrategy" to SINGLE
                        ))
                        configurations.add(mutableMapOf<String, Any?>(
                                "timeBoundType" to timeBoundType,
                                "commitmentStrategy" to CommitmentStrategy.MULTIPLE
                        ))
                    }
                    DYNAMIC -> {
                        configurations.add(mutableMapOf<String, Any?>(
                                "timeBoundType" to timeBoundType,
                                "commitmentStrategy" to CommitmentStrategy.MULTIPLE
                        ))
                    }
                }

            }
        }
        RTA_STAR -> {
            for (lookaheadDepthLimit in lookaheadLimits) {
                configurations.add(mutableMapOf<String, Any?>(
                        "lookaheadDepthLimit" to lookaheadDepthLimit,
                        "timeBoundType" to STATIC,
                        "commitmentStrategy" to SINGLE
                ))
            }
        }
        WEIGHTED_A_STAR -> {
            for (weight in weights) {
                configurations.add(mutableMapOf(
                        "weight" to weight
                ))
            }
        }
        else -> configurations.add(mutableMapOf())
    }

    return configurations
}

fun uploadConfigurations(configurations: MutableList<MutableMap<String, Any?>>) {
    val restTemplate = RestTemplate()
    //    val serverUrl = "http://aerials.cs.unh.edu:3824
    var serverUrl = "http://localhost:3824/configurations"

    println("Upload generated files. ${configurations.size}")
    val responseEntity = restTemplate.exchange(serverUrl, HttpMethod.POST, HttpEntity(configurations), Nothing::class.java)
    if (responseEntity.statusCode == HttpStatus.OK) {
        println("Upload completed! ${configurations.size}")
    } else {
        println("Upload failed! ${configurations.size}")
    }

}