package edu.unh.cs.searkt.experiment

import edu.unh.cs.searkt.MetronomeConfigurationException
import edu.unh.cs.searkt.MetronomeException
import edu.unh.cs.searkt.environment.Action
import edu.unh.cs.searkt.environment.Domain
import edu.unh.cs.searkt.environment.State
import edu.unh.cs.searkt.experiment.configuration.ExperimentConfiguration
import edu.unh.cs.searkt.experiment.configuration.realtime.TerminationType.EXPANSION
import edu.unh.cs.searkt.experiment.configuration.realtime.TerminationType.TIME
import edu.unh.cs.searkt.experiment.result.ExperimentResult
import edu.unh.cs.searkt.experiment.terminationCheckers.TerminationChecker
import edu.unh.cs.searkt.experiment.terminationCheckers.TimeTerminationChecker
import edu.unh.cs.searkt.planner.CommitmentStrategy
import edu.unh.cs.searkt.planner.RealTimePlanner
import edu.unh.cs.searkt.planner.realtime.LssLrtaStarPlanner
import edu.unh.cs.searkt.util.convertNanoUpDouble
import java.lang.System.currentTimeMillis
import java.util.concurrent.TimeUnit
import kotlin.math.ceil
import kotlin.system.measureNanoTime

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

    private val BYTES_PER_MB = 1024.0 * 1024.0

    private val maxMemoryMB = 7000 // max 7000 MB ~6.8 GB
    private val actionDuration = configuration.actionDuration
    private val expansionLimit = configuration.expansionLimit
    private val stepLimit = configuration.stepLimit
            ?: throw MetronomeConfigurationException("Step limit is not specified.")

    /**
     * Runs the experiment
     */
    override fun run(): ExperimentResult {
        val actions: MutableList<Action> = arrayListOf()
        var currentState: StateType = initialState

        var totalPlanningNanoTime = 0L
        var iterationCount = 0L
        val commitmentStrategy = configuration.commitmentStrategy
                ?: throw MetronomeConfigurationException("Lookahead strategy is not specified.")

        var timeBound = actionDuration
        val timeLimit = configuration.timeLimit ?: 0L

        var actionList: List<RealTimePlanner.ActionBundle> = listOf()
        val stateList = mutableListOf(currentState)
        val iterationCpuTimeList: MutableList<Long> = mutableListOf()

//        visualizer = initializeVisualizer()
//        if (visualizer != null) visualizerIsActive = true
//        visualizer?.initialize(initialState)

        planner.init(initialState)

        // Begin test
//        if (domain is Airspace) {
//            val levelProofLength = mutableListOf<Double>()
//            val levelRatios = mutableListOf<Double>()
//            val levelSuccessfulExpansions = mutableListOf<Double>()
//            val levelFailedExpansions = mutableListOf<Double>()
//
//            for (y in 0 until domain.height) {
//                val proofLength = mutableListOf<Int>()
//                val successfulExpansions = mutableListOf<Int>()
//                val failedExpansions = mutableListOf<Int>()
//
//                for (x in 0 until domain.width) {
//                    val state: StateType = AirspaceState(x, y) as StateType
//                    val safetyProofResult = isComfortable(state, FakeTerminationChecker, domain, false)
//
//                    if (safetyProofResult.status == SafetyProofStatus.SAFE) {
//                        proofLength.add(safetyProofResult.safetyProof.size)
//                        successfulExpansions.add(safetyProofResult.expansions)
//                    } else {
//                        failedExpansions.add(safetyProofResult.expansions)
//                    }
//                }
//
//                val safetyRatio = proofLength.size.toDouble() / domain.width
//                val averageLength = proofLength.average()
//
////                println("level: $y ratio: ${"%.2f".format(safetyRatio)} length: ${"%.2f".format(averageLength)}")
//                println("level: $y ratio: $safetyRatio length: $averageLength sexp: ${successfulExpansions.average()} fexp:${failedExpansions.average()}")
//
//
//                levelProofLength += averageLength
//                levelRatios += safetyRatio
//                levelSuccessfulExpansions += successfulExpansions.average()
//                levelFailedExpansions += failedExpansions.average()
//            }
//
//            for (i in 0 until domain.height) {
//                print("\t$i\t&")
//            }
//            println("""\\""")
//            levelRatios.forEach { print("\t${"%.2f".format(it)}\t&") }
//            println("""\\""")
//            levelProofLength.forEach { print("\t${"%.0f".format(it)}\t&") }
//            println("""\\""")
//            levelSuccessfulExpansions.forEach { print("\t${"%.0f".format(it)}\t&") }
//            println("""\\""")
//            levelFailedExpansions.forEach { print("\t${"%.0f".format(it)}\t&") }
//            println("""\\""")
//        }
        // End test

        val timerStart = currentTimeMillis()
        val maxTime = ceil(timeLimit / 1_000_000.0).toLong() + timerStart

        while (!domain.isGoal(currentState)) {
            val iterationNanoTime = measureNanoTime {
                terminationChecker.resetTo(timeBound)

                iterationCount++
                actionList = planner.selectAction(currentState, terminationChecker)

                if (actionList.size > 1 && commitmentStrategy == CommitmentStrategy.SINGLE) {
                    actionList = listOf(actionList.first()) // Trim the action list to one item
                }
            }

            timeBound = 0
            actionList.forEach {
                currentState = domain.transition(currentState, it.action)
                        ?: return ExperimentResult(experimentConfiguration = configuration, errorMessage = "Invalid transition. From $currentState with ${it.action}")// Move the planner
                stateList.add(currentState)
                actions.add(it.action) // Save the action
                timeBound += it.duration // Add up the action durations to calculate the time bound for the next iteration
            }

            //send iteration data to visualizer
//            if (visualizerIsActive) {
//                val itSummary = planner.getIterationSummary()
//                visualizer?.publishIteration(
//                        currentState,
//                        itSummary.envelopeIsFresh,
//                        itSummary.expandedNodes,
//                        itSummary.backupIsFresh,
//                        itSummary.backedUpNodes,
//                        itSummary.projectedPath,
//                        domain.isGoal(currentState))
//            }

            validateIteration(actionList, iterationNanoTime)

            totalPlanningNanoTime += iterationNanoTime
            iterationCpuTimeList.add(iterationNanoTime)

            fun createSnapshotResult(): ExperimentResult {
                val experimentResult = ExperimentResult(experimentConfiguration = configuration)
                experimentResult.apply {
                    expandedNodes = planner.expandedNodeCount
                    generatedNodes = planner.generatedNodeCount
                    planningTime = totalPlanningNanoTime
                    this.iterationCount = iterationCount
                    idlePlanningTime = actionDuration
                    pathLength = actions.size.toLong()
                    actionExecutionTime = pathLength * actionDuration
                    this.actions = actions.map(Action::toString)
                    this.iterationCpuTimeList = iterationCpuTimeList
                }
                return experimentResult
            }

            var errMsg: String? = null

            if (expansionLimit != null && expansionLimit <= planner.expandedNodeCount) {
                errMsg = "The planner exceeded the expansion limit: $expansionLimit"
            }
            if (stepLimit <= actions.size) {
                errMsg = "The planner exceeded the step limit: $stepLimit"
            }

            if (timeLimit > 0L && currentTimeMillis() > maxTime) {
                errMsg = "The planner exceeded the wallclock time limit: $timeLimit"
            }

            // check memory usage. We hard code our experiments to not use more than 7500 MB
            val usedBytes = Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory()
            val usedMB = usedBytes.toDouble() / BYTES_PER_MB
            if (usedMB > maxMemoryMB) {
                errMsg = "The planner exceeded the total memory limit. Used memory: $usedMB MB"
            }

            if (errMsg != null) {
                val experimentResult = createSnapshotResult()
                experimentResult.errorMessage = errMsg
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
                actions = actions.map(Action::toString),
                experimentRunTime = convertNanoUpDouble(totalPlanningNanoTime, TimeUnit.SECONDS),
                iterationCpuTimeList = iterationCpuTimeList
        )

        domain.appendDomainSpecificResults(experimentResult)
        planner.appendPlannerSpecificResults(experimentResult)

        return experimentResult
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
        }
    }
}

var visualizerIsActive: Boolean = false