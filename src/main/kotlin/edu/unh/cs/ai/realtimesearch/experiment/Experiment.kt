package edu.unh.cs.ai.realtimesearch.experiment

/**
 * @author Bence Cserna (bence@cserna.net)
 */
abstract class Experiment(val runs: Int) {
    /**
     * Runs the experiment
     */
    abstract fun run()
}