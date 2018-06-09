package edu.unh.cs.ai.realtimesearch.experiment.terminationCheckers

/**
 * A termination checker based on time. Will check whether timeLimit has exceeded since resetTo()
 *
 * @param timeLimit is the limit allowed after resetTo before termination is confirmed
 */
class StaticTimeTerminationChecker(override var timeLimit: Long, epsilon: Long) : TimeTerminationChecker(epsilon) {
    override var startTime: Long = 0L

    /**
     * Sets start time to now.
     *
     * The given time limit is ignored
     */
    override fun resetTo(bound: Long) {
        startTime = System.nanoTime()
    }
}