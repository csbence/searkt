package edu.unh.cs.ai.realtimesearch.experiment.terminationCheckers

import edu.unh.cs.ai.realtimesearch.experiment.TerminationChecker

/**
 * A termination checker based on time. Will check whether timeLimit has exceeded since init()
 *
 * @param timeLimit is the limit allowed after init before termination is confirmed
 */
class TimeTerminationChecker(private val timeLimit: Double) : TerminationChecker {
    private var startTime: Long = 0

    /**
     * Sets start time to now
     */
    override fun init() {
        startTime = System.currentTimeMillis()
    }

    /**
     * Checks whether the allowed time has passed since init
     */
    override fun reachedTermination() = (System.currentTimeMillis() - startTime) > timeLimit
}