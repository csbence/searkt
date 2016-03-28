package edu.unh.cs.ai.realtimesearch.environment.slidingtilepuzzle

import edu.unh.cs.ai.realtimesearch.environment.Domain
import edu.unh.cs.ai.realtimesearch.environment.SuccessorBundle
import edu.unh.cs.ai.realtimesearch.environment.vacuumworld.VacuumWorldState
import java.lang.Math.abs

class SlidingTilePuzzle(val size: Int, val actionDuration: Long) : Domain<SlidingTilePuzzle4State> {
    override fun successors(state: SlidingTilePuzzle4State): List<SuccessorBundle<SlidingTilePuzzle4State>> {
        val successorBundles: MutableList<SuccessorBundle<SlidingTilePuzzle4State>> = arrayListOf()

        for (action in SlidingTilePuzzleAction.values()) {
            val successorState = successorState(state, action.relativeX, action.relativeY)

            if (successorState != null) {
                successorBundles.add(SuccessorBundle(successorState, action, actionDuration))
            }
        }

        return successorBundles
    }

    private fun successorState(state: SlidingTilePuzzle4State, relativeX: Int, relativeY: Int): SlidingTilePuzzle4State? {
        val newZeroIndex = state.zeroIndex + state.getIndex(relativeX, relativeY)
        val savedTiles = state.tiles

        if (newZeroIndex >= 0 && newZeroIndex < size * size) {
            state[state.zeroIndex] = state[newZeroIndex]
            state[newZeroIndex] = 0

            val modifiedTiles = state.tiles
            val heuristic = initialHeuristic(state)

            state.tiles = savedTiles

            return SlidingTilePuzzle4State(newZeroIndex, modifiedTiles, heuristic)
        }

        return null
    }

    override fun heuristic(state: SlidingTilePuzzle4State): Double {
        return state.heuristic * actionDuration
    }

    override fun heuristic(startState: SlidingTilePuzzle4State, endState: SlidingTilePuzzle4State): Double {
        TODO("add new heuristic between two points for the sliding tile puzzle")
    }

    fun initialHeuristic(state: SlidingTilePuzzle4State): Double {
        var manhattanSum = 0.0
        var zero: Byte = 0

        for (x in 0..size - 1) {
            for (y in 0..size - 1) {
                val value = state[state.getIndex(x, y)]
                if (value == zero) continue

                manhattanSum += abs(value / size - y) + abs(value % size - x)
            }
        }

        return manhattanSum
    }

    override fun distance(state: SlidingTilePuzzle4State) = state.heuristic

    override fun isGoal(state: SlidingTilePuzzle4State) = state.heuristic == 0.0

    override fun print(state: SlidingTilePuzzle4State): String {
        throw UnsupportedOperationException()
    }

    override fun randomState(): SlidingTilePuzzle4State {
        throw UnsupportedOperationException()
    }

    override fun getGoal(): SlidingTilePuzzle4State {
        throw UnsupportedOperationException()
    }

    override fun predecessors(state: SlidingTilePuzzle4State): List<SuccessorBundle<SlidingTilePuzzle4State>> {
        throw UnsupportedOperationException()
    }
}