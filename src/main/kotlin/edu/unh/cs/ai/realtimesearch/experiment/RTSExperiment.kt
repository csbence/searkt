package edu.unh.cs.ai.realtimesearch.experiment

import edu.unh.cs.ai.realtimesearch.agent.RTSAgent
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
        logger.warn("Starting experiment")

        while (!world.isGoal()) {
            terminationChecker.init()

            val state = world.getState()
            val action = agent.selectAction(world.getState(), terminationChecker);

            logger.warn("Agent return action " + action.toString() + " for state " + state.toString())
            // TODO: assert termination is okay somehow?

            world.step(action)
        }

        // TODO: store results (which?)
    }
}