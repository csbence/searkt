package edu.unh.cs.ai.realtimesearch.experiment

import edu.unh.cs.ai.realtimesearch.agent.ClassicalAgent
import edu.unh.cs.ai.realtimesearch.domain.Action
import edu.unh.cs.ai.realtimesearch.domain.Domain
import edu.unh.cs.ai.realtimesearch.domain.State
import org.slf4j.LoggerFactory

/**
 * An experiment meant for classical search, such as depth first search.
 *
 * @param agent is the agent that is involved in the experiment
 * @param world is the world to test the agent in
 * @param initState is the initial state of the world
 */
class ClassicalExperiment(val agent: ClassicalAgent, val world: Domain, val initState: State) : Experiment {
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