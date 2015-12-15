package edu.unh.cs.ai.realtimesearch.experiment.terminationCheckers

import edu.unh.cs.ai.realtimesearch.experiment.TerminationChecker

/**
 * Will never fail the test, for debugging purposes
 */
class FakeTerminationChecker: TerminationChecker {
    override fun reachedTermination() = false

    override fun init() {

    }
}