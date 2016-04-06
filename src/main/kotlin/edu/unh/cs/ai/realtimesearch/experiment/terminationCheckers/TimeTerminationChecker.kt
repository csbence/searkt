package edu.unh.cs.ai.realtimesearch.experiment.terminationCheckers

import java.util.concurrent.TimeUnit

/**
 * A termination checker based on time. Will check whether timeLimit minus [epsilon] has exceeded since init().
 */
abstract class TimeTerminationChecker(val epsilon: Long = TimeUnit.NANOSECONDS.convert(3000, TimeUnit.MICROSECONDS)) {
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
    open fun reachedTermination() = (System.nanoTime() - startTime + epsilon) > timeLimit
}