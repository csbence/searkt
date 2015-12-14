package edu.unh.cs.ai.realtimesearch.environment.vacuumworld

import edu.unh.cs.ai.realtimesearch.environment.Domain
import edu.unh.cs.ai.realtimesearch.environment.State
import edu.unh.cs.ai.realtimesearch.environment.SuccessorBundle

/**
 * The vacuumworld is a problem where the agent, a vacuumcleaner, is supposed to clean
 * a specific area (grid of width by height) with possibly blocked cells. The actions are movement to each of
 * the four directions, or to vacuum.
 */
class VacuumWorld(val width: Int, val height: Int, val blockedCells: List<VacuumWorldState.Location>) : Domain {

    /**
     * Part fo the Domain interface.
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
                                1.0 )) // all actions have cost of 1

                    }
                } else if (newLocation in state.dirtyCells) { // add legit vacuum action
                    successors.add(SuccessorBundle(
                            VacuumWorldState(newLocation, state.dirtyCells.filter { it != newLocation }),
                            it,
                            1.0 ))
                }
            }
            return successors
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
     * Returns true if no dirtycells are present in the state.
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

}

