package edu.unh.cs.ai.realtimesearch.environment.gridworld

import edu.unh.cs.ai.realtimesearch.environment.Action
import edu.unh.cs.ai.realtimesearch.environment.Environment
import org.slf4j.LoggerFactory

/**
 * The SlidingTilePuzzle environment. Contains the domain and a current state.
 */
class GridWorldEnvironment(private val domain: GridWorld, private val initialState: GridWorldState) : Environment<GridWorldState> {

    private val logger = LoggerFactory.getLogger(GridWorldEnvironment::class.java)
    private var currentState = initialState

    override fun step(action: Action) {
        val successorBundles = domain.successors(currentState)

        // get the state from the successors by filtering on action
        currentState = successorBundles.first { it.action == action }.state
        logger.trace("Action " + action.toString() + " leads to state " + currentState.toString())
    }

    override fun getState() = currentState

    override fun isGoal(): Boolean {
        val goal = domain.isGoal(currentState)
        logger.trace("State $currentState is ${if (goal) "" else "not"} a goal")

        return goal
    }

    override fun reset() {
        currentState = initialState
    }

}