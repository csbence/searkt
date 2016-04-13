package edu.unh.cs.ai.realtimesearch.experiment

import edu.unh.cs.ai.realtimesearch.environment.Action
import edu.unh.cs.ai.realtimesearch.environment.Environment
import edu.unh.cs.ai.realtimesearch.environment.State
import edu.unh.cs.ai.realtimesearch.experiment.configuration.Configurations
import edu.unh.cs.ai.realtimesearch.experiment.configuration.GeneralExperimentConfiguration
import edu.unh.cs.ai.realtimesearch.experiment.configuration.InvalidFieldException
import edu.unh.cs.ai.realtimesearch.experiment.result.ExperimentResult
import edu.unh.cs.ai.realtimesearch.logging.debug
import edu.unh.cs.ai.realtimesearch.logging.info
import edu.unh.cs.ai.realtimesearch.planner.anytime.AnytimeRepairingAStar
import edu.unh.cs.ai.realtimesearch.util.convertNanoUpDouble
import org.slf4j.LoggerFactory
import java.util.concurrent.TimeUnit

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
class AnytimeExperiment<StateType : State<StateType>>(val planner: AnytimeRepairingAStar<StateType>,
                                                      val experimentConfiguration: GeneralExperimentConfiguration,
                                                      val world: Environment<StateType>) : Experiment() {

    private val logger = LoggerFactory.getLogger(AnytimeExperiment::class.java)

    /**
     * Runs the experiment
     */
    override fun run(): ExperimentResult {
        val actions: MutableList<String> = arrayListOf()
        val actionsLists: MutableList<String> = arrayListOf()
        var actionList: MutableList<Action?> = arrayListOf()
        //        val maxCount = 6
        val maxCount: Long = experimentConfiguration.getTypedValue<Long>(Configurations.ANYTIME_MAX_COUNT.toString()) ?: throw InvalidFieldException("\"${Configurations.ANYTIME_MAX_COUNT}\" is not found. Please add it to the experiment configuration.")

        logger.info { "Starting experiment from state ${world.getState()}" }
        var idlePlanningTime = 1L
        var totalPlanningTime = 0L

        while (!world.isGoal()) {
            logger.debug { "Start anytime search" }
            val startTime = getThreadCpuNanotTime()

            val tempActions = planner.solve(world.getState(), world.getGoal());

            val endTime = getThreadCpuNanotTime()
            totalPlanningTime += endTime - startTime

            logger.debug { "time: " + (endTime - startTime) }
            if (actions.size == 0) {
                idlePlanningTime = endTime - startTime
                actionList = tempActions
            } else if (experimentConfiguration.actionDuration * maxCount < endTime - startTime) {
                for (i in 1..maxCount) {
                    actionList.removeAt(0)
                }
            } else {
                actionList = tempActions
            }

            logger.debug { "Agent return actions: |${actionList.size}| to state ${world.getState()}" }

            val update = planner.update()
            if (update < 1.0) {
                actionList.forEach {
                    if (it != null) {
                        world.step(it) // Move the agent
                        actions.add(it.toString()) // Save the action
                    }
                }
            } else {

                var count = 0
                for (it in actionList) {
                    if (it != null) {

                        if (count < maxCount) {
                            world.step(it) // Move the agent
                            actions.add(it.toString())
                        }// Save the action
                        actionsLists.add(it.toString())
                        count++;
                    }
                }
            }
            if (!world.isGoal()) {
                actionsLists.add("" + update + " ")
            }

        }
        actionsLists.add("" + maxCount)
        for (it in actions) {
            actionsLists.add(it.toString())
        }

        logger.info { actionsLists.toString() }

        val pathLength = actions.size.toLong()
        val totalExecutionNanoTime = pathLength * experimentConfiguration.actionDuration
        val goalAchievementTime = idlePlanningTime + totalExecutionNanoTime

        logger.info {
            "Path length: [$pathLength] After ${planner.expandedNodeCount} expanded " +
                    "and ${planner.generatedNodeCount} generated nodes in ${idlePlanningTime} ns. " +
                    "(${planner.expandedNodeCount / convertNanoUpDouble(idlePlanningTime, TimeUnit.SECONDS)} expanded nodes per sec)"
        }

        return ExperimentResult(
                experimentConfiguration = experimentConfiguration.valueStore,
                expandedNodes = planner.expandedNodeCount,
                generatedNodes = planner.generatedNodeCount,
                planningTime = totalPlanningTime,
                actionExecutionTime = totalExecutionNanoTime,
                goalAchievementTime = goalAchievementTime,
                idlePlanningTime = idlePlanningTime,
                pathLength = pathLength,
                actions = actions.map { it.toString() }
        )
    }
}

