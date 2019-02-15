package edu.unh.cs.ai.realtimesearch.planner

import edu.unh.cs.ai.realtimesearch.environment.State
import edu.unh.cs.ai.realtimesearch.experiment.result.ExperimentResult
import edu.unh.cs.ai.realtimesearch.logging.EventLogger

abstract class Planner<StateType: State<StateType>> {
    val eventLogger: EventLogger<StateType> = EventLogger()

    var generatedNodeCount: Int = 0
    var expandedNodeCount: Int = 0
    var reexpansions: Int = 0

    open fun appendPlannerSpecificResults(results: ExperimentResult) {}
}