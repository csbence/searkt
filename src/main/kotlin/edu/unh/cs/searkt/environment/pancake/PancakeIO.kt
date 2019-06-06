package edu.unh.cs.searkt.environment.pancake

import java.io.InputStream
import java.util.*

object PancakeIO {

    fun parseFromStream(input: InputStream, actionDuration: Long): PancakeProblemInstance {
        val inputScanner = Scanner(input)
        val problemSize = inputScanner.nextInt()
        inputScanner.nextLine() // skip empty line

        val startOrdering = ByteArray(problemSize)
        val endOrdering = ByteArray(problemSize)

        (0 until problemSize).forEach { startOrdering[it] = inputScanner.nextByte() }
        inputScanner.nextLine() // skip empty line

        (0 until problemSize).forEach { endOrdering[it] = inputScanner.nextByte() }

        val puzzleVariant = 0 // default
        val initialState = PancakeState(startOrdering, 0)

        return PancakeProblemInstance(PancakeProblem(startOrdering, endOrdering, puzzleVariant), initialState)

    }
}

class InvalidPancakeProblemException(message: String, e: Exception? = null) : RuntimeException(message, e)
data class PancakeProblemInstance(val domain: PancakeProblem, val initialState: PancakeState)
