package edu.unh.cs.ai.realtimesearch.experiment.result

import com.fasterxml.jackson.databind.annotation.JsonSerialize
import edu.unh.cs.ai.realtimesearch.experiment.configuration.ExperimentData
import java.lang.management.ManagementFactory
import java.util.*


/**
 * ExperimentResult is a class to store experiment results.
 *
 * The systemProperties property is initialized at construction time.
 */
@JsonSerialize(`as` = ExperimentData::class)
class ExperimentResult(values: MutableMap<String, Any?> = hashMapOf<String, Any?>()) : ExperimentData(values) {
    constructor(experimentConfiguration: Map<String, Any?>,
                errorMessage: String?) : this() {
        this.experimentConfiguration = experimentConfiguration
        this.errorMessage = errorMessage
        this.success = false
    }

    constructor(experimentConfiguration: Map<String, Any?>,
                expandedNodes: Int,
                generatedNodes: Int,
                planningTime: Long,
                actionExecutionTime: Long,
                goalAchievementTime: Long,
                pathLength: Long,
                actions: List<String>,
                timestamp: Long = System.currentTimeMillis(),
                systemProperties: HashMap<String, Any> = HashMap()) : this() {

        this.experimentConfiguration = experimentConfiguration
        this.expandedNodes = expandedNodes
        this.generatedNodes = generatedNodes
        this.planningTime = planningTime
        this.actionExecutionTime = actionExecutionTime
        this.goalAchievementTime = goalAchievementTime
        this.actions = actions
        this.pathLength = pathLength
        this.timestamp = timestamp
        this.success = true
        this.errorMessage = null

        if (systemProperties.isNotEmpty()) {
            this.systemProperties = systemProperties
        }
    }

    var experimentConfiguration: Map<String, Any?> by valueStore
    var pathLength: Long by valueStore
    var errorMessage: String? by valueStore
    var expandedNodes: Int by valueStore
    var generatedNodes: Int by valueStore
    var planningTime: Long by valueStore
    var actionExecutionTime: Long by valueStore
    var goalAchievementTime: Long by valueStore
    var actions: List<String> by valueStore
    var timestamp: Long by valueStore
    var success: Boolean by valueStore
    var systemProperties: MutableMap<String, Any> by valueStore

    init {
        // Initialize the system properties
        systemProperties = hashMapOf<String, Any>()

        val runtimeMxBean = ManagementFactory.getRuntimeMXBean();
        val arguments = runtimeMxBean.inputArguments;

        systemProperties.put("java_vm_info", System.getProperties()["java.vm.info"].toString())
        systemProperties.put("java_vm_name", System.getProperties()["java.vm.name"].toString())
        systemProperties.put("java_vm_vendor", System.getProperties()["java.vm.vendor"].toString())
        systemProperties.put("java_version", System.getProperties()["java.version"].toString())
        systemProperties.put("java_vm_input_args", arguments)
    }
}
