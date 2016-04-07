package edu.unh.cs.ai.realtimesearch

import java.lang.management.ManagementFactory
import kotlin.system.measureNanoTime
import kotlin.system.measureTimeMillis

/**
 * @author Bence Cserna (bence@cserna.net)
 */

fun main(args: Array<String>) {
    Compiler.disable()

    var x = System.nanoTime()
    while (true) {
        val message = System.nanoTime() - x
        if (message > 100000)
            println(message)
        x = System.nanoTime()
    }

    println(measureTimeMillis {
        var test = 0L
        for (i in 1..1000000) {
            test = test xor System.nanoTime()
        }
    })

    println(measureTimeMillis {
        var test = 0L
        for (i in 1..1000000) {
            test = test xor System.currentTimeMillis()
        }
    })

    val threadMXBean = ManagementFactory.getThreadMXBean()

    println(measureTimeMillis {
        var test = 0L
        for (i in 1..1000000) {
            test = test xor threadMXBean.currentThreadCpuTime
        }
    })

    println(measureNanoTime {
        var test = 0L
        for (i in 1..1000000) {
            test = test xor System.nanoTime()
        }
    } / 1000000.0)

    println(measureNanoTime {
        var test = 0L
        for (i in 1..1000000) {
            test = test xor System.currentTimeMillis()
        }
    } / 1000000.0)

    println(measureNanoTime {
        var test = 0L
        for (i in 1..1000000) {
            test = test xor threadMXBean.currentThreadCpuTime
        }
    } / 1000000.0)

    val startThreadNano = threadMXBean.currentThreadCpuTime
    val startNano = System.nanoTime()

    val list = mutableListOf<Any>()
    for (i in 1..100000000) {
        list.add(Any())
    }

    val endNano = System.nanoTime()
    val endThreadNano = threadMXBean.currentThreadCpuTime

    println("NanoTime: ${endNano - startNano}")
    println("ThreadNanoTime: ${endThreadNano - startThreadNano}")

    list.add(Any())

    System.gc()

    val endNanoGC = System.nanoTime()
    val endThreadNanoGC = threadMXBean.currentThreadCpuTime

    println("NanoTime: ${endNanoGC - startNano}")
    println("ThreadNanoTime: ${endThreadNanoGC - startThreadNano}")

}