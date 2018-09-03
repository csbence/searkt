package edu.unh.cs.ai.realtimesearch.environment.gridworld

import edu.unh.cs.ai.realtimesearch.environment.vacuumworld.VacuumWorldIO
import java.io.InputStream

object GridWorldIO {

    fun parseFromStream(input: InputStream, actionDuration: Long, expansionDelay: Int): GridWorldInstance {
        val vacuumWorldInstance = VacuumWorldIO.parseFromStream(input)

        val gridWorld = vacuumWorldInstance.domain.run {
            GridWorld(width, height, blockedCells, vacuumWorldInstance.initialState.dirtyCells[0], actionDuration, expansionDelay)
        }

        val worldState = vacuumWorldInstance.initialState.run {
            if (dirtyCells.size != 1) {
                throw InvalidGridWorldException("Grid world should have exactly one goal. ${dirtyCells.size} found. ")
            }

            GridWorldState(agentLocation)
        }

        return GridWorldInstance(gridWorld, worldState)
    }

}

data class GridWorldInstance(val domain: GridWorld, val initialState: GridWorldState)
class InvalidGridWorldException(message: String, e: Exception? = null) : RuntimeException(message, e)
