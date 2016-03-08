package edu.unh.cs.ai.realtimesearch.environment.racetrack

import edu.unh.cs.ai.realtimesearch.environment.Action
import edu.unh.cs.ai.realtimesearch.environment.location.Location

/**
 * An action in the racetrack domain consists of an acceleration in any of the 8 directions
 * plus the no acceleration at all.
 *
 * TODO: copied much from GridWorld/VacuumWorld. Maybe refactor?
 */
enum class RaceTrackAction(val index: Int) : Action {
    LEFTUP(0), UP(1), RIGHTUP(2),
    LEFT(3), /*NOOP(4), RIGHT(5),
    LEFTDOWN(6), DOWN(7), RIGHTDOWN(8);*/
    RIGHT(4),
    LEFTDOWN(5), DOWN(6), RIGHTDOWN(7);


    // Storage of all speed moves
    private val acceleration = arrayOf(
            Location(-1, 1), Location(0, 1), Location(1, 1),
            Location(-1, 0), /*Location(0, 0),*/ Location(1, 0),
            Location(-1, -1), Location(0, -1), Location(1, -1)
    )

    fun getAcceleration() = acceleration[index]
}
