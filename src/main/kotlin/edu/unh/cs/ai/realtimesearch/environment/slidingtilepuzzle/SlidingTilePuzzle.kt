package edu.unh.cs.ai.realtimesearch.environment.slidingtilepuzzle

import edu.unh.cs.ai.realtimesearch.environment.Domain
import edu.unh.cs.ai.realtimesearch.environment.State
import edu.unh.cs.ai.realtimesearch.environment.SuccessorBundle

/**
 * @author Bence Cserna (bence@cserna.net)
 */
class SlidingTilePuzzle : Domain {
    override fun successors(state: State): List<SuccessorBundle> {
        throw UnsupportedOperationException()
    }

    override fun predecessors(state: State): List<SuccessorBundle> {
        throw UnsupportedOperationException()
    }

    override fun heuristic(state: State): Double {
        throw UnsupportedOperationException()
    }

    override fun distance(state: State): Double {
        throw UnsupportedOperationException()
    }

    override fun isGoal(state: State): Boolean {
        throw UnsupportedOperationException()
    }

    override fun print(state: State): String {
        throw UnsupportedOperationException()
    }

    override fun randomState(): State {
        throw UnsupportedOperationException()
    }

    data class Location(val x: Int, val y: Int)
}