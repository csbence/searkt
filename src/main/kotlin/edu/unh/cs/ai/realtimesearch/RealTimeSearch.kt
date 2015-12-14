package edu.unh.cs.ai.realtimesearch

import edu.unh.cs.ai.realtimesearch.agent.ClassicalAgent
import edu.unh.cs.ai.realtimesearch.environment.vacuumworld.VacuumWorld
import edu.unh.cs.ai.realtimesearch.environment.vacuumworld.VacuumWorldState
import edu.unh.cs.ai.realtimesearch.experiment.ClassicalExperiment
import edu.unh.cs.ai.realtimesearch.planner.classical.DepthFirstPlanner
import edu.unh.cs.ai.realtimesearch.planner.classical.closedlist.BreadthFirstPlanner
import edu.unh.cs.ai.realtimesearch.planner.classical.closedlist.heuristic.AStarPlanner
import edu.unh.cs.ai.realtimesearch.planner.classical.closedlist.heuristic.GreedyBestFirstPlanner
import edu.unh.cs.ai.realtimesearch.planner.classical.closedlist.heuristic.UniformPlanner

/**
 * @author Bence Cserna (bence@cserna.net)
 */
fun main(args: Array<String>) {
    // parameters / settings

    // init

    val world = VacuumWorld(7, 7, arrayListOf(
            VacuumWorldState.Location(2,2),
            VacuumWorldState.Location(2,3),
            VacuumWorldState.Location(5,3),
            VacuumWorldState.Location(4,1)
    ))
    val state = VacuumWorldState(VacuumWorldState.Location(0,0), listOf(
            VacuumWorldState.Location(3,6),
            VacuumWorldState.Location(1,4)
    ))
    val breathAgent = ClassicalAgent(BreadthFirstPlanner(world))
    val depthAgent = ClassicalAgent(DepthFirstPlanner(world))
    val uniformAgent = ClassicalAgent(UniformPlanner(world))
    val greedyAgent = ClassicalAgent(GreedyBestFirstPlanner(world))
    val aStarAgent = ClassicalAgent(AStarPlanner(world))

    val breathExperiment = ClassicalExperiment(breathAgent, state)
    val depthExperiment = ClassicalExperiment(depthAgent, state)
    val uniformExperiment = ClassicalExperiment(uniformAgent, state)
    val greedyExperiment = ClassicalExperiment(greedyAgent, state)
    val aStarExperiment = ClassicalExperiment(aStarAgent, state)

    // run experiment
    print("Breadth first:\n")
    breathExperiment.run()
    print("Depth first:\n")
    depthExperiment.run()
    print("Uniform:\n")
    uniformExperiment.run()
    print("GreedyBestFirst:\n")
    greedyExperiment.run()
    print("A*\n")
    aStarExperiment.run()

}
