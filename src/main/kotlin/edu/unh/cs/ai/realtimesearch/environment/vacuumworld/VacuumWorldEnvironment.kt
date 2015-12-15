package edu.unh.cs.ai.realtimesearch.environment.vacuumworld

import edu.unh.cs.ai.realtimesearch.environment.Action
import edu.unh.cs.ai.realtimesearch.environment.Environment
import edu.unh.cs.ai.realtimesearch.environment.State
import org.slf4j.LoggerFactory

/**
 * The vacuumworld environment. Contains the domain and a current state
 *
 * @param domainDynamics is the vacuumworld domain
 * @param currentState is the initial state
 */
class VacuumWorldEnvironment(private val domainDynamics: VacuumWorld, private var currentState: State) : Environment {
    private val logger = LoggerFactory.getLogger("VacuumWorldEnvironment")

    /**
     * Applies the action to the environment
     *
     * @param action to apply
     */
    override fun step(action: Action) {
        // contains successor per possible action
        val successorBundles = domainDynamics.successors(currentState)

        // get the state from the successors by filtering on action
        currentState = successorBundles.first { it.action == action }.state
        logger.trace("Action " + action.toString() + " leads to state " + currentState.toString())
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
    override fun isGoal(): Boolean {
        val goal = domainDynamics.isGoal(currentState)

        logger.trace("State $currentState is ${if (goal) "" else "not"} a goal")

        return goal
    }
}