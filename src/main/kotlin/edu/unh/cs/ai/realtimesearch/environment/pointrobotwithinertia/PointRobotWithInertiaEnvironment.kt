package edu.unh.cs.ai.realtimesearch.environment.pointrobotwithinertia

import edu.unh.cs.ai.realtimesearch.environment.Action
import edu.unh.cs.ai.realtimesearch.environment.Environment
import edu.unh.cs.ai.realtimesearch.logging.trace
import org.slf4j.LoggerFactory

/**
 * The SlidingTilePuzzle environment. Contains the domain and a current state.
 */
class PointRobotWithInertiaEnvironment(private val domain: PointRobotWithInertia, private val initialState: PointRobotWithInertiaState) : Environment<PointRobotWithInertiaState> {

    private val logger = LoggerFactory.getLogger(PointRobotWithInertiaEnvironment::class.java)
    private var currentState = initialState

    override fun step(action: Action) {
        val successorBundles = domain.successors(currentState)

        // get the state from the successors by filtering on action
        currentState = successorBundles.first { it.action == action }.state
        logger.trace { "Action $action leads to state $currentState" }
    }

    /**
     * Returns current state of the world
     */
    override fun getState() = currentState

    /**
     * Returns whether current state is the goal
     */
    override fun isGoal(): Boolean {
        val goal = domain.isGoal(currentState)

        logger.trace { "State $currentState is ${if (goal) "" else "not"} a goal" }

        return goal
    }

    /**
     * Resets the current state to either initial (if given at construction), or a random state
     */
    override fun reset() {
        currentState = initialState?.copy() ?: domain.randomState()
    }
}