package edu.unh.cs.ai.realtimesearch.environment.slidingtilepuzzle

import edu.unh.cs.ai.realtimesearch.environment.Domain
import edu.unh.cs.ai.realtimesearch.environment.SuccessorBundle

/**
 * @author Bence Cserna (bence@cserna.net)
 */
class SlidingTilePuzzle : Domain<SlidingTilePuzzleState> {
    override fun successors(state: SlidingTilePuzzleState): List<SuccessorBundle> {
        throw UnsupportedOperationException()
    }

    override fun predecessors(state: SlidingTilePuzzleState): List<SuccessorBundle> {
        throw UnsupportedOperationException()
    }

    override fun heuristic(state: SlidingTilePuzzleState): Double {
        throw UnsupportedOperationException()
    }

    override fun distance(state: SlidingTilePuzzleState): Double {
        throw UnsupportedOperationException()
    }

    override fun isGoal(state: SlidingTilePuzzleState): Boolean {
        throw UnsupportedOperationException()
    }

    override fun print(state: SlidingTilePuzzleState): String {
        throw UnsupportedOperationException()
    }

    override fun randomState(): SlidingTilePuzzleState {
        throw UnsupportedOperationException()
    }

    data class Location(val x: Int, val y: Int)
}