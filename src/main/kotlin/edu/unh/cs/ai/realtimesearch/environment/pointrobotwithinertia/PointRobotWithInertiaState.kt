package edu.unh.cs.ai.realtimesearch.environment.pointrobotwithinertia

import edu.unh.cs.ai.realtimesearch.environment.DiscretizableState
import edu.unh.cs.ai.realtimesearch.util.calculateDisplacement
import edu.unh.cs.ai.realtimesearch.util.calculateVelocity
import edu.unh.cs.ai.realtimesearch.util.convertNanoUpDouble
import edu.unh.cs.ai.realtimesearch.util.roundDownToDecimal
import java.util.concurrent.TimeUnit

/**
 * A state for the point robot with inertia domain.  A state consists of the x and y position of the robot as well as
 * velocities in both the x and y direction.  Location units are in fractions of cell size and velocity units are in
 * cell size per second.
 *
 * @param x x position
 * @param y y position
 * @param xdot x velocity
 * @param ydot y velocity
 */
data class PointRobotWithInertiaState(val x: Double, val y: Double, val xdot: Double, val ydot: Double) : DiscretizableState<PointRobotWithInertiaState> {
//    val xGranularity = 0.08
//    val xDotGranularity = 0.08
//    val yGranularity = 0.08
//    val yDotGranularity = 0.08
    val xGranularity = 0.32
    val xDotGranularity = xGranularity
    val yGranularity = xGranularity
    val yDotGranularity = xGranularity

    override fun discretize(): PointRobotWithInertiaState {
        return PointRobotWithInertiaState(
                roundDownToDecimal(x, xGranularity), roundDownToDecimal(y, yGranularity),
                roundDownToDecimal(xdot, xDotGranularity), roundDownToDecimal(ydot, yDotGranularity))
    }

    fun calculateNextState(action: PointRobotWithInertiaAction, actionDuration: Long): PointRobotWithInertiaState {
        val durationSeconds: Double = convertNanoUpDouble(actionDuration, TimeUnit.SECONDS)
        var newX = x + calculateDisplacement(action.xDoubleDot, xdot, durationSeconds)
        var newY = y + calculateDisplacement(action.yDoubleDot, ydot, durationSeconds)
        var newXDot = calculateVelocity(action.xDoubleDot, xdot, durationSeconds)
        var newYDot = calculateVelocity(action.yDoubleDot, ydot, durationSeconds)

        return PointRobotWithInertiaState(newX, newY, newXDot, newYDot)
    }


//    override fun equals(other: Any?): Boolean {
////        println("" + other + " " + this)
//        val fractions = 0.5; // number of values between whole numbers i.e. How many actions should there be in the range [0,1)?
//
//        return when {
//            other !is PointRobotWithInertiaState -> false
//            roundToNearestDecimal(other.x, fractions) == roundToNearestDecimal(x, fractions)
//                    && roundToNearestDecimal(other.y, fractions) == roundToNearestDecimal(y, fractions)
//                    && other.xdot == xdot
//                    && other.ydot == ydot -> true
//            else -> false
//        }
//    }
//
//    override fun hashCode(): Int {
//        return x.toInt() xor Integer.reverse(y.toInt())
//    }

    /**
     * Copy simply calls the data class implemented copy
     */
    override fun copy() = copy(x, y, xdot, ydot)
}

