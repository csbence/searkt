package edu.unh.cs.ai.realtimesearch.environment.racetrack

import edu.unh.cs.ai.realtimesearch.environment.Domain
import edu.unh.cs.ai.realtimesearch.environment.SuccessorBundle
import edu.unh.cs.ai.realtimesearch.environment.location.Location
import java.lang.Math.*

/**
 * The racetrack domain is a gridworld with a specific start 'line' and finish 'line'. The
 * agent starts at one of the cells on the starting line, and the goal is to reach one of the
 * cells at the finish line. The shape of the track is variable, and driving of the grid returns
 * the agent to a cell on the starting line.
 *
 * The car can choose to accelerate up to 1 in either x or y direction, reaching a speed of up to
 * 1 (in both directions). The dynamics are as follows:
 *
 * x(t+1) = x(t) + x.(t) + m(x,t)
 * y(t+1) = y(t) + y.(t) + m(y,t)
 * x.(t+1) = x.(t) + m(x,t)
 * y.(t+1) = y.(t) + m(y,t)
 *
 * The parameter 'p' introduces stochasticity to the problem: with probability p the car will
 * fail its action and maintain its speed.
 *
 */
class RaceTrack(val width: Int,
                val height: Int,
                val track: Set<Location>,
                val finishLine: Set<Location>,
                val actionDuration: Long) : Domain<RaceTrackState> {

    //private val logger = LoggerFactory.getLogger(RaceTrack::class.java)

    val maxXSpeed = width / 2
    val maxYSpeed = height / 2

    override fun successors(state: RaceTrackState): List<SuccessorBundle<RaceTrackState>> {
        val successors: MutableList<SuccessorBundle<RaceTrackState>> = arrayListOf()

        for (action in RaceTrackAction.values()) {
            val newDX = state.dX + action.aX
            val newDY = state.dY + action.aY
            val distance = sqrt(pow(newDX.toDouble(), 2.0) + pow(newDY.toDouble(), 2.0))

            var x = state.x.toDouble()
            var y = state.y.toDouble()

            val dt = 1 / distance
            var valid = true

            val stepX = newDX * dt
            val stepY = newDY * dt

            for (i in 0..distance.toInt() - 1) {
                x += stepX
                y += stepY

                if (!isLegalLocation(x, y)) {
                    valid = false
                    break
                }
            }

            //filter on legal moves (not too fast and on the track)
            if (valid) {
                successors.add(SuccessorBundle(
                        RaceTrackState(state.x + newDX, state.y + newDY, newDX, newDY),
                        action,
                        actionCost = actionDuration))
            }
        }

        return successors
    }

    /**
     * Returns whether location within boundaries and not a blocked cell.
     *
     * @param location the location to test
     * @return true if location is legal
     */
    fun isLegalLocation(x: Double, y: Double): Boolean {
        return x >= 0 && y >= 0 && x < width &&
                y < height && Location(Math.round(x.toFloat()), Math.round(y.toFloat())) !in track
    }

    /*
    * Heuristic is the distance divided by the max speed
    * */
    override fun heuristic(state: RaceTrackState) = distance(state) * actionDuration

    override fun heuristic(startState: RaceTrackState, endState: RaceTrackState) = distance(startState, endState) * actionDuration

    /**
     * Calculates the minimum number of steps to reach the closest goal position.
     */
    override fun distance(state: RaceTrackState): Double {
        val distanceFunction: (Location) -> Double = { (x, y) -> max(abs(state.x - x) / maxXSpeed.toDouble(), abs(state.y - y) / maxYSpeed.toDouble()) }
        return distanceFunction(finishLine.minBy(distanceFunction)!!)
        // TODO: would it make sense to have an ArrayList version of finishLine on hand for optimal iteration?
    }

    fun distance(startState: RaceTrackState, endState: RaceTrackState) = max(
            abs(startState.x - endState.x) / maxXSpeed.toDouble(),
            abs(startState.y - endState.y) / maxYSpeed.toDouble()
    )

    override fun isGoal(state: RaceTrackState): Boolean {
        val newLocation = Location(state.x, state.y)
        return newLocation in finishLine
    }

    /**
     * agent = @
     * blocked cell = ' '
     * track = #
     * finish line = $
     * start line = %
     */
    override fun print(state: RaceTrackState): String {
        val description = StringBuilder()
        for (h in 1..height) {
            (1..width)
                    .map {
                        when (Location(it, h)) {
                            Location(state.x, state.y) -> '@'
                            in finishLine -> '$'
                            in track -> '*'
                            else -> ' '
                        }
                    }
                    .forEach { description.append(it) }
            description.append("\n")
        }

        return description.toString()
    }

    override fun getGoals(): List<RaceTrackState> {
        val list: MutableList<RaceTrackState> = arrayListOf()
        for (it in finishLine) {
            for (xS in 0..maxXSpeed) {
                for (yS in 0..maxYSpeed) {
                    if (xS == 0 && yS == 0) {
                        list.add(RaceTrackState(it.x, it.y, xS, yS))
                    } else if (xS == 0) {
                        list.add(RaceTrackState(it.x, it.y, xS, yS))
                        list.add(RaceTrackState(it.x, it.y, xS, -yS))
                    } else if (yS == 0) {
                        list.add(RaceTrackState(it.x, it.y, xS, yS))
                        list.add(RaceTrackState(it.x, it.y, -xS, yS))
                    } else {
                        list.add(RaceTrackState(it.x, it.y, xS, yS))
                        list.add(RaceTrackState(it.x, it.y, -xS, yS))
                        list.add(RaceTrackState(it.x, it.y, xS, -yS))
                        list.add(RaceTrackState(it.x, it.y, -xS, -yS))
                    }
                }
            }
        }

        return list
    }

    override fun predecessors(state: RaceTrackState): List<SuccessorBundle<RaceTrackState>> {
        val predecessors: MutableList<SuccessorBundle<RaceTrackState>> = arrayListOf()

        for (action in RaceTrackAction.values()) {
            val new_x_speed = state.dX
            val new_y_speed = state.dY

            var x: Double
            var y: Double
            val dt = 0.1
            val nSteps = 10
            var valid = true

            for (i in 1..nSteps) {
                x = state.x - (new_x_speed * (dt * i))
                y = state.y - (new_y_speed * (dt * i))

                if (!isLegalLocation(x, y)) {
                    valid = false
                    break
                }
            }

            //filter on legal moves (not too fast and on the track)
            if (valid) {
                predecessors.add(SuccessorBundle(
                        RaceTrackState(state.x - new_x_speed, state.y - new_y_speed, new_x_speed - action.aX, new_y_speed - action.aY),
                        action,
                        actionCost = actionDuration))
            }
        }
        return predecessors
    }


    /**
     * The agent is safe when its velocity is zero.
     */
    override fun isSafe(state: RaceTrackState): Boolean = state.dX == 0 && state.dY == 0

    /**
     * Assuming that the acceleration of the agent is 1. To reach a safe state takes at least as many steps as the
     * maximum velocity of the agent over all dimensions.
     *
     * Int = max(abs(state.dX), abs(state.dY)
     * @return returns a lower bound on the number of steps to reach a safe state.
     */
    override fun safeDistance(state: RaceTrackState): Pair<Int, Int> = max(abs(state.dX), abs(state.dY)) to min(abs(state.dX), abs(state.dY))
}