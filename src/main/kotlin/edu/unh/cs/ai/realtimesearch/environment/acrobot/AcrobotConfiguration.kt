package edu.unh.cs.ai.realtimesearch.environment.acrobot

import groovy.json.JsonOutput
import groovy.json.JsonSlurper
import java.io.File
import java.io.InputStream

data class AcrobotConfiguration(
        val initialState: AcrobotState = defaultInitialAcrobotState,
        val endState: AcrobotState = verticalUpAcrobotState,
        val endStateLowerBound: AcrobotState = verticalUpAcrobotState - AcrobotState(0.3, 0.3, 0.3, 0.3), /*Boone*/
        val endStateUpperBound: AcrobotState = verticalUpAcrobotState + AcrobotState(0.3, 0.3, 0.3, 0.3), /*Boone*/
        val stateConfiguration: AcrobotStateConfiguration = AcrobotStateConfiguration()) {

    companion object {
        /**
         * Returns an AcrobotConfiguration from the given string contents.
         * @param string a string in JSON format representing an AcrobotConfiguration
         */
        fun fromString(string: String): AcrobotConfiguration = fromMap(JsonSlurper().parseText(string) as Map<*,*>)

        /**
         * Returns an AcrobotConfiguration from the given stream contents.
         * @param stream a stream with JSON format content representing an AcrobotConfiguration
         */
        fun fromStream(stream: InputStream): AcrobotConfiguration = fromMap(JsonSlurper().parse(stream) as Map<*,*>)

        /**
         * Returns an AcrobotConfiguration from the given map.
         * @param map a map containing AcrobotConfiguration values
         */
        fun fromMap(map: Map<*,*>): AcrobotConfiguration {
            val initialState = map["initialState"] as Map<*,*>
            val endState = map["endState"] as Map<*,*>
            val endStateLowerBound = map["endStateLowerBound"] as Map<*,*>
            val endStateUpperBound = map["endStateUpperBound"] as Map<*,*>
            val stateConfiguration = map["stateConfiguration"] as Map<*,*>

            return AcrobotConfiguration(
                    AcrobotState.fromMap(initialState),
                    AcrobotState.fromMap(endState),
                    AcrobotState.fromMap(endStateLowerBound),
                    AcrobotState.fromMap(endStateUpperBound),
                    AcrobotStateConfiguration.fromMap(stateConfiguration))
        }
    }

    /**
     * Writes the AcrobotStateConfiguration to a string in JSON format
     */
    override fun toString(): String = JsonOutput.toJson(this)
}