package edu.unh.cs.ai.realtimesearch.environment.acrobot

import groovy.json.JsonSlurper
import java.io.InputStream

data class AcrobotConfiguration(
        val initialState: AcrobotState = defaultInitialAcrobotState,
        val endState: AcrobotState = verticalUpAcrobotState,
        val endLink1LowerBound: AcrobotLink = verticalUpAcrobotState.link1 - AcrobotLink(0.3, 0.3), /*Boone*/
        val endLink2LowerBound: AcrobotLink = verticalUpAcrobotState.link2 - AcrobotLink(0.3, 0.3), /*Boone*/
        val endLink1UpperBound: AcrobotLink = verticalUpAcrobotState.link1 + AcrobotLink(0.3, 0.3), /*Boone*/
        val endLink2UpperBound: AcrobotLink = verticalUpAcrobotState.link2 + AcrobotLink(0.3, 0.3), /*Boone*/
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
            val endLink1LowerBound = map["endLink1LowerBound"] as Map<*,*>
            val endLink2LowerBound = map["endLink2LowerBound"] as Map<*,*>
            val endLink1UpperBound = map["endLink1UpperBound"] as Map<*,*>
            val endLink2UpperBound = map["endLink2UpperBound"] as Map<*,*>
            val stateConfiguration = map["stateConfiguration"] as Map<*,*>

            return AcrobotConfiguration(
                    AcrobotState.fromMap(initialState),
                    AcrobotState.fromMap(endState),
                    AcrobotLink.fromMap(endLink1LowerBound),
                    AcrobotLink.fromMap(endLink2LowerBound),
                    AcrobotLink.fromMap(endLink1UpperBound),
                    AcrobotLink.fromMap(endLink2UpperBound),
                    AcrobotStateConfiguration.fromMap(stateConfiguration))
        }
    }
}