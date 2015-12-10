package edu.unh.cs.ai.realtimesearch

import edu.unh.cs.ai.realtimesearch.agent.ClassicalAgent
import edu.unh.cs.ai.realtimesearch.domain.vacuumworld.VacuumWorld
import edu.unh.cs.ai.realtimesearch.domain.vacuumworld.VacuumWorldState
import edu.unh.cs.ai.realtimesearch.experiment.ClassicalExperiment
import edu.unh.cs.ai.realtimesearch.planner.classical.DepthFirstPlanner

/**
 * @author Bence Cserna (bence@cserna.net)
 */
fun main(args: Array<String>) {
    // parameters / settings

    // init
    val world = VacuumWorld(10, 10, emptyList())
    val state = VacuumWorldState(VacuumWorldState.Location(0,0), listOf(VacuumWorldState.Location(0,2), VacuumWorldState.Location(0,0), VacuumWorldState.Location(3,0)))
    val agent = ClassicalAgent(DepthFirstPlanner(world))

    val experiment = ClassicalExperiment(agent, world, state)

    // run experiment
    experiment.run()

}
