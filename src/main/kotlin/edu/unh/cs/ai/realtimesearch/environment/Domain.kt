package edu.unh.cs.ai.realtimesearch.environment

/**
 * @author Bence Cserna (bence@cserna.net)
 *
 * A domain is a problem setting that defines, given a state, all possible successor states with their
 * accompanied action and cost
 */
interface Domain<State> {
    public fun successors(state: State): List<SuccessorBundle<State>>
    public fun predecessors(state: State): List<SuccessorBundle<State>>
    public fun heuristic(state: State): Double
    public fun distance(state: State): Double
    public fun isGoal(state: State): Boolean
    public fun print(state: State): String
    public fun randomState(): State
}

data class SuccessorBundle<State>(val state: State, val action: Action?, val actionCost: Double)

