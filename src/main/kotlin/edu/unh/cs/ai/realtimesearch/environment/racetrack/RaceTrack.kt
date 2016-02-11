package edu.unh.cs.ai.realtimesearch.environment.racetrack

import edu.unh.cs.ai.realtimesearch.environment.Domain
import edu.unh.cs.ai.realtimesearch.environment.SuccessorBundle
import org.slf4j.LoggerFactory

/**
 * TODO: nyi
 */
class RaceTrack() : Domain<RaceTrackState> {
    private val logger = LoggerFactory.getLogger(RaceTrack::class.java)

    override fun successors(state: RaceTrackState): List<SuccessorBundle<RaceTrackState>> {
        throw UnsupportedOperationException()
    }

    override fun heuristic(state: RaceTrackState): Double {
        throw UnsupportedOperationException()
    }

    override fun distance(state: RaceTrackState): Double {
        throw UnsupportedOperationException()
    }

    override fun isGoal(state: RaceTrackState): Boolean {
        throw UnsupportedOperationException()
    }

    override fun print(state: RaceTrackState): String {
        throw UnsupportedOperationException()
    }

    override fun randomState(): RaceTrackState {
        throw UnsupportedOperationException()
    }

}

