package edu.unh.cs.ai.realtimesearch.environment

/**
 * A domain is a problem setting that defines, given a state, all possible successor states with their
 * accompanied action and cost
 */
interface Domain<State> {
    fun successors(state: State): List<SuccessorBundle<State>>
    fun predecessors(state: State): List<SuccessorBundle<State>> = TODO()
    fun heuristic(state: State): Double
    fun heuristic(startState: State, endState: State): Double = TODO()
    fun distance(state: State): Double
    fun isGoal(state: State): Boolean
    fun print(state: State): String
    fun randomState(): State = TODO()
    fun getGoal(): List<State> = TODO()

    fun transition(sourceState: State, action: Action): State? {
        val successorBundles = successors(sourceState)
        // get the state from the successors by filtering on action
        return successorBundles.firstOrNull { it.action == action }?.state
    }
}

data class SuccessorBundle<out State>(val state: State, val action: Action, val actionCost: Long)

