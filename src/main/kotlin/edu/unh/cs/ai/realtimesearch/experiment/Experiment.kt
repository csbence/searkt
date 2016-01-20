package edu.unh.cs.ai.realtimesearch.experiment

abstract class Experiment(val runs: Int) {
    /**
     * Runs the experiment
     */
    abstract fun run(): List<ExperimentResult>
}