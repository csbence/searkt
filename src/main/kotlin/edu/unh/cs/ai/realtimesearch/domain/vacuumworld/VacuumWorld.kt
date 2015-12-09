package edu.unh.cs.ai.realtimesearch.domain.vacuumworld

import edu.unh.cs.ai.realtimesearch.domain.Domain
import edu.unh.cs.ai.realtimesearch.domain.State
import edu.unh.cs.ai.realtimesearch.domain.SuccessorBundle
import java.util.*

class VacuumWorld(val width: Int, val height: Int, val blockedCells: ArrayList<VacuumWorldState.Location>) : Domain {


    override fun succesors(state: State): List<SuccessorBundle> {
        if (state is VacuumWorldState) {

            // to return
            val successors: MutableList<SuccessorBundle> = arrayListOf()

            val x = state.agentLocation.x
            val y = state.agentLocation.y

            val location = when {
                x > 0 && freeCell(x - 1, y) -> VacuumWorldState.Location(x - 1, y)
                else -> null
            }

            val newState = VacuumWorldState(location, ArrayList(state.dirtyCells))
            successors.add(SuccessorBundle(state, VacuumWorldAction.LEFT, 1.0))


            // going left
            if (x > 0 && freeCell(x - 1, y)) {
                val newLocation = VacuumWorldState.Location(x - 1, y)
                val newState = VacuumWorldState(newLocation, ArrayList(state.dirtyCells))

                successors.add(SuccessorBundle(state, VacuumWorldAction.LEFT, 1.0))
            }

            // going right
            if (x < (width - 1) && freeCell(x + 1, y)) {
                val newLocation = VacuumWorldState.Location(x + 1, y)

                // TODO: get these enums to work
            }

            if (y > 0 && freeCell(x, y - 1)) {
                val newLocation = VacuumWorldState.Location(x, y - 1)

                // TODO: get these enums to work

            }

            return successors
        }

        throw Throwable("VacuumWorld cannot handle any state other than actual VacuumWorldStates")
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
     * A state in vacuumworld is a goal state if there are no more dirty cells
     */
    override fun isGoal(state: State): Boolean {
        if (state is VacuumWorldState) {
            return state.dirtyCells.isEmpty()
        }

        throw Throwable("VacuumWorld cannot handle any state other than actual VacuumWorldStates")
    }


}