package edu.unh.cs.ai.realtimesearch.environment.pointrobotlost

import edu.unh.cs.ai.realtimesearch.environment.Action

/**
 * This is an action in the point robot domain
 *
 * @param index: the type of action, each return different relative locations
 */
data class PointRobotLOSTAction(val xdot: Double, val ydot: Double) : Action {

    override fun toString(): String {
        return "($xdot, $ydot)"
    }
}