package edu.unh.cs.ai.realtimesearch.domain

/**
 * @author Bence Cserna (bence@cserna.net)
 */
interface Domain {
    fun succesors(state: State): List<SuccessorBundle>
    fun heuristic(state: State): Double
    fun distance(state: State): Double
    fun isGoal(state: State): Boolean
}

data class SuccessorBundle(val successorState: State, val action: Action?, val cost: Double)

