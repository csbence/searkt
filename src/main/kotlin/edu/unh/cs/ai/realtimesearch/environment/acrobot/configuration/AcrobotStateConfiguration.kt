package edu.unh.cs.ai.realtimesearch.environment.acrobot.configuration

import groovy.json.JsonSlurper
import java.io.InputStream
import java.math.BigDecimal

data class AcrobotStateConfiguration(
        // Sutton: Limit angular velocities to \dot\theta_1\in[-4\pi,4\pi] and \dot\theta_2\in[-9\pi,9\pi]
        val maxAngularVelocity1: Double = 4.0 * Math.PI,
        val maxAngularVelocity2: Double = 9.0 * Math.PI,
        val minAngularVelocity1: Double = -(4.0 * Math.PI),
        val minAngularVelocity2: Double = -(9.0 * Math.PI),
        val positionGranularity1: Double = 0.2991993003, // 21 positions
        val positionGranularity2: Double = 0.2991993003,
        val velocityGranularity1: Double = 0.1045309649, // 250 velocities
        val velocityGranularity2: Double = 0.0767315570, // 750 velocities
        val timeStep: Long = 200000000) { // Sutton and Boone

    companion object {
        /**
         * Returns an AcrobotStateConfiguration from the given string contents.
         * @param string a string in JSON format representing an AcrobotStateConfiguration
         */
        fun fromJson(string: String): AcrobotStateConfiguration = fromMap(JsonSlurper().parseText(string) as Map<*, *>)

        /**
         * Returns an AcrobotStateConfiguration from the given stream contents.
         * @param stream a stream with JSON format content representing an AcrobotStateConfiguration
         */
        fun fromJsonStream(stream: InputStream): AcrobotStateConfiguration = fromMap(JsonSlurper().parse(stream) as Map<*, *>)

        /**
         * Returns an AcrobotStateConfiguration from the given map.
         * @param map a map containing AcrobotStateConfiguration values
         */
        fun fromMap(map: Map<*,*>): AcrobotStateConfiguration = AcrobotStateConfiguration(
                    (map["maxAngularVelocity1"] as BigDecimal).toDouble(),
                    (map["maxAngularVelocity2"] as BigDecimal).toDouble(),
                    (map["minAngularVelocity1"] as BigDecimal).toDouble(),
                    (map["minAngularVelocity2"] as BigDecimal).toDouble(),
                    (map["positionGranularity1"] as BigDecimal).toDouble(),
                    (map["positionGranularity2"] as BigDecimal).toDouble(),
                    (map["velocityGranularity1"] as BigDecimal).toDouble(),
                    (map["velocityGranularity2"] as BigDecimal).toDouble(),
                    (map["timeStep"] as Int).toLong()
        )
    }

    fun toMap(): MutableMap<String, Any?> = mutableMapOf(
            "maxAngularVelocity1" to maxAngularVelocity1,
            "maxAngularVelocity2" to maxAngularVelocity2,
            "minAngularVelocity1" to minAngularVelocity1,
            "minAngularVelocity2" to minAngularVelocity2,
            "positionGranularity1" to positionGranularity1,
            "positionGranularity2" to positionGranularity2,
            "velocityGranularity1" to velocityGranularity1,
            "velocityGranularity2" to velocityGranularity2,
            "timeStep" to timeStep
    )
}
