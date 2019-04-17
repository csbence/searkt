package edu.unh.cs.searkt.environment.lifegrids

import edu.unh.cs.searkt.environment.Domain
import edu.unh.cs.searkt.environment.SuccessorBundle
import edu.unh.cs.searkt.environment.location.Location

/**
 * The GridWorld is a problem where the agent, a robot, is supposed to reach
 * a specific area (grid of width by height) with possibly blocked cells. The actions are movement to each of
 * the four direction.
 *
 */
class Lifegrids(val width: Int, val height: Int, val blockedCells: Set<Location>, val targetLocation: Location, val actionDuration: Long) : Domain<LifegridsState> {

    override fun isSafe(state: LifegridsState): Boolean {
        return false
    }

    /**
     * Part of the Domain interface.
     */
    override fun successors(state: LifegridsState): List<SuccessorBundle<LifegridsState>> {
        val successors: MutableList<SuccessorBundle<LifegridsState>> = arrayListOf()

        for (action in LifegridsAction.values()) {
            val newLocation = state.agentLocation + action.getRelativeLocation()

            if (isLegalLocation(newLocation)) {
                successors.add(SuccessorBundle(
                        LifegridsState(newLocation, calculateHeuristic(newLocation)),
                        action,
                        actionCost = state.agentLocation.y.toDouble()))
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
    fun isLegalLocation(location: Location): Boolean {
        return location.x >= 0 && location.y >= 0 && location.x < width &&
                location.y < height && location !in blockedCells
    }

    /**
     * Returns a heuristic for a vacuum world state: the amount of dirty cells left
     *
     * @param state is the state to provide a heuristic for
     * @return the # of dirty cells
     */
    override fun heuristic(state: LifegridsState): Double {
        return state.heuristic
    }

    fun calculateHeuristic(location: Location): Double {
        val xManhattan = Math.abs(location.x - targetLocation.x)
        val yManhattan = Math.abs(location.y - targetLocation.y)

        var yManhattanCost = 0.0
        for (y in yManhattan downTo 0) {
            yManhattanCost += y
        }

        val lShapeDistance = ((xManhattan * location.y) + yManhattanCost)

        var yLocUp = 0.0
        for (y in location.y downTo 0) {
            yLocUp += y
        }
        var yLocDown = 0.0
        for (y in targetLocation.y downTo 0) {
            yLocDown += y
        }
        val nShapeDistance = yLocUp + yLocDown

        return minOf(lShapeDistance, nShapeDistance)
    }

    override fun heuristic(startState: LifegridsState, endState: LifegridsState) =
            startState.agentLocation.manhattanDistance(endState.agentLocation) * actionDuration.toDouble()

    /**
     * Goal distance estimate. Equal to the cost when the cost of each edge is one.
     */
    override fun distance(state: LifegridsState): Double {
        val xManhattan = Math.abs(state.agentLocation.x - targetLocation.x)
        val yManhattan = Math.abs(state.agentLocation.y - targetLocation.y)

        val lShapeDistance = xManhattan + yManhattan
        val nShapeDistance = state.agentLocation.y + targetLocation.y

        return minOf(lShapeDistance, nShapeDistance).toDouble()
    }

    /**
     * Returns whether the current state is a goal state.
     * Returns true if no dirty cells are present in the state.
     *
     * @param state: the state that is being checked on
     * @return whether the state is a goal state
     */
    override fun isGoal(state: LifegridsState): Boolean {
        return state.agentLocation == targetLocation
    }

    /**
     * Simply prints the block grid.
     *
     * @ == agent
     * # == blocked
     * * == goal
     */
    override fun print(state: LifegridsState): String {
        val description = StringBuilder()
        for (h in 1..height) {
            for (w in 1..width) {
                val charCell = when (Location(w, h)) {
                    state.agentLocation -> '@'
                    targetLocation -> '*'
                    in blockedCells -> '#'
                    else -> '_'
                }
                description.append(charCell)
            }
            description.append("\n")
        }

        return description.toString()
    }

    fun randomState(): LifegridsState {
        throw UnsupportedOperationException("not implemented")
    }

    override fun getGoals(): List<LifegridsState> {
        return listOf(LifegridsState(targetLocation, calculateHeuristic(targetLocation)))
    }

    override fun predecessors(state: LifegridsState): List<SuccessorBundle<LifegridsState>> {
        val predecessors: MutableList<SuccessorBundle<LifegridsState>> = arrayListOf()

        for (action in LifegridsAction.values()) {
            val newLocation = state.agentLocation - action.getRelativeLocation()

            if (isLegalLocation(newLocation)) {
                predecessors.add(SuccessorBundle(
                        LifegridsState(newLocation, calculateHeuristic(newLocation)),
                        action,
                        actionCost = state.agentLocation.y.toDouble()))
            }
        }

        return predecessors
    }
}

