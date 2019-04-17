package edu.unh.cs.searkt.environment.vehicle

import edu.unh.cs.searkt.environment.State
import edu.unh.cs.searkt.environment.location.Location

/**
 * A state in the vehicle domain just a location of the agent
 * and the local of the obstacles
 *
 * Created by doylew on 1/17/17.
 */
data class VehicleWorldState(val agentLocation: Location) : State<VehicleWorldState> {
    override fun hashCode(): Int {
        return calculateHashCode()
    }

    private fun calculateHashCode(): Int {
        return agentLocation.hashCode()
    }

    override fun equals(other: Any?): Boolean {
        return when {
            other == null -> false
            other == this -> true
            other !is VehicleWorldState -> false
            else -> agentLocation == other.agentLocation
        }
    }

    override fun copy() = copy(agentLocation)

}