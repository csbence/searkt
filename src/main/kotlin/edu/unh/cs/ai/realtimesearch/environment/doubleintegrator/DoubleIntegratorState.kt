package edu.unh.cs.ai.realtimesearch.environment.doubleintegrator

import edu.unh.cs.ai.realtimesearch.environment.State
import edu.unh.cs.ai.realtimesearch.environment.location.Location

/**
 *
 */
data class DoubleIntegratorState(val x : Double, val y : Double, val speed : Double, val theta : Double) : State<DoubleIntegratorState> {
//    private val hashCode: Int = calculateHashCode()
//
//    private fun calculateHashCode(): Int {
//        return agentLocation.hashCode()
//    }

    override fun equals(other: Any?): Boolean {
        if(other !is DoubleIntegratorState)
            return false
        if(other.x == x && other.y == y && other.speed == speed && other.theta == theta)
            return true;
        return false;
    }

    /**
     * Copy simply calls the data class implemented copy
     */
    override fun copy() = copy(x, y, speed, theta)
}

