package edu.unh.cs.ai.realtimesearch.environment.acrobot

import edu.unh.cs.ai.realtimesearch.environment.DiscretizableState
import edu.unh.cs.ai.realtimesearch.environment.acrobot.configuration.AcrobotStateConfiguration
import groovy.json.JsonSlurper
import java.math.BigDecimal

val verticalUpAcrobotState = AcrobotState(AcrobotLink(Math.PI / 2, 0.0), AcrobotLink.ZERO)
val verticalDownAcrobotState = AcrobotState(AcrobotLink(3 * Math.PI / 2, 0.0), AcrobotLink.ZERO)

// Initial state with both links pointed down
val defaultInitialAcrobotState = verticalDownAcrobotState

val defaultFloatAccuracy = 0.00001

fun doubleNearEquals(a: Double, b: Double, accuracy: Double = defaultFloatAccuracy): Boolean {
    return a == b || Math.abs(a - b) < accuracy
}

fun roundOperation(number: Double, decimal: Double, op: (Double, Double) -> Double, accuracy: Double = defaultFloatAccuracy): Double {
    val fraction = 1.0 / decimal
    val operand = if (doubleNearEquals(decimal, number)) 1.0 else number * fraction
    return op(operand, accuracy) / fraction
}

// Add accuracy to number due to floating point calculations causing issues with rounding operators
fun roundToNearestDecimal(number: Double, decimal: Double): Double = roundOperation(number, decimal, { num, accuracy -> Math.round(num + accuracy) + 0.0 })
fun roundDownToDecimal(number: Double, decimal: Double): Double = roundOperation(number, decimal, { num, accuracy -> Math.floor(num + accuracy) })
fun roundUpToDecimal(number: Double, decimal: Double): Double = roundOperation(number, decimal, { num, accuracy -> Math.ceil(num + accuracy) })

/**
 * Represents one link of an Acrobot.
 */
data class AcrobotLink(val position: Double, val velocity: Double) {
    companion object {
        // Angles naturally restricted to [0,2\pi)
        val minAngle = 0.0
        val maxAngle = 2 * Math.PI

        fun fromMap(map: Map<*,*>): AcrobotLink {
            val position = (map["position"] as BigDecimal).toDouble()
            val velocity = (map["velocity"] as BigDecimal).toDouble()
            return AcrobotLink(position, velocity)
        }

        val ZERO = AcrobotLink(0.0, 0.0)
    }

    operator fun plus(rhs: AcrobotLink): AcrobotLink = AcrobotLink(position + rhs.position, velocity + rhs.velocity)
    operator fun minus(rhs: AcrobotLink): AcrobotLink = AcrobotLink(position - rhs.position, velocity - rhs.velocity)

    fun inBounds(lowerBound: AcrobotLink, upperBound: AcrobotLink): Boolean {
        val positionCondition = position >= lowerBound.position && position <= upperBound.position
        val velocityCondition = velocity >= lowerBound.velocity && velocity <= upperBound.velocity
        return positionCondition && velocityCondition
    }
}

/**
 * A state in the Acrobot domain consists of the positions and angular velocities of each link.
 * Instances of this class are immutable.
 */
data class AcrobotState(val link1: AcrobotLink, val link2: AcrobotLink, val configuration: AcrobotStateConfiguration = AcrobotStateConfiguration()) : DiscretizableState<AcrobotState> {

    constructor(linkPosition1: Double, linkPosition2: Double, linkVelocity1: Double, linkVelocity2: Double, configuration: AcrobotStateConfiguration = AcrobotStateConfiguration())
         : this(AcrobotLink(linkPosition1, linkVelocity1), AcrobotLink(linkPosition2, linkVelocity2), configuration)

    override fun copy() = copy(link1, link2, configuration)

    companion object {
        // Constants
        // Given in Sutton and Barto 1998 as well as Boone 1997
        val linkMass1: Double = 1.0
        val linkMass2: Double = 1.0
        val linkLength1: Double = 1.0
        val linkLength2: Double = 1.0
        val linkCenterOfMass1: Double = 0.5
        val linkCenterOfMass2: Double = 0.5
        val linkMomentOfInertia1: Double = 1.0
        val linkMomentOfInertia2: Double = 1.0
        val gravity: Double = 9.8

        fun fromString(string: String): AcrobotState = fromMap(JsonSlurper().parseText(string) as Map<*,*>)

        fun fromMap(map: Map<*,*>): AcrobotState {
            val link1 = map["link1"] as Map<*,*>
            val link2 = map["link2"] as Map<*,*>
            val configurationObject = map["configuration"]

            if (configurationObject != null)
                return AcrobotState(AcrobotLink.fromMap(link1), AcrobotLink.fromMap(link2), AcrobotStateConfiguration.fromMap(configurationObject as Map<*,*>))
            else
                return AcrobotState(AcrobotLink.fromMap(link1), AcrobotLink.fromMap(link2))
        }
    }

    override fun discretize(): AcrobotState {
        return AcrobotState(
                AcrobotLink(roundDownToDecimal(link1.position, configuration.positionGranularity1), roundDownToDecimal(link1.velocity, configuration.velocityGranularity1)),
                AcrobotLink(roundDownToDecimal(link2.position, configuration.positionGranularity2), roundDownToDecimal(link2.velocity, configuration.velocityGranularity2)),
                configuration)
    }

    internal fun calculateVelocity(acceleration: Double, initialVelocity: Double, time: Double) = acceleration * time + initialVelocity
    internal fun calculateDisplacement(acceleration: Double, initialVelocity: Double, time: Double) = initialVelocity * time + 0.5 * acceleration * (time * time)

    fun calculateNextState(accelerations: Accelerations): AcrobotState {
        var newLinkPosition1 = link1.position + calculateDisplacement(accelerations.linkAcceleration1, link1.velocity, configuration.timeStep)
        var newLinkPosition2 = link2.position + calculateDisplacement(accelerations.linkAcceleration2, link2.velocity, configuration.timeStep)
        var newLinkVelocity1 = calculateVelocity(accelerations.linkAcceleration1, link1.velocity, configuration.timeStep)
        var newLinkVelocity2 = calculateVelocity(accelerations.linkAcceleration2, link2.velocity, configuration.timeStep)

        return AcrobotState(
                AcrobotLink(newLinkPosition1, newLinkVelocity1),
                AcrobotLink(newLinkPosition2, newLinkVelocity2),
                configuration).adjustLimits()
    }

    operator fun plus(rhs: AcrobotState): AcrobotState = AcrobotState(link1 + rhs.link1, link2 + rhs.link2, configuration)
    operator fun minus(rhs: AcrobotState): AcrobotState = AcrobotState(link1 - rhs.link1, link2 - rhs.link2, configuration)

    // Inertial acceleration matrix equations
    private val d11 = linkMass1 * (linkCenterOfMass1 * linkCenterOfMass1) + linkMass2 * ((linkLength1 * linkLength1) + (linkCenterOfMass2 * linkCenterOfMass2) + 2 * linkLength1 * linkCenterOfMass2 * Math.cos(link2.position)) + linkMomentOfInertia1 + linkMomentOfInertia2
    private val d22 = linkMass2 * (linkCenterOfMass2 * linkCenterOfMass2) + linkMomentOfInertia2
    private val d12 = linkMass2 * ((linkCenterOfMass2 * linkCenterOfMass2) + linkLength1 * linkCenterOfMass2 * Math.cos(link2.position)) + linkMomentOfInertia2
    private val d21 = d12

    // Coriolis and centrifugal force vector equations
    private val c1 = -1.0 * linkMass2 * linkLength1 * linkCenterOfMass2 * (link2.velocity * link2.velocity) * Math.sin(link2.position) - 2 * linkMass2 * linkLength1 * linkCenterOfMass2 * link1.velocity * link2.velocity * Math.sin(link2.position)
    private val c2 = linkMass2 * linkLength1 * linkCenterOfMass2 * (link1.velocity * link1.velocity) * Math.sin(link2.position)

    // Gravitational loading force vector equations
    private val phi1 = (linkMass1 * linkCenterOfMass1 + linkMass2 * linkLength1) * gravity * Math.cos(link1.position) + linkMass2 * linkCenterOfMass2 * gravity * Math.cos(link1.position + link2.position)
    private val phi2 = linkMass2 * linkCenterOfMass2 * gravity * Math.cos(link1.position + link2.position)

    // Acceleration equations
    data class Accelerations(val linkAcceleration1: Double, val linkAcceleration2: Double)
    fun calculateLinkAcceleration1(torque: AcrobotAction) = (-1.0 * d12 * (torque.torque - c2 - phi2) - d22 * (c1 + phi1)) / (d11 * d22 - (d12 * d12))
    fun calculateLinkAcceleration2(torque: AcrobotAction) = (d11 * (torque.torque - c2 - phi2) + d12 * (c1 + phi1)) / (d11 * d22 - (d12 * d12))
    fun calculateLinkAccelerations(torque: AcrobotAction): Accelerations = Accelerations(calculateLinkAcceleration1(torque), calculateLinkAcceleration2(torque))

    // Energy equations
    val kineticEnergy = 0.5 * linkMass1 * (linkCenterOfMass1 * linkCenterOfMass1) * (link1.velocity * link1.velocity) + 0.5 * linkMomentOfInertia1 * (link1.velocity * link1.velocity) + 0.5 * linkMass2 * (linkLength1 * linkLength1) * (link1.velocity * link1.velocity) + 0.5 * linkMass2 * (linkCenterOfMass2 * linkCenterOfMass2) * ((link1.velocity * link1.velocity) + 2 * link1.velocity * link2.velocity + (link2.velocity * link2.velocity)) + linkMass2 * linkLength1 * linkCenterOfMass2 * ((link1.velocity * link1.velocity) + link1.velocity * link2.velocity) * Math.cos(link2.position) + 0.5 * linkMomentOfInertia2 * ((link1.velocity * link1.velocity) + 2 * link1.velocity * link2.velocity + (link2.velocity * link2.velocity))
    val potentialEnergy = linkMass1 * gravity * linkCenterOfMass1 * Math.sin(link1.position) + linkMass2 * gravity * linkLength1 * Math.sin(link1.position) + linkMass2 * gravity * linkCenterOfMass2 * Math.sin(link1.position + link2.position)
    val totalEnergy = kineticEnergy + potentialEnergy

    /**
     * Returns whether this state is within the given bounds for each link position and velocity.
     */
    fun inBounds(lowerBound: AcrobotState, upperBound: AcrobotState): Boolean = link1.inBounds(lowerBound.link1, upperBound.link1) && link2.inBounds(lowerBound.link2, upperBound.link2)

    /**
     * Adjust a value according to the given limits.
     */
    private fun snapToLimit(value: Double, minLimit: Double, maxLimit: Double): Double {
        if (value < minLimit)
            return minLimit
        else if (value > maxLimit)
            return maxLimit
        return value
    }

    /**
     * Adjust a value according to the given limits where the value should wrap around when exceeding a limit.
     */
    private fun adjustCircularLimit(value: Double, minLimit: Double, maxLimit: Double): Double {
        var newValue = value
        while (newValue < minLimit)
            newValue = maxLimit - Math.abs(newValue)
        while (newValue> maxLimit)
            newValue = minLimit + (Math.abs(newValue) - maxLimit)
        return newValue
    }

    /**
     * Checks that the values in the state are valid.  If they are, returns the state unchanged; if not then
     * returns a new state with any values that are outside of their limits adjusted to their closest limit.
     */
    fun adjustLimits(): AcrobotState {
        val position1 = adjustCircularLimit(link1.position, AcrobotLink.minAngle, AcrobotLink.maxAngle)
        val position2 = adjustCircularLimit(link2.position, AcrobotLink.minAngle, AcrobotLink.maxAngle)
        val velocity1 = snapToLimit(link1.velocity, configuration.minAngularVelocity1, configuration.maxAngularVelocity1)
        val velocity2 = snapToLimit(link2.velocity, configuration.minAngularVelocity2, configuration.maxAngularVelocity2)
        return AcrobotState(AcrobotLink(position1, velocity1), AcrobotLink(position2, velocity2), configuration)
    }
}
