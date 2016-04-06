package edu.unh.cs.ai.realtimesearch.environment.racetrack

import edu.unh.cs.ai.realtimesearch.environment.State

/**
 * A state in the racetrack domain contains a current location (in a cell, similar to gridworld)
 * and a speed in x and y direction. The state space is defined as follows:
 *
 * (x, y, x. [-1,0,1], y. [-1,0,1])
 *
 * The actual size and shape of the world is state-independent, so not implemented here
 */
data class RaceTrackState(val x: Int, val y: Int, val x_speed: Int, val y_speed: Int) : State<RaceTrackState> {

    override fun equals(other: Any?): Boolean {
        if (other !is RaceTrackState)
            return false
        if (other.x == x && other.y == y && other.x_speed == x_speed && other.y_speed == y_speed) {
            return true;
        }
        return false;
    }

    override fun hashCode(): Int {
        return x.toInt() xor Integer.reverse(y.toInt())
    }

    override fun copy() = RaceTrackState(x, y, x_speed, y_speed)
}

