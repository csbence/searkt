package edu.unh.cs.ai.realtimesearch.experiment.terminationCheckers

/**
 * Will never fail the test, for debugging purposes
 */
class FakeTerminationChecker() : TimeTerminationChecker {
    override var startTime: Long = 0
    override var timeLimit: Long = 0

    /**
     * Does nothing.
     */
    override fun init(timeBound: Long) {
    }

    /**
     * Will never terminate.
     *
     * @return false
     */
    override fun reachedTermination() = false
}