package edu.unh.cs.ai.realtimesearch.environment.vehicle

import edu.unh.cs.ai.realtimesearch.environment.location.Location
import java.io.InputStream
import java.util.*

/**
 * reading in the vehicle world file
 * Created by doylew on 1/17/17.
 */

object VehicleWorldIO {

    fun parseFromStream(input: InputStream, actionDuration: Long) : VehicleWorldInstance {
        return parseFromStream(input, actionDuration)
    }

    private fun readVehicle(input: InputStream, actionDuration: Long) : VehicleWorldInstance {
        val inputScanner = Scanner(input)

        val rowCount: Int
        val columnCount : Int

        try {
            columnCount = inputScanner.nextLine().toInt()
            rowCount = inputScanner.nextLine().toInt()
        } catch (e: NoSuchElementException) {
            throw InvalidVehicleWorldException("Vehicle world first or second line is missing.", e)
        } catch (e : NumberFormatException) {
            throw InvalidVehicleWorldException("Vehicle world first or second line must be a integer.", e)
        }

        val obstacles = arrayListOf<Location>()
        val bunkers = arrayListOf<Location>()
        val startLocation = arrayListOf<Location>()
        val targetLocation = arrayListOf<Location>()

        try {
            (0..rowCount-1).forEach { y ->
                val line = inputScanner.nextLine()
                (0..columnCount-1).forEach { x ->
                    when (line[x]) {
                        '#' -> obstacles.add(Location(x,y))
                        '*' -> targetLocation.add(Location(x,y))
                        '@' -> startLocation.add(Location(x,y))
                        '$' -> bunkers.add(Location(x,y))
                    }
                }
            }
        } catch (e: NoSuchElementException) {
            throw InvalidVehicleWorldException("Vehicle world isn't complete.", e)
        }

        if(startLocation.size != 1) {
            throw InvalidVehicleWorldException("Vehicle world start location with @ marker not specified.")
        } else if (targetLocation.size != 1) {
            throw InvalidVehicleWorldException("Vehicle world target goal location with * marker not specified.")
        }

        val vehicleWorld = edu.unh.cs.ai.realtimesearch.environment.vehicle.VehicleWorld(columnCount, rowCount,
                bunkers.toHashSet(), targetLocation.first(), actionDuration)

        val startState = VehicleWorldState(startLocation.first(), bunkers.toHashSet())
        return VehicleWorldInstance(vehicleWorld, startState)

    }
}

data class VehicleWorldInstance(val domain: VehicleWorld, val initialState: VehicleWorldState)
class InvalidVehicleWorldException(message: String, e: Exception? = null) : RuntimeException(message, e)
