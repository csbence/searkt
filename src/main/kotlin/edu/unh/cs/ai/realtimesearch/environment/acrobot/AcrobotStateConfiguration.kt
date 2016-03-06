package edu.unh.cs.ai.realtimesearch.environment.acrobot

import java.io.File
import java.io.InputStream

val defaultAcrobotStateConfiguration = AcrobotStateConfiguration(
        // Sutton: Limit angular velocities to \dot\theta_1\in[-4\pi,4\pi] and \dot\theta_2\in[-9\pi,9\pi]
        4.0 * Math.PI,
        9.0 * Math.PI,
        -(4.0 * Math.PI),
        -(9.0 * Math.PI),
        0.2992, // 21 positions
        0.2992, // 21 positions
        0.1005, // 250 velocities
        0.0754, // 750 velocities
        0.2 // Sutton and Boone
)

data class AcrobotStateConfiguration(
        val maxAngularVelocity1: Double,
        val maxAngularVelocity2: Double,
        val minAngularVelocity1: Double,
        val minAngularVelocity2: Double,
        val positionGranularity1: Double,
        val positionGranularity2: Double,
        val velocityGranularity1: Double,
        val velocityGranularity2: Double,
        val timeStep: Double) {

    fun toJSON(): String {
        throw UnsupportedOperationException()
    }

    fun fromJSON(file: File): AcrobotConfiguration {
        throw UnsupportedOperationException()
    }

    fun fromJSON(stream: InputStream): AcrobotConfiguration {
        throw UnsupportedOperationException()
    }
}