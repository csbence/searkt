package edu.unh.cs.ai.realtimesearch

import edu.unh.cs.ai.realtimesearch.environment.Domains
import edu.unh.cs.ai.realtimesearch.environment.Domains.*
import edu.unh.cs.ai.realtimesearch.experiment.configuration.ExperimentData
import edu.unh.cs.ai.realtimesearch.planner.Planners
import edu.unh.cs.ai.realtimesearch.planner.Planners.*
import org.slf4j.LoggerFactory
import org.springframework.http.HttpEntity
import org.springframework.http.HttpMethod
import org.springframework.http.HttpStatus
import org.springframework.web.client.RestTemplate
import java.util.*
import java.util.concurrent.TimeUnit

val terminationType = "time"
val timeLimit = TimeUnit.NANOSECONDS.convert(300, TimeUnit.SECONDS)
val timeBoundTypes = listOf("STATIC", "DYNAMIC")
val actionDurationRange = 1..5
val actionDurations = actionDurationRange.map { Math.pow(10.0, it.toDouble()) * 100 }
val lookaheadLimits = 1..10

fun main(args: Array<String>) {
    val configurations = mutableListOf<MutableMap<String, Any?>>()

    for (domain in Domains.values()) {
        for (planner in Planners.values()) {
            for (actionDuration in actionDurations) {
                val configuration = mutableMapOf<String, Any?>(
                        "domainName" to domain,
                        "algorithmName" to planner,
                        "actionDuration" to actionDuration,
                        //                                "timeBoundType" to timeBoundType,
                        "timeLimit" to timeLimit,
                        "terminationType" to terminationType
                )

                val realTimePlannerConfigurations = realTimePlanners(planner)
                for (realTimePlannerConfiguration in realTimePlannerConfigurations) {
                    val domainConfigurations = getDomainConfigurations(domain)
                    for (domainConfiguration in domainConfigurations) {
                        realTimePlannerConfiguration.putAll(domainConfiguration)
                    }
                    realTimePlannerConfiguration.putAll(configuration)
                }
                // TODO integrate acrobot configurations
                if (!domain.equals(ACROBOT))
                    configurations.addAll(realTimePlannerConfigurations)
            }
        }
    }

//    for (configuration in configurations) {
//        println(configuration)
//    }

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

    val slidingTileMaps = "input/tiles/korf/4/"

    when (domain) {
        ACROBOT -> {

        }
        GRID_WORLD, VACUUM_WORLD, POINT_ROBOT, POINT_ROBOT_WITH_INERTIA -> {
            for (map in gridMaps) {
                val input = GRID_WORLD.javaClass.classLoader.getResourceAsStream(map)
                configurations.add(mutableMapOf(
                        "rawDomain" to Scanner(input).useDelimiter("\\Z").next()
                ))
            }
        }
        RACETRACK -> {
            for (map in racetrackMaps) {
                val input = GRID_WORLD.javaClass.classLoader.getResourceAsStream(map)
                configurations.add(mutableMapOf(
                        "rawDomain" to Scanner(input).useDelimiter("\\Z").next()
                ))
            }
        }
        SLIDING_TILE_PUZZLE -> {
            for (i in 1..100) {
                val input = GRID_WORLD.javaClass.classLoader.getResourceAsStream("$slidingTileMaps$i")
                configurations.add(mutableMapOf(
                        "rawDomain" to Scanner(input).useDelimiter("\\Z").next()
                ))
            }
        }
        POINT_ROBOT, POINT_ROBOT_WITH_INERTIA -> {
            for (map in pointRobotMaps) {
                val input = GRID_WORLD.javaClass.classLoader.getResourceAsStream(map)
                configurations.add(mutableMapOf(
                        "rawDomain" to Scanner(input).useDelimiter("\\Z").next()
                ))
            }
        }
    }

    return configurations
}

fun realTimePlanners(planner: Planners): MutableList<MutableMap<String, Any?>> {
    val configurations = mutableListOf<MutableMap<String, Any?>>()

    when (planner) {
        DYNAMIC_F_HAT, LSS_LRTA_STAR -> {
            for (timeBoundType in timeBoundTypes) {
                configurations.add(mutableMapOf<String, Any?>(
                        "timeBoundType" to timeBoundType
                ))
            }
        }
        RTA_STAR -> {
            for (lookaheadDepthLimit in lookaheadLimits) {
                configurations.add(mutableMapOf<String, Any?>(
                        "lookaheadDepthLimit" to lookaheadDepthLimit
                ))
            }
        }
    }

    return configurations
}

fun uploadConfigurations(configurations: MutableList<MutableMap<String, Any?>>) {
    val restTemplate = RestTemplate()
    val serverUrl = "http://aerials.cs.unh.edu:3824"
    var successCounter = 0
    var failCounter = 0

    for (configuration in configurations) {
        val configurationData = ExperimentData(configuration)

        val responseEntity = restTemplate.exchange(serverUrl, HttpMethod.POST, HttpEntity(configurationData), Nothing::class.java)
        if (responseEntity.statusCode == HttpStatus.OK) {
            successCounter++
        } else {
            failCounter++
        }
    }

    println("Upload completed! Successfully uploaded: $successCounter Failed to upload: $failCounter")
}