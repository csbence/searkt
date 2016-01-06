package edu.unh.cs.ai.realtimesearch.environment.vacuumworld

import java.io.InputStream
import java.util.*

/**
 * @author Bence Cserna (bence@cserna.net)
 */
object VacuumWorldIO {

    fun parseFromStream(input: InputStream): VacuumWorldInstance {
        val inputScanner = Scanner(input)

        val rowCount: Int
        val columnCount: Int

        try {
            columnCount = inputScanner.nextLine().toInt()
            rowCount = inputScanner.nextLine().toInt()
        } catch (e: NoSuchElementException) {
            throw InvalidVacuumWorldException("VacuumWorld's first or second line is missing.", e)
        } catch (e: NumberFormatException) {
            throw InvalidVacuumWorldException("VacuumWorld's first and second line must be a number.", e)
        }

        val blockedCells = arrayListOf<VacuumWorldState.Location>()
        val dirtyCells = arrayListOf<VacuumWorldState.Location>()
        var startLocation: VacuumWorldState.Location? = null

        try {
            for (y in 0..rowCount - 1) {
                val line = inputScanner.nextLine()

                for (x in 0..columnCount - 1) {
                    when (line[x]) {
                        '#' -> blockedCells.add(VacuumWorldState.Location(x, y))
                        '*' -> dirtyCells.add(VacuumWorldState.Location(x, y))
                        '@' -> startLocation = VacuumWorldState.Location(x, y)
                    }
                }
            }
        } catch (e: NoSuchElementException) {
            throw InvalidVacuumWorldException("VacuumWorld is not complete.", e)
        }

        if (startLocation == null) {
            throw InvalidVacuumWorldException("Unknown start location. Start location has was not defined.")
        }

        val vacuumWorld = edu.unh.cs.ai.realtimesearch.environment.vacuumworld.VacuumWorld(columnCount, rowCount, blockedCells)
        val startState = VacuumWorldState(startLocation, dirtyCells.toSet())
        return VacuumWorldInstance(vacuumWorld, startState)
    }

}

data class VacuumWorldInstance(val vacuumWorld: VacuumWorld, val startState: VacuumWorldState)
class InvalidVacuumWorldException(message: String, e: Exception? = null) : RuntimeException(message, e)
