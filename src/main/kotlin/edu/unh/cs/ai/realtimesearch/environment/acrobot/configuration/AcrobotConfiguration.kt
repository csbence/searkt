package edu.unh.cs.ai.realtimesearch.environment.acrobot.configuration

import edu.unh.cs.ai.realtimesearch.environment.acrobot.AcrobotLink
import edu.unh.cs.ai.realtimesearch.environment.acrobot.AcrobotState
import groovy.json.JsonOutput
import groovy.json.JsonSlurper
import java.io.InputStream

data class AcrobotConfiguration(
        val initialState: AcrobotState = AcrobotState.defaultInitialState,
        val endState: AcrobotState = AcrobotState.verticalUpState,
        val endLink1LowerBound: AcrobotLink = AcrobotLink(0.3, 0.3), /*Boone*/
        val endLink2LowerBound: AcrobotLink = AcrobotLink(0.3, 0.3), /*Boone*/
        val endLink1UpperBound: AcrobotLink = AcrobotLink(0.3, 0.3), /*Boone*/
        val endLink2UpperBound: AcrobotLink = AcrobotLink(0.3, 0.3), /*Boone*/
        val stateConfiguration: AcrobotStateConfiguration = AcrobotStateConfiguration()) {

    companion object {
        /**
         * Returns an AcrobotConfiguration from the given string contents.
         * @param string a string in JSON format representing an AcrobotConfiguration
         */
        fun fromJson(string: String): AcrobotConfiguration = fromMap(JsonSlurper().parseText(string) as Map<*, *>)

        /**
         * Returns an AcrobotConfiguration from the given stream contents.
         * @param stream a stream with JSON format content representing an AcrobotConfiguration
         */
        fun fromJsonStream(stream: InputStream): AcrobotConfiguration = fromMap(JsonSlurper().parse(stream) as Map<*, *>)

        /**
         * Returns an AcrobotConfiguration from the given map.
         * @param map a map containing AcrobotConfiguration values
         */
        fun fromMap(map: Map<*, *>): AcrobotConfiguration = AcrobotConfiguration(
                AcrobotState.fromMap(map["initialState"] as Map<*, *>),
                AcrobotState.fromMap(map["endState"] as Map<*, *>),
                AcrobotLink.fromMap(map["endLink1LowerBound"] as Map<*, *>),
                AcrobotLink.fromMap(map["endLink2LowerBound"] as Map<*, *>),
                AcrobotLink.fromMap(map["endLink1UpperBound"] as Map<*, *>),
                AcrobotLink.fromMap(map["endLink2UpperBound"] as Map<*, *>),
                AcrobotStateConfiguration.fromMap(map["stateConfiguration"] as Map<*, *>)
        )
    }

    fun toMap(): MutableMap<String, Any?> = mutableMapOf(
            "initialState" to initialState.toMap(),
            "endState" to endState.toMap(),
            "endLink1LowerBound" to endLink1LowerBound.toMap(),
            "endLink2LowerBound" to endLink2LowerBound.toMap(),
            "endLink1UpperBound" to endLink1UpperBound.toMap(),
            "endLink2UpperBound" to endLink2UpperBound.toMap(),
            "stateConfiguration" to stateConfiguration.toMap()
    )

    fun toJson(): String = JsonOutput.toJson(this)
}