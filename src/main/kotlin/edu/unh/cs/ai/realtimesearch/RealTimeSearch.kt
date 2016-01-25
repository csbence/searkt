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
import java.io.File
import java.io.FileInputStream
import java.io.PrintWriter
import java.util.*

fun main(args: Array<String>) {

    val instanceFileName = "input/vacuum/dylan/uniform.vw"
    val rawDomain = Scanner(File(instanceFileName)).useDelimiter("\\Z").next();
    val manualConfiguration = ManualConfiguration("grid world", rawDomain, "LSS-LRTA*", 1, "time", 10)
    ConfigurationExecutor.executeConfiguration(manualConfiguration)

//            aStartCupExperiment()
    //    aStartSlalomExperiment()
    //    aStartUniformExperiment()
    //    lssLrtaStarUniformExperiment()
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

    val rtsExperiment = RTSExperiment<VacuumWorldState>(null, lssLrtaAgent, vacuumWorldEnvironment, CallsTerminationChecker(10))
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