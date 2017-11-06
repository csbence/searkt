package edu.unh.cs.ai.realtimesearch.experiment

import edu.unh.cs.ai.realtimesearch.environment.Action
import edu.unh.cs.ai.realtimesearch.environment.Domain
import edu.unh.cs.ai.realtimesearch.environment.State
import edu.unh.cs.ai.realtimesearch.experiment.configuration.Configurations
import edu.unh.cs.ai.realtimesearch.experiment.configuration.GeneralExperimentConfiguration
import edu.unh.cs.ai.realtimesearch.experiment.configuration.InvalidFieldException
import edu.unh.cs.ai.realtimesearch.experiment.result.ExperimentResult
import edu.unh.cs.ai.realtimesearch.experiment.terminationCheckers.FakeTerminationChecker
import edu.unh.cs.ai.realtimesearch.logging.debug
import edu.unh.cs.ai.realtimesearch.logging.info
import edu.unh.cs.ai.realtimesearch.planner.AnytimePlanner
import edu.unh.cs.ai.realtimesearch.util.convertNanoUpDouble
import edu.unh.cs.ai.realtimesearch.logging.LoggerFactory
import java.util.*
import java.util.concurrent.TimeUnit

/**
 * An RTS experiment repeatedly queries the planner
 * for an action by some constraint (allowed time for example).
 * After each selected action, the experiment then applies this action
 * to its environment.
 *
 * The states are given by the environment, the domain. When creating the domain
 * it might be possible to determine what the initial state is.
 *
 * NOTE: assumes the same domain is used to create both the planner as the domain
 *
 * @param planner the planner to be executed
 *
 */
class AnytimeExperiment<StateType : State<StateType>>(val planner: AnytimePlanner<StateType>,
                                                      val configuration: GeneralExperimentConfiguration,
                                                      val domain: Domain<StateType>,
                                                      val initialState: StateType) : Experiment() {

    private val logger = LoggerFactory.getLogger(AnytimeExperiment::class.java)

    /**
     * Runs the experiment
     */
    override fun run(): ExperimentResult {
        val actions: MutableList<String> = arrayListOf()
        val actionsLists: MutableList<String> = arrayListOf()
        var actionList: MutableList<Action?> = arrayListOf()
        var currentState = initialState
        //        val maxCount = 6
        val maxCount: Long = configuration.getTypedValue<Long>(Configurations.ANYTIME_MAX_COUNT.toString()) ?: throw InvalidFieldException("\"${Configurations.ANYTIME_MAX_COUNT}\" is not found. Please add it to the experiment configuration.")

        logger.info { "Starting experiment from state $initialState" }
        var idlePlanningTime = 1L
        var totalPlanningTime = 0L
        var iterationCount = 0L

        while (!domain.isGoal(currentState)) {
            logger.debug { "Start anytime search" }
            val startTime = getThreadCpuNanotTime()

            val tempActions = ArrayList(planner.selectAction(currentState, FakeTerminationChecker))

            val endTime = getThreadCpuNanotTime()
            totalPlanningTime += endTime - startTime

            logger.debug { "time: " + (endTime - startTime) }
            if (actions.size == 0) {
                idlePlanningTime = endTime - startTime
                actionList = tempActions
            } else if (configuration.actionDuration * maxCount < endTime - startTime) {
                for (i in 1..maxCount) {
                    actionList.removeAt(0)
                }
            } else {
                actionList = tempActions
            }

            logger.debug { "Agent return actions: |${actionList.size}| to state $currentState" }

            val update = planner.update()
            if (update < 1.0) {
                actionList.forEach {
                    if (it != null) {
                        currentState = domain.transition(currentState, it) ?: return ExperimentResult(experimentConfiguration = configuration.valueStore, errorMessage = "Invalid transition. From $currentState with $it")// Move the planner
                        actions.add(it.toString()) // Save the action
                    }
                }
            } else {

                var count = 0
                for (it in actionList) {
                    if (it != null) {
                        if (count < maxCount) {
                            currentState = domain.transition(currentState, it) ?: return ExperimentResult(experimentConfiguration = configuration.valueStore, errorMessage = "Invalid transition. From $currentState with $it")// Move the planner
                            actions.add(it.toString())
                        }// Save the action
                        actionsLists.add(it.toString())
                        count++
                    }
                }
            }

            if (!domain.isGoal(currentState)) {
                actionsLists.add("" + update + " ")
            }
        }

        actionsLists.add("" + maxCount)
        actionsLists += actions

        logger.info { actionsLists.toString() }

        val pathLength = actions.size.toLong()
        val totalExecutionNanoTime = pathLength * configuration.actionDuration
        val goalAchievementTime = idlePlanningTime + totalExecutionNanoTime

        logger.info {
            "Path length: [$pathLength] After ${planner.expandedNodeCount} expanded " +
                    "and ${planner.generatedNodeCount} generated nodes in ${idlePlanningTime} ns. " +
                    "(${planner.expandedNodeCount / convertNanoUpDouble(idlePlanningTime, TimeUnit.SECONDS)} expanded nodes per sec)"
        }

        val experimentResult = ExperimentResult(
                configuration = configuration.valueStore,
                expandedNodes = planner.expandedNodeCount,
                generatedNodes = planner.generatedNodeCount,
                planningTime = totalPlanningTime,
                iterationCount = iterationCount,
                actionExecutionTime = totalExecutionNanoTime,
                goalAchievementTime = goalAchievementTime,
                idlePlanningTime = idlePlanningTime,
                pathLength = pathLength,
                actions = actions.map(String::toString)
        )

        domain.appendDomainSpecificResults(experimentResult)
        return experimentResult
    }
}

