package edu.unh.cs.ai.realtimesearch.experiment

import edu.unh.cs.ai.realtimesearch.agent.RTSAgent
import edu.unh.cs.ai.realtimesearch.environment.Action
import edu.unh.cs.ai.realtimesearch.environment.Environment
import edu.unh.cs.ai.realtimesearch.environment.State
import edu.unh.cs.ai.realtimesearch.experiment.configuration.GeneralExperimentConfiguration
import edu.unh.cs.ai.realtimesearch.experiment.configuration.lazyData
import edu.unh.cs.ai.realtimesearch.experiment.result.ExperimentResult
import edu.unh.cs.ai.realtimesearch.experiment.terminationCheckers.TimeTerminationChecker
import edu.unh.cs.ai.realtimesearch.logging.debug
import edu.unh.cs.ai.realtimesearch.logging.info
import edu.unh.cs.ai.realtimesearch.planner.AnytimePlanner
import edu.unh.cs.ai.realtimesearch.planner.anytime.AnytimeRepairingAStar
import org.slf4j.LoggerFactory
import kotlin.system.measureNanoTime

/**
 * An RTS experiment repeatedly queries the agent
 * for an action by some constraint (allowed time for example).
 * After each selected action, the experiment then applies this action
 * to its environment.
 *
 * The states are given by the environment, the world. When creating the world
 * it might be possible to determine what the initial state is.
 *
 * NOTE: assumes the same domain is used to create both the agent as the world
 *
 * @param agent is a RTS agent that will supply the actions
 * @param world is the environment
 * @param terminationChecker controls the constraint put upon the agent
 */
class ATSExperiment<StateType : State<StateType>>(val alg: AnytimeRepairingAStar<StateType>,
                                                  val experimentConfiguration: GeneralExperimentConfiguration,
                                                  /*val agent: RTSAgent<StateType>,
                                                  */val world: Environment<StateType>
                                                  /*val terminationChecker: TimeTerminationChecker*/) : Experiment() {

    private val logger = LoggerFactory.getLogger(RTSExperiment::class.java)

    /**
     * Runs the experiment
     */
    override fun run(): ExperimentResult {
        val actions: MutableList<Action> = arrayListOf()

        logger.info { "Starting experiment from state ${world.getState()}" }
        var totalNanoTime = 0L
        //var timeBound = staticStepDuration

        while (!world.isGoal()) {
            //print("" + world.getState() + " " + world.getGoal() + " ")
            var actionList = alg.solve(world.getState(), world.getGoal());
            logger.debug { "Agent return actions: |${actionList.size}| to state ${world.getState()}" }

            if(!alg.update()) {
                actionList.forEach {
                    if (it.action != null) {
                        world.step(it.action) // Move the agent
                        actions.add(it.action) // Save the action
                    }
                }
            } else {

                var count = 0;
                for (it in actionList) {
                    //println(it.action)
                    if (it.action != null) {
                        world.step(it.action) // Move the agent
                        actions.add(it.action) // Save the action
                        count++;
                    }
                    //print(world.getState())
                    if (count > 3)
                        break;
                }
            }

            System.gc()
        }

        logger.info { "Path length: [${actions.size}] \nAfter ${alg.expandedNodeCount} expanded and ${alg.generatedNodeCount} generated nodes in $totalNanoTime. (${alg.expandedNodeCount * 1000 / totalNanoTime})" }
        return ExperimentResult(experimentConfiguration.valueStore, alg.expandedNodeCount, alg.generatedNodeCount, totalNanoTime, actions.map { it.toString() })
    }
}

