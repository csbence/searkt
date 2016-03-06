package edu.unh.cs.ai.realtimesearch.environment.slidingtilepuzzle

import edu.unh.cs.ai.realtimesearch.environment.Action
import edu.unh.cs.ai.realtimesearch.environment.location.Location

enum class SlidingTilePuzzleAction(val index: Int) : Action<SlidingTilePuzzleState> {
    NORTH(0), SOUTH(1), WEST(2), EAST(3);

    // Storage of all relative locations (up down left right), returned by reference
    private val relativeLocations = arrayOf(
            Location(0, -1),
            Location(0, 1),
            Location(-1, 0),
            Location(1, 0)
    )

    fun getRelativeLocation() = relativeLocations[index]
}