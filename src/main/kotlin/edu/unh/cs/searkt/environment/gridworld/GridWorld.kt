package edu.unh.cs.searkt.environment.gridworld

import edu.unh.cs.searkt.environment.Domain
import edu.unh.cs.searkt.environment.SuccessorBundle
import edu.unh.cs.searkt.environment.location.Location
import edu.unh.cs.searkt.environment.vacuumworld.GridWorldAction

/**
 * The GridWorld is a problem where the agent, a robot, is supposed to reach
 * a specific area (grid of width by height) with possibly blocked cells. The actions are movement to each of
 * the four direction.
 *
 */
class GridWorld(val width: Int, val height: Int, val blockedCells: Set<Location>, val targetLocation: Location, val actionDuration: Long) : Domain<GridWorldState> {

    override fun isSafe(state: GridWorldState): Boolean {
        return false
    }

    /**
     * Part of the Domain interface.
     */
    override fun successors(state: GridWorldState): List<SuccessorBundle<GridWorldState>> {
        val successors: MutableList<SuccessorBundle<GridWorldState>> = arrayListOf()

        for (action in GridWorldAction.values()) {
            val newLocation = state.agentLocation + action.getRelativeLocation()

            if (isLegalLocation(newLocation)) {
                successors.add(SuccessorBundle(
                        GridWorldState(newLocation),
                        action,
                        actionCost = actionDuration.toDouble()))
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
    override fun heuristic(state: GridWorldState) = state.agentLocation.manhattanDistance(targetLocation).toDouble()

    override fun heuristic(startState: GridWorldState, endState: GridWorldState) =
            startState.agentLocation.manhattanDistance(endState.agentLocation) * actionDuration.toDouble()

    /**
     * Goal distance estimate. Equal to the cost when the cost of each edge is one.
     */
    override fun distance(state: GridWorldState) = state.run { agentLocation.manhattanDistance(targetLocation).toDouble() }

    /**
     * Returns whether the current state is a goal state.
     * Returns true if no dirty cells are present in the state.
     *
     * @param state: the state that is being checked on
     * @return whether the state is a goal state
     */
    override fun isGoal(state: GridWorldState): Boolean {
        return state.agentLocation == targetLocation
    }

    /**
     * Simply prints the block grid.
     *
     * @ == agent
     * # == blocked
     * $ == dirty
     */
    override fun print(state: GridWorldState): String {
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

    /**
     * Creates a state with a random initial location for the agent and
     * initialAmountDirty number of random dirty cells
     */
    fun randomState(): GridWorldState {
        throw UnsupportedOperationException("not implemented")
    }

    override fun getGoals(): List<GridWorldState> {
        return listOf(GridWorldState(targetLocation))
    }

    override fun predecessors(state: GridWorldState): List<SuccessorBundle<GridWorldState>> = successors(state)
}

