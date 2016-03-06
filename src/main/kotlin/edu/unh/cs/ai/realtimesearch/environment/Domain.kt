package edu.unh.cs.ai.realtimesearch.environment

/**
 * A domain is a problem setting that defines, given a state, all possible successor states with their
 * accompanied action and cost
 */
interface Domain<StateType : State<StateType>> {
    fun successors(state: StateType): List<SuccessorBundle<StateType>>
    fun heuristic(state: StateType): Double
    fun distance(state: StateType): Double
    fun actionDuration(action: Action<StateType>)
    fun isGoal(state: StateType): Boolean
    fun print(state: StateType): String
    fun randomState(): StateType
}

data class SuccessorBundle<StateType : State<StateType>>(val state: StateType, val action: Action<StateType>, val actionCost: Double)

