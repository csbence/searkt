package edu.unh.cs.ai.realtimesearch.experiment.terminationCheckers

import edu.unh.cs.ai.realtimesearch.experiment.TerminationChecker

/**
 * Will never fail the test, for debugging purposes
 */
class FakeTerminationChecker : TerminationChecker {

    /**
     * Nothing to init
     */
    override fun init() {
    }

    /**
     * Will never terminate
     *
     * @return false
     */
    override fun reachedTermination() = false
}