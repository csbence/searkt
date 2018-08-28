package edu.unh.cs.ai.realtimesearch.environment

import edu.unh.cs.ai.realtimesearch.experiment.result.ExperimentResult

/**
 * A domain is a definition of a problem setting. It is defined by the state type and the possible state transitions.
 */
interface Domain<State> {

    /**
     * Generate all possible successors of a given state.
     * @param state Source state generate the successors from.
     * @return List of successor bundles that contains that successor states, actions, and the actions cost that led to
     * the successor state.
     */
    fun successors(state: State): List<SuccessorBundle<State>>

    /**
     * Heuristic value of a given state.
     *
     * @return A lower bound on the cost-to-goal.
     */
    fun heuristic(state: State): Double

    /**
     *  Distance to the goal from the given state
     *  @return A lower bound on the number of steps to the goal.
     */
    fun distance(state: State): Double

    /**
     * Goal check.
     *
     * @return True if the given state is a goal state, else false.
     */
    fun isGoal(state: State): Boolean

    /**
     * @return all possible goal states.
     */
    fun getGoals(): List<State> = TODO()


    /**
     * @param sourceState is the start state of the transition.
     * @param action is an action to be applied on the sourceState.
     * @return the successor state from a state and a given action when the action is applicable, else null.
     */
    fun transition(sourceState: State, action: Action): State? {
        val successorBundles = successors(sourceState)
        // get the state from the successors by filtering on action
        return successorBundles.firstOrNull { it.action == action }?.state
    }

    /**
     * @param sourceState is the start state of the transition.
     * @param targetState is the end state of the transition.
     * @return the action that leads to the targetState when applied from the sourceState if exists, else null.
     */
    fun transition(sourceState: State, targetState: State): Pair<Action, Long>? {
        val successorBundles = successors(sourceState)

        val successorBundle = successorBundles.firstOrNull { it.state == targetState }

        successorBundle ?: return null
        return successorBundle.action to successorBundle.actionCost
    }

    /**
     * Convert a state to String.
     * It is practical when the state is only meaningful in the context of the domain.
     *
     * @return String representation of the given state
     */
    fun print(state: State): String = state.toString()

    /**
     * Predecessor function that would provide the possible predecessors of a given state.
     *
     * This function is only required by some of the anytime search algorithms. It is not mandatory to override it.
     */
    fun predecessors(state: State): List<SuccessorBundle<State>> = TODO()

    /**
     * Heuristic value of a given state.
     *
     * This function is only required by some of the anytime search algorithms. It is not mandatory to override it.
     *
     * @return A lower bound on the cost-to-goal.
     */
    fun heuristic(startState: State, endState: State): Double = TODO()

    /**
     * @return true if the state or any of the descendant states is safe, else false.
     */
    fun isSafe(state: State): Boolean = TODO()

    /**
     * Estimate for the number of hops to the closest safe state.
     *
     * @return heuristic lower bound on distance and an extra value for tie breaking.
     */
    fun safeDistance(state: State): Pair<Int, Int> = TODO()

    /**
     * Get an identity action if available. The identity action applied on a state leads to the same state if available.
     *
     * @return identity action for a given state if available, else null.
     */
    fun getIdentityAction(state: State): SuccessorBundle<State>? = null

    /**
     * Returns a randomized start state for the domain where the seed is used for the randomization.
     */
    fun randomizedStartState(state: State, seed: Long): State = TODO("This function is not implemented for the domain")

    fun appendDomainSpecificResults(results: ExperimentResult) {}
}

data class SuccessorBundle<out State>(val state: State, val action: Action, val actionCost: Double)

