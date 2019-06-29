package edu.unh.cs.searkt.experiment

import edu.unh.cs.searkt.MetronomeConfigurationException
import edu.unh.cs.searkt.environment.Action
import edu.unh.cs.searkt.environment.Domain
import edu.unh.cs.searkt.environment.State
import edu.unh.cs.searkt.experiment.configuration.ExperimentConfiguration
import edu.unh.cs.searkt.experiment.result.ExperimentResult
import edu.unh.cs.searkt.experiment.terminationCheckers.FakeTerminationChecker
import edu.unh.cs.searkt.planner.AnytimePlanner
import edu.unh.cs.searkt.util.convertNanoUpDouble
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
                                                      val configuration: ExperimentConfiguration,
                                                      val domain: Domain<StateType>,
                                                      val initialState: StateType) : Experiment() {

    /**
     * Runs the experiment
     */
    override fun run(): ExperimentResult {
        val actions: MutableList<String> = arrayListOf()
        val actionsLists: MutableList<String> = arrayListOf()
        var actionList: MutableList<Action?> = arrayListOf()
        var currentState = initialState
        //        val maxCount = 6
        val maxCount: Long = configuration.anytimeMaxCount
                ?: throw MetronomeConfigurationException("Anytime max count is not specified.")

        var idlePlanningTime = 1L
        var totalPlanningTime = 0L
        var iterationCount = 0L

        while (!domain.isGoal(currentState)) {
            val startTime = getThreadCpuNanoTime()

            val tempActions = ArrayList(planner.selectAction(currentState, FakeTerminationChecker))

            val endTime = getThreadCpuNanoTime()
            totalPlanningTime += endTime - startTime

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


            val update = planner.update()
            if (update < 1.0) {
                actionList.forEach {
                    if (it != null) {
                        currentState = domain.transition(currentState, it)
                                ?: return ExperimentResult(experimentConfiguration = configuration, errorMessage = "Invalid transition. From $currentState with $it")// Move the planner
                        actions.add(it.toString()) // Save the action
                    }
                }
            } else {

                var count = 0
                for (it in actionList) {
                    if (it != null) {
                        if (count < maxCount) {
                            currentState = domain.transition(currentState, it)
                                    ?: return ExperimentResult(experimentConfiguration = configuration, errorMessage = "Invalid transition. From $currentState with $it")// Move the planner
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


        val pathLength = actions.size.toLong()
        val totalExecutionNanoTime = pathLength * configuration.actionDuration
        val goalAchievementTime = idlePlanningTime + totalExecutionNanoTime

        val experimentResult = ExperimentResult(
                configuration = configuration,
                expandedNodes = planner.expandedNodeCount,
                generatedNodes = planner.generatedNodeCount,
                planningTime = totalPlanningTime,
                iterationCount = iterationCount,
                actionExecutionTime = totalExecutionNanoTime,
                goalAchievementTime = goalAchievementTime,
                idlePlanningTime = idlePlanningTime,
                pathLength = pathLength,
                actions = actions.map(String::toString),
                experimentRunTime = convertNanoUpDouble(totalPlanningTime, TimeUnit.SECONDS),
                iterationCpuTimeList = listOf()
        )

        domain.appendDomainSpecificResults(experimentResult)
        return experimentResult
    }
}

