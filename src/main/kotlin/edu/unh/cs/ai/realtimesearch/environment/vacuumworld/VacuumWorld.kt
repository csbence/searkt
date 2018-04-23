package edu.unh.cs.ai.realtimesearch.environment.vacuumworld

import edu.unh.cs.ai.realtimesearch.environment.Domain
import edu.unh.cs.ai.realtimesearch.environment.SuccessorBundle
import edu.unh.cs.ai.realtimesearch.environment.location.Location
import edu.unh.cs.ai.realtimesearch.logging.debug
import org.slf4j.LoggerFactory
import java.util.concurrent.ThreadLocalRandom

/**
 * The VacuumWorld is a problem where the agent, a vacuum cleaner, is supposed to clean
 * a specific area (grid of width by height) with possibly blocked cells. The actions are movement to each of
 * the four directions, or to vacuum.
 *
 * @param initialAmountDirty is used whenever a random state is generated to determine the amount of dirty cells
 */
class VacuumWorld(val width: Int,
                  val height: Int,
                  val blockedCells: Set<Location>,
                  val initialAmountDirty: Int = 1) : Domain<VacuumWorldState> {

    private val logger = LoggerFactory.getLogger(VacuumWorld::class.java)

    /**
     * Part of the Domain interface.
     */
    override fun successors(state: VacuumWorldState): List<SuccessorBundle<VacuumWorldState>> {
        // to return
        val successors: MutableList<SuccessorBundle<VacuumWorldState>> = arrayListOf()

        for (it in VacuumWorldAction.values()) {
            val newLocation = state.agentLocation + it.getRelativeLocation()

            // add the legal movement actions
            if (it != VacuumWorldAction.VACUUM) {
                if (isLegalLocation(newLocation)) {
                    successors.add(SuccessorBundle(
                            VacuumWorldState(newLocation, state.dirtyCells),
                            it,
                            1.0)) // all actions have cost of 1

                }
            } else if (newLocation in state.dirtyCells) {
                // add legit vacuum action
                successors.add(SuccessorBundle(
                        // TODO: inefficient?
                        VacuumWorldState(newLocation, state.dirtyCells.filter { it != newLocation }),
                        it,
                        1.0))
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
    override fun heuristic(state: VacuumWorldState): Double {
        return state.dirtyCells.size.toDouble()
    }

    //TODO: Create heuristic between two states for vacuumworld
    override fun heuristic(startState: VacuumWorldState, endState: VacuumWorldState): Double {
        return 0.0
    }

    /**
     * Goal distance estimate. Equal to the cost when the cost of each edge is one.
     */
    override fun distance(state: VacuumWorldState) = heuristic(state)

    /**
     * Returns whether the current state is a goal state.
     * Returns true if no dirty cells are present in the state.
     *
     * @param state: the state that is being checked on
     * @return whether the state is a goal state
     */
    override fun isGoal(state: VacuumWorldState): Boolean {
        return state.dirtyCells.isEmpty()
    }

    /**
     * Simply prints the block grid.
     *
     * @ == agent
     * # == blocked
     * $ == dirty
     */
    override fun print(state: VacuumWorldState): String {
        val description = StringBuilder()
        for (h in 1..height) {
            for (w in 1..width) {
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
    fun randomState(): VacuumWorldState {
        val dirtyCells = arrayListOf(randomLocation(width, height))

        // generate random locations until enough
        // do not add those that would be in blocked cells
        while (dirtyCells.size < initialAmountDirty) {
            val randomLocation = randomLocation(width, height)
            if (randomLocation !in blockedCells)
                dirtyCells.add(randomLocation)
        }

        val randomState = VacuumWorldState(randomLocation(width, height), dirtyCells)
        logger.debug { "Returning random state $randomState" }

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

    override fun getGoals(): List<VacuumWorldState> {
        throw UnsupportedOperationException()
    }

    override fun predecessors(state: VacuumWorldState): List<SuccessorBundle<VacuumWorldState>> {
        throw UnsupportedOperationException()
    }
}

