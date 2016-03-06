package edu.unh.cs.ai.realtimesearch.environment.acrobot

import java.io.File
import java.io.InputStream

val defaultAcrobotConfiguration = AcrobotConfiguration(
        verticalDownAcrobotState,
        verticalUpAcrobotState,
        verticalUpAcrobotState - AcrobotState(0.3, 0.3, 0.3, 0.3), // Boone
        verticalUpAcrobotState + AcrobotState(0.3, 0.3, 0.3, 0.3), // Boone
        defaultAcrobotStateConfiguration
)

data class AcrobotConfiguration(
        val initialState: AcrobotState?,
        val endState: AcrobotState,
        val endStateLowerBound: AcrobotState,
        val endStateUpperBound: AcrobotState,
        val stateConfiguration: AcrobotStateConfiguration) {

    fun toJSON(): String {
        throw UnsupportedOperationException()
    }

    fun fromJSON(file: File): AcrobotConfiguration {
        throw UnsupportedOperationException()
    }

    fun fromJSON(stream: InputStream): AcrobotConfiguration {
        throw UnsupportedOperationException()
    }
}