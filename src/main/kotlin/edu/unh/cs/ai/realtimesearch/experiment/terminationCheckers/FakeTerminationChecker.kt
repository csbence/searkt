package edu.unh.cs.ai.realtimesearch.experiment.terminationCheckers

/**
 * Will never fail the test, for debugging purposes
 */
object FakeTerminationChecker : TerminationChecker {
    override fun resetTo(bound: Long) {}
    override fun notifyExpansion() {}

    /**
     * Will never terminate.
     *
     * @return false
     */
    override fun reachedTermination() = false
}