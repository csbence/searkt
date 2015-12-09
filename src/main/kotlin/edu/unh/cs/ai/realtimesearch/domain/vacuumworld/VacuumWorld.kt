package edu.unh.cs.ai.realtimesearch.domain.vacuumworld

import edu.unh.cs.ai.realtimesearch.domain.Domain
import edu.unh.cs.ai.realtimesearch.domain.State
import edu.unh.cs.ai.realtimesearch.domain.SuccessorBundle
import java.util.*

class VacuumWorld(val width: Int, val height: Int, val blockedCells: ArrayList<VacuumWorldState.Location>) : Domain {

    /**
     * @brief Domain interface
     */
    override fun succesors(state: State): List<SuccessorBundle> {
        if (state is VacuumWorldState) {

            // to return
            val successors: MutableList<SuccessorBundle> = arrayListOf()

            val x = state.agentLocation.x
            val y = state.agentLocation.y

            VacuumWorldAction.values.forEach {
                if (isLegalAction(state, it))
                    successors.add(SuccessorBundle(
                            VacuumWorldState(state.agentLocation + it.getRelativeLocation(), ArrayList(state.dirtyCells)),
                            it,
                            1.0 // all actions have cost of 1
                    ))
            }

            return successors
        }

        // TODO: this proper way of dealing with this shit
        throw Throwable("VacuumWorld cannot handle any state other than actual VacuumWorldStates")
    }

    /**
     * @brief returns whether action is legal in state
     *
     * @param state the state in which the action would take place
     * @param action the action that we are applying to state
     *
     * @return true if action is allowed in state
     */
    private fun isLegalAction(state: VacuumWorldState, action: VacuumWorldAction): Boolean {
        val newLocation = state.agentLocation + action.getRelativeLocation()

        // illegal if new location is out of boundaries
        if (newLocation.x < 0 || newLocation.y < 0 || newLocation.x >= width || newLocation.y >= height)
            return false;

        // if vacuum return whether agent is on dirty cell
        if (action == VacuumWorldAction.VACUUM) {
            return newLocation in state.dirtyCells
        }

        return true // action passed all tests
    }

    private fun freeCell(x: Int, y: Int): Boolean {
        return VacuumWorldState.Location(x, y) in blockedCells
    }

    /**
     * @TODO
     */
    override fun heuristic(state: State): Double = .0

    /**
     * @TODO
     */
    override fun distance(state: State): Double = .0

    /**
     * @brief A state in vacuumworld is a goal state if there are no more dirty cells
     *
     * @param state: the state that is being checked on
     *
     * @return whether the state is a goal state
     */
    override fun isGoal(state: State): Boolean {
        if (state is VacuumWorldState) {
            return state.dirtyCells.isEmpty()
        }

        throw Throwable("VacuumWorld cannot handle any state other than actual VacuumWorldStates")
    }


}

