package edu.unh.cs.ai.realtimesearch.environment.pointrobot

import edu.unh.cs.ai.realtimesearch.environment.location.DoubleLocation
import edu.unh.cs.ai.realtimesearch.environment.location.Location
import java.io.InputStream
import java.util.*

object PointRobotIO {

    fun parseFromStream(input: InputStream): PointRobotInstance {
        val inputScanner = Scanner(input)

        val rowCount: Int
        val columnCount: Int
        var startLocation: DoubleLocation?
        var endLocation: DoubleLocation?
        var radius: Double

        try {
            columnCount = inputScanner.nextLine().toInt()
            rowCount = inputScanner.nextLine().toInt()
        } catch (e: NoSuchElementException) {
            throw InvalidPointRobotException("PointRobot's first or second line is missing.", e)
        } catch (e: NumberFormatException) {
            throw InvalidPointRobotException("PointRobot's first and second line must be a number.", e)
        }
        try {
            val x = inputScanner.nextLine().toDouble()
            val y = inputScanner.nextLine().toDouble()
            startLocation = DoubleLocation(x, y)
        } catch (e: NoSuchElementException) {
            throw InvalidPointRobotException("PointRobot's third or fourth line is missing.", e)
        } catch (e: NumberFormatException) {
            throw InvalidPointRobotException("PointRobot's third or fourth  line must be a number.", e)
        }
        try {
            val x = inputScanner.nextLine().toDouble()
            val y = inputScanner.nextLine().toDouble()
            endLocation = DoubleLocation(x, y)
        } catch (e: NoSuchElementException) {
            throw InvalidPointRobotException("PointRobot's fifth or sixth line is missing.", e)
        } catch (e: NumberFormatException) {
            throw InvalidPointRobotException("PointRobot's fifth or sixth line must be a number.", e)
        }
        try {
            radius = inputScanner.nextLine().toDouble()
        } catch (e: NoSuchElementException) {
            throw InvalidPointRobotException("PointRobot's seventh line is missing.", e)
        } catch (e: NumberFormatException) {
            throw InvalidPointRobotException("PointRobot's seventh line must be a number.", e)
        }

        val blockedCells = arrayListOf<Location>()

        try {
            for (y in 0..rowCount - 1) {
                val line = inputScanner.nextLine()

                for (x in 0..columnCount - 1) {
                    when (line[x]) {
                        '#' -> blockedCells.add(Location(x, y))
                    }
                }
            }
        } catch (e: NoSuchElementException) {
            throw InvalidPointRobotException("DoubleIntegrator is not complete.", e)
        }

        val doubleIntegrator = edu.unh.cs.ai.realtimesearch.environment.pointrobot.PointRobot(columnCount, rowCount, blockedCells.toHashSet(), endLocation, radius)
        val startState = PointRobotState(startLocation.x, startLocation.y)
        return PointRobotInstance(doubleIntegrator, startState)
    }

}

data class PointRobotInstance(val domain: PointRobot, val initialState: PointRobotState)

class InvalidPointRobotException(message: String, e: Exception? = null) : RuntimeException(message, e)
