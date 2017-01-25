package edu.unh.cs.ai.realtimesearch.environment.traffic

import edu.unh.cs.ai.realtimesearch.environment.State
import edu.unh.cs.ai.realtimesearch.environment.location.Location
import edu.unh.cs.ai.realtimesearch.environment.obstacle.MovingObstacle

/**
 * A state in the traffic domain just a location of the agent
 * and the local of the obstacles
 *
 * Created by doylew on 1/17/17.
 */
data class TrafficWorldState(val agentLocation: Location, var obstacles: Set<MovingObstacle>) : State<TrafficWorldState> {
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
            !is TrafficWorldState -> false
            else -> agentLocation == other.agentLocation && Companion.sameObstacles(this, other)
        }
    }

    override fun copy() = copy(agentLocation)

    companion object {
        private fun sameObstacles(trafficWorldState: TrafficWorldState, other: Any?) : Boolean {
            if (other is TrafficWorldState) {
                other.obstacles.forEach { if (!trafficWorldState.obstacles.contains(it)) { return false } }
            } else {
                return false
            }
            return true
        }
    }

}