package edu.unh.cs.searkt.environment.racetrack

import edu.unh.cs.searkt.environment.Action

/**
 * An action in the racetrack domain consists of an acceleration in any of the 8 directions
 * plus the no acceleration at all.
 *
 * TODO: copied much from GridWorld/VacuumWorld. Maybe refactor?
 */
enum class RaceTrackAction(val aX: Int, val aY: Int) : Action {
    LEFT_UP(-1, 1),
    UP(0, 1),
    RIGHT_UP(1, 1),
    LEFT(-1, 0),
    NO_OP(0, 0),
    RIGHT(1, 0),
    LEFT_DOWN(-1, -1),
    DOWN(0, -1),
    RIGHT_DOWN(1, -1),
    STARTUP(0, 0)
}
