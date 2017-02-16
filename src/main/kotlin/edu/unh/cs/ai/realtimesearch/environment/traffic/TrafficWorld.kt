package edu.unh.cs.ai.realtimesearch.environment.traffic

import edu.unh.cs.ai.realtimesearch.environment.Domain
import edu.unh.cs.ai.realtimesearch.environment.SuccessorBundle
import edu.unh.cs.ai.realtimesearch.environment.location.Location
import edu.unh.cs.ai.realtimesearch.environment.obstacle.MovingObstacle
import edu.unh.cs.ai.realtimesearch.environment.obstacle.toLocationSet
import org.slf4j.LoggerFactory
import java.lang.Math.abs

/**
 * TrafficWorld agent avoids being hit by moving obstacles
 * while reaching the specified goal
 * Created by doylew on 1/17/17.
 */
class TrafficWorld(val width: Int, val height: Int, var bunkers: Set<Location>, val goal: Location, val actionDuration: Long, obstacles: List<MovingObstacle>) : Domain<TrafficWorldState> {
    private val logger = LoggerFactory.getLogger(TrafficWorld::class.java)
    private val movingObstacles: List<MovingObstacle> = obstacles.map { it }
    private val obstacleTimeSequence: MutableList<Set<Location>> = arrayListOf(movingObstacles.toLocationSet())

    override fun safeDistance(state: TrafficWorldState): Pair<Int, Int> {
        val distanceFunction: (Location) -> Int = { (x, y) -> Math.max(abs(state.location.x - x), abs(state.location.y - y)) }
        return distanceFunction(bunkers.minBy(distanceFunction)!!) to 0
    }

    /**
     * part of the Domain interface - isSafe function
     *
     * predicate that tells whether a state is safe or not
     * in Traffic this means the agent is in a bunker
     * @param state the state under consideration
     */
    override fun isSafe(state: TrafficWorldState): Boolean = state.location in bunkers || isGoal(state)

    /**
     * part of the Domain interface - successor function
     * @param state the state for successor calculation
     */
    override fun successors(state: TrafficWorldState): List<SuccessorBundle<TrafficWorldState>> {
        val successors: MutableList<SuccessorBundle<TrafficWorldState>> = arrayListOf()

        val timestamp = state.timeStamp + 1
        val movedObstacles = getObstacles(timestamp)

        for (action in TrafficWorldAction.values()) {
            val newLocation = state.location + TrafficWorldAction.getRelativeLocation(action)

            if (isLegalLocation(newLocation, movedObstacles)) {
                successors.add(
                        SuccessorBundle(
                                TrafficWorldState(newLocation, timestamp),
                                action,
                                actionCost = actionDuration
                        )
                )
            }
        }
        return successors
    }

    private fun getObstacles(timestamp: Int): Set<Location> {
        if (obstacleTimeSequence.size <= timestamp) {
            // Add one more time step
            moveObstacles(movingObstacles)
            obstacleTimeSequence.add(movingObstacles.toLocationSet())
        }

        return obstacleTimeSequence[timestamp]
    }

    /**
     * checks a given state is out-of-bounds of the world
     * or is in collision with an obstacle
     *
     * @param location the test location
     * @return true if location is legal false otherwise
     */
    fun isLegalLocation(location: Location, obstacles: Set<Location>): Boolean {
        return location.x in 0..(width - 1) &&
                location.y in 0..(height - 1) &&
                location !in obstacles
    }

    /**
     * returns the heuristic manhattan distance
     *
     * @param state the test state
     * @return value representing Manhattan to goal
     */
    override fun heuristic(state: TrafficWorldState): Double {
        return distance(state) + actionDuration
    }

    override fun heuristic(startState: TrafficWorldState, endState: TrafficWorldState): Double {
        return Math.abs(startState.location.x - endState.location.x) +
                Math.abs(startState.location.y - endState.location.y).toDouble()
    }

    /**
     * estimated distance to goal
     * @param state the state under distance measurement
     */
    override fun distance(state: TrafficWorldState): Double {
        return state.run { location.manhattanDistance(goal).toDouble() }
    }

    /**
     * basic goal test function
     * @param state the state under goal test
     */
    override fun isGoal(state: TrafficWorldState): Boolean {
        return state.location == goal
    }

    /**
     * helper function to detect if a location is
     * contained within the obstacles for printing
     * and legal location checking
     * @param candidateLocation possible location of the obstacle
     */
    private fun containsObstacle(candidateLocation: Location, newObstacles: Set<MovingObstacle>): Boolean {
        return newObstacles.any { candidateLocation.x == it.x && candidateLocation.y == it.y }
    }

    /**
     * visualization ASCII style
     * @ == agent
     * # == obstacle
     * $ == bunkers
     * * == goal
     */
    override fun print(state: TrafficWorldState): String {
        val output = StringBuilder()
        (0..height - 1).forEach { y ->
            (0..width - 1).forEach { x ->
                val character = when (Location(x, y)) {
                    state.location -> '@'
                    goal -> '*'
                    in bunkers -> '$'
                    in obstacleTimeSequence[state.timeStamp] -> '#'
                    else -> '_'
                }
                output.append(character)
            }
            output.append("\n")
        }
        return output.toString()
    }

    /**
     * moves the blockedCells (obstacles) each time
     * a new successor is created
     *
     * @param obstacles the obstacles to be moved
     * @return the moved obstacles
     */
    private fun moveObstacles(obstacles: List<MovingObstacle>) {
        obstacles.forEach { obstacle ->
            val newLocation = Location(obstacle.x + obstacle.dx, obstacle.y + obstacle.dy)
            if (newLocation in bunkers || !isInBounds(newLocation) || goal == newLocation) {
                // Invert velocity if target location is not available
                obstacle.invertVelocity()
            } else {
                obstacle.x += obstacle.dx
                obstacle.y += obstacle.dy
            }
        }
    }

    /**
     * validates a movement of an location
     * essentially bounds checking
     *
     * @param location the location to test
     * @return true if the new position is ok
     */
    private fun isInBounds(location: Location): Boolean = location.x in 0..(width - 1) && location.y in 0..(height - 1)
}