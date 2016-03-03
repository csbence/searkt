package edu.unh.cs.ai.realtimesearch.visualizer

import javafx.animation.Interpolator

/**
 * An easing interpolator based on Interpolator.EASE_IN and Interpolator.EASE_OUT which allows for setting a custom
 * acceleration.  If the provided acceleration is positive then the interpolator will ease in; otherwise ease out.
 */
class CustomEaseInInterpolator(val acceleration: Double) : Interpolator() {
    private val inS1 = 25.0 / 9.0
    private val inS3 = 10.0 / 9.0
    private val inS4 = 1.0 / 9.0
    private val outS1 = -25.0 / 9.0
    private val outS2 = 50.0 / 9.0
    private val outS3 = -16.0 / 9.0
    private val outS4 = 10.0 / 9.0

    override fun curve(t: Double): Double {
        if (acceleration >= 0)
            return clamp(if (t < acceleration) inS1 * t * t else inS3 * t - inS4)
        else
            return clamp(if (t > acceleration) outS1 * t * t + outS2 * t + outS3 else outS4 * t)
    }

    private fun clamp(t: Double): Double {
        return if (t < 0.0) 0.0 else if (t > 1.0) 1.0 else t
    }
}