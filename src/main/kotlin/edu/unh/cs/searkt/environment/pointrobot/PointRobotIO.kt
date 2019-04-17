package edu.unh.cs.searkt.environment.pointrobot

import edu.unh.cs.searkt.environment.location.DoubleLocation
import edu.unh.cs.searkt.environment.location.Location
import edu.unh.cs.searkt.environment.pointrobotwithinertia.InvalidPointRobotWithInertiaException
import java.io.InputStream
import java.util.*

object PointRobotIO {

    fun parseHeader(inputScanner: Scanner): PointRobotHeader {
        val rowCount: Int
        val columnCount: Int
        val startLocationOffset: DoubleLocation
        val endLocationOffset: DoubleLocation
        val radius: Double

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
            if (x < 0.0 || x >= 1.0)
                throw InvalidPointRobotWithInertiaException("Start location x offset must be in range [0.0, 1.0)")
            if (y < 0.0 || y >= 1.0)
                throw InvalidPointRobotWithInertiaException("Start location y offset must be in range [0.0, 1.0)")
            startLocationOffset = DoubleLocation(x, y)
        } catch (e: NoSuchElementException) {
            throw InvalidPointRobotWithInertiaException("PointRobot's third or fourth line is missing.", e)
        } catch (e: NumberFormatException) {
            throw InvalidPointRobotWithInertiaException("PointRobot's third or fourth  line must be a number.", e)
        }
        try {
            val x = inputScanner.nextLine().toDouble()
            val y = inputScanner.nextLine().toDouble()
            if (x < 0.0 || x >= 1.0)
                throw InvalidPointRobotWithInertiaException("Goal location x offset must be in range [0.0, 1.0)")
            if (y < 0.0 || y >= 1.0)
                throw InvalidPointRobotWithInertiaException("Goal location y offset must be in range [0.0, 1.0)")
            endLocationOffset = DoubleLocation(x, y)
        } catch (e: NoSuchElementException) {
            throw InvalidPointRobotWithInertiaException("PointRobot's fifth or sixth line is missing.", e)
        } catch (e: NumberFormatException) {
            throw InvalidPointRobotWithInertiaException("PointRobot's fifth or sixth line must be a number.", e)
        }
        try {
            radius = inputScanner.nextLine().toDouble()
            if (radius < 0.0)
                throw InvalidPointRobotWithInertiaException("Radius must be greater than or equal to 0.0")
        } catch (e: NoSuchElementException) {
            throw InvalidPointRobotWithInertiaException("PointRobot's seventh line is missing.", e)
        } catch (e: NumberFormatException) {
            throw InvalidPointRobotWithInertiaException("PointRobot's seventh line must be a number.", e)
        }

        return PointRobotHeader(rowCount, columnCount, startLocationOffset, endLocationOffset, radius)
    }

    fun parseMap(inputScanner: Scanner, header: PointRobotHeader): PointRobotGridMapInfo {
        val blockedCells = arrayListOf<Location>()
        val goalCells = arrayListOf<DoubleLocation>()
        var startLocations = arrayListOf<DoubleLocation>()

        try {
            for (y in 0..header.rowCount - 1) {
                val line = inputScanner.nextLine()

                for (x in 0..header.columnCount - 1) {
                    when (line[x]) {
                        '#' -> blockedCells.add(Location(x, y))
                        '*' -> goalCells.add(DoubleLocation(x + header.goalLocationOffset.x, y + header.goalLocationOffset.y))
                        '@' -> startLocations.add(DoubleLocation(x + header.startLocationOffset.x, y + header.startLocationOffset.y))
                    }
                }
            }
        } catch (e: NoSuchElementException) {
            throw InvalidPointRobotWithInertiaException("PointRobotWithInertia is not complete.", e)
        }

        if (startLocations.isEmpty() || startLocations.size > 1) {
            throw InvalidPointRobotWithInertiaException("Unknown start location. Must be exactly 1 start location.")
        }

        if (goalCells.isEmpty() || goalCells.size > 1) {
            throw InvalidPointRobotWithInertiaException("Unknown goal location. Must be exactly 1 goal location.")
        }

        return PointRobotGridMapInfo(header.rowCount, header.columnCount, blockedCells, startLocations, goalCells)
    }

    fun parseFromStream(input: InputStream, actionDuration: Long): PointRobotInstance {
        val inputScanner = Scanner(input)

        val header = parseHeader(inputScanner)
        val mapInfo = parseMap(inputScanner, header)

        val startLocation = mapInfo.startCells.first()
        val endLocation = mapInfo.endCells.first()

        val pointRobot = PointRobot(header.columnCount, header.rowCount, mapInfo.blockedCells.toHashSet(),
                endLocation, header.goalRadius, actionDuration)
        val startState = PointRobotState(startLocation.x, startLocation.y)
        return PointRobotInstance(pointRobot, startState)
    }
}

data class PointRobotInstance(val domain: PointRobot, val initialState: PointRobotState)

class InvalidPointRobotException(message: String, e: Exception? = null) : RuntimeException(message, e)

data class PointRobotHeader(
        val rowCount: Int,
        val columnCount: Int,
        val startLocationOffset: DoubleLocation,
        val goalLocationOffset: DoubleLocation,
        val goalRadius: Double)

data class PointRobotGridMapInfo(
        val rowCount: Int,
        val columnCount: Int,
        val blockedCells: MutableList<Location> = mutableListOf(),
        val startCells: MutableList<DoubleLocation> = mutableListOf(),
        val endCells: MutableList<DoubleLocation> = mutableListOf()) {
    companion object {
        val ZERO = PointRobotGridMapInfo(0, 0)
    }
}