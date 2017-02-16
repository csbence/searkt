package edu.unh.cs.ai.realtimesearch.cli

import edu.unh.cs.ai.realtimesearch.experiment.configuration.GeneralExperimentConfiguration
import edu.unh.cs.ai.realtimesearch.experiment.configuration.json.experimentConfigurationFromJson
import groovyjarjarcommonscli.*
import java.io.File
import kotlin.system.exitProcess

private fun readConfig(fileConfig: String?, stringConfig: String?): GeneralExperimentConfiguration? {
    return if (fileConfig != null) {
        experimentConfigurationFromJson(File(fileConfig).readText())
    } else if (stringConfig != null) {
        experimentConfigurationFromJson(stringConfig)
    } else {
        null
    }
}

fun createCommandLineMenu(args: Array<String>): Triple<GeneralExperimentConfiguration, String, MutableList<String>> {
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

    var manualConfiguration: GeneralExperimentConfiguration = GeneralExperimentConfiguration()
    var outFile: String = ""
    val visualizerParameters = mutableListOf<String>()

    if (haveConfig != null) {
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

    return Triple(manualConfiguration, outFile, visualizerParameters)
}