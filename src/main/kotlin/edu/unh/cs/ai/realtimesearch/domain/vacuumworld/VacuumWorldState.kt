package edu.unh.cs.ai.realtimesearch.domain.vacuumworld

import edu.unh.cs.ai.realtimesearch.domain.State
import java.util.*

class VacuumWorldState(val agentLocation: VacuumWorldState.Location, val dirtyCells: ArrayList<VacuumWorldState.Location>) : State {

    /**
     * A grid location, defined by its x and y coordinate
     */
    data class Location(val x: Int, val y: Int) {
        fun plus(other: VacuumWorldState.Location) = VacuumWorldState.Location(x + other.x, y + other.y)
    }

}

