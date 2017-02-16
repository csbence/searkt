package edu.unh.cs.ai.realtimesearch.experiment.terminationCheckers

/**
 * A termination checker based on time. Will check whether timeLimit has exceeded since resetTo()
 *
 * @param timeLimit is the limit allowed after resetTo before termination is confirmed
 */
class MutableTimeTerminationChecker : TimeTerminationChecker() {
    override var startTime: Long = 0
    override var timeLimit: Long = 0

    /**
     * Sets start time to now and the time limit to the given [timeBound].
     */
    override fun resetTo(timeBound: Long) {
        startTime = System.nanoTime()
        timeLimit = timeBound
    }
}