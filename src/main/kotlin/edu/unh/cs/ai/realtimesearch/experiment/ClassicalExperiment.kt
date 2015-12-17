package edu.unh.cs.ai.realtimesearch.experiment

import edu.unh.cs.ai.realtimesearch.agent.ClassicalAgent
import edu.unh.cs.ai.realtimesearch.environment.Action
import edu.unh.cs.ai.realtimesearch.environment.Domain
import edu.unh.cs.ai.realtimesearch.environment.State
import org.slf4j.LoggerFactory

/**
 * An experiment meant for classical search, such as depth first search.
 *
 * @param agent is the agent that is involved in the experiment
 * @param domain is the domain of the agent. Used for random state generation
 * @param initState is the initial state of the world
 */
class ClassicalExperiment(val agent: ClassicalAgent,
                          val domain: Domain,
                          val initState: State? = null,
                          runs: Int = 1) : Experiment(runs) {

    private val logger = LoggerFactory.getLogger("ClassicalExperiment")
    private var plan: List<Action> = emptyList()

    override fun run() {
        for (run in 1..runs) {

            // do experiment
            val state = initState?.copy() ?: domain.randomState()
            logger.warn("Starting experiment run $run with state $state on agent $agent")

            plan = agent.plan(state)

            // log results
            logger.warn("Path: $plan\nAfter ${agent.planner.expandedNodes} expanded " +
                    "and ${agent.planner.generatedNodes} generated nodes")
        }

    }
}