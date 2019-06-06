package edu.unh.cs.searkt.environment.pancake

import edu.unh.cs.searkt.environment.Domain
import edu.unh.cs.searkt.environment.SuccessorBundle
import kotlin.math.min

/**
 * @param startOrdering the initial state ordering of the pancakes
 * @param endOrdering the final goal state ordering of the pancakes
 * @param variant the type of pancake puzzle 0: unit, else: heavy
 */
class PancakeProblem(val startOrdering: ByteArray, val endOrdering: ByteArray, private var variant: Int) : Domain<PancakeState> {

    override fun successors(state: PancakeState): List<SuccessorBundle<PancakeState>> {
        val successors = ArrayList<SuccessorBundle<PancakeState>>()

        (state.ordering.size - 1 until 0).forEach { flipLocation ->
            // do not allow inverse actions
            if (state.indexFlipped != flipLocation) {
                flipOrdering(successors, state.ordering, flipLocation)
            }
        }

        return successors
    }

    private fun flipOrdering(successors: ArrayList<SuccessorBundle<PancakeState>>,
                             ordering: ByteArray,
                             flipLocation: Int) {
        val flippedPancakes = ordering.slice((0..flipLocation)).reversed()
        val restOfPancakes = ordering.slice((flipLocation until ordering.size))
        val reorderedPancakes = (flippedPancakes + restOfPancakes).toByteArray()
        successors.add(SuccessorBundle(PancakeState(reorderedPancakes, flipLocation), PancakeAction(flipLocation), 1.0))
    }

    override fun heuristic(state: PancakeState): Double {
        // uses gap heuristic from - Landmark Heuristic for the Pancake Problem
        // where add 1 to heuristic if the adjacent sizes of the pancakes differs more than 1
        // for heavy pancake problems
        // for each gap b/w x and y, add min(x,y) to heuristic instead of just 1
        val size = state.ordering.size
        val plate = size + 1
        var sum = 0

        (0..size).forEach { i ->
            val x = state.ordering[i - 1]
            val y = state.ordering[i]
            val difference = x - y
            if (difference > 1 || difference < -1) {
                sum += when (variant) {
                    0 -> 1
                    else -> min(x.toInt(), y.toInt())
                }
            }
        }

        // last pancake special case
        val x = state.ordering[size - 1]
        val difference = x - plate
        if (difference > 1 || difference < -1) {
            sum += when (variant) {
                0 -> 1
                else -> x
            }
        }

        return sum.toDouble()
    }

    private fun calculateUnitHeuristic(state: PancakeState): Double {
        // bit of a hack to calculate unit heuristic
        // even when we are doing heavy pancakes
        variant = 0
        val unitHeuristic = heuristic(state)
        variant = 1
        return unitHeuristic
    }

    override fun distance(state: PancakeState): Double {
        return calculateUnitHeuristic(state)
    }

    override fun isGoal(state: PancakeState): Boolean {
        return startOrdering.contentEquals(endOrdering)
    }


}