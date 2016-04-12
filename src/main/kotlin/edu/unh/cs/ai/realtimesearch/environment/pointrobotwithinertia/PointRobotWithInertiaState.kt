package edu.unh.cs.ai.realtimesearch.environment.pointrobotwithinertia

import edu.unh.cs.ai.realtimesearch.environment.DiscretizableState
import edu.unh.cs.ai.realtimesearch.util.*
import org.codehaus.groovy.util.HashCodeHelper
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
    //    val xGranularity = 0.01
    //    val xGranularity = 0.0125
    //    val xGranularity = 0.015625
    //    val xGranularity = 0.02
    //    val xGranularity = 0.025
    //    val xGranularity = 0.03125
    //    val xGranularity = 0.04
    //    val xGranularity = 0.05
    //    val xGranularity = 0.0625
    //    val xGranularity = 0.10
    //    val xGranularity = 0.125
    //    val xGranularity = 0.20
    //    val xGranularity = 0.25
    //    val xGranularity = 0.5
    val xGranularity = 0.25
    val xDotGranularity = 0.32
    val yGranularity = xGranularity
    val yDotGranularity = xDotGranularity

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

    fun calculatePreviousState(previousAction: PointRobotWithInertiaAction, actionDuration: Long): PointRobotWithInertiaState {
        val durationSeconds: Double = convertNanoUpDouble(actionDuration, TimeUnit.SECONDS)
        var previousXDot = calculatePreviousVelocity(previousAction.xDoubleDot, xdot, durationSeconds)
        var previousYDot = calculatePreviousVelocity(previousAction.yDoubleDot, ydot, durationSeconds)
        var previousX = x - calculateDisplacement(previousAction.xDoubleDot, previousXDot, durationSeconds)
        var previousY = y - calculateDisplacement(previousAction.yDoubleDot, previousYDot, durationSeconds)

        return PointRobotWithInertiaState(previousX, previousY, previousXDot, previousYDot)
    }

    /**
     * Copy simply calls the data class implemented copy
     */
    override fun copy() = copy(x, y, xdot, ydot)

    override fun equals(other: Any?): Boolean {
        return when {
            other == null -> false
            other === this -> true
            other !is PointRobotWithInertiaState -> false
            else -> doubleNearEqual(x, other.x)
                    && doubleNearEqual(y, other.y)
                    && doubleNearEqual(xdot, other.xdot)
                    && doubleNearEqual(ydot, other.ydot)
        }
    }

    override fun hashCode(): Int {
        var hashCode = HashCodeHelper.initHash()
        hashCode = HashCodeHelper.updateHash(hashCode, roundToNearestDecimal(x, defaultFloatAccuracy))
        hashCode = HashCodeHelper.updateHash(hashCode, roundToNearestDecimal(y, defaultFloatAccuracy))
        hashCode = HashCodeHelper.updateHash(hashCode, roundToNearestDecimal(xdot, defaultFloatAccuracy))
        hashCode = HashCodeHelper.updateHash(hashCode, roundToNearestDecimal(ydot, defaultFloatAccuracy))
        return hashCode
    }
}
