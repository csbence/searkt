package edu.unh.cs.ai.realtimesearch.environment.acrobot

import java.io.InputStream
import java.util.*

object AcrobotIO {
    fun parseFromStream(input: InputStream): AcrobotInstance {
        val inputScanner = Scanner(input)

        val linkPosition1: Double
        val linkPosition2: Double
        val linkVelocity1: Double
        val linkVelocity2: Double

        try {
            linkPosition1 = inputScanner.nextLine().toDouble()
            linkPosition2 = inputScanner.nextLine().toDouble()
            linkVelocity1 = inputScanner.nextLine().toDouble()
            linkVelocity2 = inputScanner.nextLine().toDouble()
        } catch (e: NoSuchElementException) {
            throw InvalidAcrobotException("Acrobot's first, second, third, or fourth line is missing.", e)
        } catch (e: NumberFormatException) {
            throw InvalidAcrobotException("Acrobot's first, second, third, and fourthlines must be numbers", e)
        }

        val startState = AcrobotState(linkPosition1, linkPosition2, linkVelocity1, linkVelocity2)
        return AcrobotInstance(Acrobot(), startState)
    }
}

data class AcrobotInstance(val domain: Acrobot, val initialState: AcrobotState)
class InvalidAcrobotException(message: String, e: Exception? = null) : RuntimeException(message, e)
