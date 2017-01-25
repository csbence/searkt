package edu.unh.cs.ai.realtimesearch.environment.traffic

import edu.unh.cs.ai.realtimesearch.environment.Action
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

    init {
        logger.info("TrafficWorld starting...")
    }
    /**
     * part of the Domain interface - successor function
     * @param state the state for successor calculation
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
//
//    override fun transition(sourceState: TrafficWorldState, action: Action): TrafficWorldState? {
//        val movedObstacles = moveObstacles(sourceState.obstacles)
//
//        if (action == TrafficWorldAction.UP) {
//           val candidateState = TrafficWorldState(Location(sourceState.agentLocation.x, sourceState.agentLocation.y -1), movedObstacles)
//            if (isLegalLocation(candidateState)) {
//                return candidateState
//            }
//        }
//    }

    /**
     * checks a given state is out-of-bounds of the world
     * or is in collision with an obstacle
     *
     * @param newLocation the test newLocation
     * @return true if newLocation is legal false otherwise
     */
    fun isLegalLocation(state: TrafficWorldState, newLocation: Location): Boolean {
        if (newLocation.x in 0..(width - 1)) {
            if (newLocation.y in 0..(height - 1)) {
                if (!containsObstacle(Location(x = newLocation.x, y = newLocation.y), state)) {
                    if (state.agentLocation != newLocation) {
                        return true
                    }
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
     * @param state the state under distance measurement
     */
    override fun distance(state: TrafficWorldState): Double {
       return state.run { agentLocation.manhattanDistance(targetLocation).toDouble() }
    }

    /**
     * basic goal test function
     * @param state the state under goal test
     */
    override fun isGoal(state: TrafficWorldState): Boolean {
        return state.agentLocation == targetLocation
    }

    /**
     * helper function to detect if a location is
     * contained within the obstacles for printing
     * and legal location checking
     * @param candidateLocation possible location of the obstacle
     * @param state the state under question of containing the obstacle location
     */
    private fun containsObstacle(candidateLocation: Location, state: TrafficWorldState) : Boolean {
        return state.obstacles.any{ candidateLocation.x == it.x && candidateLocation.y == it.y }
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
                if (containsObstacle(Location(x,y), state)) {
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
        obstacles.forEach{ (x, y, dx, dy) ->
            val oldObstacleLocation = MovingObstacle(x, y, dx, dy)
            val newObstacleLocation = MovingObstacle(x + dx, y + dy, dx, dy)
            if (bunkers.contains(Location(x + dx, y + dy)) || !validObstacleLocation(Location(x + dx, y + dy)) ||
                    (targetLocation.x == newObstacleLocation.x && targetLocation.y == newObstacleLocation.y)) {
                // if the new obstacle obstacle would be a bunker
                // or the goal (target) obstacle and is valid
                // add the old obstacle and bounce the velocities
                newObstacles.add(oldObstacleLocation)
                oldObstacleLocation.dx *= -1
                oldObstacleLocation.dy *= -1
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
        if (obstacle.x in 0..(width - 1)) {
            if (obstacle.y in 0..(height - 1)) {
                return true
            }
        }
        return false
    }
}