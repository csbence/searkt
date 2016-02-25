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
import edu.unh.cs.ai.realtimesearch.planner.classical.closedlist.heuristic.AStarPlanner
import edu.unh.cs.ai.realtimesearch.planner.realtime_.LssLrtaStarPlanner
import edu.unh.cs.ai.realtimesearch.visualizer.PointVisualizer
import edu.unh.cs.ai.realtimesearch.visualizer.VaccumVisualizer
import javafx.application.Application
import java.io.File
import java.io.FileInputStream
import java.io.PrintWriter
import java.util.*

fun main(args: Array<String>) {
    //val alg = "LSS-LRTA*"
    val alg = "A*"
    //val alg = "RTA"

    val instanceFileName = "input/vacuum/dylan/wall.vw"
    val rawDomain = Scanner(File(instanceFileName)).useDelimiter("\\Z").next();
    val manualConfiguration = ManualConfiguration("point robot", rawDomain, alg, 1, "time", 40)
    val resultList = ConfigurationExecutor.executeConfiguration(manualConfiguration)

    /* Since VaccumVisualizer is an abstract class, the only choice we have to pass
        parameters is through a list of strings. The first string is the domain, and the subsequent
        ones are the list of actions.
      TODO make this more intuitive.
     */
    val params: MutableList<String> = arrayListOf()
    val actionList = resultList.first().actions

    params.add(rawDomain)
    for(action in actionList){
        params.add(action.toString())
    }

    /*val instanceFileName = "input/tiles/korf/4/87"
    val rawDomain = Scanner(File(instanceFileName)).useDelimiter("\\Z").next();
    val manualConfiguration = ManualConfiguration("sliding tile puzzle", rawDomain, "LSS-LRTA*", 1, "time", 10)
    ConfigurationExecutor.executeConfiguration(manualConfiguration)*/

    Application.launch(PointVisualizer::class.java, *params.toTypedArray())
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
    val aStarAgent = ClassicalAgent(AStarPlanner(vacuumWorldInstance.domain))
    val classicalExperiment = ClassicalExperiment<VacuumWorldState>(EmptyConfiguration, aStarAgent, vacuumWorldInstance.domain, vacuumWorldInstance.initialState)
    return classicalExperiment.run()
}

fun writeResultsToFile(name: String, results: List<ExperimentResult>) {
    val writer = PrintWriter("results/Results-$name-${Random().nextInt()}.csv", "UTF-8")
    results.forEach {
        writer.println("${it.expandedNodes}, ${it.generatedNodes}, ${it.timeInMillis}, ${it.actions.size}")
    }
    writer.close()
}