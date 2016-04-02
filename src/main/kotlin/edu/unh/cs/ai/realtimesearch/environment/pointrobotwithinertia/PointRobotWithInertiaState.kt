package edu.unh.cs.ai.realtimesearch.environment.pointrobotwithinertia

import edu.unh.cs.ai.realtimesearch.environment.State

/**
 *
 */
data class PointRobotWithInertiaState(val x: Double, val y: Double, val xdot: Double, val ydot: Double) : State<PointRobotWithInertiaState> {

    override fun equals(other: Any?): Boolean {
        return when {
            other !is PointRobotWithInertiaState -> false
            other.x.toInt() == x.toInt() && other.y.toInt() == y.toInt() && other.xdot.toInt() == xdot.toInt() && other.ydot.toInt() == ydot.toInt() -> true
            else -> false
        }
    }

    override fun hashCode(): Int {
        return x.toInt() xor Integer.reverse(y.toInt())
    }

    /**
     * Copy simply calls the data class implemented copy
     */
    override fun copy() = copy(x, y, xdot, ydot)
}

