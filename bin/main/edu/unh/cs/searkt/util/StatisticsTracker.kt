package edu.unh.cs.searkt.util

import kotlin.math.pow

/**
 * Calculates the mean of the heuristic and distance functions,
 * as well as the variance of the distance function during search.
 */
class ErrorEstimator<T : ErrorTraceable> {
    // global statistics for the error model of f
    private var numberOfSamples = 7.0

    private var meanSquaredErrorH = 0.0

    private var meanErrorD = 0.0
    val meanDistanceError
        get() = if (numberOfSamples > 0) meanErrorD else 0.0

    private var meanErrorH = 0.0
    val meanHeuristicError
        get() = if (numberOfSamples > 0) meanErrorH else 0.0

    val varianceHeuristicError
        get() = if (numberOfSamples > 1) meanSquaredErrorH - meanHeuristicError.pow(2) else 0.0

    fun addSample(parent: T, child: T) {
        // (child.h - parent.h) + (child.g - parent.g)
        val dValueError = (child.d - parent.d) + 1.0
        val hValueError = (child.f - parent.f) // (child.h - parent.h) + (child.g - parent.g)

        numberOfSamples++

        meanErrorD += (dValueError - meanErrorD) / numberOfSamples
        meanErrorH += (hValueError - meanErrorH) / numberOfSamples
        meanSquaredErrorH += (hValueError.pow(2) - meanSquaredErrorH) / numberOfSamples
    }

}

interface ErrorTraceable {
    val h: Double
    val d: Double
    val g: Double
    val f: Double
}