package edu.unh.cs.ai.realtimesearch.util

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
