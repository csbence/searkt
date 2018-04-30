package edu.unh.cs.ai.realtimesearch.experiment

import edu.unh.cs.ai.realtimesearch.MetronomeConfigurationException
import edu.unh.cs.ai.realtimesearch.MetronomeException
import edu.unh.cs.ai.realtimesearch.environment.Action
import edu.unh.cs.ai.realtimesearch.environment.Domain
import edu.unh.cs.ai.realtimesearch.environment.State
import edu.unh.cs.ai.realtimesearch.experiment.configuration.ExperimentConfiguration
import edu.unh.cs.ai.realtimesearch.experiment.configuration.realtime.TerminationType.EXPANSION
import edu.unh.cs.ai.realtimesearch.experiment.configuration.realtime.TerminationType.TIME
import edu.unh.cs.ai.realtimesearch.experiment.result.ExperimentResult
import edu.unh.cs.ai.realtimesearch.experiment.terminationCheckers.TerminationChecker
import edu.unh.cs.ai.realtimesearch.experiment.terminationCheckers.TimeTerminationChecker
import edu.unh.cs.ai.realtimesearch.logging.debug
import edu.unh.cs.ai.realtimesearch.logging.info
import edu.unh.cs.ai.realtimesearch.planner.CommitmentStrategy
import edu.unh.cs.ai.realtimesearch.planner.RealTimePlanner
import edu.unh.cs.ai.realtimesearch.planner.realtime.LssLrtaStarPlanner
import edu.unh.cs.ai.realtimesearch.util.convertNanoUpDouble
import org.slf4j.LoggerFactory
import java.lang.RuntimeException
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
 * @param planner is a RTS planner that will supply the actions
 * @param domain is the environment
 * @param terminationChecker controls the constraint put upon the planner
 */
class RealTimeExperiment<StateType : State<StateType>>(val configuration: ExperimentConfiguration,
                                                       val planner: RealTimePlanner<StateType>,
                                                       val domain: Domain<StateType>,
                                                       val initialState: StateType,
                                                       val terminationChecker: TerminationChecker) : Experiment() {

    private val logger = LoggerFactory.getLogger(RealTimeExperiment::class.java)

    private val actionDuration = configuration.actionDuration
    private val expansionLimit = configuration.expansionLimit
    private val stepLimit = configuration.stepLimit
            ?: throw MetronomeConfigurationException("Step limit is not specified.")

    /**
     * Runs the experiment
     */
    override fun run(): ExperimentResult {
        val actions: MutableList<Action> = arrayListOf()
        logger.debug { "Starting experiment from state $initialState" }
        var currentState: StateType = initialState

        var totalPlanningNanoTime = 0L
        var iterationCount = 0L
        val commitmentStrategy = configuration.commitmentStrategy
                ?: throw MetronomeConfigurationException("Lookahead strategy is not specified.")

        var timeBound = actionDuration
        var actionList: List<RealTimePlanner.ActionBundle> = listOf()

        initializeVisualizer()


        while (!domain.isGoal(currentState)) {
            val iterationNanoTime = measureThreadCpuNanoTime {
                terminationChecker.resetTo(timeBound)

                iterationCount++
                actionList = planner.selectAction(currentState, terminationChecker)

                if (actionList.size > 1 && commitmentStrategy == CommitmentStrategy.SINGLE) {
                    actionList = listOf(actionList.first()) // Trim the action list to one item
                }
            }

            timeBound = 0
            actionList.forEach {
                currentState = domain.transition(currentState, it.action) ?: return ExperimentResult(experimentConfiguration = configuration, errorMessage = "Invalid transition. From $currentState with ${it.action}")// Move the planner
                actions.add(it.action) // Save the action
                timeBound += it.duration // Add up the action durations to calculate the time bound for the next iteration
            }

            logger.debug { "Agent return actions: |${actionList.size}| to state $currentState" }
            validateIteration(actionList, iterationNanoTime)

            totalPlanningNanoTime += iterationNanoTime

            fun createSnapshotResult(): ExperimentResult {
                val experimentResult = ExperimentResult(experimentConfiguration = configuration)
                experimentResult.apply {
                    expandedNodes = planner.expandedNodeCount
                    generatedNodes = planner.generatedNodeCount
                    planningTime = totalPlanningNanoTime
                    iterationCount = iterationCount
                    goalAchievementTime = goalAchievementTime
                    idlePlanningTime = actionDuration
                    pathLength = actions.size.toLong()
                    actionExecutionTime = pathLength * actionDuration
                    this.actions = actions.map(Action::toString)
                }
                return experimentResult
            }

            if (expansionLimit <= planner.expandedNodeCount) {
                val experimentResult = createSnapshotResult()
                experimentResult.apply {
                    errorMessage = "The planner exceeded the expansion limit: $expansionLimit"
                }
                return experimentResult
            }

            if (stepLimit <= actions.size) {
                val experimentResult = createSnapshotResult()
                experimentResult.apply {
                    errorMessage = "The planner exceeded the step limit: $stepLimit"
                }
                return experimentResult
            }
        }

        val pathLength: Long = actions.size.toLong()
        val totalExecutionNanoDuration = pathLength * actionDuration
        val goalAchievementTime = when (configuration.terminationType) {
            TIME -> actionDuration + totalExecutionNanoDuration // We only plan during execution plus the first iteration
            EXPANSION -> actionDuration + pathLength * actionDuration
            else -> throw MetronomeException("Unsupported termination checker")
        }

        logger.info {
            "Path length: [$pathLength] After ${planner.expandedNodeCount} expanded " +
                    "and ${planner.generatedNodeCount} generated nodes in $totalPlanningNanoTime ns. " +
                    "(${planner.expandedNodeCount / convertNanoUpDouble(totalPlanningNanoTime, TimeUnit.SECONDS)} expanded nodes per sec)"
        }

        val experimentResult = ExperimentResult(
                configuration = configuration,
                expandedNodes = planner.expandedNodeCount,
                generatedNodes = planner.generatedNodeCount,
                planningTime = totalPlanningNanoTime,
                iterationCount = iterationCount,
                actionExecutionTime = totalExecutionNanoDuration,
                goalAchievementTime = goalAchievementTime,
                idlePlanningTime = actionDuration,
                pathLength = pathLength,
                actions = actions.map(Action::toString))

        domain.appendDomainSpecificResults(experimentResult)
        planner.appendPlannerSpecificResults(experimentResult)
        return experimentResult
    }

    private fun initializeVisualizer() {
        Thread({
            Application.launch(OnlineGridVisualizer::class.java)
        }).start()

        visualizerLatch.await()
        println("Visualizer initialized")

        val setupLatch = CountDownLatch(1)
        // Visualizer setup
        Platform.runLater {
            visualizer?.setup(domain, initialState)
            setupLatch.countDown()
        }

        setupLatch.await()
        println("Setup completed")
    }

    private fun validateIteration(actionList: List<RealTimePlanner.ActionBundle>, iterationNanoTime: Long) {
        if (actionList.isEmpty()) {
            val extras = if (planner is LssLrtaStarPlanner) {
                "A*: ${planner.aStarTimer} Learning: ${planner.dijkstraTimer}"
            } else {
                ""
            }

            throw RuntimeException("Select action did not return actions in the given bound. The planner is confused. $extras")
        }

        // Check if the algorithm satisfies the real-time bound
        if (terminationChecker !is TimeTerminationChecker) return
        if (terminationChecker.timeLimit < iterationNanoTime) {
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

