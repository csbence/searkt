package edu.unh.cs.ai.realtimesearch.environment.racetrack

import edu.unh.cs.ai.realtimesearch.environment.Action
import edu.unh.cs.ai.realtimesearch.environment.Environment
import org.slf4j.LoggerFactory

/**
 * TODO: nyi
 */
class RaceTrackEnvironment(private val domain: RaceTrack, private val initialState: RaceTrackState) : Environment<RaceTrackState> {
    private val logger = LoggerFactory.getLogger(RaceTrackEnvironment::class.java)
    private var currentState = initialState

    override fun step(action: Action) {
        val successorBundles = domain.successors(currentState)

        // get the state from the successors by filtering on action
        currentState = successorBundles.first { it.action == action }.state
    }

    override fun getState() = currentState
    override fun getGoal() = domain.getGoal()

    override fun isGoal(): Boolean {
        val goal = domain.isGoal(currentState)

        //logger.trace { "State $currentState is ${if (goal) "" else "not"} a goal" }

        return goal
    }

    override fun reset() {
        currentState = initialState.copy()
    }
}