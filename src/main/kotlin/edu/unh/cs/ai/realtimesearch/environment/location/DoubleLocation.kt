package edu.unh.cs.ai.realtimesearch.environment.location

import java.lang.Math.abs

/**
 * Created by michaelatremblay on 2/25/16.
 */

/**
 * This represents a grid location, defined by its x and y coordinate.
 *
 * @param x: x index of the location
 * @param y: y index of the location
 */
data class DoubleLocation(val x: Double, val y: Double) {
    operator fun plus(rhs: DoubleLocation): DoubleLocation {
        return DoubleLocation(x + rhs.x, y + rhs.y)
    }

    operator fun minus(rhs: DoubleLocation): DoubleLocation {
        return DoubleLocation(x - rhs.x, y - rhs.y)
    }

    fun manhattanDistance(other: DoubleLocation): Double {
        return abs(x - other.x) + abs(y - other.y)
    }

    /**
     * Check if location is inside the boundaries.
     * The lower bound is inclusive the upper bound is exclusive.
     */
    fun inBounds(upperBound: Int, lowerBound: Int = 0): Boolean {
        return x >= lowerBound && y >= lowerBound && x < upperBound && y < upperBound
    }
}