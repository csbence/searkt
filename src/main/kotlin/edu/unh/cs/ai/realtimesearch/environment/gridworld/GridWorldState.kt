package edu.unh.cs.ai.realtimesearch.environment.vacuumworld

import edu.unh.cs.ai.realtimesearch.environment.State
import edu.unh.cs.ai.realtimesearch.environment.location.Location

/**
 * A state in the vacuumworld is simply the current location of the agent,
 * and the location of all dirty cells. The blocked cells are global.
 */
data class GridWorldState(val agentLocation: Location, val targetLocation: Location) : State<GridWorldState> {
    private val hashCode: Int = calculateHashCode()

    private fun calculateHashCode(): Int {
        return agentLocation.hashCode() xor targetLocation.hashCode()
    }

    override fun equals(other: Any?): Boolean {
        return when {
            other == null -> false
            other === this -> true
            other !is GridWorldState -> false
            else -> agentLocation == other.agentLocation && this.targetLocation == other.targetLocation
        }
    }

    /**
     * Copy simply calls the data class implemented copy
     */
    override fun copy() = copy(agentLocation, targetLocation)

    public override fun toString(): String {
        return "Agent location: " + agentLocation.toString() + ", target: " + targetLocation.toString()
    }

}

