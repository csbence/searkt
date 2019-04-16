package edu.unh.cs.searkt.environment.airspace

import edu.unh.cs.searkt.environment.location.Location
import edu.unh.cs.searkt.environment.racetrack.InvalidRaceTrackException
import java.io.InputStream
import java.util.*

object AirspaceIO {

    fun parseFromStream(input: InputStream, actionDuration: Long, sizeMultiplier: Int = 1): AirspaceInstance {
        val inputScanner = Scanner(input)

        val rowCount: Int
        val columnCount: Int

        try {
            columnCount = inputScanner.nextLine().toInt()
            rowCount = inputScanner.nextLine().toInt()
        } catch (e: NoSuchElementException) {
            throw InvalidRaceTrackException("Airspace's first or second line is missing.", e)
        } catch (e: NumberFormatException) {
            throw InvalidRaceTrackException("Airspace's first and second line must be a number.", e)
        }

        val blockedCells = arrayListOf<Location>()
        var startLocation: Location? = null
        val endLocations = arrayListOf<Location>()

        try {
            for (y in 0 until rowCount) {
                val line = inputScanner.nextLine()

                for (x in 0 until columnCount) {
                    for (xOffset in 0 until sizeMultiplier) {
                        for (yOffset in 0 until sizeMultiplier) {
                            val element = Location(x * sizeMultiplier + xOffset, y * sizeMultiplier + yOffset)
                            when (line[x]) {
                                '#' -> blockedCells.add(element)
                                '*' -> endLocations.add(element)
                                '@' -> startLocation = element
                            }
                        }
                    }
                }
            }
        } catch (e: NoSuchElementException) {
            throw InvalidRaceTrackException("Airspace is not complete.", e)
        }

        if (startLocation == null) {
            throw InvalidRaceTrackException("Unknown start location. Start location has was not defined.")
        }

        if (endLocations.isEmpty()) {
            throw InvalidRaceTrackException("Unknown end location. End location has was not defined.")
        }

        val raceTrack = Airspace(
                columnCount * sizeMultiplier,
                rowCount * sizeMultiplier,
                blockedCells.toHashSet(),
                endLocations.toHashSet(),
                actionDuration
        )

        val startState = AirspaceState(startLocation.x, startLocation.y)

        return AirspaceInstance(raceTrack, startState)
    }

}

data class AirspaceInstance(val domain: Airspace, val initialState: AirspaceState)

class InvalidAirspaceException(message: String, e: Exception? = null) : RuntimeException(message, e)
