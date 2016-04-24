package edu.unh.cs.ai.realtimesearch.environment.acrobot

import edu.unh.cs.ai.realtimesearch.environment.Domain
import edu.unh.cs.ai.realtimesearch.environment.SuccessorBundle
import edu.unh.cs.ai.realtimesearch.environment.acrobot.configuration.AcrobotConfiguration
import edu.unh.cs.ai.realtimesearch.environment.acrobot.configuration.AcrobotStateConfiguration
import edu.unh.cs.ai.realtimesearch.util.angleDistance

/**
 * The Acrobot is a two-link underactuated system.  Torque may be applied to the
 * second link but not the first thereby actuating joint 2.  The goal of the system
 * is to maneuver the links such that they are pointing straight up inverted
 * vertically from a downward facing position.
 */
class Acrobot(val configuration: AcrobotConfiguration = AcrobotConfiguration(),
              val actionDuration: Long = AcrobotStateConfiguration.defaultActionDuration) : Domain<AcrobotState> {
    companion object {
        data class AcrobotBoundStates(val lowerBound: AcrobotState, val upperBound: AcrobotState)

        fun getBoundStates(endState: AcrobotState, configuration: AcrobotConfiguration): AcrobotBoundStates {
            val absoluteLink1LowerBound = endState.link1 - configuration.endLink1LowerBound
            val absoluteLink2LowerBound = endState.link2 - configuration.endLink2LowerBound
            val absoluteLink1UpperBound = endState.link1 + configuration.endLink1UpperBound
            val absoluteLink2UpperBound = endState.link2 + configuration.endLink2UpperBound

            val endStateLowerBound = AcrobotState(absoluteLink1LowerBound, absoluteLink2LowerBound, configuration.stateConfiguration)
            val endStateUpperBound = AcrobotState(absoluteLink1UpperBound, absoluteLink2UpperBound, configuration.stateConfiguration)

            return AcrobotBoundStates(endStateLowerBound, endStateUpperBound)
        }

        fun getBoundStates(configuration: AcrobotConfiguration): AcrobotBoundStates = getBoundStates(configuration.endState, configuration)
    }

    val endStateBounds = getBoundStates(configuration)

    /**
     * Calculate the next state given the current state and an action
     */
    internal fun calculateNextState(currentState: AcrobotState, action: AcrobotAction): AcrobotState {
        return currentState.calculateNextState(currentState.calculateLinkAccelerations(action), actionDuration)
    }

    /**
     * Get successor states from the given state for all valid actions.
     */
    override fun successors(state: AcrobotState): List<SuccessorBundle<AcrobotState>> {
        // to return
        val successors: MutableList<SuccessorBundle<AcrobotState>> = arrayListOf()

        for (action in AcrobotAction.values()) {
            // add the legal movement actions
            successors.add(SuccessorBundle(
                    calculateNextState(state, action),
                    action, actionCost = actionDuration))
        }

        return successors
    }

    /**
     * Returns a heuristic for a Acrobot state based on the angular distance of the two links from the goal state bounds.
     *
     * @param state the state to provide a heuristic for
     */
    override fun heuristic(state: AcrobotState): Double {
        return distanceHeuristic(state)
    }

    /**
     * Returns a heuristic for the given start state given an end state.
     */
    override fun heuristic(startState: AcrobotState, endState: AcrobotState): Double {
        if (startState.equals(endState))
            return 0.0
        return distanceHeuristic(startState, endState)
    }

    /**
     * Calculate the distance heuristic between a start start state and an end state.
     *
     * @param startState the state to provide a heuristic for
     * @param endState the ending state to calculate the distance to
     */
    private fun distanceHeuristic(startState: AcrobotState, endState: AcrobotState): Double {
        if (isGoal(startState))
            return 0.0

        val (endStateLowerBound, endStateUpperBound) = getBoundStates(endState, configuration)

        val distance1 = Math.min(angleDistance(startState.link1.position, endStateLowerBound.link1.position), angleDistance(startState.link1.position, endStateUpperBound.link1.position))
        val distance2 = Math.min(angleDistance(startState.link2.position, endStateLowerBound.link2.position), angleDistance(startState.link2.position, endStateUpperBound.link2.position))

        val link1Heuristic = distance1 / (configuration.stateConfiguration.maxAngularVelocity1 - Math.abs(startState.link1.velocity))
        val link2Heuristic = distance2 / (configuration.stateConfiguration.maxAngularVelocity2 - Math.abs(startState.link2.velocity))

        return Math.max(link1Heuristic, link2Heuristic)
    }

    /**
     * Returns a heuristic for a Acrobot state: the distance over the max velocities.  Also factors in the state's
     * velocity since we want to have very low velocity at goal.
     *
     * @param state the state to provide a heuristic for
     */
    private fun distanceHeuristic(state: AcrobotState): Double = distanceHeuristic(state, configuration.endState)

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
    override fun isGoal(state: AcrobotState): Boolean =
            state.inBounds(endStateBounds.lowerBound, endStateBounds.upperBound)

    /**
     * Simply prints the state values.
     *
     * @param state the state whose values should be printed
     */
    override fun print(state: AcrobotState): String {
        return state.toJson()
    }

    /**
     * Returns the initial state in which all state values are zeroed.
     */
    override fun randomState(): AcrobotState {
        return AcrobotState(0.0, 0.0, 0.0, 0.0, configuration.stateConfiguration)
    }

    override fun getGoal(): List<AcrobotState> {
        return listOf(configuration.endState)
    }

    override fun predecessors(state: AcrobotState): List<SuccessorBundle<AcrobotState>> {
        throw UnsupportedOperationException()
    }
}
