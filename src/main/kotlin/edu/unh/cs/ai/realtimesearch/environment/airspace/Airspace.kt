package edu.unh.cs.ai.realtimesearch.environment.airspace

import edu.unh.cs.ai.realtimesearch.environment.Action
import edu.unh.cs.ai.realtimesearch.environment.Domain
import edu.unh.cs.ai.realtimesearch.environment.SuccessorBundle
import edu.unh.cs.ai.realtimesearch.environment.airspace.AirspaceAction.NO_OP
import edu.unh.cs.ai.realtimesearch.environment.location.Location
import edu.unh.cs.ai.realtimesearch.experiment.result.ExperimentResult
import java.lang.Math.*
import java.util.*

/**
 * Airspace is a 2D domain with dead-ends.
 *
 * In Airspace the agent has to traverse the state space horizontally reaching a horizontal distance of width.
 * Y represents the altitude and X the distance.
 *
 * The agent's velocity is equivalent to its altitude. Blocked states appear at altitudes higher than one.
 *
 * The agent has 3 potential actions: increase or decrease the altitude with one or keep the current altitude.
 *
 * y(t+1) = y(t) + {1, 0, -1}
 * x(t+1) = x(t) + y(t + 1)
 */
class Airspace(val width: Int,
               val height: Int,
               val obstacles: Set<Location>,
               private val finishLine: Set<Location>,
               val actionDuration: Long) : Domain<AirspaceState> {

    private val velocities = mutableListOf<Double>()

    override fun successors(state: AirspaceState): List<SuccessorBundle<AirspaceState>> {
        val successors: MutableList<SuccessorBundle<AirspaceState>> = arrayListOf()

        for (action in AirspaceAction.values()) {
            val newDY = action.dY
            val newDX = state.y + newDY

            //filter on legal moves (not too fast and on the obstacles)
            if (isCollisionFree(state.x, state.y, newDX, newDY)) {
                successors.add(SuccessorBundle(
                        AirspaceState(state.x + newDX, state.y + newDY),
                        action,
                        actionCost = actionDuration.toDouble()))
            }
        }

        return successors
    }

    fun isCollisionFree(x: Int, y: Int, dX: Int, dY: Int): Boolean {
        val distance = round(sqrt(pow(dX.toDouble(), 2.0) + pow(dY.toDouble(), 2.0)))

        var xRunnung = x.toDouble()
        var yRunning = y.toDouble()

        val dt = 1.0 / distance
        var valid = true

        val stepX = dX * dt
        val stepY = dY * dt

        for (i in 1..distance.toInt()) {
            xRunnung += stepX
            yRunning += stepY

            if (!isLegalLocation(Math.round(xRunnung).toInt(), Math.round(yRunning).toInt())) {
                valid = false
                break
            }
        }
        return valid
    }

    /**
     * Returns whether location within boundaries and not a blocked cell.
     *
     * @return true if location is legal, else false.
     */
    private fun isLegalLocation(x: Int, y: Int): Boolean {
        return x >= 0 && y >= 0 && x < width &&
                y < height && Location(x, y) !in obstacles
    }

    /*
    * Heuristic is the distance divided by the max speed
    * */
    override fun heuristic(state: AirspaceState): Double = distance(state) * actionDuration

    override fun heuristic(startState: AirspaceState, endState: AirspaceState) = distance(startState, endState) * actionDuration

    /**
     * Calculates the minimum number of steps to reach the closest goal position.
     */
    override fun distance(state: AirspaceState): Double = (width - state.x).toDouble() / height

    fun distance(startState: AirspaceState, endState: AirspaceState) = abs(startState.x - endState.x).toDouble() / height

    override fun isGoal(state: AirspaceState): Boolean {
        val newLocation = Location(state.x, state.y)
        return newLocation in finishLine
    }

    /**
     * agent = @
     * obstacles = #
     * finish line = $
     * start line = %
     */
    override fun print(state: AirspaceState): String {
        val description = StringBuilder()
        for (h in 1..height) {
            (1..width)
                    .map {
                        when (Location(it, h)) {
                            Location(state.x, state.y) -> '@'
                            in finishLine -> '$'
                            in obstacles -> '#'
                            else -> ' '
                        }
                    }
                    .forEach { description.append(it) }
            description.append("\n")
        }

        return description.toString()
    }

    override fun getGoals(): List<AirspaceState> {
        return finishLine.map { AirspaceState(it.x, it.y) }
    }

    /**
     * The agent is safe when its velocity is zero.
     */
    override fun isSafe(state: AirspaceState): Boolean = (state.y == 0) || isGoal(state)

    override fun randomizedStartState(state: AirspaceState, seed: Long): AirspaceState {
        val startX = Random(seed).nextInt((width * 0.1).toInt())

        return AirspaceState(startX, state.y)
    }

    /**
     * Assuming that the acceleration of the agent is 1. To reach a safe state takes at least as many steps as the
     * maximum velocity of the agent over all dimensions.
     *
     * Int = max(abs(state.dX), abs(state.dY)
     * @return returns a lower bound on the number of steps to reach a safe state.
     */
    override fun safeDistance(state: AirspaceState): Pair<Int, Int> = state.y to state.y

    override fun getIdentityAction(state: AirspaceState): SuccessorBundle<AirspaceState>? = if (state.y == 0) SuccessorBundle(state, NO_OP, actionDuration.toDouble()) else null

    override fun transition(sourceState: AirspaceState, action: Action): AirspaceState? {
        val targetState = super.transition(sourceState, action)

        if (targetState != null) {
            velocities.add(sourceState.y.toDouble())
        }

        return targetState
    }

    override fun appendDomainSpecificResults(results: ExperimentResult) {
        results.averageVelocity = velocities.average()
    }
}