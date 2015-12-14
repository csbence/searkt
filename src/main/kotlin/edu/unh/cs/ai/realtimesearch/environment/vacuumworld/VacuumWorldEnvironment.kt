package edu.unh.cs.ai.realtimesearch.environment.vacuumworld

import edu.unh.cs.ai.realtimesearch.environment.Action
import edu.unh.cs.ai.realtimesearch.environment.Environment
import edu.unh.cs.ai.realtimesearch.environment.State

/**
 * The vacuumworld environment. Contains the domain and a current state
 *
 * @param simulator is the vacuumworld domain
 * @param currentState is the initial state
 */
class VacuumWorldEnvironment(private val simulator: VacuumWorld, private var currentState: State) : Environment {

    /**
     * Applies the action to the environment
     *
     * @param action to apply
     */
    override fun step(action: Action) {
        // contains successor per possible action
        val successorBundles = simulator.successors(currentState)

        // get the state from the successors by filtering on action
        currentState = successorBundles.first { it.action == action }.successorState
    }

    /**
     * Returns current state of the world
     *
     * @return current state
     */
    override fun getState() = currentState

    /**
     * Returns whether current state is the goal
     *
     * @return true if currentState is goal
     */
    override fun isGoal() = simulator.isGoal(currentState)
}