package edu.unh.cs.ai.realtimesearch.environment.racetrack

import edu.unh.cs.ai.realtimesearch.environment.State
import edu.unh.cs.ai.realtimesearch.environment.location.Location

/**
 * TODO: nyi
 */
data class RaceTrackState(val agentLocation: Location) : State<RaceTrackState> {

    override fun copy(): RaceTrackState {
        throw UnsupportedOperationException()
    }
}

