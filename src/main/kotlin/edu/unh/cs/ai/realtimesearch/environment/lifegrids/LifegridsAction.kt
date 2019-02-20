package edu.unh.cs.ai.realtimesearch.environment.lifegrids

import edu.unh.cs.ai.realtimesearch.environment.Action
import edu.unh.cs.ai.realtimesearch.environment.location.Location

/**
 * This is An action in the vacuumworld is simply an enum:
 * an action for each direction and the vacuum action.
 *
 * @param index: the type of action, each return different relative locations
 */
enum class LifegridsAction(val index: Int) : Action {
    LEFT(0), DOWN(1), UP(2), RIGHT(3);

    // Storage of all relative locations (up down left right), returned by reference
    private val relativeLocations = arrayOf(
            Location(-1, 0),
            Location(0, 1),
            Location(0, -1),
            Location(1, 0)
    )

    fun getRelativeLocation() = relativeLocations[index]
}