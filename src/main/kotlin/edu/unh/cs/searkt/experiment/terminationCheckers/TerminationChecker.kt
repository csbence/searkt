package edu.unh.cs.searkt.experiment.terminationCheckers

import edu.unh.cs.searkt.experiment.configuration.ExperimentConfiguration
import edu.unh.cs.searkt.experiment.configuration.realtime.LookaheadType
import edu.unh.cs.searkt.experiment.configuration.realtime.TerminationType

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
     * @param buffer Optional parameter which allows users to specify a dynamic quantum of time as a "buffer" in case
     * additional operations must occur after the main operation finishes
     * @return true if the termination is reached, else false.
     */
    fun reachedTermination(buffer: Long = 0L): Boolean

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

/**
 * Factory method for termination checker from experiment configuration
 */
fun getTerminationChecker(configuration: ExperimentConfiguration, durationOverride: Long = -1L): TerminationChecker {
    val lookaheadType = configuration.lookaheadType
    val terminationType = configuration.terminationType
    val duration = if (durationOverride > -1L) durationOverride else configuration.actionDuration
    val epsilon = configuration.terminationTimeEpsilon

    val termChecker = when {
        lookaheadType == LookaheadType.DYNAMIC && terminationType == TerminationType.TIME -> MutableTimeTerminationChecker(epsilon)
        lookaheadType == LookaheadType.DYNAMIC && terminationType == TerminationType.EXPANSION -> DynamicExpansionTerminationChecker()
        lookaheadType == LookaheadType.STATIC && terminationType == TerminationType.TIME -> StaticTimeTerminationChecker(duration, epsilon)
        lookaheadType == LookaheadType.STATIC && terminationType == TerminationType.EXPANSION -> StaticExpansionTerminationChecker(duration)
        terminationType == TerminationType.UNLIMITED -> FakeTerminationChecker
        else -> throw InsufficientTerminationCriterionException("Invalid termination checker configuration")
    }

    termChecker.resetTo(duration)
    return termChecker
}