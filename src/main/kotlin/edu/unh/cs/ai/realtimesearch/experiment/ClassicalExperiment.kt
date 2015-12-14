package edu.unh.cs.ai.realtimesearch.experiment

import edu.unh.cs.ai.realtimesearch.agent.ClassicalAgent
import edu.unh.cs.ai.realtimesearch.environment.Action
import edu.unh.cs.ai.realtimesearch.environment.State
import org.slf4j.LoggerFactory

/**
 * An experiment meant for classical search, such as depth first search.
 *
 * @param agent is the agent that is involved in the experiment
 * @param initState is the initial state of the world
 */
class ClassicalExperiment(val agent: ClassicalAgent, val initState: State) : Experiment {
    private val logger = LoggerFactory.getLogger("ClassicalExperiment")
    private var plan: List<Action> = emptyList()

    override fun run() {
        plan = agent.plan(initState)

        var actions = ""
        plan.forEach {
            actions += it.toString() + " "
        }

        logger.info("Path: " + actions + "\nAfter " +
                agent.planner.expandedNodes + " expanded and " +
                agent.planner.generatedNodes + " generated nodes")
    }
}