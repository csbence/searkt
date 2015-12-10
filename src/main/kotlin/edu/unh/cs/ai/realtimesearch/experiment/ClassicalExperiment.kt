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
 */
class ClassicalExperiment(val agent: ClassicalAgent, val world: Domain, val initState: State) : Experiment {
    private val logger = LoggerFactory.getLogger("ClassicalExperiment")
    private var plan: List<Action> = emptyList()

    override fun run() {
        plan = agent.plan(initState)

        plan.forEach {
            logger.info("Action " + it.toString())
            print(it.toString())
        }
    }


}