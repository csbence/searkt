package edu.unh.cs.ai.realtimesearch.experiment.terminationCheckers

import edu.unh.cs.ai.realtimesearch.experiment.TerminationChecker

/**
 * Terminates after X amount of calls
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
    override fun reachedTermination(): Boolean {
        calls += 1
        return calls >= callLimit
    }
}