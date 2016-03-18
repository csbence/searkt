package edu.unh.cs.ai.realtimesearch.environment.pointrobotwithinertia

import edu.unh.cs.ai.realtimesearch.environment.Action
import edu.unh.cs.ai.realtimesearch.environment.location.Location

/**
 * This is an action in the point robot with inertia domain.
 *
 * @param index: the type of action, each return different relative locations
 */
data class PointRobotWithInertiaAction(val xDoubleDot : Double, val yDoubleDot : Double) : Action {

    override fun toString(): String{
        return "($xDoubleDot, $yDoubleDot)"
    }
}