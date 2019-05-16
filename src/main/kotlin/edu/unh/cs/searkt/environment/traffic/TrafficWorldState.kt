package edu.unh.cs.searkt.environment.traffic

import edu.unh.cs.searkt.environment.State
import edu.unh.cs.searkt.environment.location.Location

/**
 * A state in the traffic domain just a location of the agent
 * and the local of the obstacles
 *
 * Created by doylew on 1/17/17.
 */
data class TrafficWorldState(val location: Location, val timeStamp: Int) : State<TrafficWorldState> {
    override fun hashCode(): Int {
        return location.hashCode() xor timeStamp
    }

    override fun equals(other: Any?): Boolean {
        return when {
            other == null -> false
            other === this -> true
            other !is TrafficWorldState -> false
            else -> location == other.location && timeStamp == other.timeStamp
        }
    }

    override fun copy() = copy(location)
}