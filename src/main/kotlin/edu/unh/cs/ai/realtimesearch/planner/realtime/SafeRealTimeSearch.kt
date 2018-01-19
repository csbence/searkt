package edu.unh.cs.ai.realtimesearch.planner.realtime

import edu.unh.cs.ai.realtimesearch.MetronomeConfigurationException
import edu.unh.cs.ai.realtimesearch.environment.Domain
import edu.unh.cs.ai.realtimesearch.environment.NoOperationAction
import edu.unh.cs.ai.realtimesearch.environment.State
import edu.unh.cs.ai.realtimesearch.environment.SuccessorBundle
import edu.unh.cs.ai.realtimesearch.experiment.configuration.ExperimentConfiguration
import edu.unh.cs.ai.realtimesearch.experiment.measureLong
import edu.unh.cs.ai.realtimesearch.experiment.terminationCheckers.StaticExpansionTerminationChecker
import edu.unh.cs.ai.realtimesearch.experiment.terminationCheckers.TerminationChecker
import edu.unh.cs.ai.realtimesearch.logging.debug
import edu.unh.cs.ai.realtimesearch.logging.warn
import edu.unh.cs.ai.realtimesearch.planner.*
import edu.unh.cs.ai.realtimesearch.planner.exception.GoalNotReachableException
import edu.unh.cs.ai.realtimesearch.util.AdvancedPriorityQueue
import edu.unh.cs.ai.realtimesearch.util.generateWhile
import edu.unh.cs.ai.realtimesearch.util.resize
import org.slf4j.LoggerFactory
import java.util.*
import kotlin.system.measureTimeMillis

/**
 * @author Bence Cserna (bence@cserna.net)
 */
class SafeRealTimeSearch<StateType : State<StateType>>(override val domain: Domain<StateType>, val configuration: ExperimentConfiguration) : RealTimePlanner<StateType>(), RealTimePlannerContext<StateType, SafeRealTimeSearchNode<StateType>> {
    // Configuration
    private val targetSelection = configuration.targetSelection
            ?: throw MetronomeConfigurationException("Target selection strategy is not specified.")
    private val safetyExplorationRatio: Double = configuration.safetyExplorationRatio
            ?: throw MetronomeConfigurationException("Safety/exploration ratio is not specified.")
    private val safetyProof = configuration.safetyProof
            ?: throw MetronomeConfigurationException("Safety proof is not specified.")

    private val logger = LoggerFactory.getLogger(SafeRealTimeSearch::class.java)
    override var iterationCounter = 0L

    private val nodes: HashMap<StateType, SafeRealTimeSearchNode<StateType>> = HashMap<StateType, SafeRealTimeSearchNode<StateType>>(100000000).resize()

    // LSS stores heuristic values. Use those, but initialize them according to the domain heuristic
    // The cost values are initialized to infinity
    override var openList = AdvancedPriorityQueue<SafeRealTimeSearchNode<StateType>>(10000000, fValueComparator)

    private var safeNodes = mutableListOf<SafeRealTimeSearchNode<StateType>>()

    private var rootState: StateType? = null

    private var continueSearch = false

    // Performance measurement
    private var aStarPopCounter = 0
    private var dijkstraPopCounter = 0
    var aStarTimer = 0L
        get
    var dijkstraTimer = 0L
        get

    var lastSafeNode: SafeRealTimeSearchNode<StateType>? = null

    private val aStarSequence
        get() = generateSequence {
            aStarPopCounter++

            val currentNode = openList.pop() ?: throw GoalNotReachableException("Open list is empty.")

            if (currentNode.safe || domain.isSafe(currentNode.state)) {
                currentNode.safe = true
                safeNodes.add(currentNode)
                lastSafeNode = currentNode
            }

            expandFromNode(this, currentNode) {
                if (it.safe || domain.isSafe(it.state)) {
                    safeNodes.add(it)
                    it.safe = true
                }
            }

            currentNode
        }

    /**
     * Selects a action given current sourceState.
     *
     * LSS_LRTA* will generate a full plan to some frontier, and stick to that plan. So the action returned will
     * always be the first on in the current plan.
     *
     * LSS-LRTAStar will plan to a specific frontier, and continue
     * to plan from there. This planning abides a termination criteria, meaning that it plans under constraints
     *
     * @param sourceState is the current sourceState
     * @param terminationChecker is the constraint
     * @return a current action
     */
    override fun selectAction(sourceState: StateType, terminationChecker: TerminationChecker): List<RealTimePlanner.ActionBundle> {
        // Initiate for the first search

        if (rootState == null) {
            rootState = sourceState
        } else if (sourceState != rootState) {
            // The given sourceState should be the last target
            logger.debug { "Inconsistent world sourceState. Expected $rootState got $sourceState" }
        }

        if (domain.isGoal(sourceState)) {
            // The start sourceState is the goal sourceState
            logger.warn { "selectAction: The goal sourceState is already found." }
            return emptyList()
        }

        logger.debug { "Root sourceState: $sourceState" }
        // Every turn learn then A* until time expires

        // Learning phase
        if (openList.isNotEmpty() && !continueSearch) {
            dijkstraTimer += measureTimeMillis { dijkstra(this) }
        }

        // Exploration phase
        var plan: List<RealTimePlanner.ActionBundle>? = null
        aStarTimer += measureTimeMillis {
            val (targetNode, lastSafeNode) = when (safetyProof) {
                SafetyProof.LOW_D_WINDOW -> windowedMicroIteration(sourceState, terminationChecker)
                SafetyProof.TOP_OF_OPEN -> microIteration(sourceState, terminationChecker)
            }

            // Backup safety
            predecessorSafetyPropagation(safeNodes)
            safeNodes.clear()

            val currentSafeTarget = when (targetSelection) {
            // What the safe predecessors are on a dead-path (meaning not reachable by the parent pointers)
                SafeRealTimeSearchTargetSelection.SAFE_TO_BEST -> selectSafeToBest(openList)
                SafeRealTimeSearchTargetSelection.BEST_SAFE -> lastSafeNode
            }

            val targetSafeNode = currentSafeTarget
                    ?: attemptIdentityAction(sourceState)?.apply { }
                    ?: bestSafeChild(sourceState, domain, { state -> nodes[state]?.safe ?: false })?.let { nodes[it] }
                    ?: targetNode

            plan = extractPath(targetSafeNode, sourceState)
            rootState = targetSafeNode.state
        }

        logger.debug { "AStar pops: $aStarPopCounter Dijkstra pops: $dijkstraPopCounter" }
        logger.debug { "AStar time: $aStarTimer Dijkstra pops: $dijkstraTimer" }

        return plan!!
    }

    private fun attemptIdentityAction(sourceState: StateType): SafeRealTimeSearchNode<StateType>? {
        if (domain.isSafe(sourceState)) {
            // The current state is safe, attempt an identity action
            domain.getIdentityAction(sourceState)?.let {
                continueSearch = true // The planner can continue the search in the next iteration since the state is not changed
                return nodes[it.state]
            }
        }

        return null
    }

    /**
     * Runs AStar until termination and returns the path to the head of openList
     * Will just repeatedly expand according to A*.
     */
    private fun windowedMicroIteration(sourceState: StateType, terminationChecker: TerminationChecker): Pair<SafeRealTimeSearchNode<StateType>, SafeRealTimeSearchNode<StateType>?> {
        logger.debug { "Starting A* from sourceState: $sourceState" }
        initializeAStar(sourceState)

        var totalExpansionDuration = 0L
        var currentExpansionDuration = 0L
        var totalSafetyDuration = 0L
        var costBucket = 10
        lastSafeNode = null

        aStarSequence
                .generateWhile {
                    !terminationChecker.reachedTermination() && !domain.isGoal(openList.peek()?.state
                            ?: throw GoalNotReachableException("Open list is empty."))
                }
                .onEach {
                    terminationChecker.notifyExpansion()
                    currentExpansionDuration++
                }
                .windowed(size = 10, partialWindows = true)
                .forEach { lastNodes ->
                    if (currentExpansionDuration >= costBucket) {
                        val nodeToProve = lastNodes.minBy { domain.safeDistance(it.state).first }
                                ?: throw GoalNotReachableException("Goal is not reachable")

                        // Switch to safety
                        totalExpansionDuration += currentExpansionDuration
                        currentExpansionDuration = 0L

                        val exponentialExpansionLimit = minOf((costBucket * safetyExplorationRatio).toLong(), terminationChecker.remaining())
                        val safetyTerminationChecker = StaticExpansionTerminationChecker(exponentialExpansionLimit)

                        val safetyProofDuration = proveSafety(nodeToProve, safetyTerminationChecker)

                        terminationChecker.notifyExpansion(safetyProofDuration)
                        totalSafetyDuration += safetyProofDuration

                        if (nodeToProve.safe) {
                            // If proof was successful reset the bucket
                            costBucket = 10
                            safeNodes.add(nodeToProve)
                            // TODO set last safe?
                        } else {
                            // Increase the
                            costBucket *= 2
                        }
                    }
                }

        return (openList.peek() ?: throw GoalNotReachableException("Open list is empty.")) to lastSafeNode
    }

    /**
     * Runs AStar until termination and returns the path to the head of openList
     * Will just repeatedly expand according to A*.
     */
    private fun microIteration(sourceState: StateType, terminationChecker: TerminationChecker): Pair<SafeRealTimeSearchNode<StateType>, SafeRealTimeSearchNode<StateType>?> {
        logger.debug { "Starting A* from sourceState: $sourceState" }
        initializeAStar(sourceState)

        var totalExpansionDuration = 0L
        var currentExpansionDuration = 0L
        var totalSafetyDuration = 0L
        var costBucket = 10
        lastSafeNode = null

        aStarSequence
                .generateWhile {
                    !terminationChecker.reachedTermination() && !domain.isGoal(openList.peek()?.state
                            ?: throw GoalNotReachableException("Open list is empty."))
                }
                .onEach {
                    terminationChecker.notifyExpansion()
                    currentExpansionDuration++
                }
                .forEach {
                    if (currentExpansionDuration >= costBucket) {
                        // Switch to safety
                        totalExpansionDuration += currentExpansionDuration
                        currentExpansionDuration = 0L

                        val exponentialExpansionLimit = minOf((costBucket * safetyExplorationRatio).toLong(), terminationChecker.remaining())
                        val safetyTerminationChecker = StaticExpansionTerminationChecker(exponentialExpansionLimit)

                        val nextTopNode = openList.peek() ?: throw GoalNotReachableException("Goal is not reachable")
                        val safetyProofDuration = proveSafety(nextTopNode, safetyTerminationChecker)

                        terminationChecker.notifyExpansion(safetyProofDuration)
                        totalSafetyDuration += safetyProofDuration

                        if (nextTopNode.safe) {
                            // If proof was successful reset the bucket
                            costBucket = 10
                            safeNodes.add(nextTopNode)
                        } else {
                            // Increase the
                            costBucket *= 2
                        }
                    }
                }

        return (openList.peek() ?: throw GoalNotReachableException("Open list is empty.")) to lastSafeNode
    }

    /**
     * Prove that the given node is safe and set its safety flag to true.
     * The process will terminate when the termination checker expires or when the proof is completed.
     * If the termination checker expires then the safety flag is not set.
     */
    private fun proveSafety(sourceNode: SafeRealTimeSearchNode<StateType>, terminationChecker: TerminationChecker): Long {
        return measureLong(terminationChecker::elapsed) {
            when {
                sourceNode.safe -> {
                }
                domain.isSafe(sourceNode.state) -> sourceNode.safe = true
                else -> {
                    val safetyProof = isComfortable(
                            sourceNode.state,
                            terminationChecker,
                            domain,
                            { state -> nodes[state]?.safe ?: false })

                    safetyProof?.run {
                        // Mark all nodes as safe
                        forEach {
                            val uninitializedNode = getUninitializedNode(it)
                            uninitializedNode.safe = true
                        }

                        sourceNode.safe = true
                    }
                }
            }
        }
    }

    private fun initializeAStar(state: StateType) {
        if (continueSearch) {
            continueSearch = false
        } else {
            iterationCounter++
            openList.clear()
            openList.reorder(fValueComparator)

            val node = getUninitializedNode(state)
            node.apply {
                cost = 0
                actionCost = 0
                iteration = iterationCounter
                parent = node
                action = NoOperationAction
                predecessors.clear()
                safe = safe || domain.isSafe(state)
            }

            nodes[state] = node
            openList.add(node)
        }
    }

    private fun getUninitializedNode(state: StateType): SafeRealTimeSearchNode<StateType> {
        val tempNode = nodes[state]

        return if (tempNode != null) {
            tempNode
        } else {
            generatedNodeCount++
            val node = SafeRealTimeSearchNode(
                    state = state,
                    heuristic = domain.heuristic(state),
                    actionCost = 0,
                    action = NoOperationAction,
                    cost = Long.MAX_VALUE,
                    iteration = 0)

            nodes[state] = node
            node
        }
    }

    /**
     * Get a node for the state if exists, else create a new node.
     *
     * @return node corresponding to the given state.
     */
    override fun getNode(parent: SafeRealTimeSearchNode<StateType>, successor: SuccessorBundle<StateType>): SafeRealTimeSearchNode<StateType> {
        val successorState = successor.state
        val tempSuccessorNode = nodes[successorState]

        return if (tempSuccessorNode == null) {
            generatedNodeCount++

            val undiscoveredNode = SafeRealTimeSearchNode(
                    state = successorState,
                    heuristic = domain.heuristic(successorState),
                    actionCost = successor.actionCost,
                    action = successor.action,
                    parent = parent,
                    cost = Long.MAX_VALUE,
                    iteration = iterationCounter
            )

            nodes[successorState] = undiscoveredNode
            undiscoveredNode
        } else {
            tempSuccessorNode
        }
    }
}

