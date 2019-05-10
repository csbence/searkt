package edu.unh.cs.searkt

import edu.unh.cs.searkt.experiment.configuration.ConfigurationExecutor
import edu.unh.cs.searkt.experiment.configuration.ExperimentConfiguration
import edu.unh.cs.searkt.experiment.result.ExperimentResult
import edu.unh.cs.searkt.experiment.result.summary
import kotlinx.serialization.json.Json
import kotlinx.serialization.list

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

    println("Execute ${parsedConfigurations.size} configurations.")

    // Execute the experiments
    val results = ConfigurationExecutor.executeConfigurations(parsedConfigurations, dataRootPath = null, parallelCores = 8)

    // Convert the results to json
    val rawResults = Json.stringify(ExperimentResult.serializer().list, results)

    // Print results
    val outputPath = "results/results.json"
    kotlinx.io.PrintWriter(outputPath, "UTF-8").use { it.write(rawResults) }
    System.err.println("\nResult has been saved to $outputPath")

    println(results.summary())
    println('#') // Indicator for the parser
//    println(rawResults) // This should be the last printed line
}

