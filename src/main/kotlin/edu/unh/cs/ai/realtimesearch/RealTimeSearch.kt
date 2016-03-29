package edu.unh.cs.ai.realtimesearch

import edu.unh.cs.ai.realtimesearch.environment.Domains
import edu.unh.cs.ai.realtimesearch.experiment.configuration.ConfigurationExecutor
import edu.unh.cs.ai.realtimesearch.experiment.configuration.GeneralExperimentConfiguration
import edu.unh.cs.ai.realtimesearch.experiment.configuration.json.experimentConfigurationFromJson
import edu.unh.cs.ai.realtimesearch.experiment.configuration.json.toIndentedJson
import edu.unh.cs.ai.realtimesearch.planner.CommitmentStrategy
import edu.unh.cs.ai.realtimesearch.planner.Planners
import edu.unh.cs.ai.realtimesearch.visualizer.gridbased.PointInertiaVisualizer
//import edu.unh.cs.ai.realtimesearch.visualizer.gridbased.PointVisualizer
//import edu.unh.cs.ai.realtimesearch.visualizer.gridbased.VacuumVisualizer
import groovyjarjarcommonscli.*
import javafx.application.Application
import org.slf4j.LoggerFactory
import java.io.File
import java.io.PrintWriter
import java.util.*
import java.util.concurrent.TimeUnit.*
import kotlin.system.exitProcess

class Input

private var manualConfiguration: GeneralExperimentConfiguration = GeneralExperimentConfiguration()
private var outFile: String = ""

fun main(args: Array<String>) {
    val logger = LoggerFactory.getLogger("Real-time search")

    if (args.size == 0) {
        // Default configuration
        val input = Input::class.java.classLoader.getResourceAsStream("input/pointrobot/smallmaze2.pr") ?: throw RuntimeException("Resource not found")
//        val input = Input::class.java.classLoader.getResourceAsStream("input/tiles/korf/4/all/1") ?: throw RuntimeException("Resource not found")
        val rawDomain = Scanner(input).useDelimiter("\\Z").next()
        manualConfiguration = GeneralExperimentConfiguration(
//                Domains.SLIDING_TILE_PUZZLE.toString(),
                Domains.POINT_ROBOT_WITH_INERTIA.toString(),
                rawDomain,
                Planners.ARA_STAR.toString(),
                "time")
        manualConfiguration["lookaheadDepthLimit"] = 4L
        manualConfiguration["actionDuration"] = 10L
        manualConfiguration["timeBoundType"] = "STATIC"
        manualConfiguration["commitmentStrategy"] = CommitmentStrategy.MULTIPLE.toString()
        manualConfiguration["singleStepLookahead"] = false
        manualConfiguration["timeLimit"] = NANOSECONDS.convert(5, MINUTES)

    } else {
        // Read configuration from command line
        createCommandLineMenu(args)
    }

    val result = ConfigurationExecutor.executeConfiguration(manualConfiguration)

    /* output the results */
    if (!outFile.isEmpty()) {
        PrintWriter(outFile, "UTF-8").use {
            it.write(result.toIndentedJson())
        }
    } else if (result.errorMessage != null) {
        logger.error("Something went wrong: ${result.errorMessage}")
    } else {
        logger.info("Execution time: ${MILLISECONDS.convert(result.planningTime, NANOSECONDS)}ms")
        //        logger.info(result.toIndentedJson())

        val params: MutableList<String> = arrayListOf()
        params.add(manualConfiguration.rawDomain)
        for(action in result.actions)
            params.add(action.toString())

        Application.launch(PointInertiaVisualizer::class.java, *params.toTypedArray())
        //Application.launch(PointVisualizer::class.java, *params.toTypedArray())
        //Application.launch(VacuumVisualizer::class.java, *params.toTypedArray())
        //Application.launch(RacetrackVisualizer::class.java, *params.toTypedArray())
    }

}

private fun readConfig(fileConfig: String?, stringConfig: String?): Boolean {
    val config: String
    if (fileConfig != null) {
        config = File(fileConfig).readText()
    } else if (stringConfig != null) {
        config = stringConfig
    } else {
        return false
    }

    manualConfiguration = experimentConfigurationFromJson(config)

    return true
}

private fun createCommandLineMenu(args: Array<String>) {
    val mainOptions = Options()
    val fileOptions = Options()
    val separateOptions = Options()

    val appName = "real-time-search"

    // Setup the options
    val helpOption = Option("h", "help", false, "Print help and exit")

    // Separated options
    val mapFileOption = Option("m", "map", true, "The path to map file")
    val domainOption = Option("d", "domain", true, "The domain name")
    val algorithmOption = Option("a", "alg-name", true, "The algorithm name")
    val terminationTypeOption = Option("t", "term-type", true, "The termination type")
    val terminationParameterOption = Option("p", "term-param", true, "The termination parameter")
    val outFileOption = Option("o", "outfile", true, "Outfile of experiments")
    val extraOption = Option ("e", "extra", true, "Extra configuration option key/value pairs")

    // Configuration file options
    val fileOptionGroup = OptionGroup()
    val fileOption = Option("f", "file", true, "The path to configuration file")
    val stringOption = Option("c", "config", true, "The configuration as a string")
    fileOptionGroup.addOption(fileOption)
    fileOptionGroup.addOption(stringOption)

    // Set required options
    mapFileOption.isRequired = true
    domainOption.isRequired = true
    algorithmOption.isRequired = true
    terminationTypeOption.isRequired = true
    terminationParameterOption.isRequired = true

    // Add the options
    // Separate options
    separateOptions.addOption(helpOption)
    separateOptions.addOptionGroup(fileOptionGroup)
    separateOptions.addOption(mapFileOption)
    separateOptions.addOption(domainOption)
    separateOptions.addOption(algorithmOption)
    separateOptions.addOption(terminationTypeOption)
    separateOptions.addOption(terminationParameterOption)
    separateOptions.addOption(outFileOption)
    separateOptions.addOption(extraOption)

    // File options
    fileOptions.addOptionGroup(fileOptionGroup)

    // Main options
    for (option in separateOptions.options)
        mainOptions.addOption(option as Option)
    for (option in fileOptions.options)
        mainOptions.addOption(option as Option)

    // Common options
    fileOptions.addOption(helpOption)
    fileOptions.addOption(outFileOption)

    /* parse command line arguments */
    val fileCmd = GnuParser().parse(fileOptions, args, true)

    /* print help if help option was specified*/
    val formatter = HelpFormatter()
    if (fileCmd.hasOption(helpOption.opt)) {
        formatter.printHelp(appName, mainOptions)
        exitProcess(1)
    }

    val fileConfig = fileCmd.getOptionValue(fileOption.opt)
    val stringConfig = fileCmd.getOptionValue(stringOption.opt)
    val haveConfig = readConfig(fileConfig, stringConfig)

    if (haveConfig) {
        outFile = fileCmd.getOptionValue(outFileOption.opt, "out.json")
    } else {
        val separateCmd = GnuParser().parse(separateOptions, args)

        val domainName = separateCmd.getOptionValue(domainOption.opt)
        val mapFile = separateCmd.getOptionValue(mapFileOption.opt)
        val algName = separateCmd.getOptionValue(algorithmOption.opt)
        val termType = separateCmd.getOptionValue(terminationTypeOption.opt)
        val termParam = separateCmd.getOptionValue(terminationParameterOption.opt)
        outFile = separateCmd.getOptionValue(outFileOption.opt, "out.json")
        val extras = separateCmd.getOptionValues(extraOption.opt)

        /* run the experiment */
        val rawDomain = File(mapFile).readText()
        manualConfiguration = GeneralExperimentConfiguration(domainName, rawDomain, algName, termType)

        for (extra in extras) {
            val values = extra.split('=', limit = 2)
            if (values.size != 2) {
                System.err.println("Extra value '$extra' formatted incorrectly")
                continue
            }
            var key: String = values[0]
            val value = values[1]

            // Check for type
            val keyVals = key.split('(', ')')
            if (keyVals.size > 1) {
                key = keyVals[0]
                val type = keyVals[1]
                when (type.toLowerCase()) {
                    "long" -> manualConfiguration[key] = value.toLong()
                    "int" -> manualConfiguration[key] = value.toInt()
                    "boolean" -> manualConfiguration[key] = value.toBoolean()
                    "double" -> manualConfiguration[key] = value.toDouble()
                    "float" -> manualConfiguration[key] = value.toFloat()
                    "byte" -> manualConfiguration[key] = value.toByte()
                    "short" -> manualConfiguration[key] = value.toShort()
                    else -> System.err.println("Extra value '$extra' formatted incorrectly")
                }
            } else {
                manualConfiguration[key] = value
            }
        }
    }
}
