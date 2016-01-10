package edu.unh.cs.ai.realtimesearch.environment.slidingtilepuzzle

import edu.unh.cs.ai.realtimesearch.environment.Domain
import edu.unh.cs.ai.realtimesearch.environment.State
import edu.unh.cs.ai.realtimesearch.environment.SuccessorBundle
import java.lang.Math.abs

/**
 * @author Bence Cserna (bence@cserna.net)
 */
class SlidingTilePuzzle(val size: Int) : Domain {
    override fun successors(state: State): List<SuccessorBundle> = state.cast {
        val successorBundles: MutableList<SuccessorBundle> = arrayListOf()

        for (action in SlidingTilePuzzleAction.values) {
            val successorState = successorState(it, action.getRelativeLocation())

            if (successorState != null) {
                successorBundles.add(SuccessorBundle(successorState, action, 1.0))

            }
        }

        return successorBundles
    }

    private fun successorState(state: SlidingTilePuzzleState, relativeLocation: SlidingTilePuzzleState.Location): SlidingTilePuzzleState? {
        val zeroLocation = state.zeroLocation + relativeLocation
        if (zeroLocation.inBounds(size)) {
            val tiles: Array<ByteArray> = state.copyTiles()

            tiles[state.zeroLocation] = tiles[zeroLocation]
            tiles[zeroLocation] = 0.toByte()

            return SlidingTilePuzzleState(zeroLocation, tiles, heuristic(tiles))
        }

        return null
    }

    override fun predecessors(state: State) = successors(state)

    override fun heuristic(state: State): Double = state.cast {
        val tiles = it.tiles
        return heuristic(tiles)
    }

    public inline fun <R> State.cast(f: (SlidingTilePuzzleState) -> R): R = f(this as SlidingTilePuzzleState)

    fun heuristic(tiles: Array<ByteArray>): Double {
        var manhattanSum = 0.0
        var zero: Byte = 0

        for (x in 0..size - 1) {
            for (y in 0..size - 1) {
                val value = tiles[x][y]
                if (value == zero) continue

                manhattanSum += abs(value / size - x) + abs(value % size - y)
            }
        }

        return manhattanSum
    }

    override fun distance(state: State): Double {
        throw UnsupportedOperationException()
    }

    override fun isGoal(state: State) = state.cast { it.heuristic == 0.0 }

    override fun print(state: State): String {
        throw UnsupportedOperationException()
    }

    override fun randomState(): State {
        throw UnsupportedOperationException()
    }


}