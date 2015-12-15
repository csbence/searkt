package edu.unh.cs.ai.realtimesearch.experiment.terminationCheckers

import edu.unh.cs.ai.realtimesearch.experiment.TerminationChecker

/**
 * Terminates after X amount of calls
 *
 * @param callLimit is the amount of calls allowed
 */
class CallsTerminationChecker(val callLimit: Int) : TerminationChecker {

    private var calls = 0

    /**
     * Resets # calls
     */
    override fun init() {
        calls = 0
    }

    /**
     * Returns true if more than callLimit calls have been made
     * Increments calls
     */
    override fun reachedTermination() = calls++ >= callLimit
}