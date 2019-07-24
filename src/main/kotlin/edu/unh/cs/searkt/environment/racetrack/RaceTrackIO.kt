package edu.unh.cs.searkt.environment.racetrack

import edu.unh.cs.searkt.environment.location.Location
import java.io.InputStream
import java.util.*

object RaceTrackIO {

    fun parseFromStream(input: InputStream, actionDuration: Long, isSafe: Boolean, sizeMultiplier: Int = 1): RaceTrackInstance {
        val inputScanner = Scanner(input)

        val rowCount: Int
        val columnCount: Int

        try {
            columnCount = inputScanner.nextLine().toInt()
            rowCount = inputScanner.nextLine().toInt()
        } catch (e: NoSuchElementException) {
            throw InvalidRaceTrackException("RaceTracks's first or second line is missing.", e)
        } catch (e: NumberFormatException) {
            throw InvalidRaceTrackException("RaceTrack's first and second line must be a number.", e)
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
            throw InvalidRaceTrackException("RaceTrack is not complete.", e)
        }

        if (startLocation == null) {
            throw InvalidRaceTrackException("Unknown start location. Start location has was not defined.")
        }

        if (endLocations.isEmpty()) {
            throw InvalidRaceTrackException("Unknown end location. End location has was not defined.")
        }

        val raceTrack = RaceTrack(
                columnCount * sizeMultiplier,
                rowCount * sizeMultiplier,
                blockedCells.toHashSet(),
                endLocations.toHashSet(),
                isSafe,
                actionDuration
        )

        val startState = RaceTrackState(startLocation.x, startLocation.y, 0, 0)

        return RaceTrackInstance(raceTrack, startState)
    }

}

data class RaceTrackInstance(val domain: RaceTrack, val initialState: RaceTrackState)

class InvalidRaceTrackException(message: String, e: Exception? = null) : RuntimeException(message, e)
