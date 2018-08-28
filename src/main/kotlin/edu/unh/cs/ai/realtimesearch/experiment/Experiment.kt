package edu.unh.cs.ai.realtimesearch.experiment

import edu.unh.cs.ai.realtimesearch.experiment.result.ExperimentResult
import java.lang.management.ManagementFactory

abstract class Experiment {

    val threadMXBean = ManagementFactory.getThreadMXBean()

    /**
     * Runs the experiment
     */
    abstract fun run(): ExperimentResult

    inline fun measureThreadCpuNanoTime(block: () -> Unit): Long {
        val start = threadMXBean.currentThreadCpuTime
        block()
        return threadMXBean.currentThreadCpuTime - start
    }

    fun getThreadCpuNanoTime() = threadMXBean.currentThreadCpuTime
}