package edu.unh.cs.searkt.planner

import edu.unh.cs.searkt.environment.State
import edu.unh.cs.searkt.experiment.result.ExperimentResult
import edu.unh.cs.searkt.logging.EventLogger

abstract class Planner<StateType : State<StateType>> {
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