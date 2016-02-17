package edu.unh.cs.ai.realtimesearch.environment.doubleintegrator

import edu.unh.cs.ai.realtimesearch.environment.Action
import edu.unh.cs.ai.realtimesearch.environment.location.Location

/**
 * This is An action in the vacuumworld is simply an enum:
 * an action for each direction and the vacuum action.
 *
 * @param index: the type of action, each return different relative locations
 */
enum class DoubleIntegratorAction(val index: Int) : Action {
    DEG_0(0), DEG_20(1), DEG_40(2), DEG_60(3), DEG_80(4), DEG_100(5), DEG_120(6), DEG_140(7),
    DEG_160(8), DEG_180(9), DEG_200(10), DEG_220(11), DEG_240(12), DEG_260(13), DEG_280(14),
    DEG_300(15), DEG_320(16), DEG_340(17), ACC(18), DEC(19)
}