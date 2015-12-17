package edu.unh.cs.ai.realtimesearch.experiment

import edu.unh.cs.ai.realtimesearch.environment.Action

/**
 * @author Bence Cserna (bence@cserna.net)
 */
data class ExperimentResult(val expandedNodes: Int, val generatedNodes: Int, val timeInMillis: Long, val actions: List<Action>, val pathLength: Double? = null)