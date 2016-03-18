package edu.unh.cs.ai.realtimesearch.experiment.terminationCheckers

/**
 * A termination checker based on time. Will check whether timeLimit has exceeded since init()
 *
 * @param [timeLimit] is the limit allowed after init before termination is confirmed
 */
interface TimeTerminationChecker {
    var startTime: Long
    var timeLimit: Long

    /**
     * Sets start time to now
     */
    fun init(timeBound: Long = 0)

    /**
     * Checks whether the allowed time has passed since init
     */
    fun reachedTermination() = (System.currentTimeMillis() - startTime) > timeLimit
}