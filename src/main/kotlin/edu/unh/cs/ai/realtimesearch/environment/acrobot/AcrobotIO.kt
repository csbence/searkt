package edu.unh.cs.ai.realtimesearch.environment.acrobot

import edu.unh.cs.ai.realtimesearch.environment.DiscretizedDomain
import edu.unh.cs.ai.realtimesearch.environment.DiscretizedState
import edu.unh.cs.ai.realtimesearch.environment.Domain
import edu.unh.cs.ai.realtimesearch.environment.acrobot.configuration.AcrobotConfiguration
import java.io.InputStream

object AcrobotIO {
    fun parseFromStream(input: InputStream): AcrobotInstance {
        val configuration = AcrobotConfiguration.fromJsonStream(input)
        return AcrobotInstance(DiscretizedDomain(Acrobot(configuration)), DiscretizedState(configuration.initialState))
    }
}

data class AcrobotInstance(val domain: Domain<DiscretizedState<AcrobotState>>, val initialState: DiscretizedState<AcrobotState>)
class InvalidAcrobotException(message: String, e: Exception? = null) : RuntimeException(message, e)
