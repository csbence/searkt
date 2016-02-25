package edu.unh.cs.ai.realtimesearch.environment.pointrobot

import edu.unh.cs.ai.realtimesearch.environment.location.Location
import java.io.InputStream
import java.util.*

object PointRobotIO {

    fun parseFromStream(input: InputStream): PointRobotInstance {
        val inputScanner = Scanner(input)

        val rowCount: Int
        val columnCount: Int

        try {
            columnCount = inputScanner.nextLine().toInt()
            rowCount = inputScanner.nextLine().toInt()
        } catch (e: NoSuchElementException) {
            throw InvalidDoubleIntegratorException("DoubleIntegrator's first or second line is missing.", e)
        } catch (e: NumberFormatException) {
            throw InvalidDoubleIntegratorException("DoubleIntegrator's first and second line must be a number.", e)
        }

        val blockedCells = arrayListOf<Location>()
        var startLocation: Location? = null
        var endLocation: Location? = null

        try {
            for (y in 0..rowCount - 1) {
                val line = inputScanner.nextLine()

                for (x in 0..columnCount - 1) {
                    when (line[x]) {
                        '#' -> blockedCells.add(Location(x, y))
                        '*' -> endLocation = Location(x, y)
                        '@' -> startLocation = Location(x, y)
                    }
                }
            }
        } catch (e: NoSuchElementException) {
            throw InvalidDoubleIntegratorException("DoubleIntegrator is not complete.", e)
        }

        if (startLocation == null) {
            throw InvalidDoubleIntegratorException("Unknown start location. Start location has was not defined.")
        }

        if (endLocation == null) {
            throw InvalidDoubleIntegratorException("Unknown end location. End location has was not defined.")
        }

        val doubleIntegrator = edu.unh.cs.ai.realtimesearch.environment.pointrobot.PointRobot(columnCount, rowCount, blockedCells.toHashSet(), endLocation)
        val startState = PointRobotState(startLocation.x.toDouble(), startLocation.y.toDouble())
        return PointRobotInstance(doubleIntegrator, startState)
    }

}

data class PointRobotInstance(val domain: PointRobot, val initialState: PointRobotState)

class InvalidDoubleIntegratorException(message: String, e: Exception? = null) : RuntimeException(message, e)
