package edu.unh.cs.ai.realtimesearch

import edu.unh.cs.ai.realtimesearch.agent.ClassicalAgent
import edu.unh.cs.ai.realtimesearch.agent.RTSAgent
import edu.unh.cs.ai.realtimesearch.environment.vacuumworld.VacuumWorld
import edu.unh.cs.ai.realtimesearch.environment.vacuumworld.VacuumWorldEnvironment
import edu.unh.cs.ai.realtimesearch.environment.vacuumworld.VacuumWorldState
import edu.unh.cs.ai.realtimesearch.experiment.ClassicalExperiment
import edu.unh.cs.ai.realtimesearch.experiment.RTSExperiment
import edu.unh.cs.ai.realtimesearch.experiment.terminationCheckers.CallsTerminationChecker
import edu.unh.cs.ai.realtimesearch.experiment.terminationCheckers.FakeTerminationChecker
import edu.unh.cs.ai.realtimesearch.experiment.terminationCheckers.TimeTerminationChecker
import edu.unh.cs.ai.realtimesearch.planner.classical.DepthFirstPlanner
import edu.unh.cs.ai.realtimesearch.planner.classical.closedlist.BreadthFirstPlanner
import edu.unh.cs.ai.realtimesearch.planner.classical.closedlist.heuristic.AStarPlanner
import edu.unh.cs.ai.realtimesearch.planner.classical.closedlist.heuristic.GreedyBestFirstPlanner
import edu.unh.cs.ai.realtimesearch.planner.classical.closedlist.heuristic.UniformPlanner
import edu.unh.cs.ai.realtimesearch.planner.realTime.LSS_LRT_AStarPlanner

/**
 * @author Bence Cserna (bence@cserna.net)
 */
fun main(args: Array<String>) {
    // parameters / settings

    // init

    val world = VacuumWorld(6, 6, arrayListOf( ))

    val state = VacuumWorldState(VacuumWorldState.Location(0,1), listOf(
            VacuumWorldState.Location(1,2),
            VacuumWorldState.Location(2,4),
            VacuumWorldState.Location(2,0),
            VacuumWorldState.Location(2,3),
            VacuumWorldState.Location(0,4),
            VacuumWorldState.Location(3,5),
            VacuumWorldState.Location(5,0)
    ))

    val vacuumEnvironment = VacuumWorldEnvironment(world, state)
    //val terminalCondition = TimeTerminationChecker(10.0)
    //val terminalCondition = FakeTerminationChecker()
    val terminalCondition = CallsTerminationChecker(20)

    val breathAgent = ClassicalAgent(BreadthFirstPlanner(world))
    val depthAgent = ClassicalAgent(DepthFirstPlanner(world))
    val uniformAgent = ClassicalAgent(UniformPlanner(world))
    val greedyAgent = ClassicalAgent(GreedyBestFirstPlanner(world))
    val aStarAgent = ClassicalAgent(AStarPlanner(world))
    val lssRTAAgent = RTSAgent(LSS_LRT_AStarPlanner(world))

    val breathExperiment = ClassicalExperiment(breathAgent, state)
    val depthExperiment = ClassicalExperiment(depthAgent, state)
    val uniformExperiment = ClassicalExperiment(uniformAgent, state)
    val greedyExperiment = ClassicalExperiment(greedyAgent, state)
    val aStarExperiment = ClassicalExperiment(aStarAgent, state)
    val lssRTAExperiment = RTSExperiment(lssRTAAgent, vacuumEnvironment, terminalCondition)

    // run experiment
    print("Breadth first:\n")
    breathExperiment.run()
    print("Depth first:\n")
    //depthExperiment.run()
    print("Uniform:\n")
    uniformExperiment.run()
    print("GreedyBestFirst:\n")
    greedyExperiment.run()
    print("A*\n")
    aStarExperiment.run()
    print("LSS RTA*\n")
    lssRTAExperiment.run()

}
