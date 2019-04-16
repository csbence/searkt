package edu.unh.cs.ai.realtimesearch.planner

import edu.unh.cs.ai.realtimesearch.environment.State
import edu.unh.cs.ai.realtimesearch.experiment.result.ExperimentResult
import edu.unh.cs.ai.realtimesearch.logging.EventLogger

abstract class Planner<StateType: State<StateType>> {
    val eventLogger: EventLogger<StateType> = EventLogger()
    val attributes = mutableMapOf<String, MutableCollection<Int>>()
    val counters = mutableMapOf<String, Int>()

    var generatedNodeCount: Int = 0
    var expandedNodeCount: Int = 0
    var reexpansions: Int = 0

    open fun appendPlannerSpecificResults(results: ExperimentResult) {}

    fun appendAttribute(key: String, value: Int) {
        val collection = attributes[key] ?: mutableListOf()
        collection.add(value)

        attributes[key] = collection
    }

    fun incrementCounter(key: String, increment: Int = 1) {
        val value = counters[key] ?: 0
        counters[key] = value + increment
    }
}