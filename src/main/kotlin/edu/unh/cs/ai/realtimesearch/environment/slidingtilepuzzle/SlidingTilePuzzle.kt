package edu.unh.cs.ai.realtimesearch.environment.slidingtilepuzzle

import edu.unh.cs.ai.realtimesearch.environment.Domain
import edu.unh.cs.ai.realtimesearch.environment.SuccessorBundle
import java.lang.Math.abs

class SlidingTilePuzzle(val size: Int, val actionDuration: Double = 10.0) : Domain<SlidingTilePuzzleState> {
    override fun successors(state: SlidingTilePuzzleState): List<SuccessorBundle<SlidingTilePuzzleState>> {
        val successorBundles: MutableList<SuccessorBundle<SlidingTilePuzzleState>> = arrayListOf()

        for (action in SlidingTilePuzzleAction.values()) {
            val successorState = successorState(state, action.relativeX, action.relativeY)

            if (successorState != null) {
                successorBundles.add(SuccessorBundle(successorState, action, actionDuration))
            }
        }

        return successorBundles
    }

    private fun successorState(state: SlidingTilePuzzleState, relativeX: Int, relativeY: Int): SlidingTilePuzzleState? {
        val zeroX = state.zeroX + relativeX
        val zeroY = state.zeroY + relativeY

        if (zeroX >= 0 && zeroY >= 0 && zeroX < size && zeroY < size) {
            val tiles = state.tiles.copy()

            tiles.set(state.zeroX, state.zeroY, tiles.get(zeroX, zeroY))
            assert(!tiles.tiles.any { it == 0.toByte() })
            tiles.set(zeroX, zeroY, 0)

            return SlidingTilePuzzleState(zeroX, zeroY, tiles, heuristic(tiles))
        }

        return null
    }

    override fun heuristic(state: SlidingTilePuzzleState): Double {
        return state.heuristic
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

    override fun distance(state: SlidingTilePuzzleState) = state.heuristic

    override fun isGoal(state: SlidingTilePuzzleState) = state.heuristic == 0.0

    override fun print(state: SlidingTilePuzzleState): String {
        throw UnsupportedOperationException()
    }

    override fun randomState(): SlidingTilePuzzleState {
        throw UnsupportedOperationException()
    }
}