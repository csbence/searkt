package edu.unh.cs.ai.realtimesearch.experiment.result

import com.fasterxml.jackson.databind.annotation.JsonSerialize
import edu.unh.cs.ai.realtimesearch.experiment.configuration.Configurations
import edu.unh.cs.ai.realtimesearch.experiment.configuration.ExperimentData
import edu.unh.cs.ai.realtimesearch.experiment.configuration.realtime.TerminationType
import edu.unh.cs.ai.realtimesearch.util.convertNanoUpDouble
import java.lang.management.ManagementFactory
import java.util.*
import java.util.concurrent.TimeUnit


/**
 * ExperimentResult is a class to store experiment results.
 *
 * The systemProperties property is initialized at construction time.
 */
@JsonSerialize(`as` = ExperimentData::class)
class ExperimentResult(values: MutableMap<String, Any?> = hashMapOf<String, Any?>()) : ExperimentData(values) {
    constructor(experimentConfiguration: Map<String, Any?>,
                errorMessage: String? = null) : this() {
        this.configuration = experimentConfiguration
        this.errorMessage = errorMessage
        this.success = false
    }

    constructor(configuration: Map<String, Any?>,
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
                systemProperties: HashMap<String, Any> = HashMap()) : this() {

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

        if (systemProperties.isNotEmpty()) {
            this.systemProperties = systemProperties
        }
    }

    var configuration: Map<String, Any?> by valueStore
    var pathLength: Long by valueStore
    var errorMessage: String? by valueStore
    var expandedNodes: Int by valueStore
    var generatedNodes: Int by valueStore
    var planningTime: Long by valueStore
    var iterationCount: Long by valueStore
    var actionExecutionTime: Long by valueStore
    var goalAchievementTime: Long by valueStore
    var idlePlanningTime: Long by valueStore
    var actions: List<String> by valueStore
    var timestamp: Long by valueStore
    var success: Boolean by valueStore
    var systemProperties: MutableMap<String, Any> by valueStore

    init {
        // Initialize the system properties
        systemProperties = hashMapOf<String, Any>()

        val runtimeMxBean = ManagementFactory.getRuntimeMXBean()
        val arguments = runtimeMxBean.inputArguments

        systemProperties.put("java_vm_info", System.getProperties()["java.vm.info"].toString())
        systemProperties.put("java_vm_name", System.getProperties()["java.vm.name"].toString())
        systemProperties.put("java_vm_vendor", System.getProperties()["java.vm.vendor"].toString())
        systemProperties.put("java_version", System.getProperties()["java.version"].toString())
        systemProperties.put("java_vm_input_args", arguments)
    }

    override fun toString(): String {
        val builder = StringBuilder("Result:")

        if (errorMessage != null) {
            builder.appendln("Something went wrong: $errorMessage")
        } else {
            val terminationType = TerminationType.valueOf(configuration[Configurations.TERMINATION_TYPE.toString()] as String)
            builder.appendln("Planning time: ${convertNanoUpDouble(planningTime, TimeUnit.MILLISECONDS)} ms")
            builder.appendln("Generated Nodes: $generatedNodes, Expanded Nodes $expandedNodes")
            builder.appendln("Path Length: $pathLength")

            when (terminationType) {
                TerminationType.TIME -> {
                    builder.appendln("Action duration: ${convertNanoUpDouble(configuration["actionDuration"] as Long, TimeUnit.MILLISECONDS)} ms")
                    builder.appendln("Execution time: ${convertNanoUpDouble(actionExecutionTime, TimeUnit.MILLISECONDS)} ms")
                    builder.appendln("Idle planning time: ${convertNanoUpDouble(idlePlanningTime, TimeUnit.MILLISECONDS)} ms")
                    builder.appendln("GAT: ${convertNanoUpDouble(goalAchievementTime, TimeUnit.MILLISECONDS)} ms")
                }
                TerminationType.EXPANSION, TerminationType.UNLIMITED -> {
                    builder.appendln("Action duration: ${configuration["actionDuration"] as Long} expansions")
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
    val builder = StringBuilder("Results: [${this.size}]")
    val successfulExperiments = this.filter { it.errorMessage == null }
    val failedExperiments = this.filter { it.errorMessage != null }
    builder.appendln("Successful: ${successfulExperiments.size} Failed: ${failedExperiments.size}")

    this.forEach {
        val algorithm = it.configuration[Configurations.ALGORITHM_NAME.toString()] as String
        val domain = it.configuration[Configurations.DOMAIN_PATH.toString()] as String
        val GAT = it.valueStore["goalAchievementTime"]

        builder.appendln("Algorithm: $algorithm domain: $domain GAT: $GAT error: ${it.errorMessage ?: "None"}")
    }

    return builder.toString()
}