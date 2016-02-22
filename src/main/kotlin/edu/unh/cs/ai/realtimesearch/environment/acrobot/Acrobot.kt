package edu.unh.cs.ai.realtimesearch.environment.acrobot

import edu.unh.cs.ai.realtimesearch.environment.Domain
import edu.unh.cs.ai.realtimesearch.environment.SuccessorBundle

val timeStep = 0.2
// Sutton: Limit angular velocities to \dot\theta_1\in[-4\pi,4\pi] and \dot\theta_2\in[-9\pi,9\pi]
val maxAngularVelocity1 = 4.0 * Math.PI
val maxAngularVelocity2 = 9.0 * Math.PI

/**
 * The Acrobot is a two-link underactuated system.  Torque may be applied to the
 * second link but not the first thereby actuating joint 2.  The goal of the system
 * is to maneuver the links such that they are pointing straight up inverted
 * vertically from a downward facing position.
 */
class Acrobot() : Domain<AcrobotState> {
    // Angles naturally restricted to [0,2\pi]
    /**
     * Goal values for the Acrobot domain.
     */
    object goal {
        val verticalLinkPosition1: Double = Math.PI / 2
        val verticalLinkPosition2: Double = 0.0
        // Capture region around goal for both positions and velocities
        val lowerBound = AcrobotState(verticalLinkPosition1 - 0.3, verticalLinkPosition2 - 0.3, -0.3, -0.3)
        val upperBound = AcrobotState(verticalLinkPosition1 + 0.3, verticalLinkPosition2 + 0.3, 0.3, 0.3)
    }

    private fun calculateVelocity(acceleration: Double, initialVelocity: Double, time: Double) = acceleration * time + initialVelocity
    private fun calculateDisplacement(acceleration: Double, initialVelocity: Double, time: Double) = initialVelocity * time + 0.5 * acceleration * (time * time)

    /**
     * Get successor states from the given state for all valid actions.
     */
    override fun successors(state: AcrobotState): List<SuccessorBundle<AcrobotState>> {
        // to return
        val successors : MutableList<SuccessorBundle<AcrobotState>> = arrayListOf()

        for (action in AcrobotAction.values()) {
            // add the legal movement actions
            val (linkAcceleration1, linkAcceleration2) = state.calculateLinkAccelerations(action)
            val newLinkVelocity1 = calculateVelocity(linkAcceleration1, state.linkVelocity1, timeStep)
            val newLinkVelocity2 = calculateVelocity(linkAcceleration2, state.linkVelocity2, timeStep)
            val newLinkPosition1 = state.linkPosition1 + calculateDisplacement(linkAcceleration1, state.linkVelocity1, timeStep)
            val newLinkPosition2 = state.linkPosition2 + calculateDisplacement(linkAcceleration2, state.linkVelocity2, timeStep)

            successors.add(SuccessorBundle(
                    AcrobotState(newLinkPosition1, newLinkPosition2, newLinkVelocity1, newLinkVelocity2),
                    action, actionCost = timeStep))
        }

        return successors
    }

    /**
     * Returns a heuristic for a Acrobot state: the distance over the max velocities.
     *
     * @param state the state to provide a heuristic for
     */
    override fun heuristic(state: AcrobotState): Double {
        // Dumb heuristic 1 (distance over max velocity)
        val distance1 = Math.min(angleDistance(state.linkPosition1, Acrobot.goal.lowerBound.linkPosition1), angleDistance(state.linkPosition1, Acrobot.goal.upperBound.linkPosition1))
        val distance2 = Math.min(angleDistance(state.linkPosition2, Acrobot.goal.lowerBound.linkPosition2), angleDistance(state.linkPosition2, Acrobot.goal.upperBound.linkPosition2))

        return distance1 / maxAngularVelocity1 + distance2 / maxAngularVelocity2
    }

    /**
     * Goal distance estimate.  Equal to the difference between the goal positions and actual positions.
     */
    override fun distance(state: AcrobotState): Double {
        return angleDistance(state.linkPosition1, Acrobot.goal.verticalLinkPosition1) +
                angleDistance(state.linkPosition2, Acrobot.goal.verticalLinkPosition2)
    }

    /**
     * Calculate the difference between an angle and a goal angle.  The resulting difference will be in the range
     * [-pi,pi] to avoid attempting to rotate completely around in one direction.
     */
    private fun angleDifference(angle: Double, goalAngle: Double): Double {
        var difference = goalAngle - angle
        if (difference < -Math.PI)
            difference += 2 * Math.PI
        else if (difference > Math.PI)
            difference -= 2 * Math.PI
        return difference
    }

    /**
     * Calculate the angle distance between an angle and a goal angle.  The resulting distance will be in the range
     * [0,pi].
     */
    private fun angleDistance(angle: Double, goalAngle: Double): Double {
        val distance = angleDifference(angle, goalAngle)
        return if (distance < 0) distance * -1 else distance
    }

    /**
     * Returns whether the given state is a goal state.
     * @return true if the links within a threshold of positions and velocities.
     */
    override fun isGoal(state: AcrobotState): Boolean = state.inBounds(Acrobot.goal.lowerBound, Acrobot.goal.upperBound)

    /**
     * Simply prints the state values.  TODO would be nice to have some rough ASCII art
     *
     * @param state the state whose values should be printed
     */
    override fun print(state: AcrobotState): String {
        val description = StringBuilder()
        description.append("linkPosition1=").appendln(state.linkPosition1)
        description.append("linkPosition2=").appendln(state.linkPosition2)
        description.append("linkVelocity1=").appendln(state.linkVelocity1)
        description.append("linkVelocity2=").appendln(state.linkVelocity2)
        return description.toString()
    }

    /**
     * Returns the initial state in which all state values are zeroed.
     */
    override fun randomState(): AcrobotState {
        return AcrobotState(0.0, 0.0, 0.0, 0.0)
    }

}

