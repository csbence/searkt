package edu.unh.cs.ai.realtimesearch.environment.acrobot

import edu.unh.cs.ai.realtimesearch.environment.State
import org.apache.commons.math3.linear.RealMatrix
import org.apache.commons.math3.linear.MatrixUtils

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

/**
 * A state in the Acrobot domain consists of the positions and angular velocities of each link.
 */
data class AcrobotState(val linkPosition1: Double, val linkPosition2: Double, val linkVelocity1: Double, val linkVelocity2: Double) : State<AcrobotState> {
    override fun copy() = copy(linkPosition1, linkPosition2, linkVelocity1, linkVelocity2)

    // TODO need to override equals and hashcode ?

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
    data class Accelerations(val linkAcceleration1: Double, val LinkAcceleration2: Double)
//    private fun calculateLinkAcceleration1(linkAcceleration2: Double) = (d12 * linkAcceleration2 + c1 + phi1) / (-1.0 * d11)
//    fun calculateLinkAcceleration1(torque: AcrobotAction) = calculateLinkAcceleration1(calculateLinkAcceleration2(torque))
    fun calculateLinkAcceleration1(torque: AcrobotAction) = (-1.0 * d12 * (torque.torque - c2 - phi2) - d22 * (c1 + phi1)) / (d11 * d22 - (d12 * d12))
    fun calculateLinkAcceleration2(torque: AcrobotAction) = (d11 * (torque.torque - c2 - phi2) + d12 * (c1 + phi1)) / (d11 * d22 - (d12 * d12))
    fun calculateLinkAccelerations(torque: AcrobotAction): Accelerations = Accelerations(calculateLinkAcceleration1(torque), calculateLinkAcceleration2(torque))/*{
        val linkAcceleration2 = calculateLinkAcceleration2(torque)
        return Accelerations(calculateLinkAcceleration1(linkAcceleration2), linkAcceleration2)
    }*/
    fun calculateForwardDynamics(torque: AcrobotAction): RealMatrix {
        val (linkAcceleration1, linkAcceleration2) = calculateLinkAccelerations(torque)
        return MatrixUtils.createColumnRealMatrix(doubleArrayOf(linkAcceleration1, linkAcceleration2))
    }

    // Energy equations
    val kineticEnergy = 0.5 * linkMass1 * (linkCenterOfMass1 * linkCenterOfMass1) * (linkVelocity1 * linkVelocity1) + 0.5 * linkMomentOfInertia1 * (linkVelocity1 * linkVelocity1) + 0.5 * linkMass2 * (linkLength1 * linkLength1) * (linkVelocity1 * linkVelocity1) + 0.5 * linkMass2 * (linkCenterOfMass2 * linkCenterOfMass2) * ((linkVelocity1 * linkVelocity1) + 2 * linkVelocity1 * linkVelocity2 + (linkVelocity2 * linkVelocity2)) + linkMass2 * linkLength1 * linkCenterOfMass2 * ((linkVelocity1 * linkVelocity1) + linkVelocity1 * linkVelocity2) * Math.cos(linkPosition2) + 0.5 * linkMomentOfInertia2 * ((linkVelocity1 * linkVelocity1) + 2 * linkVelocity1 * linkVelocity2 + (linkVelocity2 * linkVelocity2))
    val potentialEnergy = linkMass1 * gravity * linkCenterOfMass1 * Math.sin(linkPosition1) + linkMass2 * gravity * linkLength1 * Math.sin(linkPosition1) + linkMass2 * gravity * linkCenterOfMass2 * Math.sin(linkPosition1 + linkPosition2)
    val totalEnergy = kineticEnergy + potentialEnergy

//    var inertialAccelerationMatrix: RealMatrix
//    var coriolisCentrifugalForceVector: RealMatrix
//    var gravitationalLoadingForceVector: RealMatrix
//    init {
//        val dValues = Array(2) { DoubleArray(2) }
//        dValues[0][0] = d11
//        dValues[0][1] = d12
//        dValues[1][0] = d21
//        dValues[1][1] = d22
//        inertialAccelerationMatrix = MatrixUtils.createRealMatrix(dValues)
//        coriolisCentrifugalForceVector = MatrixUtils.createColumnRealMatrix(doubleArrayOf(c1, c2))
//        gravitationalLoadingForceVector = MatrixUtils.createColumnRealMatrix(doubleArrayOf(phi1, phi2))
//    }
}

