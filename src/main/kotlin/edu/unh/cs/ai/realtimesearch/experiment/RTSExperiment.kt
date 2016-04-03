package edu.unh.cs.ai.realtimesearch.experiment

import edu.unh.cs.ai.realtimesearch.agent.RTSAgent
import edu.unh.cs.ai.realtimesearch.environment.Action
import edu.unh.cs.ai.realtimesearch.environment.Environment
import edu.unh.cs.ai.realtimesearch.environment.State
import edu.unh.cs.ai.realtimesearch.experiment.configuration.Configurations
import edu.unh.cs.ai.realtimesearch.experiment.configuration.GeneralExperimentConfiguration
import edu.unh.cs.ai.realtimesearch.experiment.configuration.lazyData
import edu.unh.cs.ai.realtimesearch.experiment.result.ExperimentResult
import edu.unh.cs.ai.realtimesearch.experiment.terminationCheckers.TimeTerminationChecker
import edu.unh.cs.ai.realtimesearch.logging.debug
import edu.unh.cs.ai.realtimesearch.logging.info
import edu.unh.cs.ai.realtimesearch.planner.CommitmentStrategy
import edu.unh.cs.ai.realtimesearch.util.convertNanoUpDouble
import org.slf4j.LoggerFactory
import java.util.concurrent.TimeUnit
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
class RTSExperiment<StateType : State<StateType>>(val experimentConfiguration: GeneralExperimentConfiguration,
                                                  val agent: RTSAgent<StateType>,
                                                  val world: Environment<StateType>,
                                                  val terminationChecker: TimeTerminationChecker) : Experiment() {

    private val logger = LoggerFactory.getLogger(RTSExperiment::class.java)
    private val commitmentStrategy by lazyData<String>(experimentConfiguration, Configurations.COMMITMENT_STRATEGY.toString())
    private val actionDuration by lazyData<Long>(experimentConfiguration, Configurations.ACTION_DURATION.toString())

    /**
     * Runs the experiment
     */
    override fun run(): ExperimentResult {
        val actions: MutableList<Action> = arrayListOf()
        logger.info { "Starting experiment from state ${world.getState()}" }

        var totalPlanningNanoTime = 0L
        var timeBound = actionDuration
        var singleStepLookahead = CommitmentStrategy.valueOf(commitmentStrategy) == CommitmentStrategy.SINGLE

        while (!world.isGoal()) {
            totalPlanningNanoTime += measureNanoTime {
                terminationChecker.init(timeBound)

                var actionList = agent.selectAction(world.getState(), terminationChecker);

                if (actionList.size > 1 && singleStepLookahead) {
                    actionList = listOf(actionList.first()) // Trim the action list to one item
                }

                logger.debug { "Agent return actions: |${actionList.size}| to state ${world.getState()}" }

                timeBound = 0
                actionList.forEach {
                    world.step(it.action) // Move the agent
                    actions.add(it.action) // Save the action
                    timeBound += it.duration.toLong() // Add up the action durations to calculate the time bound for the next iteration
                }
            }
        }

        val pathLength: Long = actions.size.toLong()
        val totalExecutionNanoTime = pathLength * actionDuration
        val goalAchievementTime = totalPlanningNanoTime + totalExecutionNanoTime // TODO fix for overlap
        logger.info { "Path length: [$pathLength] \nAfter ${agent.planner.expandedNodeCount} expanded " +
                "and ${agent.planner.generatedNodeCount} generated nodes in ${totalPlanningNanoTime} ns. " +
                "(${agent.planner.expandedNodeCount / convertNanoUpDouble(totalPlanningNanoTime, TimeUnit.SECONDS)} expanded nodes per sec)" }

        return ExperimentResult(
                experimentConfiguration.valueStore,
                agent.planner.expandedNodeCount,
                agent.planner.generatedNodeCount,
                totalPlanningNanoTime,
                totalExecutionNanoTime,
                goalAchievementTime,
                pathLength,
                actions.map { it.toString() })
    }
}

