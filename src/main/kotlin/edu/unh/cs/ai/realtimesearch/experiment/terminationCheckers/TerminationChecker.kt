package edu.unh.cs.ai.realtimesearch.experiment.terminationCheckers

/**
 * @author Bence Cserna (bence@cserna.net)
 */
interface TerminationChecker {
    fun reachedTermination(): Boolean
    fun notifyExpansion()
    fun resetTo(bound: Long)
}