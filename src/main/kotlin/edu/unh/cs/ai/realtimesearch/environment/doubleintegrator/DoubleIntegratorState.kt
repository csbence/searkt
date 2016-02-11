package edu.unh.cs.ai.realtimesearch.environment.doubleintegrator

import edu.unh.cs.ai.realtimesearch.environment.State
import edu.unh.cs.ai.realtimesearch.environment.location.Location

/**
 * A state in the vacuumworld is simply the current location of the agent,
 * and the location of all dirty cells. The blocked cells are global.
 */
data class DoubleIntegratorState(val agentLocation: Location) : State<DoubleIntegratorState> {
    private val hashCode: Int = calculateHashCode()

    private fun calculateHashCode(): Int {
        return agentLocation.hashCode()
    }

    override fun equals(other: Any?): Boolean {
        return when {
            other == null -> false
            other === this -> true
            other !is DoubleIntegratorState -> false
            else -> agentLocation == other.agentLocation
        }
    }

    /**
     * Copy simply calls the data class implemented copy
     */
    override fun copy() = copy(agentLocation)
}

