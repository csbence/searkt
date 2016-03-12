package edu.unh.cs.ai.realtimesearch

import edu.unh.cs.ai.realtimesearch.experiment.configuration.ConfigurationExecutor
import edu.unh.cs.ai.realtimesearch.experiment.configuration.ExperimentData
import edu.unh.cs.ai.realtimesearch.experiment.configuration.GeneralExperimentConfiguration
import edu.unh.cs.ai.realtimesearch.experiment.configuration.json.toIndentedJson
import edu.unh.cs.ai.realtimesearch.experiment.configuration.json.toJson
import groovyjarjarcommonscli.GnuParser
import groovyjarjarcommonscli.HelpFormatter
import groovyjarjarcommonscli.Option
import groovyjarjarcommonscli.Options
import java.io.File
import java.io.PrintWriter
import java.util.*
import kotlin.system.exitProcess

class Input

private var manualConfiguration: GeneralExperimentConfiguration = GeneralExperimentConfiguration()
private var outFile: String = ""

fun main(args: Array<String>) {
    if (args.size < 2) {
        // Default configuration
        val input = Input::class.java.classLoader.getResourceAsStream("input/vacuum/maze.vw")!!
        val rawDomain = Scanner(input).useDelimiter("\\Z").next()
        manualConfiguration = GeneralExperimentConfiguration("grid world", rawDomain, "RTA*", "time", 10)
        manualConfiguration["lookahead depth limit"] = 4
        manualConfiguration["action duration"] = 10L
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
        println(result.toJson())
    }
}

private fun createCommandLineMenu(args: Array<String>) {
    val options = Options()
    val appName = "real-time-search"

    // Setup the options
    val helpOption = Option("h", "help", false, "Print help and exit")
    val mapFileOption = Option("m", "map", true, "The path to map file")
    val domainOption = Option("d", "domain", true, "The domain name")
    val algorithmOption = Option("a", "alg-name", true, "The algorithm name")
    val terminationTypeOption = Option("t", "term-type", true, "The termination type")
    val terminationParameterOption = Option("p", "term-param", true, "The termination parameter")
    val outFileOption = Option("o", "outfile", true, "Outfile of experiments")
    val extraOption = Option ("e", "extra", true, "Extra configuration option key/value pairs")

    // Set required options
    mapFileOption.isRequired = true
    domainOption.isRequired = true
    algorithmOption.isRequired = true
    terminationTypeOption.isRequired = true
    terminationParameterOption.isRequired = true
    outFileOption.isRequired = true

    // Add the options
    options.addOption(helpOption)
    options.addOption(mapFileOption)
    options.addOption(domainOption)
    options.addOption(algorithmOption)
    options.addOption(terminationTypeOption)
    options.addOption(terminationParameterOption)
    options.addOption(outFileOption)
    options.addOption(extraOption)

    /* parse command line arguments */
    val parser = GnuParser()
    val cmd = parser.parse(options, args)

    val domainName = cmd.getOptionValue(domainOption.opt)
    val mapFile = cmd.getOptionValue(mapFileOption.opt)
    val algName = cmd.getOptionValue(algorithmOption.opt)
    val termType = cmd.getOptionValue(terminationTypeOption.opt)
    val termParam = cmd.getOptionValue(terminationParameterOption.opt)
    outFile = cmd.getOptionValue(outFileOption.opt)
    val extras = cmd.getOptionValues(extraOption.opt)

    /* print help if help option was specified*/
    val formatter = HelpFormatter()
    if (cmd.hasOption(helpOption.opt)) {
        formatter.printHelp(appName, options)
        exitProcess(1)
    }

    /* run the experiment */
    val rawDomain = File(mapFile).readText()
    manualConfiguration = GeneralExperimentConfiguration(domainName, rawDomain, algName,
            termType, termParam.toInt())

    for (extra in extras) {
        val values = extra.split('=', limit=2)
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
                "long"      -> manualConfiguration[key] = value.toLong()
                "int"       -> manualConfiguration[key] = value.toInt()
                "boolean"   -> manualConfiguration[key] = value.toBoolean()
                "double"    -> manualConfiguration[key] = value.toDouble()
                "float"     -> manualConfiguration[key] = value.toFloat()
                "byte"      -> manualConfiguration[key] = value.toByte()
                "short"     -> manualConfiguration[key] = value.toShort()
                else        -> System.err.println("Extra value '$extra' formatted incorrectly")
            }
        } else {
            manualConfiguration[key] = value
        }
    }
}
