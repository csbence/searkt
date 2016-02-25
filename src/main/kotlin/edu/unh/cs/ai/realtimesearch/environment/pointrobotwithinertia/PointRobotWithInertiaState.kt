package edu.unh.cs.ai.realtimesearch.environment.pointrobotwithinertia

import edu.unh.cs.ai.realtimesearch.environment.State
import edu.unh.cs.ai.realtimesearch.environment.location.Location

/**
 *
 */
data class PointRobotWithInertiaState(val x : Double, val y : Double, val xdot : Double, val ydot : Double) : State<PointRobotWithInertiaState> {

    override fun equals(other: Any?): Boolean {
        if(other !is PointRobotWithInertiaState)
            return false
        if(other.x.toInt() == x.toInt() && other.y.toInt() == y.toInt()) {
            return true;
        }
        return false;
    }

    /**
     * Copy simply calls the data class implemented copy
     */
    override fun copy() = copy(x, y, xdot, ydot)
}

