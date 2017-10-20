package edu.unh.cs.ai.realtimesearch.experiment.terminationCheckers

/**
 * General interface for time and expansion based termination checkers.
 *
 * It is used by real-time planners do measure and enforce the time/expansion bound on the planning iteration.
 *
 * @author Bence Cserna (bence@cserna.net)
 */
interface TerminationChecker {
    /**
     * Check if the termination is reached.
     *
     * @return true if the termination is reached, else false.
     */
    fun reachedTermination(): Boolean

    /**
     * Notify the termination checker about expansions.
     * Planners should call this function after each expansion.
     *
     * It is only used by the expansion based termination checkers.
     */
    fun notifyExpansion(expansions: Long = 1)

    /**
     * Reset the termination checker to a given bound.
     *
     * @param bound Termination bound expressed in nanoseconds or number of expansions.
     */
    fun resetTo(bound: Long)

    /**
     * @return The number of expansion or nanoseconds until the termination.
     */
    fun remaining(): Long

    /**
     * @return The number of expansion or nanosecond elapsed since the last reset of the termination checker.
     */
    fun elapsed(): Long
}