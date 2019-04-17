package edu.unh.cs.searkt.experiment.terminationCheckers

/**
 * @author Bence Cserna (bence@cserna.net)
 */

abstract class ExpansionTerminationChecker : TerminationChecker {
    abstract var expansionLimit: Long
    abstract var expansionCount: Long

    override fun notifyExpansion(expansions: Long) {
        expansionCount += expansions
    }

    override fun reachedTermination(buffer: Long) = (expansionCount + buffer) >= expansionLimit

    override fun remaining(): Long = expansionLimit - expansionCount

    override fun elapsed(): Long = expansionCount
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
