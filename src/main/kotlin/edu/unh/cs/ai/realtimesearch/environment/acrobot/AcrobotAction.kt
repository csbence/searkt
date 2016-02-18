package edu.unh.cs.ai.realtimesearch.environment.acrobot

import edu.unh.cs.ai.realtimesearch.environment.Action

/**
 * This is an action in the Acrobot domain.
 * Valid actions are a positive torque, negative torque, or no torque applied to joint 2.
 */
enum class AcrobotAction: Action {
    NONE(0, 0.0), POSITIVE(1, 1.0), NEGATIVE(2, -1.0);

    var torque: Double = 0.0
    private var index: Int = 0
    private constructor(index: Int, torque: Double) {
        this.torque = torque
        this.index = index
    }
}