package edu.unh.cs.ai.realtimesearch

import edu.unh.cs.ai.realtimesearch.agent.ClassicalAgent
import edu.unh.cs.ai.realtimesearch.environment.vacuumworld.VacuumWorld
import edu.unh.cs.ai.realtimesearch.environment.vacuumworld.VacuumWorldEnvironment
import edu.unh.cs.ai.realtimesearch.environment.vacuumworld.VacuumWorldState
import edu.unh.cs.ai.realtimesearch.experiment.ClassicalExperiment
import edu.unh.cs.ai.realtimesearch.experiment.ExperimentResult
import edu.unh.cs.ai.realtimesearch.experiment.terminationCheckers.CallsTerminationChecker
import edu.unh.cs.ai.realtimesearch.planner.classical.closedlist.heuristic.AStarPlanner
import java.io.PrintWriter
import java.util.*

/**
 * @author Bence Cserna (bence@cserna.net)
 */
fun main(args: Array<String>) {
    // parameters / settings

    // init
    val world = VacuumWorld(3, 3, arrayListOf(
    ), 2)

    val state = VacuumWorldState(VacuumWorldState.Location(0, 0), setOf(
            VacuumWorldState.Location(2, 0),
            VacuumWorldState.Location(1, 1)
    ))

    val vacuumEnvironment = VacuumWorldEnvironment(world, state)
    val randomVacuumEnvironment = VacuumWorldEnvironment(world)

    //val terminalCondition = TimeTerminationChecker(10.0)
    //val terminalCondition = FakeTerminationChecker()
    val terminalCondition = CallsTerminationChecker(10)

    val aStarAgent = ClassicalAgent(AStarPlanner(world))
    //val lssRTAAgent = RTSAgent(LSSLRTAStarPlanner(world))

    val aStarExperiment = ClassicalExperiment(aStarAgent, world, null, 10)
    //val lssRTAExperiment = RTSExperiment(lssRTAAgent, vacuumEnvironment, terminalCondition)
    //val randomLssRTAExperiment = RTSExperiment(lssRTAAgent, randomVacuumEnvironment, terminalCondition)

    // run experiment
    print("A*\n")
    val results = aStarExperiment.run()
    writeResultsToFile("AStar", results)
    //print("LSS RTA*\n")
//    val lssResults = lssRTAExperiment.run()
    //lssRTAExperiment.run()

    //for (i in 1..5) {
    //randomLssRTAExperiment.run()
    //}
}

fun writeResultsToFile(name: String, results: List<ExperimentResult>) {
    val writer = PrintWriter("results/Results-$name-${Random().nextInt()}.csv", "UTF-8")
    results.forEach {
        writer.println("${it.expandedNodes}, ${it.generatedNodes}, ${it.timeInMillis}, ${it.actions.size}")
    }
    writer.close()
}