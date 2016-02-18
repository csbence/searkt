package edu.unh.cs.ai.realtimesearch.environment.acrobot

import edu.unh.cs.ai.realtimesearch.environment.Domain
import edu.unh.cs.ai.realtimesearch.environment.SuccessorBundle
import edu.unh.cs.ai.realtimesearch.environment.location.Location
import edu.unh.cs.ai.realtimesearch.environment.acrobot.AcrobotAction
import org.slf4j.LoggerFactory


val anglePrecision = 1.0
val velocityPrecision = 1.0

/**
 * The Acrobot is a two-link underactuated system.  Torque may be applied to the
 * second link but not the first thereby actuating joint 2.  The goal of the system
 * is to maneuver the links such that they are pointing straight up inverted
 * vertically from a downward facing position.
 */
class Acrobot() : Domain<AcrobotState> {
    object goal {
        val linkPosition1 : Double = Math.PI / 2
        val linkPosition2 : Double = 0.0
    }

    /**
     * TODO not 100% sure what this needs to generate
     */
    override fun successors(state: AcrobotState): List<SuccessorBundle<AcrobotState>> {
        throw UnsupportedOperationException()
        // to return
        val successors : MutableList<SuccessorBundle<AcrobotState>> = arrayListOf()

        for (it in AcrobotAction.values()) {
            // add the legal movement actions
        }

        return successors
    }

    override fun heuristic(state: AcrobotState): Double {
        // Dumb heuristic 1 (distance)
        return distance(state)
    }

    /**
     * Goal distance estimate.  Equal to the difference between the goal positions and actual positions.
     * TODO this is dumb since it may be necessary to move away from the goal with one link in order to build velocity to be able to reach the goal
     */
    override fun distance(state: AcrobotState): Double {
        return Acrobot.goal.linkPosition1 - state.linkPosition1 + Acrobot.goal.linkPosition2 - state.linkPosition2
    }

    /**
     * Returns whether the given state is a goal state.
     * Returns true if the links are in their goal positions and the velocities are within a threshold.
     * TODO add thresholds for both position and velocity since exact values are unrealistic
     */
    override fun isGoal(state: AcrobotState): Boolean {
        val positionCondition = state.linkPosition1 == Acrobot.goal.linkPosition1 && state.linkPosition2 == Acrobot.goal.linkPosition2
        val velocityCondition = state.linkVelocity1 == 0.0 && state.linkVelocity2 == 0.0
        return positionCondition && velocityCondition
    }

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

