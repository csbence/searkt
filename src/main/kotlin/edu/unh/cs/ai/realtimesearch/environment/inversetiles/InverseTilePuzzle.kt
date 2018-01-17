package edu.unh.cs.ai.realtimesearch.environment.inversetiles

import edu.unh.cs.ai.realtimesearch.environment.Domain
import edu.unh.cs.ai.realtimesearch.environment.SuccessorBundle
import org.slf4j.LoggerFactory
import java.lang.Math.abs

class InverseTilePuzzle(val size: Int, val actionDuration: Long) : Domain<InverseTilePuzzle4State> {
    val significantDigitFactor = 100000L
    val logger = LoggerFactory.getLogger(InverseTilePuzzle::class.java)!!

    private val goalState: InverseTilePuzzle4State by lazy {
        val tiles = ByteArray(16, { it.toByte() })
        val state = InverseTilePuzzle4State(0, tiles, 0.0)
        assert(initialHeuristic(state) == 0.0)
        state
    }

    override fun successors(state: InverseTilePuzzle4State): List<SuccessorBundle<InverseTilePuzzle4State>> {
        val successorBundles: MutableList<SuccessorBundle<InverseTilePuzzle4State>> = arrayListOf()

        for (action in InverseTilePuzzleAction.values()) {
            val successorState = successorState(state, action.relativeX, action.relativeY, action)

            if (successorState != null) {
                val tileToBeMoved = state.tiles[state.zeroIndex + state.getIndex(action.relativeX, action.relativeY)]
                successorBundles.add(SuccessorBundle(successorState, action, actionDuration * ((1.0 / tileToBeMoved) * significantDigitFactor).toLong()))
            }
        }

        return successorBundles
    }

    private fun successorState(state: InverseTilePuzzle4State, relativeX: Int, relativeY: Int, action: InverseTilePuzzleAction): InverseTilePuzzle4State? {
        val newZeroIndex = state.zeroIndex + state.getIndex(relativeX, relativeY)
        val actionAllowed = when (action) {
            InverseTilePuzzleAction.NORTH -> state.zeroIndex >= size
            InverseTilePuzzleAction.SOUTH -> state.zeroIndex < ((size * size) - size)
            InverseTilePuzzleAction.WEST -> (state.zeroIndex % size) > 0
            InverseTilePuzzleAction.EAST -> (state.zeroIndex % size) < (size - 1)
        }
        val savedTiles = ByteArray(16, { state.tiles[it] })

        if (newZeroIndex >= 0 && newZeroIndex < size * size && actionAllowed) {
            state[state.zeroIndex] = state[newZeroIndex]
            state[newZeroIndex] = 0

            val modifiedTiles = ByteArray(16, { state.tiles[it] })
            val heuristic = initialHeuristic(state)

            state.tiles = savedTiles

            return InverseTilePuzzle4State(newZeroIndex, modifiedTiles, heuristic)
        }

        return null
    }

    override fun heuristic(state: InverseTilePuzzle4State): Double = state.heuristic * actionDuration

    override fun heuristic(startState: InverseTilePuzzle4State, endState: InverseTilePuzzle4State): Double {
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

                    manhattanSum += (abs(endX - yStart) + abs(endY - xStart)) * ((1.0 / value) * significantDigitFactor).toLong()
                    break
                }
            }
        }

        return manhattanSum
    }

    fun initialHeuristic(state: InverseTilePuzzle4State): Double {
        var manhattanSum = 0.0
        val zero: Byte = 0

        for (x in 0 until size) {
            for (y in 0 until size) {
                val value = state[state.getIndex(x, y)]
                if (value == zero) continue

                manhattanSum += (abs(value / size - y) + abs(value % size - x)) * ((1.0 / value) * significantDigitFactor).toLong()
            }
        }

        return manhattanSum
    }

    override fun distance(state: InverseTilePuzzle4State) = state.heuristic

    override fun isGoal(state: InverseTilePuzzle4State) = state.heuristic == 0.0 && state == goalState

    override fun getGoals(): List<InverseTilePuzzle4State> = listOf(goalState)

    override fun predecessors(state: InverseTilePuzzle4State) = successors(state)
}