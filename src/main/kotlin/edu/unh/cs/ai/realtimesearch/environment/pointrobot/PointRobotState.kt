package edu.unh.cs.ai.realtimesearch.environment.pointrobot

import edu.unh.cs.ai.realtimesearch.environment.State
import edu.unh.cs.ai.realtimesearch.environment.location.Location

/**
 *
 */
data class PointRobotState(val x : Double, val y : Double) : State<PointRobotState> {

    override fun equals(other: Any?): Boolean {
        if(other !is PointRobotState)
            return false
        if(other.x.toInt() == x.toInt() && other.y.toInt() == y.toInt()) {
            return true;
        }
        return false;
    }

    /**
     * Copy simply calls the data class implemented copy
     */
    override fun copy() = copy(x, y)
}

