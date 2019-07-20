package edu.unh.cs.searkt.environment.slidingtilepuzzle

import edu.unh.cs.searkt.MetronomeException
import edu.unh.cs.searkt.environment.Operator
import edu.unh.cs.searkt.environment.State
import edu.unh.cs.searkt.environment.location.Location

/**
 * Type param for Operator is permissive because there are different sizes for sliding tile, so possibly different
 * state types with the same actions
 */
enum class SlidingTilePuzzleAction(val index: Int, val relativeX: Int, val relativeY: Int) : Operator<State<*>> {
    NORTH(0, 0, -1), SOUTH(1, 0, 1), WEST(2, -1, 0), EAST(3, 1, 0);

    // Storage of all relative locations (up down left right), returned by reference
    private val relativeLocations = arrayOf(
            Location(0, -1),
            Location(0, 1),
            Location(-1, 0),
            Location(1, 0)
    )

    fun getRelativeLocation() = relativeLocations[index]
    override fun getCost(state: State<*>): Double {
        TODO("not implemented")
    }
    override fun reverse(state: State<*>): Operator<State<*>> = when (this.name) {
        "NORTH" -> SOUTH
        "SOUTH" -> NORTH
        "WEST" -> EAST
        "EAST" -> WEST
        else -> throw MetronomeException("Invalid reversal")
    }
}