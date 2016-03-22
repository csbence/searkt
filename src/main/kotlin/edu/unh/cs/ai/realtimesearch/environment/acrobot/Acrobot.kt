package edu.unh.cs.ai.realtimesearch.environment.acrobot

import edu.unh.cs.ai.realtimesearch.environment.Domain
import edu.unh.cs.ai.realtimesearch.environment.SuccessorBundle
import edu.unh.cs.ai.realtimesearch.environment.acrobot.configuration.AcrobotConfiguration
import edu.unh.cs.ai.realtimesearch.environment.pointrobot.PointRobotState

/**
 * Calculate the difference between an angle and a goal angle.  The resulting difference will be in the range
 * [-pi,pi] to avoid attempting to rotate completely around in one direction.
 */
fun angleDifference(angle: Double, goalAngle: Double): Double {
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
fun angleDistance(angle: Double, goalAngle: Double): Double {
    val distance = angleDifference(angle, goalAngle)
    return if (distance < 0) distance * -1 else distance
}

/**
 * The Acrobot is a two-link underactuated system.  Torque may be applied to the
 * second link but not the first thereby actuating joint 2.  The goal of the system
 * is to maneuver the links such that they are pointing straight up inverted
 * vertically from a downward facing position.
 */
class Acrobot(val configuration: AcrobotConfiguration = AcrobotConfiguration()) : Domain<AcrobotState> {
    internal val endStateLowerBound = AcrobotState(configuration.endLink1LowerBound, configuration.endLink2LowerBound, configuration.stateConfiguration)
    internal val endStateUpperBound = AcrobotState(configuration.endLink1UpperBound, configuration.endLink2UpperBound, configuration.stateConfiguration)

    /**
     * Calculate the next state given the current state and an action
     */
    internal fun calculateNextState(currentState: AcrobotState, action: AcrobotAction): AcrobotState {
        return currentState.calculateNextState(currentState.calculateLinkAccelerations(action))
    }

    /**
     * Get successor states from the given state for all valid actions.
     */
    override fun successors(state: AcrobotState): List<SuccessorBundle<AcrobotState>> {
        // to return
        val successors : MutableList<SuccessorBundle<AcrobotState>> = arrayListOf()

        for (action in AcrobotAction.values()) {
            // add the legal movement actions
            successors.add(SuccessorBundle(
                    calculateNextState(state, action),
                    action, actionCost = configuration.stateConfiguration.timeStep))
        }

        return successors
    }

    /**
     * Returns a heuristic for a Acrobot state.  If the state does not have enough energy to reach the goal, must
     * inject energy before trying to reach the goal.  If the state does have enough energy, attempt to move towards
     * the goal.
     *
     * @param state the state to provide a heuristic for
     */
    override fun heuristic(state: AcrobotState): Double {
            return distanceHeuristic(state)
    }

    override fun heuristic(startState: AcrobotState, endState: AcrobotState): Double {
        if (startState.equals(endState))
            return 0.0
        val distance1 = Math.min(angleDistance(startState.link1.position, endState.link1.position), angleDistance(startState.link1.position, endState.link1.position))
        val distance2 = Math.min(angleDistance(startState.link2.position, endState.link2.position), angleDistance(startState.link2.position, endState.link2.position))

        return distance1 / (configuration.stateConfiguration.maxAngularVelocity1 - Math.abs(startState.link1.velocity)) + distance2 / (configuration.stateConfiguration.maxAngularVelocity2 - Math.abs(startState.link2.velocity))

    }

    /**
     * Returns a heuristic for a Acrobot state: the distance over the max velocities.  Also factors in the state's
     * velocity since we want to have very low velocity at goal.
     *
     * @param state the state to provide a heuristic for
     */
    private fun distanceHeuristic(state: AcrobotState): Double {
        // Dumb heuristic 1 (distance over velocity)
        if (isGoal(state))
            return 0.0
        val distance1 = Math.min(angleDistance(state.link1.position, endStateLowerBound.link1.position), angleDistance(state.link1.position, endStateUpperBound.link1.position))
        val distance2 = Math.min(angleDistance(state.link2.position, endStateLowerBound.link2.position), angleDistance(state.link2.position, endStateUpperBound.link2.position))

        return distance1 / (configuration.stateConfiguration.maxAngularVelocity1 - Math.abs(state.link1.velocity)) + distance2 / (configuration.stateConfiguration.maxAngularVelocity2 - Math.abs(state.link2.velocity))
    }

    /**
     * Goal distance estimate.  Equal to the difference between the goal positions and actual positions.
     */
    override fun distance(state: AcrobotState): Double {
        return angleDistance(state.link1.position, configuration.endState.link1.position) +
               angleDistance(state.link2.position, configuration.endState.link2.position)
    }

    /**
     * Returns whether the given state is a goal state.
     * @return true if the links within a threshold of positions and velocities.
     */
    override fun isGoal(state: AcrobotState): Boolean = state.inBounds(endStateLowerBound, endStateUpperBound)

    /**
     * Simply prints the state values.
     *
     * @param state the state whose values should be printed
     */
    override fun print(state: AcrobotState): String {
        val description = StringBuilder()
        description.append("linkPosition1=").appendln(state.link1.position)
        description.append("linkPosition2=").appendln(state.link2.position)
        description.append("linkVelocity1=").appendln(state.link1.velocity)
        description.append("linkVelocity2=").appendln(state.link2.velocity)
        return description.toString()
    }

    /**
     * Returns the initial state in which all state values are zeroed.
     */
    override fun randomState(): AcrobotState {
        return AcrobotState(0.0, 0.0, 0.0, 0.0, configuration.stateConfiguration)
    }

    override fun getGoal(): AcrobotState {
        throw UnsupportedOperationException()
    }

    override fun predecessors(state: AcrobotState): List<SuccessorBundle<AcrobotState>> {
        throw UnsupportedOperationException()
    }
}
