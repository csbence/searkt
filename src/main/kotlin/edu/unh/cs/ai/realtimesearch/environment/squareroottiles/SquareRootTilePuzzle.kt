package edu.unh.cs.ai.realtimesearch.environment.squareroottiles

import edu.unh.cs.ai.realtimesearch.environment.Domain
import edu.unh.cs.ai.realtimesearch.environment.SuccessorBundle
import org.slf4j.LoggerFactory
import java.lang.Math.abs
import kotlin.math.sqrt

class SquareRootTilePuzzle(val size: Int, val actionDuration: Long) : Domain<SquareRootTiles4State> {
    val logger = LoggerFactory.getLogger(SquareRootTilePuzzle::class.java)!!

    private val goalState: SquareRootTiles4State by lazy {
        val tiles = ByteArray(16, { it.toByte() })
        val state = SquareRootTiles4State(0, tiles, 0.0, 0.0)
        assert(initialHeuristic(state) == 0.0)
        state
    }

    override fun successors(state: SquareRootTiles4State): List<SuccessorBundle<SquareRootTiles4State>> {
        val successorBundles: MutableList<SuccessorBundle<SquareRootTiles4State>> = arrayListOf()

        for (action in SquareRootTilePuzzleAction.values()) {
            val successorState = successorState(state, action.relativeX, action.relativeY, action)

            if (successorState != null) {
                val tileToBeMoved = state.tiles[state.zeroIndex + state.getIndex(action.relativeX, action.relativeY)]
                successorBundles.add(SuccessorBundle(successorState, action, ((sqrt((tileToBeMoved.toDouble()))))))
            }
        }

        return successorBundles
    }

    private fun successorState(state: SquareRootTiles4State, relativeX: Int, relativeY: Int,
                               action: SquareRootTilePuzzleAction): SquareRootTiles4State? {
        val newZeroIndex = state.zeroIndex + state.getIndex(relativeX, relativeY)
        val actionAllowed = when (action) {
            SquareRootTilePuzzleAction.NORTH -> state.zeroIndex >= size
            SquareRootTilePuzzleAction.SOUTH -> state.zeroIndex < ((size * size) - size)
            SquareRootTilePuzzleAction.WEST -> (state.zeroIndex % size) > 0
            SquareRootTilePuzzleAction.EAST -> (state.zeroIndex % size) < (size - 1)
        }
        val savedTiles = ByteArray(16, { state.tiles[it] })

        if (newZeroIndex >= 0 && newZeroIndex < size * size && actionAllowed) {
            state[state.zeroIndex] = state[newZeroIndex]
            state[newZeroIndex] = 0

            val modifiedTiles = ByteArray(16, { state.tiles[it] })
            val heuristic = initialHeuristic(state)
            val distance = initialDistance(state)

            state.tiles = savedTiles

            return SquareRootTiles4State(newZeroIndex, modifiedTiles, heuristic, distance)
        }

        return null
    }

    fun initialDistance(state: SquareRootTiles4State): Double {
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

    override fun heuristic(state: SquareRootTiles4State): Double = state.heuristic

    override fun heuristic(startState: SquareRootTiles4State, endState: SquareRootTiles4State): Double {
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

                    manhattanSum += (((abs(endX - yStart) + abs(endY - xStart) ) * (sqrt(value.toDouble()))))
                    break
                }
            }
        }

        return manhattanSum
    }

    fun initialHeuristic(state: SquareRootTiles4State): Double {
        var manhattanSum = 0.0
        val zero: Byte = 0

        for (x in 0 until size) {
            for (y in 0 until size) {
                val value = state[state.getIndex(x, y)]
                if (value == zero) continue
//                println("tile $value -> h += ${abs(value / size - y) + abs(value % size - x)} * ${1.0/value}")
                manhattanSum += (((abs(value / size - y) + abs(value % size - x)) * (sqrt(value.toDouble()))))
            }
        }
//        println("---$manhattanSum---")
        return manhattanSum
    }

    override fun distance(state: SquareRootTiles4State) = state.distance // state.heuristic

    override fun isGoal(state: SquareRootTiles4State) = state.heuristic == 0.0 && state == goalState

    override fun getGoals(): List<SquareRootTiles4State> = listOf(goalState)

    override fun predecessors(state: SquareRootTiles4State) = successors(state)
}