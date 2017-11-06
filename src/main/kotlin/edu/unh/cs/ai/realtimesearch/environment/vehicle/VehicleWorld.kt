package edu.unh.cs.ai.realtimesearch.environment.vehicle

import edu.unh.cs.ai.realtimesearch.environment.Domain
import edu.unh.cs.ai.realtimesearch.environment.SuccessorBundle
import edu.unh.cs.ai.realtimesearch.environment.location.Location
import edu.unh.cs.ai.realtimesearch.logging.LoggerFactory
import java.util.*

/**
 * VehicleWorld agent avoids being hit by moving obstacles
 * while reaching the specified goal
 * Created by doylew on 1/17/17.
 */
class VehicleWorld(val width: Int, val height: Int, var blockedCells: Set<Location>, var bunkers: Set<Location>, val targetLocation: Location, val actionDuration: Long) : Domain<VehicleWorldState> {
    private val logger = LoggerFactory.getLogger(VehicleWorld::class.java)

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
    override fun successors(state: VehicleWorldState): List<SuccessorBundle<VehicleWorldState>> {
        val successors: MutableList<SuccessorBundle<VehicleWorldState>> = arrayListOf()
        blockedCells = moveObstacles(blockedCells)

        for (action in VehicleWorldAction.values()) {
            val newLocation = state.agentLocation + action.getRelativeLocation()

            if (isLegalLocation(newLocation)) {
                successors.add(
                        SuccessorBundle(
                                VehicleWorldState(newLocation),
                                action,
                                actionCost = actionDuration
                        )
                )
            }
        }
        return successors
    }

    /**
     * returns if a given location is legal or not
     *
     * @param location the test location
     * @return true if location is legal
     */
    fun isLegalLocation(location: Location): Boolean {
        if (location.x >= 0 && location.x < width) {
            if (location.y >= 0 && location.y < height) {
                if (!blockedCells.contains(Location(location.x, location.y))) {
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
    override fun heuristic(state: VehicleWorldState): Double {
        return distance(state) + actionDuration
    }

    override fun heuristic(startState: VehicleWorldState, endState: VehicleWorldState): Double {
        return Math.abs(startState.agentLocation.x - endState.agentLocation.x) +
                Math.abs(startState.agentLocation.y - endState.agentLocation.y).toDouble()
    }

    /**
     * estimated distance to goal
     */
    override fun distance(state: VehicleWorldState): Double {
       return state.run { agentLocation.manhattanDistance(targetLocation).toDouble() }
    }

    /**
     * basic goal test function
     */
    override fun isGoal(state: VehicleWorldState): Boolean {
        return state.agentLocation == targetLocation
    }

    /**
     * visualization ASCII sssstyle
     * @ == agent
     * # == obstacle
     * $ == bunkers
     * * == GOOOOOOOLLLLLLLLLLLLL
     */
    override fun print(state: VehicleWorldState): String {
        val output = StringBuilder()
        (0..height-1).forEach { y ->
            (0..width-1).forEach { x ->
                val character = when (Location(x,y)) {
                   state.agentLocation -> '@'
                    targetLocation -> '*'
                    in blockedCells -> '#'
                    in bunkers -> '$'
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
    private fun moveObstacles(obstacles: Set<Location>): Set<Location> {
        val newObstacles = mutableSetOf<Location>()
        obstacles.forEachIndexed { i, location ->
            val oldObstacleLocation = Location(location.x, location.y)
            val newObstacleLocation = Location(location.x + obstacleVelocities[i].x,
                    location.y + obstacleVelocities[i].y)
            if (bunkers.contains(newObstacleLocation) || !validObstacleLocation(newObstacleLocation) ||
                    (targetLocation.x == newObstacleLocation.x && targetLocation.y == newObstacleLocation.y)) {
                // if the new obstacle location would be a bunker
                // or the goal (target) location and is valid
                // add the old location and bounce the velocities
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