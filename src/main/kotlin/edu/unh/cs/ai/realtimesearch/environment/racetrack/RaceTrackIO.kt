package edu.unh.cs.ai.realtimesearch.environment.racetrack

import edu.unh.cs.ai.realtimesearch.environment.location.Location
import java.io.InputStream
import java.util.*

object RaceTrackIO {

    fun parseFromStream(input: InputStream): RaceTrackInstance {
        val inputScanner = Scanner(input)

        val rowCount: Int
        val columnCount: Int

        try {
            columnCount = inputScanner.nextLine().toInt()
            rowCount = inputScanner.nextLine().toInt()
        } catch (e: NoSuchElementException) {
            throw InvalidRaceTrackException("DoubleIntegrator's first or second line is missing.", e)
        } catch (e: NumberFormatException) {
            throw InvalidRaceTrackException("DoubleIntegrator's first and second line must be a number.", e)
        }

        val blockedCells = arrayListOf<Location>()
        var startLocation: Location? = null
        val endLocations = arrayListOf<Location>()

        try {
            for (y in 0..rowCount - 1) {
                val line = inputScanner.nextLine()

                for (x in 0..columnCount - 1) {
                    when (line[x]) {
                        '#' -> blockedCells.add(Location(x, y))
                        '*' -> endLocations.add(Location(x, y))
                        '@' -> startLocation = Location(x, y)
                    }
                }
            }
        } catch (e: NoSuchElementException) {
            throw InvalidRaceTrackException("DoubleIntegrator is not complete.", e)
        }

        if (startLocation == null) {
            throw InvalidRaceTrackException("Unknown start location. Start location has was not defined.")
        }

        if (endLocations.isEmpty()) {
            throw InvalidRaceTrackException("Unknown end location. End location has was not defined.")
        }

        val raceTrack = edu.unh.cs.ai.realtimesearch.environment.racetrack.RaceTrack(columnCount, rowCount, blockedCells.toHashSet(), endLocations.toHashSet())
        val startState = RaceTrackState(startLocation.x, startLocation.y, 0, 0)
        return RaceTrackInstance(raceTrack, startState)
    }

}

data class RaceTrackInstance(val domain: RaceTrack, val initialState: RaceTrackState)

class InvalidRaceTrackException(message: String, e: Exception? = null) : RuntimeException(message, e)
