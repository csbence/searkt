package edu.unh.cs.ai.realtimesearch.environment.pointrobotwithinertia

import edu.unh.cs.ai.realtimesearch.environment.State
import edu.unh.cs.ai.realtimesearch.environment.location.DoubleLocation
import edu.unh.cs.ai.realtimesearch.environment.location.Location

/**
 *
 */
data class PointRobotWithInertiaState(val loc : DoubleLocation, val xdot : Double, val ydot : Double) : State<PointRobotWithInertiaState> {

    override fun equals(other: Any?): Boolean {
        if(other !is PointRobotWithInertiaState)
            return false
        if(other.loc.x.toInt() == loc.x.toInt() && other.loc.y.toInt() == loc.y.toInt()) {
            return true;
        }
        return false;
    }

    /**
     * Copy simply calls the data class implemented copy
     */
    override fun copy() = copy(loc, xdot, ydot)
}

