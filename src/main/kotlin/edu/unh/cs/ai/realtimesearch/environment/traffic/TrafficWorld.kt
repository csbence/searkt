package edu.unh.cs.ai.realtimesearch.environment.traffic

import edu.unh.cs.ai.realtimesearch.environment.Domain
import edu.unh.cs.ai.realtimesearch.environment.SuccessorBundle
import edu.unh.cs.ai.realtimesearch.environment.location.Location
import edu.unh.cs.ai.realtimesearch.environment.obstacle.MovingObstacle
import org.slf4j.LoggerFactory
import java.util.*

/**
 * TrafficWorld agent avoids being hit by moving obstacles
 * while reaching the specified goal
 * Created by doylew on 1/17/17.
 */
class TrafficWorld(val width: Int, val height: Int, var bunkers: Set<Location>, val targetLocation: Location, val actionDuration: Long) : Domain<TrafficWorldState> {
    private val logger = LoggerFactory.getLogger(TrafficWorld::class.java)

    private data class Pair(var x: Int, var y: Int)

    private val obstacleVelocities: ArrayList<Pair>
    private fun initializeObstacleVelocities(): ArrayList<Pair> {
        val velocities = ArrayList<Pair>()
        val random = Random()
        (0..99999).forEach {
            velocities.add(
                    if (random.nextBoolean()) Pair(random.nextInt(1) + 1, 0)
                    else Pair(0, random.nextInt(1) + 1)
            )
        }
        return velocities
    }

    init {
        obstacleVelocities = initializeObstacleVelocities()
    }

    /**
     * part of the Domain interface
     */
    override fun successors(state: TrafficWorldState): List<SuccessorBundle<TrafficWorldState>> {
        val successors: MutableList<SuccessorBundle<TrafficWorldState>> = arrayListOf()
        val newObstacles = moveObstacles(state.obstacles)

        for (action in TrafficWorldAction.values()) {
            val newLocation = state.agentLocation + TrafficWorldAction.getRelativeLocation(action)

            if (isLegalLocation(state, newLocation)) {
                successors.add(
                        SuccessorBundle(
                                TrafficWorldState(newLocation, newObstacles),
                                action,
                                actionCost = actionDuration
                        )
                )
            }
        }
        return successors
    }

    /**
     * returns if a given newLocation is legal or not
     *
     * @param newLocation the test newLocation
     * @return true if newLocation is legal
     */
    fun isLegalLocation(state: TrafficWorldState, newLocation: Location): Boolean {
        if (newLocation.x in 0..(width - 1)) {
            if (newLocation.y in 0..(height - 1)) {
                if (!containsLocation(Location(x = newLocation.x, y = newLocation.y), state)) {
                    return true
                }
            }
        }
        return false
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
        return Math.abs(startState.agentLocation.x - endState.agentLocation.x) +
                Math.abs(startState.agentLocation.y - endState.agentLocation.y).toDouble()
    }

    /**
     * estimated distance to goal
     */
    override fun distance(state: TrafficWorldState): Double {
       return state.run { agentLocation.manhattanDistance(targetLocation).toDouble() }
    }

    /**
     * basic goal test function
     */
    override fun isGoal(state: TrafficWorldState): Boolean {
        return state.agentLocation == targetLocation
    }


    private fun containsLocation(candidateLocation: Location, state: TrafficWorldState) : Boolean {
        return state.obstacles.filter { candidateLocation.x == it.x && candidateLocation.y == it.y }.isNotEmpty()
    }

    /**
     * visualization ASCII sssstyle
     * @ == agent
     * # == obstacle
     * $ == bunkers
     * * == goal
     */
    override fun print(state: TrafficWorldState): String {
        val output = StringBuilder()
        (0..height-1).forEach { y ->
            (0..width-1).forEach { x ->
                var character = when (Location(x,y)) {
                   state.agentLocation -> '@'
                    targetLocation -> '*'
                    in bunkers -> '$'
                    else -> '_'
                }
                if (containsLocation(Location(x,y), state)) {
                  character = '#'
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
    private fun moveObstacles(obstacles: Set<MovingObstacle>): Set<MovingObstacle> {
        val newObstacles = mutableSetOf<MovingObstacle>()
        obstacles.forEachIndexed { i, (x, y, dx, dy) ->
            val oldObstacleLocation = MovingObstacle(x, y, dx, dy)
            val newObstacleLocation = MovingObstacle(x + dx, y + dy, dx, dy)
            if (bunkers.contains(Location(x + dx, y + dy)) || !validObstacleLocation(Location(x + dx, y + dy)) ||
                    (targetLocation.x == newObstacleLocation.x && targetLocation.y == newObstacleLocation.y)) {
                // if the new obstacle obstacle would be a bunker
                // or the goal (target) obstacle and is valid
                // add the old obstacle and bounce the velocities
                newObstacles.add(oldObstacleLocation)
                obstacleVelocities[i].x *= -1
                obstacleVelocities[i].y *= -1
            } else {
                newObstacles.add(newObstacleLocation)
            }
        }
        return newObstacles
    }

    /**
     * validates a movement of an obstacle
     * essentially bounds checking
     *
     * @param obstacle the obstacle to test
     * @return true if the new position is ok
     */
    private fun validObstacleLocation(obstacle: Location): Boolean {
        if (obstacle.x >= 0 && obstacle.x < width) {
            if (obstacle.y >= 0 && obstacle.y < height) {
                return true
            }
        }
        return false
    }
}