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
        throw UnsupportedOperationException()
    }

    override fun getState() = currentState

    override fun isGoal(): Boolean {
        throw UnsupportedOperationException()
    }

    override fun reset() {
        throw UnsupportedOperationException()
    }
}