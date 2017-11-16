package edu.unh.cs.ai.realtimesearch.environment.gridworld

import edu.unh.cs.ai.realtimesearch.environment.Domain
import edu.unh.cs.ai.realtimesearch.environment.SuccessorBundle
import edu.unh.cs.ai.realtimesearch.environment.location.Location
import edu.unh.cs.ai.realtimesearch.environment.vacuumworld.GridWorldAction
import org.slf4j.LoggerFactory

/**
 * The VacuumWorld is a problem where the agent, a vacuum cleaner, is supposed to clean
 * a specific area (grid of width by height) with possibly blocked cells. The actions are movement to each of
 * the four directions, or to vacuum.
 *
 */
class GridWorld(val width: Int, val height: Int, val blockedCells: Set<Location>, val targetLocation: Location, val actionDuration: Long) : Domain<GridWorldState> {
    private val logger = LoggerFactory.getLogger(GridWorld::class.java)

    override fun isSafe(state: GridWorldState): Boolean = false

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
    override fun heuristic(state: GridWorldState) = distance(state) * actionDuration

    override fun heuristic(startState: GridWorldState, endState: GridWorldState): Double {
        return Math.abs(startState.agentLocation.x - endState.agentLocation.x) + Math.abs(startState.agentLocation.y - endState.agentLocation.y).toDouble()
        //        return state.run { location.manhattanDistance(goal).toDouble() }
    }

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
            (1..width)
                    .map {
                        when (Location(it, h)) {
                            state.agentLocation -> '@'
                            targetLocation -> '*'
                            in blockedCells -> '#'
                            else -> '_'
                        }
                    }
                    .forEach { description.append(it) }
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

    override fun getGoals(): List<GridWorldState> = listOf(GridWorldState(targetLocation))

    override fun predecessors(state: GridWorldState): List<SuccessorBundle<GridWorldState>> {
        val predecessors: MutableList<SuccessorBundle<GridWorldState>> = arrayListOf()

        for (action in GridWorldAction.values()) {
            val newLocation = state.agentLocation - action.getRelativeLocation()

            if (isLegalLocation(newLocation)) {
                predecessors.add(SuccessorBundle(
                        GridWorldState(newLocation),
                        action,
                        actionCost = 1))
            }
        }

        return predecessors
    }
}

