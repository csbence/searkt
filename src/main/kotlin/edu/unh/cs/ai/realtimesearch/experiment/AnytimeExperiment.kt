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
import edu.unh.cs.ai.realtimesearch.planner.anytime.AnytimeRepairingAStarPlanner
import edu.unh.cs.ai.realtimesearch.util.convertNanoUpDouble
import org.slf4j.LoggerFactory
import java.util.*
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
class AnytimeExperiment<StateType : State<StateType>>(val planner: AnytimeRepairingAStarPlanner<StateType>,
                                                      val experimentConfiguration: GeneralExperimentConfiguration,
                                                      val world: Environment<StateType>) : Experiment() {

    private val logger = LoggerFactory.getLogger(AnytimeExperiment::class.java)

    private val maxCount: Long =
            experimentConfiguration.getTypedValue<Long>(Configurations.ANYTIME_MAX_COUNT.toString()) ?:
                    throw InvalidFieldException("\"${Configurations.ANYTIME_MAX_COUNT}\" is not found. Please add it to the experiment configuration.")
    private val maxPlanningTime: Long = experimentConfiguration.actionDuration * maxCount

    /**
     * Runs the experiment
     */
    override fun run(): ExperimentResult {
        val executedActions: MutableList<Action> = arrayListOf()
        val allPlannedActions: MutableMap<Double, List<String>> = mutableMapOf()
        var workingActionList: List<Action?> = listOf()
        var actionIterator: Iterator<Action?> = workingActionList.iterator()
        var solvedPlan: List<Action?> = listOf()

        logger.info { "Starting experiment from state ${world.getState()}" }
        var idlePlanningTime = 1L
        var totalPlanningTime = 0L

        while (!world.isGoal()) {
            logger.debug { "Start anytime search" }

            val iterationNanoTime = measureThreadCpuNanoTime {
                solvedPlan = planner.solve(world.getState(), world.getGoal())
            }
            totalPlanningTime += iterationNanoTime

            logger.debug { "time: $iterationNanoTime" }
            if (executedActions.size == 0) {
                // First plan
                idlePlanningTime = iterationNanoTime
                workingActionList = solvedPlan
                actionIterator = workingActionList.iterator()
            } else if (iterationNanoTime <= maxPlanningTime) {
                // TODO could try to keep planning if have extra time
                workingActionList = solvedPlan
                actionIterator = workingActionList.iterator()
            }
            // else {
            // Didn't finish within planning time so keep current plan

            logger.debug { "Agent return actions: |${workingActionList.size}| to state ${world.getState()}" }

            val updatedInflationFactor = planner.update()
            if (updatedInflationFactor < 1.0) {
                // TODO paper says while inflation > 1, so should be <= 1.0 here?
                // Done planning; execute remaining actions
                while (actionIterator.hasNext()) {
                    val action = actionIterator.next()
                    if (action != null) {
                        world.step(action) // Move the agent
                        executedActions.add(action) // Save the action
                    }
                }
            } else {
                // Have agent perform next maxCount actions
                var count = 0
                while (actionIterator.hasNext() && count < maxCount) {
                    val action = actionIterator.next()
                    if (action != null) {
                        world.step(action) // Move the agent
                        executedActions.add(action) // Save the action
                    }
                    count++
                }
            }

            allPlannedActions.put(updatedInflationFactor, solvedPlan.filter { it != null }.map { it.toString() })
        }

        logger.info { allPlannedActions.toString() }

        val pathLength = executedActions.size.toLong()
        val totalExecutionNanoTime = pathLength * experimentConfiguration.actionDuration
        val goalAchievementTime = idlePlanningTime + totalExecutionNanoTime

        logger.info {
            "Path length: [$pathLength] After ${planner.expandedNodeCount} expanded " +
                    "and ${planner.generatedNodeCount} generated nodes in $idlePlanningTime ns. " +
                    "(${planner.expandedNodeCount / convertNanoUpDouble(idlePlanningTime, TimeUnit.SECONDS)} expanded nodes per sec)"
        }

        val result = ExperimentResult(
                experimentConfiguration = experimentConfiguration.valueStore,
                expandedNodes = planner.expandedNodeCount,
                generatedNodes = planner.generatedNodeCount,
                planningTime = totalPlanningTime,
                actionExecutionTime = totalExecutionNanoTime,
                goalAchievementTime = goalAchievementTime,
                idlePlanningTime = idlePlanningTime,
                pathLength = pathLength,
                actions = executedActions.map { it.toString() }
        )

        result["allPlans"] = allPlannedActions

        return result
    }
}

