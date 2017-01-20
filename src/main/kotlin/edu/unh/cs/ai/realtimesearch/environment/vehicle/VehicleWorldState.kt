package edu.unh.cs.ai.realtimesearch.environment.vehicle

import edu.unh.cs.ai.realtimesearch.environment.State
import edu.unh.cs.ai.realtimesearch.environment.location.Location

/**
 * A state in the vehicle domain just a location of the agent
 * and the local of the obstacles
 *
 * Created by doylew on 1/17/17.
 */
data class VehicleWorldState(val agentLocation: Location, var obstacles: Set<Location>) : State<VehicleWorldState> {
    override fun hashCode(): Int {
        return calculateHashCode()
    }

    private fun calculateHashCode(): Int {
        return agentLocation.hashCode()
    }

    override fun equals(other: Any?): Boolean {
        return when (other) {
            null -> false
            this -> true
            !is VehicleWorldState -> false
            else -> agentLocation == other.agentLocation && sameObstacles(other)
        }
    }

    private fun sameObstacles(other: Any?) : Boolean {
        if (other is VehicleWorldState) {
           other.obstacles.forEach { if (!obstacles.contains(it)) { return false } }
        } else {
            return false
        }
        return true
    }

    override fun copy() = copy(agentLocation)

}