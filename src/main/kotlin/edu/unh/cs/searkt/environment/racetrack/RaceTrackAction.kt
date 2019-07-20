package edu.unh.cs.searkt.environment.racetrack

import edu.unh.cs.searkt.MetronomeException
import edu.unh.cs.searkt.environment.Action
import edu.unh.cs.searkt.environment.Operator

/**
 * An action in the racetrack domain consists of an acceleration in any of the 8 directions
 * plus the no acceleration at all.
 *
 * TODO: copied much from GridWorld/VacuumWorld. Maybe refactor?
 */
enum class RaceTrackAction(val aX: Int, val aY: Int) : Operator<RaceTrackState> {
    LEFT_UP(-1, 1),
    UP(0, 1),
    RIGHT_UP(1, 1),
    LEFT(-1, 0),
    NO_OP(0, 0),
    RIGHT(1, 0),
    LEFT_DOWN(-1, -1),
    DOWN(0, -1),
    RIGHT_DOWN(1, -1);

    override fun getCost(state: RaceTrackState): Double {
        TODO("not implemented")
    }

    override fun reverse(state: RaceTrackState): Operator<RaceTrackState> {
        return when (this.name) {
            "LEFT_UP" -> RIGHT_DOWN
            "UP" -> DOWN
            "RIGHT_UP" -> LEFT_DOWN
            "LEFT" -> RIGHT
            "NO_OP" -> NO_OP
            "RIGHT" -> LEFT
            "LEFT_DOWN" -> RIGHT_UP
            "DOWN" -> UP
            "RIGHT_DOWN" -> LEFT_UP
            else -> throw MetronomeException("Invalid reversal of RaceTrackAction")
        }
    }
}
