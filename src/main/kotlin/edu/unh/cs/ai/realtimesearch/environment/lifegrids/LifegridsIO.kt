package edu.unh.cs.ai.realtimesearch.environment.lifegrids

import edu.unh.cs.ai.realtimesearch.environment.vacuumworld.VacuumWorldIO
import java.io.InputStream

object LifegridsIO {

    fun parseFromStream(input: InputStream, actionDuration: Long): LifegridsInstance {
        val vacuumWorldInstance = VacuumWorldIO.parseFromStream(input)

        val gridWorld = vacuumWorldInstance.domain.run {
            Lifegrids(width, height, blockedCells, vacuumWorldInstance.initialState.dirtyCells[0], actionDuration)
        }

        val worldState = vacuumWorldInstance.initialState.run {
            if (dirtyCells.size != 1) {
                throw InvalidLifegridsException("Grid world should have exactly one goal. ${dirtyCells.size} found. ")
            }

            LifegridsState(agentLocation)
        }

        return LifegridsInstance(gridWorld, worldState)
    }

}

data class LifegridsInstance(val domain: Lifegrids, val initialState: LifegridsState)
class InvalidLifegridsException(message: String, e: Exception? = null) : RuntimeException(message, e)
