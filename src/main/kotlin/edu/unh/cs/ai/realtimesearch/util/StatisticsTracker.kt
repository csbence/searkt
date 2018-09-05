package edu.unh.cs.ai.realtimesearch.util

import java.io.File

/**
 * Calculates the mean of the heuristic and distance functions,
 * as well as the variance of the distance function during search.
 */
class ErrorEstimator<T: ErrorTraceable> {
    private val samples = File("/home/aifs2/doylew/samples.out").bufferedWriter()
    // global statistics for the error model of f
    private var numberOfSamples = 0.0
    private var oldMeanErrorD = 0.0
    private var newMeanErrorD = 0.0
    val meanErrorDistance
        get() = if (numberOfSamples > 0) newMeanErrorD else 0.0
    private var oldMeanErrorH = 0.0
    private var newMeanErrorH = 0.0
    val meanErrorHeuristic
        get() = if (numberOfSamples > 0) newMeanErrorH else 0.0
    private var oldS = 0.0
    private var newS = 0.0
    val varianceDistance
        get() = if (numberOfSamples > 1) newS / (numberOfSamples - 1) else 0.0

    fun addSample(parent: T, child: T) {
        // (child.h - parent.h) + (child.g - parent.g)
        val dValueError = (child.d - parent.d) + 1.0 //
        val hValueError = (child.h - parent.h) + (child.g - parent.g)

        samples.write(dValueError.toString() + "|" + hValueError.toString() + "\n")

        numberOfSamples++

        if (numberOfSamples == 1.0) {
            oldMeanErrorH = hValueError
            newMeanErrorH = hValueError

            oldMeanErrorD = dValueError
            newMeanErrorD = dValueError

            oldS = 0.0
        } else {
            newMeanErrorH = oldMeanErrorH + (hValueError - oldMeanErrorH) / numberOfSamples
            newMeanErrorD = oldMeanErrorD + (dValueError - oldMeanErrorD) / numberOfSamples
            newS = oldS + ((dValueError - oldMeanErrorD) * (dValueError - newMeanErrorD))

            oldMeanErrorH = newMeanErrorH
            oldMeanErrorD = newMeanErrorD
            oldS = newS
        }
    }

    fun close() {
        samples.close()
    }
}

interface ErrorTraceable {
    val h : Double
    val d : Double
    val g: Double
}