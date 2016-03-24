package edu.unh.cs.ai.realtimesearch.environment

/**
 * A domain is a problem setting that defines, given a state, all possible successor states with their
 * accompanied action and cost
 */
interface Domain<State> {
    fun successors(state: State): List<SuccessorBundle<State>>
    //    public fun predecessors(state: State): List<SuccessorBundle<State>>
    fun heuristic(state: State): Double
    fun distance(state: State): Double
    fun isGoal(state: State): Boolean
    fun print(state: State): String
    fun randomState(): State
}

data class SuccessorBundle<out State>(val state: State, val action: Action, val actionCost: Long)

