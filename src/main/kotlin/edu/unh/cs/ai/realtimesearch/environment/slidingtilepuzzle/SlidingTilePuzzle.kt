package edu.unh.cs.ai.realtimesearch.environment.slidingtilepuzzle

import edu.unh.cs.ai.realtimesearch.environment.Action
import edu.unh.cs.ai.realtimesearch.environment.Domain
import edu.unh.cs.ai.realtimesearch.environment.SuccessorBundle
import edu.unh.cs.ai.realtimesearch.environment.location.Location
import java.lang.Math.abs

class SlidingTilePuzzle(val size: Int) : Domain<SlidingTilePuzzleState> {

    override fun successors(state: SlidingTilePuzzleState): List<SuccessorBundle<SlidingTilePuzzleState>> {
        val successorBundles: MutableList<SuccessorBundle<SlidingTilePuzzleState>> = arrayListOf()

        for (action in SlidingTilePuzzleAction.values()) {
            val successorState = successorState(state, action.getRelativeLocation())

            if (successorState != null) {
                successorBundles.add(SuccessorBundle(successorState, action, 1.0))

            }
        }

        return successorBundles
    }

    private fun successorState(state: SlidingTilePuzzleState, relativeLocation: Location): SlidingTilePuzzleState? {
        val zeroLocation = state.zeroLocation + relativeLocation
        if (zeroLocation.inBounds(size)) {
            val tiles = state.tiles.copy()

            tiles[state.zeroLocation] = tiles[zeroLocation]
            assert(!tiles.tiles.any { it == 0.toByte() })

            tiles[zeroLocation] = 0

            return SlidingTilePuzzleState(zeroLocation, tiles, heuristic(tiles))
        }

        return null
    }

    override fun heuristic(state: SlidingTilePuzzleState): Double {
        val tiles = state.tiles
        return heuristic(tiles)
    }

    fun heuristic(tiles: SlidingTilePuzzleState.Tiles): Double {
        var manhattanSum = 0.0
        var zero: Byte = 0

        for (x in 0..size - 1) {
            for (y in 0..size - 1) {
                val value = tiles[tiles.getIndex(x, y)]
                if (value == zero) continue

                manhattanSum += abs(value / size - y) + abs(value % size - x)
            }
        }

        return manhattanSum
    }

    override fun distance(state: SlidingTilePuzzleState): Double {
        throw UnsupportedOperationException()
    }

    override fun isGoal(state: SlidingTilePuzzleState) = state.heuristic == 0.0

    override fun print(state: SlidingTilePuzzleState): String {
        throw UnsupportedOperationException()
    }

    override fun randomState(): SlidingTilePuzzleState {
        throw UnsupportedOperationException()
    }

    override fun actionDuration(action: Action<SlidingTilePuzzleState>) {
        throw UnsupportedOperationException()
    }
}