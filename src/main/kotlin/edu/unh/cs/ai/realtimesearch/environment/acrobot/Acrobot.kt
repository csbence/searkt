package edu.unh.cs.ai.realtimesearch.environment.acrobot

import edu.unh.cs.ai.realtimesearch.environment.Domain
import edu.unh.cs.ai.realtimesearch.environment.SuccessorBundle
import edu.unh.cs.ai.realtimesearch.environment.acrobot.configuration.AcrobotConfiguration
import edu.unh.cs.ai.realtimesearch.environment.acrobot.configuration.AcrobotStateConfiguration
import edu.unh.cs.ai.realtimesearch.util.angleDistance

/**
 * The Acrobot is a two-link underactuated system.  Torque may be applied to the second link but not the first thereby
 * actuating joint 2.  The goal of the system is to maneuver the links such that they are pointing straight up inverted
 * vertically from a downward facing position.
 *
 * Acrobot concept from:
 *
 * Hauser, John, and Richard M. Murray. "Nonlinear controllers for non-integrable systems: The acrobot example." In American Control Conference, 1990, pp. 669-671. IEEE, 1990.
 *
 * @author Mike Bogochow (mgp36@unh.edu)
 *
 * @param configuration the configuration for the domain instance
 * @param actionDuration the duration actions are applied for
 */
class Acrobot(val configuration: AcrobotConfiguration = AcrobotConfiguration(),
              val actionDuration: Long = AcrobotStateConfiguration.defaultActionDuration) : Domain<AcrobotState> {

    companion object {
        data class AcrobotBoundStates(val lowerBound: AcrobotState, val upperBound: AcrobotState)

        /**
         * Returns the lower and upper bounds around the given goal state as {@link AcrobotState}s.
         *
         * @param goalState the goal state
         * @param configuration the configuration containing the relative goal bounds
         */
        fun getBoundStates(goalState: AcrobotState, configuration: AcrobotConfiguration): AcrobotBoundStates {
            val absoluteLink1LowerBound = goalState.link1 - configuration.goalLink1LowerBound
            val absoluteLink2LowerBound = goalState.link2 - configuration.goalLink2LowerBound
            val absoluteLink1UpperBound = goalState.link1 + configuration.goalLink1UpperBound
            val absoluteLink2UpperBound = goalState.link2 + configuration.goalLink2UpperBound

            val endStateLowerBound =
                    AcrobotState(absoluteLink1LowerBound, absoluteLink2LowerBound, configuration.stateConfiguration)
            val endStateUpperBound =
                    AcrobotState(absoluteLink1UpperBound, absoluteLink2UpperBound, configuration.stateConfiguration)

            return AcrobotBoundStates(endStateLowerBound, endStateUpperBound)
        }

        /**
         * Returns the lower and upper bounds around the goal state as {@link AcrobotState}s.
         *
         * @param configuration the configuration containing the relative goal bounds and the goal state
         */
        fun getBoundStates(configuration: AcrobotConfiguration): AcrobotBoundStates =
                getBoundStates(configuration.goalState, configuration)
    }

    val endStateBounds = getBoundStates(configuration)

    /**
     * Calculate the next state given the current state and an action.
     *
     * @param currentState the current state of the acrobot
     * @param action the action to apply to the current state
     * @return the resulting state from applying the given action to the current state
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
     * @return the distance heuristic between the two states
     */
    private fun distanceHeuristic(startState: AcrobotState, endState: AcrobotState): Double {
        if (isGoal(startState))
            return 0.0

        val (endStateLowerBound, endStateUpperBound) = getBoundStates(endState, configuration)

        val distance1 = Math.min(
                angleDistance(startState.link1.position, endStateLowerBound.link1.position),
                angleDistance(startState.link1.position, endStateUpperBound.link1.position))
        val distance2 = Math.min(
                angleDistance(startState.link2.position, endStateLowerBound.link2.position),
                angleDistance(startState.link2.position, endStateUpperBound.link2.position))

        val link1Heuristic =
                distance1 / (configuration.stateConfiguration.maxAngularVelocity1 - Math.abs(startState.link1.velocity))
        val link2Heuristic =
                distance2 / (configuration.stateConfiguration.maxAngularVelocity2 - Math.abs(startState.link2.velocity))

        return Math.max(link1Heuristic, link2Heuristic)
    }

    /**
     * Returns a heuristic for a Acrobot state: the distance over the max velocities.  Also factors in the state's
     * velocity since we want to have very low velocity at goal.
     *
     * @param state the state to provide a heuristic for
     * @return the distance heuristic between the state and the goal state of the domain instance configuration.
     */
    private fun distanceHeuristic(state: AcrobotState): Double = distanceHeuristic(state, configuration.goalState)

    /**
     * Goal distance estimate.  Equal to the difference between the goal positions and actual positions.
     */
    override fun distance(state: AcrobotState): Double {
        return angleDistance(state.link1.position, configuration.goalState.link1.position) +
                angleDistance(state.link2.position, configuration.goalState.link2.position)
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
    fun randomState(): AcrobotState {
        return AcrobotState(0.0, 0.0, 0.0, 0.0, configuration.stateConfiguration)
    }

    override fun getGoals(): List<AcrobotState> {
        return listOf(configuration.goalState)
    }

    /**
     * TODO: If there is a way to calculate a predecessor given a torque action then this method should be implemented
     */
    override fun predecessors(state: AcrobotState): List<SuccessorBundle<AcrobotState>> {
        throw UnsupportedOperationException()
    }
}
