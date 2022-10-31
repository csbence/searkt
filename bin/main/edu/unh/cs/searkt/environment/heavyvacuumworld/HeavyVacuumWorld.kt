package edu.unh.cs.searkt.environment.heavyvacuumworld

import edu.unh.cs.searkt.environment.Domain
import edu.unh.cs.searkt.environment.SuccessorBundle
import edu.unh.cs.searkt.environment.location.Location
import java.util.concurrent.ThreadLocalRandom
import kotlin.math.abs

/**
 * The VacuumWorld is a problem where the agent, a vacuum cleaner, is supposed to clean
 * a specific area (grid of width by height) with possibly blocked cells. The actions are movement to each of
 * the four directions, or to vacuum.
 *
 * @param initialAmountDirty is used whenever a random state is generated to determine the amount of dirty cells
 */
class HeavyVacuumWorld(val width: Int,
                       val height: Int,
                       val blockedCells: Set<Location>,
                       private val initialAmountDirty: Int) : Domain<HeavyVacuumWorldState> {

    /**
     * Part of the Domain interface.
     */
    override fun successors(state: HeavyVacuumWorldState): List<SuccessorBundle<HeavyVacuumWorldState>> {
        // to return
        val successors: MutableList<SuccessorBundle<HeavyVacuumWorldState>> = arrayListOf()

        for (it in HeavyVacuumWorldAction.values()) {
            val newLocation = state.agentLocation + it.getRelativeLocation()

            // add the legal movement actions
            if (it != HeavyVacuumWorldAction.VACUUM) {
                if (isLegalLocation(newLocation)) {
                    successors.add(SuccessorBundle(
                            HeavyVacuumWorldState(newLocation, state.dirtyCells, calculateHeuristic(newLocation, state.dirtyCells)),
                            it,
                            ((initialAmountDirty + 1) - state.dirtyCells.size).toDouble()))
                    // all actions have cost of how ever much dirt the robot has picked up

                }
            } else if (newLocation in state.dirtyCells) {
                // add legit vacuum action
                val newDirtyCells = state.dirtyCells.filter { it != newLocation }
                successors.add(SuccessorBundle(
                        // TODO: inefficient? nah it seems alright
                        HeavyVacuumWorldState(newLocation, newDirtyCells, calculateHeuristic(newLocation, newDirtyCells)),
                        it,
                        ((initialAmountDirty + 1) - newDirtyCells.size).toDouble()))
            }
        }
        //        logger.trace { "State $state produces successors: $successors" }
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
    override fun heuristic(state: HeavyVacuumWorldState): Double {
        return state.heuristic //state.dirtyCells.size.toDouble()
    }

    //TODO: Create heuristic between two states for vacuumworld
    override fun heuristic(startState: HeavyVacuumWorldState, endState: HeavyVacuumWorldState): Double {
        return 0.0
    }

    /**
     * Goal distance estimate. Equal to the cost when the cost of each edge is one.
     */
    override fun distance(state: HeavyVacuumWorldState) = state.heuristic //heuristic(state)

    /**
     * Returns whether the current state is a goal state.
     * Returns true if no dirty cells are present in the state.
     *
     * @param state: the state that is being checked on
     * @return whether the state is a goal state
     */
    override fun isGoal(state: HeavyVacuumWorldState): Boolean {
        return state.dirtyCells.isEmpty()
    }

    /**
     * Simply prints the block grid.
     *
     * @ == agent
     * # == blocked
     * $ == dirty
     */
    override fun print(state: HeavyVacuumWorldState): String {
        val description = StringBuilder()
        for (h in 0 until height) {
            for (w in 0 until width) {
                val charCell = when (Location(w, h)) {
                    state.agentLocation -> '@'
                    in blockedCells -> '#'
                    in state.dirtyCells -> '*'
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
    fun randomState(): HeavyVacuumWorldState {
        val dirtyCells = arrayListOf(randomLocation(width, height))

        // generate random locations until enough
        // do not add those that would be in blocked cells
        while (dirtyCells.size < initialAmountDirty) {
            val randomLocation = randomLocation(width, height)
            if (randomLocation !in blockedCells)
                dirtyCells.add(randomLocation)
        }

        val randomLocation = randomLocation(width, height)
        val randomState = HeavyVacuumWorldState(randomLocation, dirtyCells, calculateHeuristic(randomLocation, dirtyCells))

        return randomState
    }

    /**
     * Returns a random location within width and height
     */
    private fun randomLocation(width: Int, height: Int): Location {
        return Location(
                ThreadLocalRandom.current().nextInt(0, width),
                ThreadLocalRandom.current().nextInt(0, height)
        )
    }

    override fun getGoals(): List<HeavyVacuumWorldState> {
        throw UnsupportedOperationException()
    }

    override fun predecessors(state: HeavyVacuumWorldState): List<SuccessorBundle<HeavyVacuumWorldState>> {
        throw UnsupportedOperationException()
    }

    private fun shortestManhattanToDirt(state: Location, dirtyCells: List<Location>): Pair<Double, Location> {
        val agentX = state.x
        val agentY = state.y

        var closestDirtyLocation = dirtyCells.first()
        var shortestPathToDirtyCell = Double.MAX_VALUE

        dirtyCells.forEach { dirt ->
            val pathToDirtyCell = abs(dirt.x - agentX) + abs(dirt.y - agentY)
            if (pathToDirtyCell < shortestPathToDirtyCell) {
                shortestPathToDirtyCell = pathToDirtyCell.toDouble()
                closestDirtyLocation = dirt
            }
        }

        return Pair(shortestPathToDirtyCell, closestDirtyLocation)

    }

    fun calculateHeuristic(state: Location, dirtyCells: List<Location>): Double {
        val dirtCells = mutableListOf<Location>()
        dirtyCells.forEach { dirtCells.add(it) }
        var previousLocation = state
        var heuristic = 0.0
        (0 until dirtyCells.size).forEach {
            val closestDirt = shortestManhattanToDirt(previousLocation, dirtCells)
            heuristic += closestDirt.first * ((initialAmountDirty + 1) - dirtyCells.size)
            dirtCells.remove(closestDirt.second)
            previousLocation = closestDirt.second
        }
        return heuristic
    }

    fun testHeuristic(state: Location, dirtyCells: List<Location>): Double {
        val dirtCells = mutableListOf<Location>()
        dirtyCells.forEach { dirtCells.add(it) }
        var previousLocation = state
        var heuristic = 0.0
        (0 until dirtyCells.size).forEach {
            val closestDirt = shortestManhattanToDirt(previousLocation, dirtCells)
            heuristic += closestDirt.first * ((initialAmountDirty + 1) - dirtCells.size)
            dirtCells.remove(closestDirt.second)
            previousLocation = closestDirt.second
        }
        return heuristic
    }
}

