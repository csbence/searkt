package edu.unh.cs.ai.realtimesearch.experiment.terminationCheckers

/**
 * A termination checker based on time. Will check whether timeLimit has exceeded since init()
 *
 * @param timeLimit is the limit allowed after init before termination is confirmed
 */
class StaticTimeTerminationChecker(override var timeLimit: Long) : TimeTerminationChecker() {
    override var startTime: Long = 0

    /**
     * Sets start time to now.
     *
     * The given time limit is ignored
     */
    override fun init(timeBound: Long) {
        startTime = System.nanoTime()
    }
}