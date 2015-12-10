package edu.unh.cs.ai.realtimesearch.domain.vacuumworld

import edu.unh.cs.ai.realtimesearch.domain.State

data class VacuumWorldState(val agentLocation: VacuumWorldState.Location, val dirtyCells: List<VacuumWorldState.Location>) : State {

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
    }

    public override fun toString(): String { return "Agent location: " + agentLocation.toString() + ", dirty: "  + dirtyCells.toString()}

}

