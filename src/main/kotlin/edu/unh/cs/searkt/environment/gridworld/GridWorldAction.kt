package edu.unh.cs.searkt.environment.vacuumworld

import edu.unh.cs.searkt.MetronomeException
import edu.unh.cs.searkt.environment.Operator
import edu.unh.cs.searkt.environment.gridworld.GridWorldState
import edu.unh.cs.searkt.environment.location.Location

/**
 * This is An action in the vacuumworld is simply an enum:
 * an action for each direction and the vacuum action.
 *
 * @param index: the type of action, each return different relative locations
 */
enum class GridWorldAction(val index: Int) : Operator<GridWorldState> {
    LEFT(0), DOWN(1), UP(2), RIGHT(3);

    // Storage of all relative locations (up down left right), returned by reference
    private val relativeLocations = arrayOf(
            Location(-1, 0),
            Location(0, 1),
            Location(0, -1),
            Location(1, 0)
    )

    fun getRelativeLocation() = relativeLocations[index]
    override fun getCost(state: GridWorldState): Double {
        TODO("not implemented")
    }
    override fun reverse(state: GridWorldState): Operator<GridWorldState> {
        return when (index) {
            0 -> RIGHT
            1 -> UP
            2 -> DOWN
            3 -> LEFT
            else -> throw MetronomeException("Invalid reversal of GridWorldAction")
        }
    }
}