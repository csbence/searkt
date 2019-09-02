package edu.unh.cs.searkt.experiment.result

import edu.unh.cs.searkt.experiment.configuration.ExperimentConfiguration
import edu.unh.cs.searkt.experiment.configuration.SimpleSerializer
import edu.unh.cs.searkt.experiment.configuration.realtime.TerminationType
import edu.unh.cs.searkt.util.convertNanoUpDouble
import kotlinx.serialization.ContextualSerialization
import kotlinx.serialization.ImplicitReflectionSerializer
import kotlinx.serialization.Optional
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import java.util.*
import java.util.concurrent.TimeUnit
import kotlin.math.roundToInt

/**
 * ExperimentResult is a class to store experiment results.
 *
 * The systemProperties property is initialized at construction time.
 */
@Serializable
class ExperimentResult {
    constructor(experimentConfiguration: ExperimentConfiguration,
                errorMessage: String? = null) {
        this.configuration = experimentConfiguration
        this.errorMessage = errorMessage
        this.success = false
    }

    constructor(configuration: ExperimentConfiguration,
                expandedNodes: Int,
                generatedNodes: Int,
                planningTime: Long,
                iterationCount: Long,
                actionExecutionTime: Long,
                goalAchievementTime: Long,
                idlePlanningTime: Long,
                pathLength: Long,
                actions: List<String>,
                timestamp: Long = System.currentTimeMillis(),
                systemProperties: HashMap<String, String> = HashMap(),
                experimentRunTime: Double,
                iterationCpuTimeList: List<Long>) {

        this.configuration = configuration
        this.expandedNodes = expandedNodes
        this.generatedNodes = generatedNodes
        this.planningTime = planningTime
        this.iterationCount = iterationCount
        this.actionExecutionTime = actionExecutionTime
        this.goalAchievementTime = goalAchievementTime
        this.idlePlanningTime = idlePlanningTime
        this.actions = actions
        this.pathLength = pathLength
        this.timestamp = timestamp
        this.success = true
        this.errorMessage = null
        this.experimentRunTime = experimentRunTime
        this.iterationCpuTimeList = iterationCpuTimeList

        if (systemProperties.isNotEmpty()) {
            this.systemProperties = systemProperties
        }
    }

    var configuration: ExperimentConfiguration
    var pathLength: Long = 0
    var errorMessage: String? = null
    var expandedNodes: Int = 0
    var generatedNodes: Int = 0
    var planningTime: Long = 0
    var iterationCount: Long = 0
    var actionExecutionTime: Long = 0
    var goalAchievementTime: Long = 0
    var idlePlanningTime: Long = 0
    var actions: List<String> = listOf()
    var timestamp: Long = 0
    var success: Boolean = false
    var systemProperties: MutableMap<String, String>
    var experimentRunTime: Double = 0.0
    var iterationCpuTimeList: List<Long> = listOf()

    @ImplicitReflectionSerializer
    @Serializable(with = SimpleSerializer::class)
    var attributes = mutableMapOf<String, Long>()

    @Optional
    var reexpansions: Int = 0

    @Optional
    var errorDetails: String = ""

    // Racetrack domain
    @Optional
    var averageVelocity: Double = 0.0
    @Optional
    var numberOfCrashes: Int = 0

    // Safety stats tracking
    @Serializable
    data class DepthRankPair(val depth: Int, val rankOnOpen: Int)

    //Comprehensive Envelope stats
    @Optional
    var backupCount: Int = 0


    init {
        // Initialize the system properties
        systemProperties = hashMapOf()

//        val runtimeMxBean = ManagementFactory.getRuntimeMXBean()
//        val arguments = runtimeMxBean.inputArguments
//
//        systemProperties.put("java_vm_info", System.getProperties()["java.vm.info"].toString())
//        systemProperties.put("java_vm_name", System.getProperties()["java.vm.name"].toString())
//        systemProperties.put("java_vm_vendor", System.getProperties()["java.vm.vendor"].toString())
//        systemProperties.put("java_version", System.getProperties()["java.version"].toString())
//        systemProperties.put("java_vm_input_args", arguments.toString())
    }

    override fun toString(): String {
        val builder = StringBuilder("Result:")

        if (errorMessage != null) {
            builder.appendln("Something went wrong: $errorMessage")
        } else {
            builder.appendln("Planning time: ${convertNanoUpDouble(planningTime, TimeUnit.MILLISECONDS)} ms")
            builder.appendln("Generated Nodes: $generatedNodes, Expanded Nodes $expandedNodes")
            builder.appendln("Path Length: $pathLength")

            when (configuration.terminationType) {
                TerminationType.TIME -> {
                    builder.appendln("Action duration: ${convertNanoUpDouble(configuration.actionDuration, TimeUnit.MILLISECONDS)} ms")
                    builder.appendln("Execution time: ${convertNanoUpDouble(actionExecutionTime, TimeUnit.MILLISECONDS)} ms")
                    builder.appendln("Idle planning time: ${convertNanoUpDouble(idlePlanningTime, TimeUnit.MILLISECONDS)} ms")
                    builder.appendln("GAT: ${convertNanoUpDouble(goalAchievementTime, TimeUnit.MILLISECONDS)} ms")
                }
                TerminationType.EXPANSION, TerminationType.UNLIMITED -> {
                    builder.appendln("Action duration: ${configuration.actionDuration} expansions")
                    builder.appendln("Execution time: ${actionExecutionTime} expansions")
                    builder.appendln("Idle planning time: ${idlePlanningTime} expansions")
                    builder.appendln("GAT: $goalAchievementTime expansions")
                }
            }
        }
        return builder.toString()
    }
}

fun Collection<ExperimentResult>.summary(): String {
    val algorithmNameLength = this.map { it.configuration.algorithmName }.maxBy { it.length }!!.length
    val fieldLength = this.map { (it.expandedNodes / 1000).toString() }.maxBy { it.length }!!.length + 1

    this.groupBy { it.configuration.domainName }.forEach { (domainName, domainGroup) ->
        // Print headers
        println("Domain: $domainName")

        val transforms = listOf(
                "Expansions" to { result: ExperimentResult -> result.expandedNodes.toDouble()}
                , "Re-expansions" to { result: ExperimentResult -> result.reexpansions.toDouble()}
                , "Planning time (ms)" to { result: ExperimentResult -> result.planningTime.toDouble() / 1000000}
                , "Path Length (Steps)" to { result: ExperimentResult -> result.pathLength.toDouble()}
        )
        transforms.forEach { (name, transform) ->
            println(name)
            print("Weight".padStart(algorithmNameLength) + "|")
            this.map { it.configuration.weight }.distinct().forEach { print(it.toString().padStart(fieldLength) + " |") }
            println()


            domainGroup.groupBy { it.configuration.algorithmName }.forEach { (algorithmName, algorithmGroup) ->
                print(algorithmName.padStart(algorithmNameLength) + "|")

                algorithmGroup.sortedBy { it.configuration.weight }.groupBy { it.configuration.weight!! }.forEach { (weight, weightGroup) ->
                    val avgValue = weightGroup.map(transform).average()
                    print(avgValue.roundToInt().toString().padStart(fieldLength) + " |")
                }

                println()
            }

            println()
        }
    }

    println()

    val builder = StringBuilder("Results: [${this.size}]")
    val successfulExperiments = this.filter { it.errorMessage == null }
    val failedExperiments = this.filter { it.errorMessage != null }
    builder.appendln("Successful: ${successfulExperiments.size} Failed: ${failedExperiments.size}")

//    this.forEach {
//        val algorithm = it.configuration.algorithmName
//        val domain = it.configuration.domainPath
//        val GAT = it.goalAchievementTime
//
//        builder.appendln(
//                " GAT: $GAT" +
//                        " path:${it.pathLength}" +
//                        " dur:${it.configuration.actionDuration}" +
//                        " algorithm: $algorithm" +
//                        " domain: $domain" +
//                        " error: ${it.errorMessage ?: "None"}")
//    }

    return builder.toString()
}