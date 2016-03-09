package edu.unh.cs.ai.realtimesearch.experiment.result

import com.fasterxml.jackson.databind.annotation.JsonSerialize
import edu.unh.cs.ai.realtimesearch.experiment.configuration.ExperimentData
import groovy.json.JsonSlurper
import java.io.InputStream
import java.math.BigDecimal
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

    companion object {
        fun fromStream(stream: InputStream): ExperimentResult = fromMap(JsonSlurper().parse(stream) as Map<*,*>)
        fun fromString(string: String): ExperimentResult = fromMap(JsonSlurper().parseText(string) as Map<*,*>)

        fun fromMap(map: Map<*,*>): ExperimentResult {
            val rawExperimentConfiguration = map["experimentConfiguration"]
            val experimentConfiguration = mutableMapOf<String,Any?>()
            if (rawExperimentConfiguration != null) {
                for ((key, value) in rawExperimentConfiguration as Map<*,*>) {
                    experimentConfiguration.put(key.toString(), value as Any)
                }
            }

            val actions = map["actions"] as List<*>
            val actionList: MutableList<String> = mutableListOf()
            for (action in actions) {
                actionList.add(action as String)
            }

            val rawSystemProperties = map["systemProperties"]
            val systemProperties = HashMap<String, String>()
            if (rawSystemProperties != null) {
                for ((key, value) in rawSystemProperties as Map<*,*>) {
                    systemProperties.put(key.toString(), value.toString())
                }
            }

            val rawValues = map["values"]
            val rawTimestamp = map["timestamp"]

            return ExperimentResult(
                    experimentConfiguration,
                    map["expandedNodes"] as Int,
                    map["generatedNodes"] as Int,
                    (map["timeInMillis"] as Int).toLong(),
                    actionList,
                    pathLength = (map["pathLength"] as BigDecimal?)?.toDouble(),
                    errorMessage = map["errorMessage"] as String?,
                    values = if (rawValues != null) rawValues as Map<String, Any> else HashMap(),
                    timestamp = if (rawTimestamp != null) (rawTimestamp as Int).toLong() else System.currentTimeMillis(),
                    systemProperties = systemProperties)
        }
    }

    init {
        // Initialize the system properties
        systemProperties = hashMapOf<String, String>()
        System.getProperties().forEach {
            systemProperties.put(it.key.toString(), it.value.toString())
        }
    }
}
