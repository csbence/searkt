package edu.unh.cs.ai.realtimesearch

import edu.unh.cs.ai.realtimesearch.agent.ClassicalAgent
import edu.unh.cs.ai.realtimesearch.agent.RTSAgent
import edu.unh.cs.ai.realtimesearch.environment.vacuumworld.VacuumWorldEnvironment
import edu.unh.cs.ai.realtimesearch.environment.vacuumworld.VacuumWorldIO
import edu.unh.cs.ai.realtimesearch.environment.vacuumworld.VacuumWorldState
import edu.unh.cs.ai.realtimesearch.experiment.ClassicalExperiment
import edu.unh.cs.ai.realtimesearch.experiment.ExperimentResult
import edu.unh.cs.ai.realtimesearch.experiment.RTSExperiment
import edu.unh.cs.ai.realtimesearch.experiment.configuration.ConfigurationExecutor
import edu.unh.cs.ai.realtimesearch.experiment.configuration.EmptyConfiguration
import edu.unh.cs.ai.realtimesearch.experiment.configuration.ManualConfiguration
import edu.unh.cs.ai.realtimesearch.experiment.terminationCheckers.CallsTerminationChecker
import edu.unh.cs.ai.realtimesearch.planner.classical.closedlist.heuristic.ClassicalAStarPlanner
import edu.unh.cs.ai.realtimesearch.planner.realtime_.LssLrtaStarPlanner
import groovyjarjarcommonscli.GnuParser
import groovyjarjarcommonscli.HelpFormatter
import groovyjarjarcommonscli.Options
import java.io.File
import java.io.FileInputStream
import java.io.PrintWriter
import java.net.InetAddress
import java.text.SimpleDateFormat
import java.util.*

fun main(args: Array<String>) {

    if(args.isEmpty()){
        val instanceFileName = "input/vacuum/dylan/uniform.vw"
        val rawDomain = Scanner(File(instanceFileName)).useDelimiter("\\Z").next();
        val manualConfiguration = ManualConfiguration("grid world", rawDomain, "RTA*", 1, "time", 10)
        manualConfiguration.setValue("lookahead depth limit", 4)
        ConfigurationExecutor.executeConfiguration(manualConfiguration)
    }
    else{
        /* create options */
        val options = Options()

        options.addOption("h", "help", true, "Print help and exit")
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
        if(options.hasOption("h")){
            formatter.printHelp("real-time-search", options)
        }

        /* print help if any options weren't specified */
        if(domainName == null || mapFile == null || numRuns == null ||
                termType == null || termParam == null || outFile == null){
            formatter.printHelp("real-time-search", options)
        }

        /* run the experiment */
        val rawDomain = Scanner(File(mapFile)).useDelimiter("\\Z").next();
        val manualConfiguration = ManualConfiguration(domainName, rawDomain, algName,
                numRuns.toInt(), termType, termParam.toInt())
        val resultList = ConfigurationExecutor.executeConfiguration(manualConfiguration)

        /* output the results */
        val writer =  PrintWriter(outFile, "UTF-8");
        val dateFormat = SimpleDateFormat("yyyy/MM/dd HH:mm:ss")
        val date = Date()

        for(result in resultList){
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

        if(options.hasOption("v")){
            /* visualize the output */
            /* TODO: Make visualizer easier to use and read from file, then merge with master
             */

        }
    }



//    val instanceFileName = "input/tiles/korf/4/87"
//    val rawDomain = Scanner(File(instanceFileName)).useDelimiter("\\Z").next();
//    val manualConfiguration = ManualConfiguration("sliding tile puzzle", rawDomain, "LSS-LRTA*", 1, "time", 10)
//    ConfigurationExecutor.executeConfiguration(manualConfiguration)

//    val instanceFileName = "input/tiles/korf/4/1"
//    val rawDomain = Scanner(File(instanceFileName)).useDelimiter("\\Z").next();
//    val manualConfiguration = ManualConfiguration("sliding tile puzzle", rawDomain, "A*", 1, "time", 10)
//    ConfigurationExecutor.executeConfiguration(manualConfiguration)
}

fun lssLrtaStarUniformExperiment() {
    val instanceFileName = "input/vacuum/dylan/uniform.vw"
    return lssLrtaVacuumWorldExperiment(instanceFileName)

}

fun lssLrtaVacuumWorldExperiment(instanceFileName: String) {
    val vacuumWorldInstance = VacuumWorldIO.parseFromStream(FileInputStream(File(instanceFileName)))
    val lssLrtaPlanner = LssLrtaStarPlanner(vacuumWorldInstance.domain)
    val lssLrtaAgent = RTSAgent(lssLrtaPlanner)
    val vacuumWorldEnvironment = VacuumWorldEnvironment(vacuumWorldInstance.domain, vacuumWorldInstance.initialState)

    val rtsExperiment = RTSExperiment<VacuumWorldState>(null, lssLrtaAgent, vacuumWorldEnvironment, CallsTerminationChecker(40))
    rtsExperiment.run()
}

fun aStartCupExperiment(): List<ExperimentResult> {
    val instanceFileName = "input/vacuum/dylan/cups.vw"
    return aStarVacuumWorldExperiment(instanceFileName)
}

fun aStartSlalomExperiment(): List<ExperimentResult> {
    val instanceFileName = "input/vacuum/dylan/slalom.vw"
    return aStarVacuumWorldExperiment(instanceFileName)
}

fun aStartUniformExperiment(): List<ExperimentResult> {
    val instanceFileName = "input/vacuum/dylan/uniform.vw"
    return aStarVacuumWorldExperiment(instanceFileName)
}

private fun aStarVacuumWorldExperiment(instanceFileName: String): List<ExperimentResult> {
    val vacuumWorldInstance = VacuumWorldIO.parseFromStream(FileInputStream(File(instanceFileName)))
    val aStarAgent = ClassicalAgent(ClassicalAStarPlanner(vacuumWorldInstance.domain))
    val classicalExperiment = ClassicalExperiment(EmptyConfiguration, aStarAgent, vacuumWorldInstance.domain, vacuumWorldInstance.initialState)
    return classicalExperiment.run()
}

fun writeResultsToFile(name: String, results: List<ExperimentResult>) {
    val writer = PrintWriter("results/Results-$name-${Random().nextInt()}.csv", "UTF-8")
    results.forEach {
        writer.println("${it.expandedNodes}, ${it.generatedNodes}, ${it.timeInMillis}, ${it.actions.size}")
    }
    writer.close()
}