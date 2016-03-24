package edu.unh.cs.ai.realtimesearch

import edu.unh.cs.ai.realtimesearch.environment.Domains
import edu.unh.cs.ai.realtimesearch.experiment.configuration.ExperimentData
import edu.unh.cs.ai.realtimesearch.environment.Domains.*
import edu.unh.cs.ai.realtimesearch.experiment.configuration.GeneralExperimentConfiguration
import edu.unh.cs.ai.realtimesearch.planner.Planners
import edu.unh.cs.ai.realtimesearch.planner.Planners.*
import org.springframework.http.HttpEntity
import org.springframework.http.HttpMethod
import org.springframework.http.HttpStatus
import org.springframework.web.client.RestTemplate
import java.util.concurrent.TimeUnit

val terminationType = "time"
val timeLimit = TimeUnit.SECONDS.convert(300, TimeUnit.NANOSECONDS)
val timeBoundTypes = listOf("STATIC", "DYNAMIC")
val actionDurations = listOf(
        TimeUnit.NANOSECONDS.convert(1, TimeUnit.MILLISECONDS),
        TimeUnit.NANOSECONDS.convert(100, TimeUnit.MILLISECONDS),
        TimeUnit.NANOSECONDS.convert(200, TimeUnit.MILLISECONDS),
        TimeUnit.NANOSECONDS.convert(500, TimeUnit.MILLISECONDS),
        TimeUnit.NANOSECONDS.convert(1000, TimeUnit.MILLISECONDS),
        TimeUnit.NANOSECONDS.convert(1500, TimeUnit.MILLISECONDS),
        TimeUnit.NANOSECONDS.convert(2000, TimeUnit.MILLISECONDS),
        TimeUnit.NANOSECONDS.convert(2500, TimeUnit.MILLISECONDS)
)
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

                val realTimePlanners = realTimePlanners(planner)
                for (realTimePlanner in realTimePlanners) {
                    realTimePlanner.putAll(configuration)
                    configurations.add(realTimePlanner)
                }
            }
        }
    }

    for (configuration in configurations) {
        println(configuration)
    }

//    uploadConfigurations(configurations)
}

fun domainConfigurations(domain: Domains): MutableList<MutableMap<String, Any?>> {
    val configurations = mutableListOf<MutableMap<String, Any?>>()

    when (domain) {
        ACROBOT -> {

        }
        GRID_WORLD, VACUUM_WORLD, POINT_ROBOT, POINT_ROBOT_WITH_INERTIA -> {
            GRID_WORLD.javaClass.classLoader.getResource("input/vacuum/dylan")
        }
        RACETRACK -> {

        }
        SLIDING_TILE_PUZZLE -> {

        }
    }

    return configurations
}

fun realTimePlanners(planner: Planners): MutableList<MutableMap<String, Any?>> {
    val configurations = mutableListOf<MutableMap<String, Any?>>()

    when(planner) {
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