package edu.unh.cs.ai.realtimesearch

import edu.unh.cs.ai.realtimesearch.environment.Domains.*
import edu.unh.cs.ai.realtimesearch.experiment.configuration.*
import edu.unh.cs.ai.realtimesearch.experiment.configuration.Configurations.COMMITMENT_STRATEGY
import edu.unh.cs.ai.realtimesearch.experiment.configuration.realtime.LookaheadType.DYNAMIC
import edu.unh.cs.ai.realtimesearch.experiment.configuration.realtime.TerminationType.EXPANSION
import edu.unh.cs.ai.realtimesearch.planner.realtime.RealTimeComprehensiveSearch.ComprehensiveConfigurations.BACKLOG_RATIO
import edu.unh.cs.ai.realtimesearch.experiment.result.ExperimentResult
import edu.unh.cs.ai.realtimesearch.planner.CommitmentStrategy
import edu.unh.cs.ai.realtimesearch.planner.Planners.*
import kotlinx.io.PrintWriter
import kotlinx.serialization.json.JSON
import kotlinx.serialization.list
import java.io.File
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.concurrent.TimeUnit.MINUTES
import java.util.concurrent.TimeUnit.NANOSECONDS

fun main(args: Array<String>) {

    var outputPath : String?
    var basePath : String?
    if (args.isNotEmpty()) {
        outputPath = args[0]

        val fileNameIndex = outputPath.lastIndexOf("\\")

        basePath = StringBuilder(outputPath).insert(fileNameIndex + 1, "base_").toString()
    } else {
        File("output").mkdir()
        val path = "results_${LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)}.json"
        outputPath = "output/" + path
        basePath = "output/base_" + path
    }
    val outputFile = File(outputPath)
    outputFile.createNewFile()
    if (!outputFile.isFile || !outputFile.canWrite()) throw MetronomeException("Can't write the output file: $outputPath")

    val baseFile = File(basePath)
    baseFile.createNewFile()
    if (!baseFile.isFile || !baseFile.canWrite()) throw MetronomeException("Can't write the output file: $basePath")

//    println("Please provide a JSON list of configurations to execute:")
//    val rawConfiguration: String = readLine() ?: throw MetronomeException("Mission configuration on stdin.")
//    if (rawConfiguration.isBlank()) throw MetronomeException("No configurations were provided.")
//    val rawConfiguration = if (rawConfigurations != null && rawConfigurations.isNotBlank()) rawConfigurations else generateConfigurations()
    val baselineConfig = generateConfigurations(true)

    val experimentConfig = generateConfigurations(false)
    println("Experiment Configuration")
    println(experimentConfig)

    val baselineLoader = ExperimentConfiguration.serializer().list
    val parsedBaseConfigurations = JSON.parse(baselineLoader, baselineConfig)

    val experimentLoader = ExperimentConfiguration.serializer().list
    val parsedExperimentConfigurations = JSON.parse(experimentLoader, experimentConfig)
    println(parsedExperimentConfigurations)

    val baseResults = ConfigurationExecutor.executeConfigurations(parsedBaseConfigurations, dataRootPath = null, parallelCores = 1)
    val rawBaseResults = JSON.Companion.stringify(ExperimentResult.serializer().list, baseResults)
    PrintWriter(basePath, "UTF-8").use { it.write(rawBaseResults) }


    val experimentResults = ConfigurationExecutor.executeConfigurations(parsedExperimentConfigurations, dataRootPath = null, parallelCores = 1)
    val rawExperimentResults = JSON.Companion.stringify(ExperimentResult.serializer().list, experimentResults)
    PrintWriter(outputPath, "UTF-8").use { it.write(rawExperimentResults) }

    println('#') // Indicator for the parser
    println(rawExperimentResults) // This should be the last printed line

//    runVisualizer(result = results.first())
}

private fun generateConfigurations(baseline: Boolean): String {
    val commitmentStrategy = CommitmentStrategy.SINGLE.toString()

    val planners = if (baseline) listOf(A_STAR)
        else listOf(CES, LSS_LRTA_STAR)

    val configurations = generateConfigurations(
//            domains = listOf(
//                    GRID_WORLD to "input/vacuum/empty.vw"
//                    GRID_WORLD to "input/vacuum/h_400.vw",
//                    GRID_WORLD to "input/vacuum/slalom_04.vw",
//                    GRID_WORLD to "input/vacuum/big_minimum.vw",
//                    GRID_WORLD to "input/vacuum/minima1500/minima1500_1500-0.vw"
//                    GRID_WORLD to "input/vacuum/minima/minima1.vw",
//                    GRID_WORLD to "input/vacuum/minima/minima2.vw",
//                    GRID_WORLD to "input/vacuum/minima/minima3.vw"
//                    GRID_WORLD to "input/vacuum/wall.vw"
//                    GRID_WORLD to "input/vacuum/randomNoisy1k.vw",
//                    GRID_WORLD to "input/vacuum/cups.vw",
//                    GRID_WORLD to "input/vacuum/randomShapes1k.vw",
//                    GRID_WORLD to "input/vacuum/openBox_400.vw"
//                    GRID_WORLD to "input/vacuum/maze.vw"
//            ),
            domains = (0..49).map { GRID_WORLD to "input/vacuum/minima1500/minima1500_1500-$it.vw" },
            planners = planners,
            actionDurations = listOf(10L, 20L, 50L, 100L, 150L, 250L, 500L, 1000L, 2000L),
//            actionDurations = listOf(150L),
            terminationType = EXPANSION,
            lookaheadType = DYNAMIC,
            timeLimit = NANOSECONDS.convert(1999, MINUTES),
            expansionLimit = 100000000,
            stepLimit = 10000000,
            plannerExtras = listOf(
                    Triple(LSS_LRTA_STAR, COMMITMENT_STRATEGY, listOf(commitmentStrategy)),
                    Triple(CES, COMMITMENT_STRATEGY, listOf(commitmentStrategy)),
                    Triple(CES, BACKLOG_RATIO, listOf(10.0, 50.0, 100.0)),
                    Triple(WEIGHTED_A_STAR, Configurations.WEIGHT, listOf(1.0))
            )
    )
    println("${configurations.size} configuration has been generated.")
    return JSON.indented.stringify(SimpleSerializer.list, configurations.toList())
}


