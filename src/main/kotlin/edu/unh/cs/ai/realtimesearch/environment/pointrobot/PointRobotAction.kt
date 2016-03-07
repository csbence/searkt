package edu.unh.cs.ai.realtimesearch.environment.pointrobot

import edu.unh.cs.ai.realtimesearch.environment.Action
import edu.unh.cs.ai.realtimesearch.environment.location.Location

/**
 * This is An action in the vacuumworld is simply an enum:
 * an action for each direction and the vacuum action.
 *
 * @param index: the type of action, each return different relative locations
 */
class PointRobotAction(val xdot : Double, val ydot : Double) : Action {

    override fun toString(): String{
        return "(" + xdot + ", " + ydot + ")"
    }
}