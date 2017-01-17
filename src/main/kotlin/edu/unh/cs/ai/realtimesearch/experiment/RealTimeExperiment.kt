package edu.unh.cs.ai.realtimesearch.experiment

import edu.unh.cs.ai.realtimesearch.agent.RTSAgent
import edu.unh.cs.ai.realtimesearch.environment.Action
import edu.unh.cs.ai.realtimesearch.environment.Domain
import edu.unh.cs.ai.realtimesearch.environment.State
import edu.unh.cs.ai.realtimesearch.experiment.configuration.Configurations
import edu.unh.cs.ai.realtimesearch.experiment.configuration.GeneralExperimentConfiguration
import edu.unh.cs.ai.realtimesearch.experiment.configuration.lazyData
import edu.unh.cs.ai.realtimesearch.experiment.result.ExperimentResult
import edu.unh.cs.ai.realtimesearch.experiment.terminationCheckers.TimeTerminationChecker
import edu.unh.cs.ai.realtimesearch.logging.debug
import edu.unh.cs.ai.realtimesearch.logging.info
import edu.unh.cs.ai.realtimesearch.planner.CommitmentStrategy
import edu.unh.cs.ai.realtimesearch.planner.RealTimePlanner
import edu.unh.cs.ai.realtimesearch.planner.realtime.LssLrtaStarPlanner
import edu.unh.cs.ai.realtimesearch.util.convertNanoUpDouble
import org.slf4j.LoggerFactory
import java.util.concurrent.TimeUnit

/**
 * An RTS experiment repeatedly queries the agent
 * for an action by some constraint (allowed time for example).
 * After each selected action, the experiment then applies this action
 * to its environment.
 *
 * The states are given by the environment, the domain. When creating the domain
 * it might be possible to determine what the initial state is.
 *
 * NOTE: assumes the same domain is used to create both the agent as the domain
 *
 * @param agent is a RTS agent that will supply the actions
 * @param domain is the environment
 * @param terminationChecker controls the constraint put upon the agent
 */
class RealTimeExperiment<StateType : State<StateType>>(val experimentConfiguration: GeneralExperimentConfiguration,
                                                       val agent: RTSAgent<StateType>,
                                                       val domain: Domain<StateType>,
                                                       val initialState: State<StateType>,
                                                       val terminationChecker: TimeTerminationChecker) : Experiment() {

    private val logger = LoggerFactory.getLogger(RealTimeExperiment::class.java)
    private val commitmentStrategy by lazyData<String>(experimentConfiguration, Configurations.COMMITMENT_STRATEGY.toString())
    private val actionDuration by lazyData<Long>(experimentConfiguration, Configurations.ACTION_DURATION.toString())

    /**
     * Runs the experiment
     */
    override fun run(): ExperimentResult {
        val actions: MutableList<Action> = arrayListOf()
        logger.info { "Starting experiment from state ${domain.getStaState()}" }

        var totalPlanningNanoTime = 0L
        val singleStepLookahead = CommitmentStrategy.valueOf(commitmentStrategy) == CommitmentStrategy.SINGLE

        var timeBound = actionDuration
        var actionList: List<RealTimePlanner.ActionBundle> = listOf()

        while (!domain.isGoal()) {
            val iterationNanoTime = measureThreadCpuNanoTime {
                terminationChecker.init(timeBound)

                actionList = agent.selectAction(domain.getState(), terminationChecker);

                if (actionList.size > 1 && singleStepLookahead) {
                    actionList = listOf(actionList.first()) // Trim the action list to one item
                }

                timeBound = 0
                actionList.forEach {
                    domain.step(it.action) // Move the agent
                    actions.add(it.action) // Save the action
                    timeBound += it.duration.toLong() // Add up the action durations to calculate the time bound for the next iteration
                }
            }

            logger.debug { "Agent return actions: |${actionList.size}| to state ${domain.getState()}" }

            validateInteration(actionList, iterationNanoTime)

            totalPlanningNanoTime += iterationNanoTime

        }

        val pathLength: Long = actions.size.toLong()
        val totalExecutionNanoTime = pathLength * actionDuration
        val goalAchievementTime = actionDuration + totalExecutionNanoTime // We only plan during execution plus the first iteration
        logger.info {
            "Path length: [$pathLength] After ${agent.planner.expandedNodeCount} expanded " +
                    "and ${agent.planner.generatedNodeCount} generated nodes in ${totalPlanningNanoTime} ns. " +
                    "(${agent.planner.expandedNodeCount / convertNanoUpDouble(totalPlanningNanoTime, TimeUnit.SECONDS)} expanded nodes per sec)"
        }

        return ExperimentResult(
                experimentConfiguration = experimentConfiguration.valueStore,
                expandedNodes = agent.planner.expandedNodeCount,
                generatedNodes = agent.planner.generatedNodeCount,
                planningTime = totalPlanningNanoTime,
                actionExecutionTime = totalExecutionNanoTime,
                goalAchievementTime = goalAchievementTime,
                idlePlanningTime = actionDuration,
                pathLength = pathLength,
                actions = actions.map { it.toString() })
    }

    private fun validateInteration(actionList: List<RealTimePlanner.ActionBundle>, iterationNanoTime: Long) {
        if (actionList.isEmpty()) {
            val planner = agent.planner
            val extras = if (planner is LssLrtaStarPlanner) {
                "A*: ${planner.aStarTimer} Learning: ${planner.dijkstraTimer}"
            } else {
                ""
            }

            throw RuntimeException("Select action did not return actions in the given time bound: ${terminationChecker.timeLimit}. The agent is confused. $extras")
        }

        // Check if the algorithm satisfies the real-time bound
        if (terminationChecker.timeLimit < iterationNanoTime) {
            val planner = agent.planner
            val extras = if (planner is LssLrtaStarPlanner) {
                "A*: ${planner.aStarTimer} Learning: ${planner.dijkstraTimer}"
            } else {
                ""
            }

            throw RuntimeException("Real-time bound is violated. Time bound: ${terminationChecker.timeLimit} but execution took $iterationNanoTime. $extras")
        } else {
            logger.info { "Time bound: ${terminationChecker.timeLimit} execution took $iterationNanoTime." }
        }
    }
}

