package edu.unh.cs.ai.realtimesearch.util

import edu.unh.cs.ai.realtimesearch.environment.location.Location
import java.util.concurrent.TimeUnit

/*
 * Author: Mike Bogochow (mgp36@unh.edu)
 */

/**
 * The accuracy to use when comparing double numbers.
 */
val defaultFloatAccuracy = 0.00001

/**
 * Compare two double for equality up to a given accuracy.
 */
fun doubleNearEqual(a: Double, b: Double, accuracy: Double = defaultFloatAccuracy): Boolean
        = (a == b) || Math.abs(a - b) < accuracy

fun doubleNearLessThanOrEqual(a: Double, b: Double, accuracy: Double = defaultFloatAccuracy): Boolean
        = (a < b) || doubleNearEqual(a, b, accuracy)

fun doubleNearGreaterThanOrEqual(a: Double, b: Double, accuracy: Double = defaultFloatAccuracy): Boolean
        = (a > b) || doubleNearEqual(a, b, accuracy)

fun doubleNearLessThan(a: Double, b: Double, accuracy: Double = defaultFloatAccuracy): Boolean
        = !doubleNearEqual(a, b, accuracy) && a < b

fun doubleNearGreaterThan(a: Double, b: Double, accuracy: Double = defaultFloatAccuracy): Boolean
        = !doubleNearEqual(a, b, accuracy) && a > b

/**
 * Round a number to a given decimal provided the type of rounding operation.
 */
fun roundOperation(number: Double, decimal: Double, op: (Double, Double) -> Double, accuracy: Double = defaultFloatAccuracy): Double {
    val fraction = 1.0 / decimal
    val operand = if (doubleNearEqual(decimal, number)) 1.0 else number * fraction
    return op(operand, accuracy) / fraction
}

// Rounding wrapper methods
// Add accuracy to number due to floating point calculations causing issues with rounding operators (1 != 1.000000...)
fun roundToNearestDecimal(number: Double, decimal: Double): Double = roundOperation(number, decimal, { num, accuracy -> Math.round(num + accuracy) + 0.0 })

fun roundDownToDecimal(number: Double, decimal: Double): Double = roundOperation(number, decimal, { num, accuracy -> Math.floor(num + accuracy) })
fun roundUpToDecimal(number: Double, decimal: Double): Double = roundOperation(number, decimal, { num, accuracy -> Math.ceil(num + accuracy) })


/**
 * Convert time in ns to double in specified time unit
 */
fun convertNanoUpDouble(time: Long, unit: TimeUnit): Double = time.toDouble() / TimeUnit.NANOSECONDS.convert(1, unit).toDouble()

/**
 * Calculate the difference between an angle and a goal angle.  The resulting difference will be in the range
 * [-pi,pi] to avoid attempting to rotate completely around in one direction.
 */
fun angleDifference(angle: Double, goalAngle: Double): Double {
    var difference = goalAngle - angle
    if (difference < -Math.PI)
        difference += 2 * Math.PI
    else if (difference > Math.PI)
        difference -= 2 * Math.PI
    return difference
}

/**
 * Calculate the angle distance between an angle and a goal angle.  The resulting distance will be in the range
 * [0,pi].
 */
fun angleDistance(angle: Double, goalAngle: Double): Double {
    val distance = angleDifference(angle, goalAngle)
    return if (distance < 0) distance * -1 else distance
}

/**
 * Calculate a new velocity given an initial velocity and a constant acceleration to be applied over the specified time
 * period.  Assumes the units of the parameters provided are the same.
 */
fun calculateVelocity(acceleration: Double, initialVelocity: Double, time: Double) =
        initialVelocity + acceleration * time

fun calculatePreviousVelocity(previousAcceleration: Double, currentVelocity: Double, time: Double) =
        currentVelocity - previousAcceleration * time

/**
 * Calculates the distance travelled over a period of time given an initial velocity and a constant acceleration
 * applied over the time period.  Assumes the units of the parameters provided are the same.
 */
fun calculateDisplacement(acceleration: Double, initialVelocity: Double, time: Double) =
        (initialVelocity * time) + (acceleration * 0.5 * (time * time))

/**
 * Perform raytracing to find all cells the line connecting the two given points pass through.
 * Implementation adapted from {@link http://playtechs.blogspot.ca/2007/03/raytracing-on-grid.html}
 */
fun raytrace(x0: Double, y0: Double, x1: Double, y1: Double): Set<Location> {
    val visitedCells = mutableSetOf<Location>()
    val dx = Math.abs(x1 - x0)
    val dy = Math.abs(y1 - y0)

    var x = Math.floor(x0).toInt()
    var y = Math.floor(y0).toInt()

    var n = 1
    var x_inc: Int
    var y_inc: Int
    var error: Double

    if (dx == 0.0) {
        x_inc = 0
        error = Double.POSITIVE_INFINITY
    } else if (x1 > x0) {
        x_inc = 1
        n += Math.floor(x1).toInt() - x
        error = (Math.floor(x0) + 1 - x0) * dy
    } else {
        x_inc = -1;
        n += x - Math.floor(x1).toInt()
        error = (x0 - Math.floor(x0)) * dy
    }

    if (dy == 0.0) {
        y_inc = 0
        error -= Double.POSITIVE_INFINITY
    } else if (y1 > y0) {
        y_inc = 1;
        n += Math.floor(y1).toInt() - y
        error -= (Math.floor(y0) + 1 - y0) * dx
    } else {
        y_inc = -1;
        n += y - Math.floor(y1).toInt()
        error -= (y0 - Math.floor(y0)) * dx
    }

    while (n > 0) {
        visitedCells.add(Location(x, y))

        if (error > 0) {
            y += y_inc
            error -= dx
        } else {
            x += x_inc
            error += dy
        }

        n -= 1
    }

    return visitedCells
}