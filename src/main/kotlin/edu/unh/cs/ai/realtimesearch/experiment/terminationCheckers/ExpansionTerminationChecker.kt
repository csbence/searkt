package edu.unh.cs.ai.realtimesearch.experiment.terminationCheckers

/**
 * @author Bence Cserna (bence@cserna.net)
 */

abstract class ExpansionTerminationChecker : TerminationChecker {
    abstract var expansionLimit: Long
    abstract var expansionCount: Long

    override fun notifyExpansion() {
        expansionCount++
    }

    override fun reachedTermination() = expansionCount >= expansionLimit
}

class StaticExpansionTerminationChecker(override var expansionLimit: Long) : ExpansionTerminationChecker() {
    override var expansionCount: Long = 0

    override fun resetTo(bound: Long) {
        expansionCount = 0
    }
}

class DynamicExpansionTerminationChecker : ExpansionTerminationChecker() {
    override var expansionLimit: Long = 0
    override var expansionCount: Long = 0

    override fun resetTo(bound: Long) {
        expansionCount = 0
        expansionLimit = bound
    }
}