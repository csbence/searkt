package edu.unh.cs.searkt

import edu.unh.cs.searkt.experiment.configuration.ConfigurationExecutor
import edu.unh.cs.searkt.experiment.configuration.ExperimentConfiguration
import edu.unh.cs.searkt.experiment.result.ExperimentResult
import kotlinx.serialization.json.Json
import kotlinx.serialization.list
import kotlin.math.min

fun main(args: Array<String>) {
    if (args.isEmpty()) {
        println(
                """Please provide one of the following flags to determine the source of configurations:
            | -internalConfiguration    - Generate the hard coded configurations.
            | -stdinConfiguration       - Receive configurations on the standard input.
        """.trimMargin())

        return
    }

    // Get the configurations string from the appropriate source
    val rawConfiguration = when (val configurationSource = args[0]) {
        "-internalConfiguration" -> generateConfigurations()
        "-stdinConfiguration" -> readLine() ?: throw MetronomeException("Missing configuration on stdin.")
        else -> throw MetronomeException("Unknown configuration source: $configurationSource")
    }


    if (rawConfiguration.isBlank()) throw MetronomeException("No configurations were provided.")

    // Convert the json configuration string to experiment configuration instances
    val parsedConfigurations = Json.parse(ExperimentConfiguration.serializer().list, rawConfiguration)

    System.err.println("Execute ${parsedConfigurations.size} configurations.")

    val runtime = Runtime.getRuntime()

    // Leave at least 2 threads for the GC
//    val threadLimit = runtime.availableProcessors() - 2
    val threadLimit = 2

    // Assume that an experiment uses 2 gigs
    val memoryLimit = (runtime.maxMemory() shr 31).toInt()
    val desiredThreadCount = min(threadLimit, memoryLimit).coerceAtLeast(1)

    // println("Automatic thread count: $desiredThreadCount")

    // Execute the experiments
    val results = ConfigurationExecutor.executeConfigurations(parsedConfigurations, dataRootPath = null, parallelCores = desiredThreadCount)

    // Convert the results to json
    val rawResults = Json.stringify(ExperimentResult.serializer().list, results)
    // println(rawResults)

    // Print results
     val outputPath = "results/results_test.json"
     kotlinx.io.PrintWriter(outputPath, "UTF-8").use { it.write(rawResults) }
     System.err.println("\nResult has been saved to $outputPath")

    // System.err.println(results.summary())
//    println('#') // Indicator for the parser
//    println(rawResults) // This should be the last printed line
}

