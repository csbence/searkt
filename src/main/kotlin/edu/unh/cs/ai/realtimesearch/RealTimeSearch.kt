package edu.unh.cs.ai.realtimesearch

import edu.unh.cs.ai.realtimesearch.experiment.configuration.ConfigurationExecutor
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

fun main(args: Array<String>) {
    if (args.size < 2) {
        val input = Input::class.java.classLoader.getResourceAsStream("input/vacuum/maze.vw")!!
        val rawDomain = Scanner(input).useDelimiter("\\Z").next();
        val manualConfiguration = GeneralExperimentConfiguration("grid world", rawDomain, "RTA*", "time", 10)
        manualConfiguration["lookahead depth limit"] = 4
        manualConfiguration["action duration"] = 10L

        val experimentResult = ConfigurationExecutor.executeConfiguration(manualConfiguration)

        val params: MutableList<String> = arrayListOf()
        val actionList = experimentResult.actions

        params.add(rawDomain)
        for (action in actionList) {
            params.add(action.toString())
        }

        //Application.launch(PointIntertiaVisualizer::class.java, *params.toTypedArray())
        //Application.launch(PointVisualizer::class.java, *params.toTypedArray())
        //        Application.launch(VacuumVisualizer::class.java, *params.toTypedArray())
        //Application.launch(RacetrackVisualizer::class.java, *params.toTypedArray())

    } else {
        /* create options */
        createCommandLineMenu(args)
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
    val actionDurationOption = Option ("l", "length", true, "Length of action durations")

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
    options.addOption(actionDurationOption)

    /* parse command line arguments */
    val parser = GnuParser()
    val cmd = parser.parse(options, args)

    val domainName = cmd.getOptionValue(domainOption.opt)
    val mapFile = cmd.getOptionValue(mapFileOption.opt)
    val algName = cmd.getOptionValue(algorithmOption.opt)
    val termType = cmd.getOptionValue(terminationTypeOption.opt)
    val termParam = cmd.getOptionValue(terminationParameterOption.opt)
    val outFile = cmd.getOptionValue(outFileOption.opt)
    val actionDuration = cmd.getOptionValue(actionDurationOption.opt)

    /* print help if help option was specified*/
    val formatter = HelpFormatter()
    if (cmd.hasOption(helpOption.opt)) {
        formatter.printHelp(appName, options)
        exitProcess(1)
    }

    /* run the experiment */
    val rawDomain = Scanner(File(mapFile)).useDelimiter("\\Z").next();
    val manualConfiguration = GeneralExperimentConfiguration(domainName, rawDomain, algName,
            termType, termParam.toInt())
    if (actionDuration != null)
        manualConfiguration["action duration"] = actionDuration.toLong()
    val result = ConfigurationExecutor.executeConfiguration(manualConfiguration)

    /* output the results */
    PrintWriter(outFile, "UTF-8").use {
        it.write(result.toIndentedJson())
    }
}
