package edu.unh.cs.ai.realtimesearch.environment.pointrobotwithinertia

import edu.unh.cs.ai.realtimesearch.environment.Action

/**
 * This is an action in the point robot with inertia domain.  An action is in the form of a change in acceleration in
 * both the x and y direction.
 *
 * @param xDoubleDot change in x direction acceleration
 * @param yDoubleDot change in y direction acceleration
 */
data class PointRobotWithInertiaAction(val xDoubleDot: Double, val yDoubleDot: Double) : Action {

    override fun toString(): String {
        return "($xDoubleDot, $yDoubleDot)"
    }
}