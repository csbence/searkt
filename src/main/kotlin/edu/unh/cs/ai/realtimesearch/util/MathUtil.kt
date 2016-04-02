package edu.unh.cs.ai.realtimesearch.util

import java.util.concurrent.TimeUnit

/**
 * The accuracy to use when comparing double numbers.
 */
val defaultFloatAccuracy = 0.00001

/**
 * Compare two double for equality up to a given accuracy.
 */
fun doubleNearEquals(a: Double, b: Double, accuracy: Double = defaultFloatAccuracy): Boolean {
    return a == b || Math.abs(a - b) < accuracy
}

/**
 * Round a number to a given decimal provided the type of rounding operation.
 */
fun roundOperation(number: Double, decimal: Double, op: (Double, Double) -> Double, accuracy: Double = defaultFloatAccuracy): Double {
    val fraction = 1.0 / decimal
    val operand = if (doubleNearEquals(decimal, number)) 1.0 else number * fraction
    return op(operand, accuracy) / fraction
}

// Rounding wrapper methods
// Add accuracy to number due to floating point calculations causing issues with rounding operators (1 != 1.000000...)
fun roundToNearestDecimal(number: Double, decimal: Double): Double = roundOperation(number, decimal, { num, accuracy -> Math.round(num + accuracy) + 0.0 })
fun roundDownToDecimal(number: Double, decimal: Double): Double = roundOperation(number, decimal, { num, accuracy -> Math.floor(num + accuracy) })
fun roundUpToDecimal(number: Double, decimal: Double): Double = roundOperation(number, decimal, { num, accuracy -> Math.ceil(num + accuracy) })

/**
 * Convert time in ns to double in seconds
 */
fun convertTime (time: Long): Double = TimeUnit.SECONDS.convert(time, TimeUnit.NANOSECONDS).toDouble()

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