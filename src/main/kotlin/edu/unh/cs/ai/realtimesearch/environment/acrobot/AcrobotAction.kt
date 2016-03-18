package edu.unh.cs.ai.realtimesearch.environment.acrobot

import edu.unh.cs.ai.realtimesearch.environment.Action
import groovy.json.JsonSlurper
import java.io.InputStream

/**
 * This is an action in the Acrobot domain.
 * Valid actions are a positive torque, negative torque, or no torque applied to joint 2.
 */
enum class AcrobotAction: Action {
    NONE(0.0), POSITIVE(1.0), NEGATIVE(-1.0);

    var torque: Double = 0.0
    private constructor(torque: Double) {
        this.torque = torque
    }
}