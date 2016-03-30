package edu.unh.cs.ai.realtimesearch.experiment

import edu.unh.cs.ai.realtimesearch.environment.Environment
import edu.unh.cs.ai.realtimesearch.environment.State
import edu.unh.cs.ai.realtimesearch.experiment.configuration.GeneralExperimentConfiguration
import edu.unh.cs.ai.realtimesearch.experiment.result.ExperimentResult
import edu.unh.cs.ai.realtimesearch.logging.debug
import edu.unh.cs.ai.realtimesearch.logging.info
import edu.unh.cs.ai.realtimesearch.planner.anytime.AnytimeRepairingAStar
import org.slf4j.LoggerFactory

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
 * @param world is the environment
 */
class ATSExperiment<StateType : State<StateType>>(val planner: AnytimeRepairingAStar<StateType>,
                                                  val experimentConfiguration: GeneralExperimentConfiguration,
        /*val agent: RTSAgent<StateType>,
        */val world: Environment<StateType>
        /*val terminationChecker: TimeTerminationChecker*/) : Experiment() {

    private val logger = LoggerFactory.getLogger(RTSExperiment::class.java)

    /**
     * Runs the experiment
     */
    override fun run(): ExperimentResult {
        val actions: MutableList<String> = arrayListOf()
        val actionsLists: MutableList<String> = arrayListOf()
        val maxCount = 3

        logger.info { "Starting experiment from state ${world.getState()}" }
        var executionNanoTime = 1L
        //var timeBound = staticStepDuration

        while (!world.isGoal()) {
            //print("" + world.getState() + " " + world.getGoal() + " ")
            println("start")
            val startTime = System.nanoTime()
            var actionList = planner.solve(world.getState(), world.getGoal());
            val endTime = System.nanoTime()
            println("time: " + (endTime - startTime))
            if(actions.size == 0)
                executionNanoTime = endTime - startTime
            logger.debug { "Agent return actions: |${actionList.size}| to state ${world.getState()}" }

            val update = planner.update()
            if (update < 1.0) {
                actionList.forEach {
                    if (it.action != null) {
                        world.step(it.action) // Move the agent
                        actions.add(it.action.toString()) // Save the action
                    }
                }
            } else {

                var count = 0;
                for (it in actionList) {
                    //println(it.action)
                    if (it.action != null) {


                        if (count < maxCount) {
                            world.step(it.action) // Move the agent
                            actions.add(it.action.toString())
                        }// Save the action
                        actionsLists.add(it.action.toString())
                        count++;
                    }
                    //print(world.getState())
                    //   break;
                }
            }
            if (!world.isGoal()) {
                actionsLists.add("" + update + " ")
                //actionsLists.add("" + world.getState())
            }

            System.gc()
        }
        actionsLists.add("" + maxCount)
        for (it in actions) {
            actionsLists.add(it.toString())
        }
        //actionsLists.add(" " + maxCount + " ")


        println(actionsLists);

        val pathLength = actions.size.toLong()
        logger.info { "Path length: [$pathLength] \nAfter ${planner.expandedNodeCount} expanded and ${planner.generatedNodeCount} generated nodes in $executionNanoTime. (${planner.expandedNodeCount * 1000 / executionNanoTime})" }
        return ExperimentResult(
                experimentConfiguration.valueStore,
                planner.expandedNodeCount,
                planner.generatedNodeCount,
                executionNanoTime,
                pathLength * experimentConfiguration.actionDuration,
                executionNanoTime + pathLength * experimentConfiguration.actionDuration,
                pathLength,
                actions.map { it.toString() })
    }
}

