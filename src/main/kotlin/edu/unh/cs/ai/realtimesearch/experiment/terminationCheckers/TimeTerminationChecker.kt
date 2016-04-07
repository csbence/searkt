package edu.unh.cs.ai.realtimesearch.experiment.terminationCheckers

import java.util.concurrent.TimeUnit

/**
 * A termination checker based on time. Will check whether timeLimit minus [epsilon] has exceeded since init().
 */
abstract class TimeTerminationChecker(val epsilon: Long = TimeUnit.NANOSECONDS.convert(5000, TimeUnit.MICROSECONDS)) {
    abstract var startTime: Long
    abstract var timeLimit: Long

    /**
     * Sets start time to now
     */
    abstract fun init(timeBound: Long = 0)

    fun elapsedTime() = System.nanoTime() - startTime

    /**
     * Checks whether the allowed time has passed since init
     */

    var previous = 0L
    open fun reachedTermination(): Boolean { // TODO fixme
        val nanoTime = System.nanoTime()
        val b = (nanoTime - startTime + epsilon) > timeLimit
        if (b)  {
            println(elapsedTime())
            println(previous)
        } else {
            previous = elapsedTime()
        }
        return b
    }
}