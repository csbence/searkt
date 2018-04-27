package edu.unh.cs.ai.realtimesearch.environment.racetrack

import edu.unh.cs.ai.realtimesearch.MetronomeException
import edu.unh.cs.ai.realtimesearch.environment.Action
import edu.unh.cs.ai.realtimesearch.environment.Domain
import edu.unh.cs.ai.realtimesearch.environment.SuccessorBundle
import edu.unh.cs.ai.realtimesearch.environment.location.Location
import edu.unh.cs.ai.realtimesearch.environment.racetrack.RaceTrackAction.NO_OP
import edu.unh.cs.ai.realtimesearch.experiment.result.ExperimentResult
import kotlinx.io.PrintWriter
import org.slf4j.LoggerFactory
import java.lang.Math.*
import java.util.*

/**
 * The racetrack domain is a gridworld with a specific start 'line' and finish 'line'. The
 * agent starts at one of the cells on the starting line, and the goal is to reach one of the
 * cells at the finish line. The shape of the obstacles is variable, and driving of the grid returns
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
                val obstacles: Set<Location>,
                val finishLine: Set<Location>,
                val actionDuration: Long) : Domain<RaceTrackState> {

    /** Pre-calculated heuristic value store */
    val maxXSpeed = width / 2
    val maxYSpeed = height / 2
    val maxSpeed = max(maxXSpeed, maxYSpeed)
    val heuristicMap: Map<Location, Double> = calculateDijkstraHeuristic()

    private val velocities = mutableListOf<Double>()

    private val logger = LoggerFactory.getLogger(RaceTrack::class.java)

    private fun calculateDijkstraHeuristic(): Map<Location, Double> {
        data class Node(val location: Location, val goalDistance: Double)

        val heuristicMap = hashMapOf<Location, Double>()

        val nodeComparator = java.util.Comparator<Node> { (_, lhsDistance), (_, rhsDistance) ->
            when {
                lhsDistance < rhsDistance -> -1
                lhsDistance > rhsDistance -> 1
                else -> 0
            }
        }

        val queue = PriorityQueue<Node>(nodeComparator)

        val discovered = HashSet<Location>()
        finishLine.forEach {
            queue.add(Node(it, 0.0))
            heuristicMap[it] = 0.0
            discovered += it
        }

        while (queue.isNotEmpty()) {
            val (location, goalDistance) = queue.poll()

            predecessors(RaceTrackState(location.x, location.y, 0, 0))
                    .filter { it.action != NO_OP }
                    .map { Location(it.state.x, it.state.y) }
                    .filter { it !in discovered }
                    .forEach {
                        discovered += it
                        heuristicMap[it] = (goalDistance + actionDuration) / max(maxXSpeed, maxYSpeed)
                        queue.add(Node(it, goalDistance + actionDuration))
                    }
        }

        return heuristicMap
    }

    fun printHeuristicMap() {
        PrintWriter("racetrack_heuristic", "UTF-8").use { writer ->
            val maxHeuristic = heuristicMap.entries.maxBy { it.value }!!.value
            writer.println(maxHeuristic)
            for (y in 0..height) {
                for (x in 0..width) {
                    val location = Location(x, y)
                    if (heuristicMap.containsKey(location)) {
                        val heuristic = heuristicMap[location]!!
                        writer.print((heuristic * 9 / maxHeuristic).toInt())
                    } else {
                        writer.print(".")
                    }
                }

                writer.println()
            }
        }
    }

    override fun successors(state: RaceTrackState): List<SuccessorBundle<RaceTrackState>> {
        val successors: MutableList<SuccessorBundle<RaceTrackState>> = arrayListOf()

        for (action in RaceTrackAction.values()) {
            val newDX = state.dX + action.aX
            val newDY = state.dY + action.aY

            if (newDX == 0 && newDY == 0) {
                // Identity and stop action
                successors.add(SuccessorBundle(
                        state = RaceTrackState(state.x, state.y, newDX, newDY),
                        action = action,
                        actionCost = actionDuration.toDouble())
                )
                continue
            }

            //filter on legal moves (not too fast and on the obstacles)
            if (isCollisionFree(state.x, state.y, newDX, newDY)) {
                successors.add(SuccessorBundle(
                        RaceTrackState(state.x + newDX, state.y + newDY, newDX, newDY),
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

            if (!isLegalLocation(xRunnung, yRunning)) {
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
    private fun isLegalLocation(x: Double, y: Double): Boolean {
        return x >= 0 && y >= 0 && x < width &&
                y < height && Location(Math.round(x).toInt(), Math.round(y).toInt()) !in obstacles
    }

    /*
    * Heuristic is the distance divided by the max speed
    * */
    override fun heuristic(state: RaceTrackState): Double {
        val dijkstraDistance = heuristicMap[Location(state.x, state.y)]
                ?: throw MetronomeException("No pre-calculated heuristic exists for state: $state")

        return dijkstraDistance / maxSpeed * actionDuration
    }

    override fun heuristic(startState: RaceTrackState, endState: RaceTrackState) = distance(startState, endState) * actionDuration

    /**
     * Calculates the minimum number of steps to reach the closest goal position.
     */
    override fun distance(state: RaceTrackState): Double {
        val distanceFunction: (Location) -> Double = { (x, y) -> max(abs(state.x - x) / max(maxXSpeed, maxYSpeed).toDouble(), abs(state.y - y) / max(maxYSpeed, maxXSpeed).toDouble()) }
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
     * obstacles = #
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
                            in obstacles -> '#'
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
        for ((x, y) in finishLine) {
            for (xS in 0..maxXSpeed) {
                for (yS in 0..maxYSpeed) {
                    when {
                        xS == 0 && yS == 0 -> list.add(RaceTrackState(x, y, xS, yS))
                        xS == 0 -> {
                            list.add(RaceTrackState(x, y, xS, yS))
                            list.add(RaceTrackState(x, y, xS, -yS))
                        }
                        yS == 0 -> {
                            list.add(RaceTrackState(x, y, xS, yS))
                            list.add(RaceTrackState(x, y, -xS, yS))
                        }
                        else -> {
                            list.add(RaceTrackState(x, y, xS, yS))
                            list.add(RaceTrackState(x, y, -xS, yS))
                            list.add(RaceTrackState(x, y, xS, -yS))
                            list.add(RaceTrackState(x, y, -xS, -yS))
                        }
                    }
                }
            }
        }

        return list
    }

    override fun predecessors(state: RaceTrackState) = successors(state)

    /**
     * The agent is safe when its velocity is zero.
     */
    override fun isSafe(state: RaceTrackState): Boolean = (state.dX == 0 && state.dY == 0) || isGoal(state)

    override fun randomizedStartState(state: RaceTrackState, seed: Long): RaceTrackState {
        val startLocation = Location(state.x, state.y)
        val goalDistance = heuristicMap[startLocation]
                ?: throw MetronomeException("Goal is not reachable from initial state.")
        val locations = ArrayList<Location>()
        heuristicMap.filter { (_, dist) -> dist in (goalDistance * 0.9)..(goalDistance) }
                .mapTo(locations, { it.key })

        locations[Random(seed).nextInt(locations.size)].let {
            return RaceTrackState(it.x, it.y, 0, 0)
        }
    }

    /**
     * Assuming that the acceleration of the agent is 1. To reach a safe state takes at least as many steps as the
     * maximum velocity of the agent over all dimensions.
     *
     * Int = max(abs(state.dX), abs(state.dY)
     * @return returns a lower bound on the number of steps to reach a safe state.
     */
    override fun safeDistance(state: RaceTrackState): Pair<Int, Int> = max(abs(state.dX), abs(state.dY)) to min(abs(state.dX), abs(state.dY))

    override fun getIdentityAction(state: RaceTrackState): SuccessorBundle<RaceTrackState>? = if (state.dX == 0 && state.dY == 0) SuccessorBundle(state, NO_OP, actionDuration.toDouble()) else null

    override fun transition(sourceState: RaceTrackState, action: Action): RaceTrackState? {
        val targetState = super.transition(sourceState, action)

        if (targetState != null) {
            val xVelocity = (sourceState.x - targetState.x).toDouble()
            val yVelocity = (sourceState.y - targetState.y).toDouble()
            velocities.add(hypot(xVelocity, yVelocity))
        }

        return targetState
    }

    override fun appendDomainSpecificResults(results: ExperimentResult) {
        results.averageVelocity = velocities.average()
    }
}