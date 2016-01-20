package edu.unh.cs.ai.realtimesearch.environment.vacuumworld

import edu.unh.cs.ai.realtimesearch.environment.State

/**
 * A state in the vacuumworld is simply the current location of the agent,
 * and the location of all dirty cells. The blocked cells are global.
 */
data class VacuumWorldState(val agentLocation: Location, val dirtyCells: List<Location>) : State<VacuumWorldState> {
    private val hashCode: Int = calculateHashCode()

    private fun calculateHashCode(): Int {
        var hashCode: Int = dirtyCells.size
        return agentLocation.hashCode() xor hashCode
    }

    override fun equals(other: Any?): Boolean {
        return when {
            other == null -> false
            other === this -> true
            other !is VacuumWorldState -> false
            else -> agentLocation == other.agentLocation && this.dirtyCells == other.dirtyCells
        }
    }

    /**
     * Copy simply calls the data class implemented copy
     */
    override fun copy() = copy(agentLocation, dirtyCells)

    /**
     * This represents a grid location, defined by its x and y coordinate.
     *
     * @param x: x index of the location
     * @param y: y index of the location
     */
    data class Location(val x: Int, val y: Int) {

        /**
         * Adds two locations together the intuitive way (x+x, y+y).
         *
         * @param other: the other location
         * @return the addition of this and other location
         */
        operator fun plus(other: VacuumWorldState.Location) = VacuumWorldState.Location(x + other.x, y + other.y)

        operator fun minus(other: VacuumWorldState.Location) = VacuumWorldState.Location(x - other.x, y - other.y)
    }

    public override fun toString(): String {
        return "Agent location: " + agentLocation.toString() + ", dirty: " + dirtyCells.toString()
    }

}

