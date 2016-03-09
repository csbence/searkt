package edu.unh.cs.ai.realtimesearch.experiment.result

import com.fasterxml.jackson.databind.annotation.JsonSerialize
import edu.unh.cs.ai.realtimesearch.experiment.configuration.ExperimentData
import java.util.*


/**
 * ExperimentResult is a class to store experiment results.
 *
 * The systemProperties property is initialized at construction time.
 */
@JsonSerialize(`as` = ExperimentData::class)
class ExperimentResult() : ExperimentData() {
    constructor(experimentConfiguration: MutableMap<String, Any?>,
                expandedNodes: Int = 0,
                generatedNodes: Int = 0,
                timeInMillis: Long = 0,
                actions: List<String> = emptyList(),
                pathLength: Double? = null,
                errorMessage: String? = null,
                values: Map<String, Any> = HashMap(),
                timestamp: Long = System.currentTimeMillis(),
                systemProperties: HashMap<String, String> = HashMap()) : this() {

        this.experimentConfiguration = experimentConfiguration
        this.expandedNodes = expandedNodes
        this.generatedNodes = generatedNodes
        this.timeInMillis = timeInMillis
        this.actions = actions
        this.pathLength = pathLength
        this.errorMessage = errorMessage
        this.values = values
        this.timestamp = timestamp

        if (systemProperties.isNotEmpty()) {
            this.systemProperties = systemProperties
        }
    }

    var experimentConfiguration: MutableMap<String, Any?> by valueStore
    var pathLength: Double? by valueStore
    var errorMessage: String? by valueStore
    var expandedNodes: Int by valueStore
    var generatedNodes: Int by valueStore
    var timeInMillis: Long by valueStore
    var actions: List<String> by valueStore
    var values: Map<String, Any> by valueStore
    var timestamp: Long by valueStore
    var systemProperties: MutableMap<String, String> by valueStore

    init {
        // Initialize the system properties
        systemProperties = hashMapOf<String, String>()
        System.getProperties().forEach {
            // MongoDB cannot handle . in keys
            systemProperties.put(it.key.toString().replace('.', '_'), it.value.toString())
        }
    }
}
