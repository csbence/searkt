package edu.unh.cs.ai.realtimesearch.environment.gridworld

import edu.unh.cs.ai.realtimesearch.environment.vacuumworld.GridWorld
import edu.unh.cs.ai.realtimesearch.environment.vacuumworld.GridWorldState
import edu.unh.cs.ai.realtimesearch.environment.vacuumworld.VacuumWorldIO
import java.io.InputStream

object GridWorldIO {

    public fun parseFromStream(input: InputStream): GridWorldInstance {
        val vacuumWorldInstance = VacuumWorldIO.parseFromStream(input)

        val gridWorld = vacuumWorldInstance.domain.run {
            GridWorld(width, height, blockedCells)
        }

        val worldState = vacuumWorldInstance.initialState.run {
            if (dirtyCells.size != 1) {
                throw InvalidGridWorldException("Grid world should have exactly one goal. ${dirtyCells.size} found. ")
            }

            GridWorldState(agentLocation, dirtyCells[0])
        }

        return GridWorldInstance(gridWorld, worldState)
    }

}

data class GridWorldInstance(val domain: GridWorld, val initialState: GridWorldState)
class InvalidGridWorldException(message: String, e: Exception? = null) : RuntimeException(message, e)
