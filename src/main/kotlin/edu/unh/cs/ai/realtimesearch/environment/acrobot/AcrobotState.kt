package edu.unh.cs.ai.realtimesearch.environment.acrobot

import edu.unh.cs.ai.realtimesearch.environment.DiscretizableState

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

val verticalUpAcrobotState = AcrobotState(Math.PI / 2, 0.0, 0.0, 0.0)
val verticalDownAcrobotState = AcrobotState(3 * Math.PI / 2, 0.0, 0.0, 0.0)

// Initial state with both links pointed down
val defaultInitialAcrobotState = AcrobotState(3 * Math.PI / 2, 0.0, 0.0, 0.0)

fun roundOperation(number: Double, decimal: Double, op: (Double) -> Double): Double {
    val fraction = 1.0 / decimal
    return op(number * fraction) / fraction
}

fun roundToNearestDecimal(number: Double, decimal: Double): Double = roundOperation(number, decimal, { num -> Math.round(num) + 0.0 })
fun roundDownToDecimal(number: Double, decimal: Double): Double = roundOperation(number, decimal, { num -> Math.floor(num) })
fun roundUpToDecimal(number: Double, decimal: Double): Double = roundOperation(number, decimal, { num -> Math.ceil(num) })

/**
 * A state in the Acrobot domain consists of the positions and angular velocities of each link.
 * Instances of this class are immutable.
 */
data class AcrobotState(val linkPosition1: Double, val linkPosition2: Double, val linkVelocity1: Double, val linkVelocity2: Double, val configuration: AcrobotStateConfiguration = defaultAcrobotStateConfiguration) : DiscretizableState<AcrobotState> {

    override fun copy() = copy(linkPosition1, linkPosition2, linkVelocity1, linkVelocity2)

    // Angles naturally restricted to [0,2\pi)
    val minAngle = 0.0
    val maxAngle = 2 * Math.PI

    override fun discretize(): AcrobotState {
        return AcrobotState(
                roundDownToDecimal(linkPosition1, configuration.positionGranularity1),
                roundDownToDecimal(linkPosition2, configuration.positionGranularity2),
                roundDownToDecimal(linkVelocity1, configuration.velocityGranularity1),
                roundDownToDecimal(linkVelocity2, configuration.velocityGranularity2),
                configuration)
    }

    internal fun calculateVelocity(acceleration: Double, initialVelocity: Double, time: Double) = acceleration * time + initialVelocity
    internal fun calculateDisplacement(acceleration: Double, initialVelocity: Double, time: Double) = initialVelocity * time + 0.5 * acceleration * (time * time)

    fun calculateNextState(accelerations: Accelerations): AcrobotState {
        var newLinkPosition1 = linkPosition1 + calculateDisplacement(accelerations.linkAcceleration1, linkVelocity1, configuration.timeStep)
        var newLinkPosition2 = linkPosition2 + calculateDisplacement(accelerations.linkAcceleration2, linkVelocity2, configuration.timeStep)
        var newLinkVelocity1 = calculateVelocity(accelerations.linkAcceleration1, linkVelocity1, configuration.timeStep)
        var newLinkVelocity2 = calculateVelocity(accelerations.linkAcceleration2, linkVelocity2, configuration.timeStep)

        return AcrobotState(newLinkPosition1, newLinkPosition2, newLinkVelocity1, newLinkVelocity2, configuration).adjustLimits()
    }

    operator fun plus(rhs: AcrobotState): AcrobotState = AcrobotState(linkPosition1 + rhs.linkPosition1, linkPosition2 + rhs.linkPosition2, linkVelocity1 + rhs.linkVelocity1, linkVelocity2 + rhs.linkVelocity2, configuration)
    operator fun minus(rhs: AcrobotState): AcrobotState = AcrobotState(linkPosition1 - rhs.linkPosition1, linkPosition2 - rhs.linkPosition2, linkVelocity1 - rhs.linkVelocity1, linkVelocity2 - rhs.linkVelocity2, configuration)

    // Inertial acceleration matrix equations
    private val d11 = linkMass1 * (linkCenterOfMass1 * linkCenterOfMass1) + linkMass2 * ((linkLength1 * linkLength1) + (linkCenterOfMass2 * linkCenterOfMass2) + 2 * linkLength1 * linkCenterOfMass2 * Math.cos(linkPosition2)) + linkMomentOfInertia1 + linkMomentOfInertia2
    private val d22 = linkMass2 * (linkCenterOfMass2 * linkCenterOfMass2) + linkMomentOfInertia2
    private val d12 = linkMass2 * ((linkCenterOfMass2 * linkCenterOfMass2) + linkLength1 * linkCenterOfMass2 * Math.cos(linkPosition2)) + linkMomentOfInertia2
    private val d21 = d12

    // Coriolis and centrifugal force vector equations
    private val c1 = -1.0 * linkMass2 * linkLength1 * linkCenterOfMass2 * (linkVelocity2 * linkVelocity2) * Math.sin(linkPosition2) - 2 * linkMass2 * linkLength1 * linkCenterOfMass2 * linkVelocity1 * linkVelocity2 * Math.sin(linkPosition2)
    private val c2 = linkMass2 * linkLength1 * linkCenterOfMass2 * (linkVelocity1 * linkVelocity1) * Math.sin(linkPosition2)

    // Gravitational loading force vector equations
    private val phi1 = (linkMass1 * linkCenterOfMass1 + linkMass2 * linkLength1) * gravity * Math.cos(linkPosition1) + linkMass2 * linkCenterOfMass2 * gravity * Math.cos(linkPosition1 + linkPosition2)
    private val phi2 = linkMass2 * linkCenterOfMass2 * gravity * Math.cos(linkPosition1 + linkPosition2)

    // Acceleration equations
    data class Accelerations(val linkAcceleration1: Double, val linkAcceleration2: Double)
    fun calculateLinkAcceleration1(torque: AcrobotAction) = (-1.0 * d12 * (torque.torque - c2 - phi2) - d22 * (c1 + phi1)) / (d11 * d22 - (d12 * d12))
    fun calculateLinkAcceleration2(torque: AcrobotAction) = (d11 * (torque.torque - c2 - phi2) + d12 * (c1 + phi1)) / (d11 * d22 - (d12 * d12))
    fun calculateLinkAccelerations(torque: AcrobotAction): Accelerations = Accelerations(calculateLinkAcceleration1(torque), calculateLinkAcceleration2(torque))

    // Energy equations
    val kineticEnergy = 0.5 * linkMass1 * (linkCenterOfMass1 * linkCenterOfMass1) * (linkVelocity1 * linkVelocity1) + 0.5 * linkMomentOfInertia1 * (linkVelocity1 * linkVelocity1) + 0.5 * linkMass2 * (linkLength1 * linkLength1) * (linkVelocity1 * linkVelocity1) + 0.5 * linkMass2 * (linkCenterOfMass2 * linkCenterOfMass2) * ((linkVelocity1 * linkVelocity1) + 2 * linkVelocity1 * linkVelocity2 + (linkVelocity2 * linkVelocity2)) + linkMass2 * linkLength1 * linkCenterOfMass2 * ((linkVelocity1 * linkVelocity1) + linkVelocity1 * linkVelocity2) * Math.cos(linkPosition2) + 0.5 * linkMomentOfInertia2 * ((linkVelocity1 * linkVelocity1) + 2 * linkVelocity1 * linkVelocity2 + (linkVelocity2 * linkVelocity2))
    val potentialEnergy = linkMass1 * gravity * linkCenterOfMass1 * Math.sin(linkPosition1) + linkMass2 * gravity * linkLength1 * Math.sin(linkPosition1) + linkMass2 * gravity * linkCenterOfMass2 * Math.sin(linkPosition1 + linkPosition2)
    val totalEnergy = kineticEnergy + potentialEnergy

    /**
     * Returns whether this state is within the given bounds for each link position and velocity.
     */
    fun inBounds(lowerBound: AcrobotState, upperBound: AcrobotState): Boolean {
        val positionCondition = linkPosition1 >= lowerBound.linkPosition1 && linkPosition1 <= upperBound.linkPosition1 && linkPosition2 >= lowerBound.linkPosition2 && linkPosition2 <= upperBound.linkPosition2
        val velocityCondition = linkVelocity1 >= lowerBound.linkVelocity1 && linkVelocity1 <= upperBound.linkVelocity1 && linkVelocity2 >= lowerBound.linkVelocity2 && linkVelocity2 <= upperBound.linkVelocity2
        return positionCondition && velocityCondition
    }

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
        val position1 = adjustCircularLimit(linkPosition1, minAngle, maxAngle)
        val position2 = adjustCircularLimit(linkPosition2, minAngle, maxAngle)
        val velocity1 = snapToLimit(linkVelocity1, configuration.minAngularVelocity1, configuration.maxAngularVelocity1)
        val velocity2 = snapToLimit(linkVelocity2, configuration.minAngularVelocity2, configuration.maxAngularVelocity2)
        return AcrobotState(position1, position2, velocity1, velocity2, configuration)
    }
}

