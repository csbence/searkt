package edu.unh.cs.ai.realtimesearch.experiment.terminationCheckers

/**
 * Will never fail the test, for debugging purposes
 */
object FakeTerminationChecker : TerminationChecker {
    override fun remaining(): Long = Long.MAX_VALUE

    override fun elapsed(): Long = 0

    override fun resetTo(bound: Long) {}
    override fun notifyExpansion(expansions: Long) {}

    /**
     * Will never terminate.
     *
     * @return false
     */
    override fun reachedTermination(buffer: Long) = false
}