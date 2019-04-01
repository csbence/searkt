package edu.unh.cs.ai.realtimesearch.environment.airspace

import edu.unh.cs.ai.realtimesearch.environment.State
import java.lang.Integer.rotateLeft

/**
 * A state in the racetrack domain contains a current location (in a cell, similar to gridworld)
 * and a speed in x and y direction. The state space is defined as follows:
 *
 * (x, y, x. [-1,0,1], y. [-1,0,1])
 *
 * The actual size and shape of the world is state-independent, so not implemented here
 */
data class AirspaceState(val x: Int, val y: Int) : State<AirspaceState> {

    override fun equals(other: Any?): Boolean {
        return when {
            other !is AirspaceState -> false
            other.x == x && other.y == y  -> true
            else -> false
        }
    }

    override fun hashCode(): Int {
        return x xor rotateLeft(y, 8)
    }

    override fun copy() = AirspaceState(x, y)

    override fun projectX() = x
    override fun projectY() = y
}

