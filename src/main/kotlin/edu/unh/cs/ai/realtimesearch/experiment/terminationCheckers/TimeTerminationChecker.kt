package edu.unh.cs.ai.realtimesearch.experiment.terminationCheckers

import java.util.concurrent.TimeUnit

/**
 * A termination checker based on time. Will check whether timeLimit minus [epsilon] has exceeded since resetTo().
 */
abstract class TimeTerminationChecker(val epsilon: Long = TimeUnit.NANOSECONDS.convert(2000, TimeUnit.MICROSECONDS)) : TerminationChecker {
    abstract var startTime: Long
    abstract var timeLimit: Long

    /**
     * Sets start time to now
     */
    abstract override fun resetTo(timeBound: Long)

    fun elapsedTime() = System.nanoTime() - startTime

    override fun notifyExpansion() {}

    /**
     * Checks whether the allowed time has passed since resetTo
     */
    override fun reachedTermination(): Boolean = (System.nanoTime() - startTime + epsilon + timeLimit * 0.01) > timeLimit
}