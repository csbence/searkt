package edu.unh.cs.ai.realtimesearch.experiment

import edu.unh.cs.ai.realtimesearch.experiment.result.ExperimentResult

abstract class Experiment {
    /**
     * Runs the experiment
     */
    abstract fun run(): ExperimentResult
}