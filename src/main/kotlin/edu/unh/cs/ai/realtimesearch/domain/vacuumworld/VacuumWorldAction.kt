package edu.unh.cs.ai.realtimesearch.domain.vacuumworld

import edu.unh.cs.ai.realtimesearch.domain.Action

/**
 * This is An action in the vacuumworld is simply an enum:
 * an action for each direction and the vacuum action.
 *
 * @param index: the type of action, each return different relative locations
 */
enum class VacuumWorldAction(val index: Int) : Action {
    RIGHT(0), LEFT(1), DOWN(2), UP (3), VACUUM(4);

    // Storage of all relative locations (up down left right), returned by reference
    protected val relativeLocations = arrayOf(
            VacuumWorldState.Location(1, 0),
            VacuumWorldState.Location(-1, 0),
            VacuumWorldState.Location(0, -1),
            VacuumWorldState.Location(0, 1),
            VacuumWorldState.Location(0, 0)
    )

    fun getRelativeLocation() = relativeLocations[index]
}