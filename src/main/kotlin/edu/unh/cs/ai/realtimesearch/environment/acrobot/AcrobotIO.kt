package edu.unh.cs.ai.realtimesearch.environment.acrobot

import edu.unh.cs.ai.realtimesearch.environment.vacuumworld.VacuumWorldIO
import java.io.InputStream

object AcrobotIO {

}

data class AcrobotInstance(val domain: Acrobot, val initialState: AcrobotState)
class InvalidAcrobotException(message: String, e: Exception? = null) : RuntimeException(message, e)
