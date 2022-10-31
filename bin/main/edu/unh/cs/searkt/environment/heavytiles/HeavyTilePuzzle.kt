package edu.unh.cs.searkt.environment.heavytiles

import edu.unh.cs.searkt.environment.Domain
import edu.unh.cs.searkt.environment.SuccessorBundle
import java.lang.Math.abs
import java.util.*

class HeavyTilePuzzle(val size: Int, val actionDuration: Long) : Domain<HeavyTilePuzzle4State> {

    private val randomIntegerTable = Array(256, { Random(Random().nextLong()).nextInt() })

    private val goalState: HeavyTilePuzzle4State by lazy {
        val tiles = ByteArray(16, { it.toByte() })
        val state = HeavyTilePuzzle4State(0, tiles, 0.0, 0.0, calculateHashCode(tiles))
        assert(initialHeuristic(state) == 0.0)
        state
    }

    override fun successors(state: HeavyTilePuzzle4State): List<SuccessorBundle<HeavyTilePuzzle4State>> {
        val successorBundles: MutableList<SuccessorBundle<HeavyTilePuzzle4State>> = arrayListOf()

        for (action in HeavyTilePuzzleAction.values()) {
            val successorState = successorState(state, action.relativeX, action.relativeY, action)

            if (successorState != null) {
                val tileToBeMoved = state.tiles[state.zeroIndex + state.getIndex(action.relativeX, action.relativeY)]
                successorBundles.add(SuccessorBundle(successorState, action, tileToBeMoved.toDouble()))
            }
        }

        return successorBundles
    }

    private fun successorState(state: HeavyTilePuzzle4State, relativeX: Int, relativeY: Int, action: HeavyTilePuzzleAction): HeavyTilePuzzle4State? {
        val newZeroIndex = state.zeroIndex + state.getIndex(relativeX, relativeY)
        val actionAllowed = when (action) {
            HeavyTilePuzzleAction.NORTH -> state.zeroIndex >= size
            HeavyTilePuzzleAction.SOUTH -> state.zeroIndex < ((size * size) - size)
            HeavyTilePuzzleAction.WEST -> (state.zeroIndex % size) > 0
            HeavyTilePuzzleAction.EAST -> (state.zeroIndex % size) < (size - 1)
        }
        val savedTiles = ByteArray(16, { state.tiles[it] })

        if (newZeroIndex >= 0 && newZeroIndex < size * size && actionAllowed) {
            state[state.zeroIndex] = state[newZeroIndex]
            state[newZeroIndex] = 0

            val modifiedTiles = ByteArray(16, { state.tiles[it] })
            val heuristic = initialHeuristic(state)
            val distance = initialDistance(state)
            val hashCode = calculateHashCode(state.tiles)

            state.tiles = savedTiles

            return HeavyTilePuzzle4State(newZeroIndex, modifiedTiles, heuristic, distance, hashCode)
        }

        return null
    }

    fun calculateHashCode(state: ByteArray): Int {
        var hashCode = 0
        state.forEach { byte ->
            hashCode = (Integer.rotateLeft(hashCode, 1) xor randomIntegerTable[byte.toInt()])
        }
        return hashCode
    }

    fun initialDistance(state: HeavyTilePuzzle4State): Double {
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

    override fun heuristic(state: HeavyTilePuzzle4State): Double = state.heuristic

    override fun heuristic(startState: HeavyTilePuzzle4State, endState: HeavyTilePuzzle4State): Double {
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

                    manhattanSum += (abs(endX - yStart) + abs(endY - xStart)) * value
                    break
                }
            }
        }

        return manhattanSum
    }

    fun initialHeuristic(state: HeavyTilePuzzle4State): Double {
        var manhattanSum = 0.0
        val zero: Byte = 0

        for (x in 0 until size) {
            for (y in 0 until size) {
                val value = state[state.getIndex(x, y)]
                if (value == zero) continue

                manhattanSum += (abs(value / size - y) + abs(value % size - x)) * value
            }
        }

        return manhattanSum
    }

    override fun distance(state: HeavyTilePuzzle4State) = state.distance // state.heuristic

    override fun isGoal(state: HeavyTilePuzzle4State) = state.heuristic == 0.0 && state == goalState

    override fun getGoals(): List<HeavyTilePuzzle4State> = listOf(goalState)

    override fun predecessors(state: HeavyTilePuzzle4State) = successors(state)

    private fun calculateHeuristic(tiles: ByteArray): Double {
        var manhattanSum = 0.0
        val zero: Byte = 0

        for (x in 0 until size) {
            for (y in 0 until size) {
                val value = tiles[4 * y + x]
                if (value == zero) continue

                manhattanSum += (abs(value / size - y) + abs(value % size - x)) * value
            }
        }

        return manhattanSum
    }

    private fun calculateDistance(tiles: ByteArray): Double {
        var manhattanSum = 0.0
        val zero: Byte = 0

        for (x in 0 until size) {
            for (y in 0 until size) {
                val value = tiles[4 * y + x]
                if (value == zero) continue

                manhattanSum += (abs(value / size - y) + abs(value % size - x))
            }
        }

        return manhattanSum
    }

    override fun pack(state: HeavyTilePuzzle4State): Long {
        var word = 0L
        state.tiles[state.zeroIndex] = 0
        state.tiles.forEach { tile ->
            word = (word shl 4) or tile.toLong()
        }
        return word
    }

    override fun unpack(state: Long): HeavyTilePuzzle4State {
        var word = state
        var zeroIndex = -1
        val tiles = ByteArray(16)
        for (i in 15 downTo 0) {
            val t = word and 0xF
            word = word shr 4
//            tiles[i] = t.toByte()
            tiles[i] = t.toByte()
            if (t == 0L) {
                zeroIndex = i
            }
        }
        return HeavyTilePuzzle4State(zeroIndex, tiles, calculateHeuristic(tiles), calculateDistance(tiles), calculateHashCode(tiles))
    }
}