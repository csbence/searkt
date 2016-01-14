package edu.unh.cs.ai.realtimesearch

import edu.unh.cs.ai.realtimesearch.agent.ClassicalAgent
import edu.unh.cs.ai.realtimesearch.agent.RTSAgent
import edu.unh.cs.ai.realtimesearch.environment.slidingtilepuzzle.SlidingTilePuzzleEnvironment
import edu.unh.cs.ai.realtimesearch.environment.slidingtilepuzzle.SlidingTilePuzzleIO
import edu.unh.cs.ai.realtimesearch.environment.slidingtilepuzzle.SlidingTilePuzzleState
import edu.unh.cs.ai.realtimesearch.experiment.ClassicalExperiment
import edu.unh.cs.ai.realtimesearch.experiment.ExperimentResult
import edu.unh.cs.ai.realtimesearch.experiment.RTSExperiment
import edu.unh.cs.ai.realtimesearch.experiment.terminationCheckers.CallsTerminationChecker
import edu.unh.cs.ai.realtimesearch.planner.classical.closedlist.heuristic.AStarPlanner
import edu.unh.cs.ai.realtimesearch.planner.realtime.LssLrtaStarPlanner
import java.io.File
import java.io.FileInputStream
import java.io.PrintWriter
import java.util.*

/**
 * @author Bence Cserna (bence@cserna.net)
 */
fun main(args: Array<String>) {
    // parameters / settings

    // init
//    val world = VacuumWorld(3, 3, arrayListOf(
//    ), 2)
//
//    val state = VacuumWorldState(VacuumWorldState.Location(0, 0), setOf(
//            VacuumWorldState.Location(2, 0),
//            VacuumWorldState.Location(1, 1)
//    ))

    val slidingTilePuzzleInstance = SlidingTilePuzzleIO.parseFromStream(FileInputStream(File("input/tiles/korf/5/1")))
    val world = slidingTilePuzzleInstance.slidingTilePuzzle
    val state = slidingTilePuzzleInstance.startState

    val environment = SlidingTilePuzzleEnvironment(world, state)
    val terminalCondition = CallsTerminationChecker(10)

    val aStarAgent = ClassicalAgent<SlidingTilePuzzleState>(AStarPlanner(world))
    val lssRTAAgent = RTSAgent<SlidingTilePuzzleState>(LssLrtaStarPlanner(world))

    val aStarExperiment = ClassicalExperiment(aStarAgent, world, state, 10)
    val lssRTAExperiment = RTSExperiment(lssRTAAgent, environment, terminalCondition)
    //val randomLssRTAExperiment = RTSExperiment(lssRTAAgent, randomVacuumEnvironment, terminalCondition)

    // run experiment
    print("A*\n")
    val results = lssRTAExperiment.run()
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