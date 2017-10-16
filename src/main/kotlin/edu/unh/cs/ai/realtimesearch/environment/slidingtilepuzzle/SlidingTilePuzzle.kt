package edu.unh.cs.ai.realtimesearch.environment.slidingtilepuzzle

import edu.unh.cs.ai.realtimesearch.environment.Domain
import edu.unh.cs.ai.realtimesearch.environment.SuccessorBundle
import org.slf4j.LoggerFactory
import java.lang.Math.abs

class SlidingTilePuzzle(val size: Int, val actionDuration: Long) : Domain<SlidingTilePuzzle4State> {
    val logger = LoggerFactory.getLogger(SlidingTilePuzzle::class.java)!!

    private val goalState: SlidingTilePuzzle4State by lazy {
        val tiles = ByteArray(16, { it.toByte() })
        val state = SlidingTilePuzzle4State(0, tiles, 0.0)
        assert(initialHeuristic(state) == 0.0)
        state
    }

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
        val savedTiles = ByteArray(16, {state.tiles[it]})

        if (newZeroIndex >= 0 && newZeroIndex < size * size) {
            state[state.zeroIndex] = state[newZeroIndex]
            state[newZeroIndex] = 0

            val modifiedTiles = ByteArray(16, {state.tiles[it]})
            val heuristic = initialHeuristic(state)

            state.tiles = savedTiles

            return SlidingTilePuzzle4State(newZeroIndex, modifiedTiles, heuristic)
        }

        return null
    }

    override fun heuristic(state: SlidingTilePuzzle4State): Double = state.heuristic * actionDuration

    override fun heuristic(startState: SlidingTilePuzzle4State, endState: SlidingTilePuzzle4State): Double {
        var manhattanSum = 0.0
        val zero: Byte = 0

        for (xStart in 0 until size) {
            for (yStart in 0 until size) {
                val value = startState[startState.getIndex(xStart, yStart)]
                if (value == zero) continue

                for (endIndex in 0 until size * size) {
                    if (endState[endIndex] != value) {
                        continue
                    }
                    val endX = endIndex / size
                    val endY = endIndex % size

                    manhattanSum += abs(endX - yStart) + abs(endY - xStart)
                    break
                }
            }
        }

        return manhattanSum
    }

    fun initialHeuristic(state: SlidingTilePuzzle4State): Double {
        var manhattanSum = 0.0
        val zero: Byte = 0

        for (x in 0 until size) {
            for (y in 0 until size) {
                val value = state[state.getIndex(x, y)]
                if (value == zero) continue

                manhattanSum += abs(value / size - y) + abs(value % size - x)
            }
        }

        return manhattanSum
    }

    override fun distance(state: SlidingTilePuzzle4State) = state.heuristic

    override fun isGoal(state: SlidingTilePuzzle4State) = state.heuristic == 0.0

    override fun getGoals(): List<SlidingTilePuzzle4State> = listOf(goalState)

    override fun predecessors(state: SlidingTilePuzzle4State) = successors(state)
}