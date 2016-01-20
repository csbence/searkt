package edu.unh.cs.ai.realtimesearch.experiment

import edu.unh.cs.ai.realtimesearch.environment.Action

data class ExperimentResult(val experimentConfiguration: ExperimentConfiguration?,
                            val expandedNodes: Int = 0,
                            val generatedNodes: Int = 0, val timeInMillis: Long = 0,
                            val actions: List<Action> = emptyList(),
                            val pathLength: Double? = null,
                            val errorMessage: String? = null
)