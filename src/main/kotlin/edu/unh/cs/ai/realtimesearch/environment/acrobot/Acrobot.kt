package edu.unh.cs.ai.realtimesearch.environment.acrobot

import edu.unh.cs.ai.realtimesearch.environment.Domain
import edu.unh.cs.ai.realtimesearch.environment.SuccessorBundle

val timeStep = .2

/**
 * The Acrobot is a two-link underactuated system.  Torque may be applied to the
 * second link but not the first thereby actuating joint 2.  The goal of the system
 * is to maneuver the links such that they are pointing straight up inverted
 * vertically from a downward facing position.
 */
class Acrobot() : Domain<AcrobotState> {
    object goal {
        val verticalLinkPosition1: Double = Math.PI / 2
        val verticalLinkPosition2: Double = 0.0
        val lowerBound = AcrobotState(verticalLinkPosition1 - 0.3, verticalLinkPosition2 - 0.3, -0.3, -0.3)
        val upperBound = AcrobotState(verticalLinkPosition1 + 0.3, verticalLinkPosition2 + 0.3, 0.3, 0.3)
    }

    private fun calculateVelocity(acceleration: Double, initialVelocity: Double, time: Double) = acceleration * time + initialVelocity
    private fun calculateDisplacement(acceleration: Double, initialVelocity: Double, time: Double) = initialVelocity * time + acceleration * time

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
                    action,
                    actionCost = 1.0))
        }

        return successors
    }

    override fun heuristic(state: AcrobotState): Double {
        // Dumb heuristic 1 (distance)
        return distance(state)
    }

    /**
     * Goal distance estimate.  Equal to the difference between the goal positions and actual positions.
     */
    override fun distance(state: AcrobotState): Double {
        return Acrobot.goal.verticalLinkPosition1 - state.linkPosition1 + Acrobot.goal.verticalLinkPosition2 - state.linkPosition2
    }

    /**
     * Returns whether the given state is a goal state.
     * Returns true if the links within a threshold of positions and velocities.
     */
    override fun isGoal(state: AcrobotState): Boolean = state.withinBounds(Acrobot.goal.lowerBound, Acrobot.goal.upperBound)

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

