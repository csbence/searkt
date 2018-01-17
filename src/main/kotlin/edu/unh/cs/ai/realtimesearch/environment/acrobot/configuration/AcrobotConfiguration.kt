package edu.unh.cs.ai.realtimesearch.environment.acrobot.configuration

import edu.unh.cs.ai.realtimesearch.environment.acrobot.AcrobotLink
import edu.unh.cs.ai.realtimesearch.environment.acrobot.AcrobotState

/**
 * An acrobot domain configuration.  The acrobot domain does not have different map instances like
 * the grid-based domains and so instances are controlled by domain configuration parameters.
 *
 * Default bound values are from:
 *
 * * Boone, Gary. "Minimum-time control of the acrobot." In Robotics and Automation, 1997. Proceedings., 1997 IEEE International Conference on, vol. 4, pp. 3281-3287. IEEE, 1997.
 *
 * @author Mike Bogochow (mgp36@unh.edu)
 *
 * @param initialState the initial state the domain should start in
 * @param goalState the goal state that the agent must reach
 * @param goalLink1LowerBound the lower goal region bound for link 1's position and velocity relative to goalState
 * @param goalLink2LowerBound the lower goal region bound for link 2's position and velocity relative to goalState
 * @param goalLink1UpperBound the upper goal region bound for link 1's position and velocity relative to goalState
 * @param goalLink2UpperBound the upper goal region bound for link 2's position and velocity relative to goalState
 * @param stateConfiguration the configuration parameters for each state in the domain
 */
data class AcrobotConfiguration(
        val initialState: AcrobotState = AcrobotState.defaultInitialState,
        val goalState: AcrobotState = AcrobotState.verticalUpState,
        val goalLink1LowerBound: AcrobotLink = AcrobotLink(0.3, 0.3), /*Boone*/
        val goalLink2LowerBound: AcrobotLink = AcrobotLink(0.3, 0.3), /*Boone*/
        val goalLink1UpperBound: AcrobotLink = AcrobotLink(0.3, 0.3), /*Boone*/
        val goalLink2UpperBound: AcrobotLink = AcrobotLink(0.3, 0.3), /*Boone*/
        val stateConfiguration: AcrobotStateConfiguration = AcrobotStateConfiguration()) {

    companion object {
        /**
         * Returns an AcrobotConfiguration from the given map.
         * @param map a map containing AcrobotConfiguration values
         */
        fun fromMap(map: Map<*, *>): AcrobotConfiguration = AcrobotConfiguration(
                AcrobotState.fromMap(map["initialState"] as Map<*, *>),
                AcrobotState.fromMap(map["goalState"] as Map<*, *>),
                AcrobotLink.fromMap(map["goalLink1LowerBound"] as Map<*, *>),
                AcrobotLink.fromMap(map["goalLink2LowerBound"] as Map<*, *>),
                AcrobotLink.fromMap(map["goalLink1UpperBound"] as Map<*, *>),
                AcrobotLink.fromMap(map["goalLink2UpperBound"] as Map<*, *>),
                AcrobotStateConfiguration.fromMap(map["stateConfiguration"] as Map<*, *>)
        )
    }

    fun toMap(): MutableMap<String, Any?> = mutableMapOf(
            "initialState" to initialState.toMap(),
            "endState" to goalState.toMap(),
            "endLink1LowerBound" to goalLink1LowerBound.toMap(),
            "endLink2LowerBound" to goalLink2LowerBound.toMap(),
            "endLink1UpperBound" to goalLink1UpperBound.toMap(),
            "endLink2UpperBound" to goalLink2UpperBound.toMap(),
            "stateConfiguration" to stateConfiguration.toMap()
    )
}