package edu.unh.cs.ai.realtimesearch.environment.racetrack

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
data class RaceTrackState(val x: Int, val y: Int, val dX: Int, val dY: Int) : State<RaceTrackState> {

    override fun equals(other: Any?): Boolean {
        return when {
            other !is RaceTrackState -> false
            other.x == x && other.y == y && other.dX == dX && other.dY == dY -> true
            else -> false
        }
    }

    override fun hashCode(): Int {
        return x xor rotateLeft(y, 8) xor rotateLeft(dX, 16) xor rotateLeft(dY, 24)
    }

    override fun copy() = RaceTrackState(x, y, dX, dY)

    override fun projectX() = x
    override fun projectY() = y
    override fun projectZ() = dX * dX + dY * dY
}

