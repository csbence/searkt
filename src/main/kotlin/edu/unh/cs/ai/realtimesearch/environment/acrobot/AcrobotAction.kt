package edu.unh.cs.ai.realtimesearch.environment.acrobot

import edu.unh.cs.ai.realtimesearch.environment.Action

/**
 * This is an action in the Acrobot domain.
 * Valid actions are a positive torque, negative torque, or no torque applied to joint 2.
 *
 * @param torque the torque to apply to the joint
 */
enum class AcrobotAction: Action {
    NONE(0, 0.0), POSITIVE(1, 1.0), NEGATIVE(2, -1.0);

    var value : Double = 0.0
    private var index: Int = 0
    private constructor(index: Int, value : Double) {
        this.value = value
        this.index = index
    }
}