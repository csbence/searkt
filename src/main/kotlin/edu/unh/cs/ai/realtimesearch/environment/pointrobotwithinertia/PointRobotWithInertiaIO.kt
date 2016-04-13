package edu.unh.cs.ai.realtimesearch.environment.pointrobotwithinertia

import edu.unh.cs.ai.realtimesearch.environment.location.DoubleLocation
import edu.unh.cs.ai.realtimesearch.environment.location.Location
import java.io.InputStream
import java.util.*

object PointRobotWithInertiaIO {

    fun parseFromStream(input: InputStream, numAction: Int,
                        actionFraction: Double, stateFraction: Double, actionDuration: Long): PointRobotWithInertiaInstance {
        val inputScanner = Scanner(input)

        val rowCount: Int
        val columnCount: Int
        var startLocation: DoubleLocation?
        var endLocation: DoubleLocation?
        var radius: Double;

        try {
            columnCount = inputScanner.nextLine().toInt()
            rowCount = inputScanner.nextLine().toInt()
        } catch (e: NoSuchElementException) {
            throw InvalidPointRobotWithInertiaException("PointRobot's first or second line is missing.", e)
        } catch (e: NumberFormatException) {
            throw InvalidPointRobotWithInertiaException("PointRobot's first and second line must be a number.", e)
        }
        try {
            val x = inputScanner.nextLine().toDouble()
            val y = inputScanner.nextLine().toDouble()
            startLocation = DoubleLocation(x, y)
        } catch (e: NoSuchElementException) {
            throw InvalidPointRobotWithInertiaException("PointRobot's third or fourth line is missing.", e)
        } catch (e: NumberFormatException) {
            throw InvalidPointRobotWithInertiaException("PointRobot's third or fourth  line must be a number.", e)
        }
        try {
            val x = inputScanner.nextLine().toDouble()
            val y = inputScanner.nextLine().toDouble()
            endLocation = DoubleLocation(x, y)
        } catch (e: NoSuchElementException) {
            throw InvalidPointRobotWithInertiaException("PointRobot's fifth or sixth line is missing.", e)
        } catch (e: NumberFormatException) {
            throw InvalidPointRobotWithInertiaException("PointRobot's fifth or sixth line must be a number.", e)
        }
        try {
            radius = inputScanner.nextLine().toDouble()
        } catch (e: NoSuchElementException) {
            throw InvalidPointRobotWithInertiaException("PointRobot's seventh line is missing.", e)
        } catch (e: NumberFormatException) {
            throw InvalidPointRobotWithInertiaException("PointRobot's seventh line must be a number.", e)
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
            throw InvalidPointRobotWithInertiaException("DoubleIntegrator is not complete.", e)
        }

        val doubleIntegrator = edu.unh.cs.ai.realtimesearch.environment.pointrobotwithinertia.
                PointRobotWithInertia(columnCount, rowCount, blockedCells.toHashSet(), endLocation, radius, numAction, actionFraction, stateFraction, actionDuration)
        val startState = PointRobotWithInertiaState(startLocation.x, startLocation.y, 0.0, 0.0, stateFraction)
        return PointRobotWithInertiaInstance(doubleIntegrator, startState)
    }

}

data class PointRobotWithInertiaInstance(val domain: PointRobotWithInertia, val initialState: PointRobotWithInertiaState)

class InvalidPointRobotWithInertiaException(message: String, e: Exception? = null) : RuntimeException(message, e)
