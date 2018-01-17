package edu.unh.cs.ai.realtimesearch.environment.location

import java.lang.Math.abs

/**
 * This represents a grid location, defined by its x and y coordinate.
 *
 * @property x: x index of the location
 * @property y: y index of the location
 * @constructor creates a Location with values (x,y)
 */
data class Location(val x: Int, val y: Int) {

    /**
     * Add two Locations together pair-wise
     *
     * @param rhs the right hand side Location being added
     * @return a new Location representing the two Locations added
     */
    operator fun plus(rhs: Location): Location = Location(x + rhs.x, y + rhs.y)

    /**
     * Take the difference of two Locations together pair-wise
     *
     * @param rhs right hand side Location being subtracted
     * @return a new Location representing the two Locations subtracted
     */
    operator fun minus(rhs: Location): Location = Location(x - rhs.x, y - rhs.y)

    /**
     * Manhattan distance of two Locations
     * abs (this.x - other.x) + abs(this.y - other.y)
     *
     * @param other the Location which designates the Manhattan distance
     * @return the Int value representing the Manhattan distance of the Locations
     */
    fun manhattanDistance(other: Location): Int = abs(x - other.x) + abs(y - other.y)

    /**
     * Equality of Locations pair-wise, each must be of type Location and satisfy:
     * other.x == this.x && other.y == this.y
     *
     * @param other any object to compare equality against
     * @return a Boolean representing (other.x == this.x && other.y == this.y)
     */
    override fun equals(other: Any?): Boolean = other is Location && other.x == x && other.y == y

    /**
     * Calculates a basic hash of the Location pair by performing
     * an xor of the two values (x,y) with y bit-shifted left by 16
     *
     * @return an Int hash value representing the Location pair
     */
    override fun hashCode(): Int = x xor Integer.rotateLeft(y, 16)

    /**
     * Check if location is inside the boundaries.
     * The lower bound is inclusive the upper bound is exclusive.
     *
     * @param upperBound the upper limit
     * @param lowerBound the lower limit
     * @return a Boolean satisfying (x >= lowerBound && y >= lowerBound && x < upperBound && y < upperBound)
     */
    fun inBounds(upperBound: Int, lowerBound: Int = 0): Boolean =
            x >= lowerBound && y >= lowerBound && x < upperBound && y < upperBound

    /**
     * Converts the Int Location to a DoubleLocation by casting (x.toDouble(), y.toDouble)
     *
     * @return a DoubleLocation with the same values in a Double format
     */
    fun toDoubleLocation(): DoubleLocation = DoubleLocation(x.toDouble(), y.toDouble())
}