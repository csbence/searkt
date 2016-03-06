package edu.unh.cs.ai.realtimesearch

import edu.unh.cs.ai.realtimesearch.experiment.configuration.ConfigurationExecutor
import edu.unh.cs.ai.realtimesearch.experiment.configuration.ManualConfiguration
import edu.unh.cs.ai.realtimesearch.visualizer.VacuumVisualizer
import groovyjarjarcommonscli.GnuParser
import groovyjarjarcommonscli.HelpFormatter
import groovyjarjarcommonscli.Options
import javafx.application.Application
import java.io.File
import java.io.PrintWriter
import java.net.InetAddress
import java.text.SimpleDateFormat
import java.util.*
import kotlin.system.exitProcess

class Input

fun main(args: Array<String>) {

    if (args.size < 2) {
        val input = Input::class.java.classLoader.getResourceAsStream("input/vacuum/maze.vw")!!
        val rawDomain = Scanner(input).useDelimiter("\\Z").next();
        val manualConfiguration = ManualConfiguration("grid world", rawDomain, "RTA*", 1, "time", 10)
        manualConfiguration.setValue("lookahead depth limit", 4)
        val resultList = ConfigurationExecutor.executeConfiguration(manualConfiguration)

        val params: MutableList<String> = arrayListOf()
        val actionList = resultList.first().actions

        params.add(rawDomain)
        for (action in actionList) {
            params.add(action.toString())
        }

        //Application.launch(PointIntertiaVisualizer::class.java, *params.toTypedArray())
        //Application.launch(PointVisualizer::class.java, *params.toTypedArray())
        Application.launch(VacuumVisualizer::class.java, *params.toTypedArray())
        //Application.launch(RacetrackVisualizer::class.java, *params.toTypedArray())

    } else {
        /* create options */
        createCommandLineMenu(args)
    }
}

private fun createCommandLineMenu(args: Array<String>) {
    val options = Options()

    options.addOption("h", "help", false, "Print help and exit")
    options.addOption("d", "domain", true, "The domain name")
    options.addOption("m", "map", true, "The path to map file")
    options.addOption("a", "alg-name", true, "The algorithm name")
    options.addOption("n", "num-runs", true, "The number of runs")
    options.addOption("t", "term-type", true, "The termination type")
    options.addOption("p", "term-param", true, "The termination parameter")
    options.addOption("v", "visualize", false, "Whether or not to visualize")
    options.addOption("o", "outfile", true, "Outfile of experiments")


    /* parse command line arguments */
    val parser = GnuParser()
    val cmd = parser.parse(options, args)
    val domainName = cmd.getOptionValue('d')
    val mapFile = cmd.getOptionValue('m')
    val algName = cmd.getOptionValue('a')
    val numRuns = cmd.getOptionValue('n')
    val termType = cmd.getOptionValue('n')
    val termParam = cmd.getOptionValue('n')
    val outFile = cmd.getOptionValue('o')

    /* print help if help option was specified*/
    val formatter = HelpFormatter()
    if (cmd.hasOption("h")) {
        formatter.printHelp("real-time-search", options)
        exitProcess(1)
    }

    /* print help if any options weren't specified */
    if (domainName == null || mapFile == null || numRuns == null ||
            termType == null || termParam == null || outFile == null) {
        formatter.printHelp("real-time-search", options)
        exitProcess(1)
    }

    /* run the experiment */
    val rawDomain = Scanner(File(mapFile)).useDelimiter("\\Z").next();
    val manualConfiguration = ManualConfiguration(domainName, rawDomain, algName,
            numRuns.toInt(), termType, termParam.toInt())
    val resultList = ConfigurationExecutor.executeConfiguration(manualConfiguration)

    /* output the results */
    val writer = PrintWriter(outFile, "UTF-8");
    val dateFormat = SimpleDateFormat("yyyy/MM/dd HH:mm:ss")
    val date = Date()

    for (result in resultList) {
        writer.println("Date: " + dateFormat.format(date))
        writer.println("Hostname: " + InetAddress.getLocalHost().getHostName())
        writer.println("Termination Type: " + termType)
        writer.println("Termination parameter: " + termParam)
        writer.println("Expanded nodes: " + result.expandedNodes)
        writer.println("Generated nodes: " + result.generatedNodes)
        writer.println("Time in millis: " + result.timeInMillis)
        writer.println("Action list: " + result.actions)
        writer.println("Path length: " + result.pathLength)
    }
    writer.close()

    if (options.hasOption("v")) {
        /* visualize the output */
        /* TODO: Make visualizer easier to use and read from file, then merge with master
             */

    }
}