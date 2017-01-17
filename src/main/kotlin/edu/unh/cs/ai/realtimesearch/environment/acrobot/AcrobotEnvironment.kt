package edu.unh.cs.ai.realtimesearch.environment.acrobot

import edu.unh.cs.ai.realtimesearch.environment.Action
import edu.unh.cs.ai.realtimesearch.environment.Domain
import edu.unh.cs.ai.realtimesearch.logging.trace
import org.slf4j.LoggerFactory

/**
 * The Acrobot environment.  Contains the domain and a current state.
 *
 * @author Mike Bogochow (mgp36@unh.edu)
 *
 * @param domain is the Acrobot domain
 * @param initialState is the initial state.  Will use default initial state if not provided
 */
class AcrobotEnvironment(private val domain: Acrobot, private val initialState: AcrobotState? = null) : Domain<AcrobotState> {

    private val logger = LoggerFactory.getLogger(AcrobotEnvironment::class.java)
    private var currentState: AcrobotState = initialState ?: AcrobotState.Companion.defaultInitialState

    /**
     * Applies the action to the environment
     */
    override fun step(action: Action) {
        var successorBundle = domain.successors(currentState)

        // get the state from the successors by filtering on action
        currentState = successorBundle.first { it.action == action }.state
        logger.trace { "Action $action leads to state $currentState" }
    }

    /**
     * Returns current state of the domain
     */
    override fun getState(): AcrobotState = currentState

    override fun getGoal() = domain.getGoal()

    /**
     * Returns wHether current state is the goal
     */
    override fun isGoal(): Boolean {
        val goal = domain.isGoal(currentState)

        logger.trace { "State $currentState is ${if (goal) "" else "not"} a goal" }

        return goal
    }

    /**
     * Resets teh current state to either initial (if given at construction), or a random state
     */
    override fun reset() {
        currentState = initialState?.copy() ?: AcrobotState.defaultInitialState // TODO necessary to copy?
    }
}