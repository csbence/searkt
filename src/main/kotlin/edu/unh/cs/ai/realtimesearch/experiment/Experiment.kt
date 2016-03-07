package edu.unh.cs.ai.realtimesearch.experiment

abstract class Experiment {
    /**
     * Runs the experiment
     */
    abstract fun run(): ExperimentResult
}