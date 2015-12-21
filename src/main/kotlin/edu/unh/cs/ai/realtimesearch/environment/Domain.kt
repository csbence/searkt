package edu.unh.cs.ai.realtimesearch.environment

/**
 * @author Bence Cserna (bence@cserna.net)
 *
 * A domain is a problem setting that defines, given a state, all possible successor states with their
 * accompanied action and cost
 */
interface Domain<T : State> {
    public fun successors(state: T): List<SuccessorBundle>
    public fun predecessors(state: T): List<SuccessorBundle>
    public fun heuristic(state: T): Double
    public fun distance(state: T): Double
    public fun isGoal(state: T): Boolean
    public fun print(state: T): String
    public fun randomState(): T
}

data class SuccessorBundle(val state: State, val action: Action?, val actionCost: Double)

