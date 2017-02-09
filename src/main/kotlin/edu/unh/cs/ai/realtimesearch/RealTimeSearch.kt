package edu.unh.cs.ai.realtimesearch

import edu.unh.cs.ai.realtimesearch.environment.Domains.RACETRACK
import edu.unh.cs.ai.realtimesearch.environment.Domains.TRAFFIC
import edu.unh.cs.ai.realtimesearch.experiment.configuration.ConfigurationExecutor
import edu.unh.cs.ai.realtimesearch.experiment.configuration.Configurations
import edu.unh.cs.ai.realtimesearch.experiment.configuration.GeneralExperimentConfiguration
import edu.unh.cs.ai.realtimesearch.experiment.configuration.generateConfigurations
import edu.unh.cs.ai.realtimesearch.experiment.configuration.json.experimentConfigurationFromJson
import edu.unh.cs.ai.realtimesearch.experiment.configuration.json.toIndentedJson
import edu.unh.cs.ai.realtimesearch.experiment.configuration.realtime.LookaheadType
import edu.unh.cs.ai.realtimesearch.experiment.configuration.realtime.LookaheadType.DYNAMIC
import edu.unh.cs.ai.realtimesearch.experiment.configuration.realtime.TerminationType.EXPANSION
import edu.unh.cs.ai.realtimesearch.logging.info
import edu.unh.cs.ai.realtimesearch.planner.CommitmentStrategy.MULTIPLE
import edu.unh.cs.ai.realtimesearch.planner.CommitmentStrategy.SINGLE
import edu.unh.cs.ai.realtimesearch.planner.Planners
import edu.unh.cs.ai.realtimesearch.planner.Planners.*
import groovyjarjarcommonscli.*
import org.slf4j.LoggerFactory
import java.io.File
import java.io.PrintWriter
import java.util.*
import java.util.concurrent.TimeUnit.MINUTES
import java.util.concurrent.TimeUnit.NANOSECONDS
import kotlin.system.exitProcess

class Input

private var manualConfiguration: GeneralExperimentConfiguration = GeneralExperimentConfiguration()
private var outFile: String = ""
private val visualizerParameters = mutableListOf<String>()

fun main(args: Array<String>) {
    val logger = LoggerFactory.getLogger("Real-time search")

    val configurations = generateConfigurations(
            domains = listOf(
                    TRAFFIC to "input/traffic/vehicle0.v",
                    RACETRACK to "input/racetrack/hansen-bigger-doubled.track"
            ),
            planners = listOf(A_STAR, LSS_LRTA_STAR, SAFE_RTS, S_ONE, S_ZERO),
            commitmentStrategy = listOf(SINGLE, MULTIPLE),
            actionDurations = listOf(1000L, 10000L),
            terminationType = EXPANSION,
            lookaheadType = DYNAMIC,
            timeLimit = NANOSECONDS.convert(15, MINUTES)
    )

    configurations.forEach { println(it.toIndentedJson()) }

    val results = ConfigurationExecutor.executeConfigurations(configurations, dataRootPath = null, parallel = true)

    results.forEach { println(it.toIndentedJson()) }

    return

    if (args.isEmpty()) {
        // Default configuration
//        val instanceFileName = "input/racetrack/hansen-bigger-doubled.obstacles"
        //        val instanceFileName = "input/pointrobot/squiggle.pr"
//        val instanceFileName = "input/traffic/vehicle0.v"
//        val instanceFileName = "input/racetrack/barto-big.obstacles"
//        val instanceFileName = "input/racetrack/hansen-bigger-doubled.track"
        val instanceFileName = "input/vacuum/uniform.vw"
        val input = Input::class.java.classLoader.getResourceAsStream(instanceFileName) ?: throw RuntimeException("Resource not found")
        val rawDomain = Scanner(input).useDelimiter("\\Z").next()
        manualConfiguration = GeneralExperimentConfiguration(
                RACETRACK.toString(),
                rawDomain,
                Planners.SAFE_RTS.toString(),
                EXPANSION.toString())

        manualConfiguration[Configurations.ACTION_DURATION.toString()] = 1000L
        manualConfiguration[Configurations.LOOKAHEAD_TYPE.toString()] = LookaheadType.DYNAMIC.toString()
        manualConfiguration[Configurations.COMMITMENT_STRATEGY.toString()] = SINGLE.toString()
        manualConfiguration[Configurations.TIME_LIMIT.toString()] = NANOSECONDS.convert(15, MINUTES)
        manualConfiguration[Configurations.DOMAIN_INSTANCE_NAME.toString()] = instanceFileName

        // Domain specific configurations
        manualConfiguration[Configurations.LOOKAHEAD_DEPTH_LIMIT.toString()] = 4L
        manualConfiguration[Configurations.ANYTIME_MAX_COUNT.toString()] = 3L
        manualConfiguration[Configurations.NUM_ACTIONS.toString()] = 3
        manualConfiguration[Configurations.ACTION_FRACTION.toString()] = 1.0
        manualConfiguration[Configurations.STATE_FRACTION.toString()] = 0.5

        //        visualizerParameters.add("--path")
        //        visualizerParameters.add("--tracker")
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
    } else {
        logger.info { result.toString() }

        //        logger.info(result.toIndentedJson())
        //        runVisualizer(result, visualizerParameters)
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
    val outFileOption = Option("o", "outfile", true, "Outfile of experiments")
    val extraOption = Option("e", "extra", true, "Extra configuration option key/value pairs")
    val visualizerOption = Option("v", "visualizer", true, "Visualizer configuration key/value pairs")

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

    // Add the options
    // Separate options
    separateOptions.addOption(helpOption)
    separateOptions.addOptionGroup(fileOptionGroup)
    separateOptions.addOption(mapFileOption)
    separateOptions.addOption(domainOption)
    separateOptions.addOption(algorithmOption)
    separateOptions.addOption(terminationTypeOption)
    separateOptions.addOption(outFileOption)
    separateOptions.addOption(extraOption)
    separateOptions.addOption(visualizerOption)

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
        outFile = separateCmd.getOptionValue(outFileOption.opt, "")
        val extras = separateCmd.getOptionValues(extraOption.opt)
        val visualizerArgs = separateCmd.getOptionValues(visualizerOption.opt)

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

        if (visualizerArgs != null) {
            for (arg in visualizerArgs) {
                val values = arg.split('=', limit = 2)
                if (values.size != 2) {
                    visualizerParameters.add("--$arg")
                } else {
                    val key: String = values[0]
                    val value = values[1]

                    visualizerParameters.add("--$key")
                    visualizerParameters.add(value)
                }
            }
        }
    }
}
