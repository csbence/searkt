package edu.unh.cs.ai.realtimesearch.environment.slidingtilepuzzle

import edu.unh.cs.ai.realtimesearch.environment.State

/**
 * @author Bence Cserna (bence@cserna.net)
 */
class SlidingTilePuzzleState(val location: SlidingTilePuzzle.Location, val tiles : Array<Array<Char>>) : State {
    override fun copy(): State {
        throw UnsupportedOperationException()
    }
}
