package edu.unh.cs.ai.realtimesearch.environment.doubleintegrator

import edu.unh.cs.ai.realtimesearch.environment.vacuumworld.VacuumWorldIO
import java.io.InputStream

object DoubleIntegratorIO {

    public fun parseFromStream(input: InputStream): DoubleIntegratorInstance {
        val doubleIntegratorInstance = VacuumWorldIO.parseFromStream(input)

        val DoubleIntegrator = doubleIntegratorInstance.domain.run {
            DoubleIntegrator(width, height, blockedCells, doubleIntegratorInstance.initialState.dirtyCells[0])
        }

        val worldState = doubleIntegratorInstance.initialState.run {
            if (dirtyCells.size != 1) {
                throw InvalidDoubleIntegratorException("Grid world should have exactly one goal. ${dirtyCells.size} found. ")
            }

            DoubleIntegratorState(agentLocation)
        }

        return DoubleIntegratorInstance(DoubleIntegrator, worldState)
    }

}

data class DoubleIntegratorInstance(val domain: DoubleIntegrator, val initialState: DoubleIntegratorState)
class InvalidDoubleIntegratorException(message: String, e: Exception? = null) : RuntimeException(message, e)
