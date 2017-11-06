package edu.unh.cs.ai.realtimesearch.environment.acrobot.configuration

import java.math.BigDecimal
import java.util.concurrent.TimeUnit

/**
 * An acrobot domain state configuration.
 *
 * Default bound values are from:
 *
 * * Boone, Gary. "Minimum-time control of the acrobot." In Robotics and Automation, 1997. Proceedings., 1997 IEEE International Conference on, vol. 4, pp. 3281-3287. IEEE, 1997.
 * * Sutton, Richard S. "Generalization in reinforcement learning: Successful examples using sparse coarse coding." Advances in neural information processing systems (1996): 1038-1044.
 *
 * @author Mike Bogochow (mgp36@unh.edu)
 *
 * @param maxAngularVelocity1 the maximum angular velocity of link 1
 * @param maxAngularVelocity2 the maximum angular velocity of link 2
 * @param minAngularVelocity1 the minimum angular velocity of link 1
 * @param minAngularVelocity2 the minimum angular velocity of link 2
 * @param positionGranularity1 the discretization granularity of link 1's position
 * @param positionGranularity2 the discretization granularity of link 2's position
 * @param velocityGranularity1 the discretization granularity of link 1's velocity
 * @param velocityGranularity2 the discretization granularity of link 2's velocity
 */
data class AcrobotStateConfiguration(
        // Sutton: Limit angular velocities to \dot\theta_1\in[-4\pi,4\pi] and \dot\theta_2\in[-9\pi,9\pi]
        val maxAngularVelocity1: Double = 4.0 * Math.PI,
        val maxAngularVelocity2: Double = 9.0 * Math.PI,
        val minAngularVelocity1: Double = -(4.0 * Math.PI),
        val minAngularVelocity2: Double = -(9.0 * Math.PI),
        val positionGranularity1: Double = 0.2991993003, // 21 unique positions in range [0,2pi)
        val positionGranularity2: Double = 0.2991993003,
        val velocityGranularity1: Double = 0.1045309649, // 250 unique velocities in default velocity range
        val velocityGranularity2: Double = 0.0767315570) { // 750 unique velocities in default velocity range

    companion object {
        val defaultActionDuration = TimeUnit.NANOSECONDS.convert(200, TimeUnit.MILLISECONDS) // Sutton and Boone

        /**
         * Returns an AcrobotStateConfiguration from the given string contents.
         * @param string a string in JSON format representing an AcrobotStateConfiguration
         */
//        fun fromJson(string: String): AcrobotStateConfiguration = fromMap(JsonSlurper().parseText(string) as Map<*, *>)

        /**
         * Returns an AcrobotStateConfiguration from the given stream contents.
         * @param stream a stream with JSON format content representing an AcrobotStateConfiguration
         */
//        fun fromJsonStream(stream: InputStream): AcrobotStateConfiguration = fromMap(JsonSlurper().parse(stream) as Map<*, *>)

        /**
         * Returns an AcrobotStateConfiguration from the given map.
         * @param map a map containing AcrobotStateConfiguration values
         */
        fun fromMap(map: Map<*, *>): AcrobotStateConfiguration = AcrobotStateConfiguration(
                (map["maxAngularVelocity1"] as BigDecimal).toDouble(),
                (map["maxAngularVelocity2"] as BigDecimal).toDouble(),
                (map["minAngularVelocity1"] as BigDecimal).toDouble(),
                (map["minAngularVelocity2"] as BigDecimal).toDouble(),
                (map["positionGranularity1"] as BigDecimal).toDouble(),
                (map["positionGranularity2"] as BigDecimal).toDouble(),
                (map["velocityGranularity1"] as BigDecimal).toDouble(),
                (map["velocityGranularity2"] as BigDecimal).toDouble()
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
            "velocityGranularity2" to velocityGranularity2
    )

//    fun toJson(): String = JsonOutput.toJson(this)
}
