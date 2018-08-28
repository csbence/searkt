package edu.unh.cs.ai.realtimesearch.experiment.terminationCheckers

import java.util.concurrent.TimeUnit

/**
 * A termination checker based on time. Will check whether timeLimit minus [epsilon] has exceeded since resetTo().
 * In the reachedTermination function:
 */
abstract class TimeTerminationChecker(private val epsilon: Long) : TerminationChecker {
    abstract var startTime: Long
    abstract var timeLimit: Long

    /**
     * Sets start time to now
     */
    abstract override fun resetTo(bound: Long)

    override fun notifyExpansion(expansions: Long) {}

    /**
     * Checks whether the allowed time has passed since resetTo.
     * [epsilon] and 1% of [timeLimit] are a guard against system time overflows.
     * [buffer] is a user-provided quantum for reserving time
     * for critical operations
     */
    override fun reachedTermination(buffer: Long): Boolean = (elapsed() + buffer + epsilon + timeLimit * 0.01) > timeLimit

    override fun remaining(): Long = timeLimit - elapsed()

    override fun elapsed() = System.nanoTime() - startTime
}
