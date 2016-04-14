package edu.unh.cs.ai.realtimesearch.environment.location

import java.lang.Math.abs

/**
 * This represents a grid location, defined by its x and y coordinate.
 *
 * @param x: x index of the location
 * @param y: y index of the location
 */
data class Location(val x: Int, val y: Int) {
    public operator fun plus(rhs: Location): Location {
        return Location(x + rhs.x, y + rhs.y)
    }

    public operator fun minus(rhs: Location): Location {
        return Location(x - rhs.x, y - rhs.y)
    }

    public fun manhattanDistance(other: Location): Int {
        return abs(x - other.x) + abs(y - other.y)
    }

    /**
     * Check if location is inside the boundaries.
     * The lower bound is inclusive the upper bound is exclusive.
     */
    public fun inBounds(upperBound: Int, lowerBound: Int = 0): Boolean {
        return x >= lowerBound && y >= lowerBound && x < upperBound && y < upperBound
    }

    fun toDoubleLocation(): DoubleLocation = DoubleLocation(x.toDouble(), y.toDouble())
}