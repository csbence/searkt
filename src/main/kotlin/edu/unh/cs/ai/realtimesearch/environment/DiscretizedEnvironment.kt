package edu.unh.cs.ai.realtimesearch.environment

import edu.unh.cs.ai.realtimesearch.logging.trace
import org.slf4j.LoggerFactory

/**
 * Wrapper around a discretized domain.
 *
 * @author Mike Bogochow (mgp36@unh.edu)
 */
class DiscretizedEnvironment<StateType : DiscretizableState<StateType>, DomainType : Domain<DiscretizedState<StateType>>>(
        private val domain: DomainType, private val initialState: DiscretizedState<StateType>? = null) :
        Domain<DiscretizedState<StateType>> {
    private val logger = LoggerFactory.getLogger(DiscretizedEnvironment::class.java)
    private var currentState: DiscretizedState<StateType> = initialState ?: domain.randomState()

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
    override fun getState(): DiscretizedState<StateType> = currentState

    override fun getGoal(): List<DiscretizedState<StateType>> = domain.getGoal()

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
        currentState = initialState ?: domain.randomState() // TODO necessary to copy?
    }
}