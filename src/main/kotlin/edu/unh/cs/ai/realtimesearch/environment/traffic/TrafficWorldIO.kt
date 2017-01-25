package edu.unh.cs.ai.realtimesearch.environment.traffic

import edu.unh.cs.ai.realtimesearch.environment.location.Location
import edu.unh.cs.ai.realtimesearch.environment.obstacle.MovingObstacle
import java.io.InputStream
import java.util.*

/**
 * reading in the traffic world file
 * Created by doylew on 1/17/17.
 */

object VehicleWorldIO {

    fun parseFromStream(input: InputStream, actionDuration: Long) : VehicleWorldInstance {
        return readVehicle(input, actionDuration)
    }

    private fun readVehicle(input: InputStream, actionDuration: Long) : VehicleWorldInstance {
        val inputScanner = Scanner(input)

        val random = Random()

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

        val obstacles = arrayListOf<MovingObstacle>()
        val bunkers = arrayListOf<Location>()
        val startLocation = arrayListOf<Location>()
        val targetLocation = arrayListOf<Location>()

        try {
            (0..rowCount-1).forEach { y ->
                val line = inputScanner.nextLine()
                (0..columnCount-1).forEach { x ->
                    val coin = random.nextBoolean()
                    when (line[x]) {
                        '#' -> obstacles.add(MovingObstacle(x,y,if(coin) random.nextInt(1)+1 else 0, if(!coin) random.nextInt(1)+1 else 0))
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

        val vehicleWorld = edu.unh.cs.ai.realtimesearch.environment.traffic.TrafficWorld(columnCount, rowCount,
                bunkers.toHashSet(), targetLocation.first(), actionDuration)

        val startState = TrafficWorldState(startLocation.first(), obstacles.toHashSet())
        return VehicleWorldInstance(vehicleWorld, startState)

    }
}

data class VehicleWorldInstance(val domain: TrafficWorld, val initialState: TrafficWorldState)
class InvalidVehicleWorldException(message: String, e: Exception? = null) : RuntimeException(message, e)
