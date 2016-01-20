package edu.unh.cs.ai.realtimesearch.environment.vacuumworld

import edu.unh.cs.ai.realtimesearch.environment.location.Location
import java.io.InputStream
import java.util.*

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

        val blockedCells = arrayListOf<Location>()
        val dirtyCells = arrayListOf<Location>()
        var startLocation: Location? = null

        try {
            for (y in 0..rowCount - 1) {
                val line = inputScanner.nextLine()

                for (x in 0..columnCount - 1) {
                    when (line[x]) {
                        '#' -> blockedCells.add(Location(x, y))
                        '*' -> dirtyCells.add(Location(x, y))
                        '@' -> startLocation = Location(x, y)
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
        val startState = VacuumWorldState(startLocation, dirtyCells)
        return VacuumWorldInstance(vacuumWorld, startState)
    }

}

data class VacuumWorldInstance(val domain: VacuumWorld, val initialState: VacuumWorldState)

class InvalidVacuumWorldException(message: String, e: Exception? = null) : RuntimeException(message, e)
