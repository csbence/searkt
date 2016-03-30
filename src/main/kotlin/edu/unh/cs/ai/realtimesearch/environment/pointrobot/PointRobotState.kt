package edu.unh.cs.ai.realtimesearch.environment.pointrobot

import edu.unh.cs.ai.realtimesearch.environment.State

/**
 *
 */
data class PointRobotState(val x: Double, val y: Double) : State<PointRobotState> {

    override fun equals(other: Any?): Boolean {
        return when (other) {
            !is PointRobotState -> false
            other.x.toInt() == x.toInt() && other.y.toInt() == y.toInt() -> true
            else -> false
        }
    }

    override fun hashCode(): Int {
        return x.toInt() xor Integer.reverse(y.toInt())
    }

    /**
     * Copy simply calls the data class implemented copy
     */
    override fun copy() = copy(x, y)
}

