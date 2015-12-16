package edu.unh.cs.ai.realtimesearch.experiment

import edu.unh.cs.ai.realtimesearch.agent.RTSAgent
import edu.unh.cs.ai.realtimesearch.environment.Action
import edu.unh.cs.ai.realtimesearch.environment.Environment
import org.slf4j.LoggerFactory

/**
 * @author Bence Cserna (bence@cserna.net)
 *
 * An RTS experiment repeatedly queries the agent for an action by some constraint (allowed time for example).
 * After each selected action, the experiment then applies this action to its environment.
 *
 * It's currently assuming fully observable environments.
 *
 * @param agent is a RTS agent that will supply the actions
 * @param world is the environment
 * @param terminationChecker controls the constraint put upon the agent
 */
class RTSExperiment(val agent: RTSAgent, val world: Environment, val terminationChecker: TerminationChecker) : Experiment {
    private val logger = LoggerFactory.getLogger("RTSExperiment")

    /**
     * Runs the experiment
     */
    override fun run() {
        val actions: MutableList<Action> = arrayListOf()

        logger.warn("Starting experiment from state ${world.getState()}")

        while (!world.isGoal()) {

            terminationChecker.init()
            val action = agent.selectAction(world.getState(), terminationChecker);

            actions.add(action)
            world.step(action)

            logger.warn("Agent return action $action to state ${world.getState()}")
        }

        logger.info("Path: " + actions + "\nAfter " +
                agent.planner.expandedNodes + " expanded and " +
                agent.planner.generatedNodes + " generated nodes")
    }
}