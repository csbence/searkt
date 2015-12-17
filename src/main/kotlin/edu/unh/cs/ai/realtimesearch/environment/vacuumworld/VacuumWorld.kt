package edu.unh.cs.ai.realtimesearch.environment.vacuumworld

import edu.unh.cs.ai.realtimesearch.environment.Domain
import edu.unh.cs.ai.realtimesearch.environment.State
import edu.unh.cs.ai.realtimesearch.environment.SuccessorBundle
import org.slf4j.LoggerFactory
import java.util.concurrent.ThreadLocalRandom

/**
 * The VacuumWorld is a problem where the agent, a vacuum cleaner, is supposed to clean
 * a specific area (grid of width by height) with possibly blocked cells. The actions are movement to each of
 * the four directions, or to vacuum.
 *
 * @param initialAmountDirty is used whenever a random state is generated to determine the amount of dirty cells
 */
class VacuumWorld(val width: Int, val height: Int, val blockedCells: List<VacuumWorldState.Location>,
                  val initialAmountDirty: Int = 1) : Domain {

    private val logger = LoggerFactory.getLogger("VacuumWorld")

    /**
     * Part of the Domain interface.
     */
    override fun successors(state: State): List<SuccessorBundle> {
        if (state is VacuumWorldState) {

            // to return
            val successors: MutableList<SuccessorBundle> = arrayListOf()

            VacuumWorldAction.values.forEach {
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
                            VacuumWorldState(newLocation, state.dirtyCells.filter { it != newLocation }.toSet()),
                            it,
                            1.0))
                }
            }

            logger.trace("State $state produces successors: $successors")
            return successors
        }

        throw RuntimeException("VacuumWorld only handles VacuumWorldStates")
    }

    override fun predecessors(state: State): List<SuccessorBundle> {
        if (state is VacuumWorldState) {

            // to return
            val predecessors: MutableList<SuccessorBundle> = arrayListOf()

            VacuumWorldAction.values.forEach {
                val newLocation = state.agentLocation - it.getRelativeLocation()

                // add the legal movement actions
                if (it != VacuumWorldAction.VACUUM) {
                    if (isLegalLocation(newLocation)) {
                        predecessors.add(SuccessorBundle(
                                VacuumWorldState(newLocation, state.dirtyCells),
                                it,
                                1.0)) // all actions have cost of 1

                    }
                } else if (newLocation !in state.dirtyCells) {
                    // no dirty means might have been dirty
                    predecessors.add(SuccessorBundle(
                            VacuumWorldState(newLocation, state.dirtyCells + newLocation),
                            it,
                            1.0))
                }
            }

            logger.trace("State $state produces predecessors: $predecessors")
            return predecessors
        }

        throw RuntimeException("VacuumWorld only handles VacuumWorldStates")
    }

    /**
     * Returns whether location within boundaries and not a blocked cell.
     *
     * @param location the location to test
     * @return true if location is legal
     */
    fun isLegalLocation(location: VacuumWorldState.Location): Boolean {
        return location.x >= 0 && location.y >= 0 && location.x < width &&
                location.y < height && location !in blockedCells
    }

    /**
     * Returns a heuristic for a vacuum world state: the amount of dirty cells left
     *
     * @param state is the state to provide a heuristic for
     * @return the # of dirty cells
     */
    override fun heuristic(state: State) =
            if (state is VacuumWorldState) state.dirtyCells.size.toDouble()
            else throw RuntimeException("VacuumWorld expects VacuumWorldState")

    /**
     * @TODO: document & implement
     */
    override fun distance(state: State): Double = .0

    /**
     * Returns whether the current state is a goal state.
     * Returns true if no dirty cells are present in the state.
     *
     * @param state: the state that is being checked on
     * @return whether the state is a goal state
     */
    override fun isGoal(state: State): Boolean {
        if (state is VacuumWorldState) {
            return state.dirtyCells.isEmpty()
        }

        throw RuntimeException("VacuumWorld only handles VacuumWorldStates")
    }


    override fun print(state: State): String {
        if (state is VacuumWorldState) {
            var description = ""
            for (h in 1..height) {
                for (w in 1..width) {
                    when (VacuumWorldState.Location(w, h)) {
                        state.agentLocation -> description += "@ "
                        in blockedCells -> description += "# "
                        in state.dirtyCells -> description += "$ "
                    }
                }
                description += "\n"
            }

            return description
        }

        throw RuntimeException("VacuumWorld only accepts VacuumWorldStates")
    }

    /**
     * Creates a state with a random initial location for the agent and
     * initialAmountDirty number of random dirty cells
     */
    override fun randomState(): State {
        val dirtyCells: MutableSet<VacuumWorldState.Location> = hashSetOf(randomLocation(width, height))

        while (dirtyCells.size < initialAmountDirty)
            dirtyCells.add(randomLocation(width, height))

        val randomState = VacuumWorldState(randomLocation(width, height), dirtyCells)
        logger.debug("Returning random state $randomState")

        return randomState
    }

    /**
     * Returns a random location within width and height
     */
    private fun randomLocation(width: Int, height: Int): VacuumWorldState.Location {
        return VacuumWorldState.Location(
                ThreadLocalRandom.current().nextInt(0, width),
                ThreadLocalRandom.current().nextInt(0, height)
        )
    }
}

