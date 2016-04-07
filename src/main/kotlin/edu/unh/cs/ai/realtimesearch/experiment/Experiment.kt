package edu.unh.cs.ai.realtimesearch.experiment

import edu.unh.cs.ai.realtimesearch.experiment.result.ExperimentResult
import java.lang.management.ManagementFactory

abstract class Experiment {

    val threadMXBean = ManagementFactory.getThreadMXBean()

    init {
        threadMXBean.isThreadCpuTimeEnabled = true
    }

    /**
     * Runs the experiment
     */
    abstract fun run(): ExperimentResult

    inline fun measureThreadCpuNanoTime(block: () -> Unit): Long {
        val wallStart = System.nanoTime()
        val start = threadMXBean.currentThreadCpuTime
        block()
        val cpuTime = threadMXBean.currentThreadCpuTime - start
        println(System.nanoTime() - wallStart
        )
        return cpuTime
    }

    fun getThreadCpuNanotTime() = threadMXBean.currentThreadCpuTime
}