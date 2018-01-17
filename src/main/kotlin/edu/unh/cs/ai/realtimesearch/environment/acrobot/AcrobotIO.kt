package edu.unh.cs.ai.realtimesearch.environment.acrobot

import edu.unh.cs.ai.realtimesearch.environment.DiscretizedState
import edu.unh.cs.ai.realtimesearch.environment.Domain
import edu.unh.cs.ai.realtimesearch.environment.acrobot.configuration.AcrobotStateConfiguration
import java.io.InputStream

/**
 * Acrobot domain configuration I/O class.  Acrobot domain configurations are stored in JSON format.
 *
 * @author Mike Bogochow (mgp36@unh.edu)
 */
object AcrobotIO {
    fun parseFromStream(input: InputStream, actionDuration: Long = AcrobotStateConfiguration.defaultActionDuration): AcrobotInstance {
//        val configuration = AcrobotConfiguration.fromJsonStream(input)
//        return AcrobotInstance(DiscretizedDomain(Acrobot(configuration, actionDuration)), DiscretizedState(configuration.initialState))
        TODO()
    }
}

data class AcrobotInstance(val domain: Domain<DiscretizedState<AcrobotState>>, val initialState: DiscretizedState<AcrobotState>)
class InvalidAcrobotException(message: String, e: Exception? = null) : RuntimeException(message, e)
