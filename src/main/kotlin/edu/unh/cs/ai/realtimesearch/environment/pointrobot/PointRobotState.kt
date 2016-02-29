package edu.unh.cs.ai.realtimesearch.environment.pointrobot

import edu.unh.cs.ai.realtimesearch.environment.State
import edu.unh.cs.ai.realtimesearch.environment.location.DoubleLocation
import edu.unh.cs.ai.realtimesearch.environment.location.Location

/**
 *
 */
data class PointRobotState(val loc : DoubleLocation) : State<PointRobotState> {

    override fun equals(other: Any?): Boolean {
        if(other !is PointRobotState)
            return false
        if(other.loc.x.toInt() == loc.x.toInt() && other.loc.y.toInt() == loc.y.toInt()) {
            return true;
        }
        return false;
    }

    /**
     * Copy simply calls the data class implemented copy
     */
    override fun copy() = copy(loc)
}

