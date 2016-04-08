package edu.unh.cs.ai.realtimesearch

import edu.unh.cs.ai.realtimesearch.environment.Domains
import edu.unh.cs.ai.realtimesearch.experiment.configuration.ConfigurationExecutor
import edu.unh.cs.ai.realtimesearch.experiment.configuration.Configurations
import edu.unh.cs.ai.realtimesearch.experiment.configuration.GeneralExperimentConfiguration
import edu.unh.cs.ai.realtimesearch.experiment.configuration.json.experimentConfigurationFromJson
import edu.unh.cs.ai.realtimesearch.experiment.configuration.json.toIndentedJson
import edu.unh.cs.ai.realtimesearch.planner.CommitmentStrategy
import edu.unh.cs.ai.realtimesearch.planner.Planners
import edu.unh.cs.ai.realtimesearch.util.convertNanoUpDouble
import edu.unh.cs.ai.realtimesearch.visualizer.runVisualizer
import groovyjarjarcommonscli.*
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

//        val map = "input/racetrack/bigger-track.track"
        val map = "input/pointrobot/wall.pr"
//        val map = "input/vacuum/big-squiggle.vw"
//        val map = "input/acrobot/default_0.07-0.07.ab"
        val input = Input::class.java.classLoader.getResourceAsStream(map) ?: throw RuntimeException("Resource not found")
        val rawDomain = Scanner(input).useDelimiter("\\Z").next()
        manualConfiguration = GeneralExperimentConfiguration(
                Domains.POINT_ROBOT_WITH_INERTIA.toString(),
                rawDomain,
                Planners.A_STAR.toString(),
                "time")

        manualConfiguration[Configurations.LOOKAHEAD_DEPTH_LIMIT.toString()] = 4L
        manualConfiguration[Configurations.ACTION_DURATION.toString()] = NANOSECONDS.convert(320, MILLISECONDS)
        manualConfiguration[Configurations.TIME_BOUND_TYPE.toString()] = "STATIC"
        manualConfiguration[Configurations.COMMITMENT_STRATEGY.toString()] = CommitmentStrategy.MULTIPLE.toString()
        manualConfiguration[Configurations.TIME_LIMIT.toString()] = NANOSECONDS.convert(5, MINUTES)
        manualConfiguration[Configurations.ANYTIME_MAX_COUNT.toString()] = 3L
        manualConfiguration[Configurations.DOMAIN_INSTANCE_NAME.toString()] = map

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
        logger.info("Planning time: ${convertNanoUpDouble(result.planningTime, MILLISECONDS)} ms")
        logger.info("Execution time: ${convertNanoUpDouble(result.actionExecutionTime, MILLISECONDS)} ms")
        logger.info("GAT: ${convertNanoUpDouble(result.goalAchievementTime, MILLISECONDS)} ms")
                logger.info(result.toIndentedJson())

        runVisualizer(result)
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
