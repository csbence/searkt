package edu.unh.cs.ai.realtimesearch.planner.realtime

import edu.unh.cs.ai.realtimesearch.MetronomeConfigurationException
import edu.unh.cs.ai.realtimesearch.environment.Domain
import edu.unh.cs.ai.realtimesearch.environment.NoOperationAction
import edu.unh.cs.ai.realtimesearch.environment.State
import edu.unh.cs.ai.realtimesearch.environment.SuccessorBundle
import edu.unh.cs.ai.realtimesearch.experiment.configuration.ExperimentConfiguration
import edu.unh.cs.ai.realtimesearch.experiment.measureLong
import edu.unh.cs.ai.realtimesearch.experiment.result.ExperimentResult
import edu.unh.cs.ai.realtimesearch.experiment.terminationCheckers.StaticExpansionTerminationChecker
import edu.unh.cs.ai.realtimesearch.experiment.terminationCheckers.TerminationChecker
import edu.unh.cs.ai.realtimesearch.planner.*
import edu.unh.cs.ai.realtimesearch.planner.SafetyProof.A_STAR_FIRST
import edu.unh.cs.ai.realtimesearch.planner.SafetyProof.TOP_OF_OPEN
import edu.unh.cs.ai.realtimesearch.planner.SafetyProofStatus.*
import edu.unh.cs.ai.realtimesearch.planner.exception.GoalNotReachableException
import edu.unh.cs.ai.realtimesearch.util.AdvancedPriorityQueue
import edu.unh.cs.ai.realtimesearch.util.generateWhile
import edu.unh.cs.ai.realtimesearch.util.resize
import kotlinx.serialization.ImplicitReflectionSerializer
import java.util.*

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

    private val filterUnsafe: Boolean = configuration.filterUnsafe

    override var iterationCounter = 0L

    private val nodes: HashMap<StateType, SafeRealTimeSearchNode<StateType>> = HashMap<StateType, SafeRealTimeSearchNode<StateType>>(1000000).resize()

    private val explorationComparator = ConfigurableComparator(configuration)

    override var openList = AdvancedPriorityQueue<SafeRealTimeSearchNode<StateType>>(1000000, explorationComparator)

    private var safeNodes = mutableListOf<SafeRealTimeSearchNode<StateType>>()

    private var rootState: StateType? = null

    private var continueSearch = false

    @ImplicitReflectionSerializer
    override fun appendPlannerSpecificResults(results: ExperimentResult) {
        results.attributes["unsafeSearchReexpansion"] = counters["unsafeSearchReexpansion"] ?: 0
        results.attributes["unsafeProofReexpansion"] = counters["unsafeProofReexpansion"] ?: 0
    }

    // Performance measurement
    private var aStarPopCounter = 0
    private var dijkstraPopCounter = 0

    var lastSafeNode: SafeRealTimeSearchNode<StateType>? = null

    private val aStarSequence
        get() = generateSequence {
            aStarPopCounter++

            var currentNode = openList.pop() ?: throw GoalNotReachableException("Open list is empty.[2]")


            if (filterUnsafe) {
                while (currentNode.unsafe) {
                    currentNode = openList.pop() ?: throw GoalNotReachableException("Open list is empty.[3]")
                }
            } else if (currentNode.unsafe) {
                incrementCounter("unsafeSearchReexpansion")
            }

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

    private var unusedExpansions = 0L

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
    override fun selectAction(sourceState: StateType, terminationChecker: TerminationChecker): List<ActionBundle> {
        // Initiate for the first search
        terminationChecker.resetTo(unusedExpansions + terminationChecker.remaining())
        unusedExpansions = 0

        if (rootState == null) {
            rootState = sourceState
        } else if (sourceState != rootState) {
            // The given sourceState should be the last target
        }

        if (domain.isGoal(sourceState)) {
            // The start sourceState is the goal sourceState
            return emptyList()
        }

        // Every turn learn then A* until time expires

        // Learning phase
        if (openList.isNotEmpty() && !continueSearch) {
            dijkstra(this)
        }

        // Exploration phase

        val (targetNode, lastSafeNode) = when (safetyProof) {
            TOP_OF_OPEN -> microIteration(sourceState, terminationChecker)
            A_STAR_FIRST -> macroIteration(sourceState, terminationChecker)
            else -> TODO()
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
                ?: bestSafeChild(sourceState, domain) { state -> nodes[state]?.safe ?: false }?.let { nodes[it] }
                ?: targetNode

        rootState = targetSafeNode.state
        unusedExpansions = terminationChecker.remaining()

        return extractPath(targetSafeNode, sourceState)
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
    private fun microIteration(sourceState: StateType, terminationChecker: TerminationChecker): Pair<SafeRealTimeSearchNode<StateType>, SafeRealTimeSearchNode<StateType>?> {
        initializeAStar(sourceState)

        var totalExpansionDuration = 0L
        var currentExpansionDuration = 0L
        var totalSafetyDuration = 0L
        var costBucket = 10
        lastSafeNode = null

        aStarSequence
                .generateWhile {
                    !terminationChecker.reachedTermination() && !domain.isGoal(openList.peek()?.state
                            ?: throw GoalNotReachableException("Open list is empty. [1]"))
                }
                .onEach {
                    terminationChecker.notifyExpansion()
                    currentExpansionDuration++
                }
                .forEach { _ ->
                    if (currentExpansionDuration >= costBucket * 2 * safetyExplorationRatio) {
                        // Switch to safety
                        appendAttribute("expansions", currentExpansionDuration.toInt())

                        totalExpansionDuration += currentExpansionDuration
                        currentExpansionDuration = 0L

                        val desiredExpansionLimit = costBucket * 2 - currentExpansionDuration
                        val exponentialExpansionLimit = minOf(desiredExpansionLimit, terminationChecker.remaining())
                        val safetyTerminationChecker = StaticExpansionTerminationChecker(exponentialExpansionLimit)

                        val nextTopNode = openList.peek() ?: throw GoalNotReachableException("Goal is not reachable")
                        val safetyProofDuration = proveSafety(nextTopNode, safetyTerminationChecker)

                        terminationChecker.notifyExpansion(safetyProofDuration)
                        totalSafetyDuration += safetyProofDuration

                        appendAttribute("safeExpansions", safetyProofDuration.toInt())

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
     * Runs AStar until termination and returns the path to the head of openList
     * Will just repeatedly expand according to A*.
     */
    private fun macroIteration(sourceState: StateType, terminationChecker: TerminationChecker): Pair<SafeRealTimeSearchNode<StateType>, SafeRealTimeSearchNode<StateType>?> {
        initializeAStar(sourceState)

        lastSafeNode = null

        val aStarTerminationChecker = StaticExpansionTerminationChecker((terminationChecker.remaining() * safetyExplorationRatio).toLong())

        aStarSequence
                .generateWhile {
                    !aStarTerminationChecker.reachedTermination() && !domain.isGoal(openList.peek()?.state
                            ?: throw GoalNotReachableException("Open list is empty."))
                }
                .forEach { _ ->
                    terminationChecker.notifyExpansion()
                    aStarTerminationChecker.notifyExpansion()
                }

        while(openList.isNotEmpty()) {
            val topNode = openList.peek()!!
            proveSafety(topNode, terminationChecker)

            if (topNode.safe) {
                safeNodes.add(topNode)

                break
            } else {
                openList.pop()
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
                            filterUnsafe
                    ) { state ->
                        val node = nodes[state]
                        when {
                            node == null -> UNKNOWN
                            node.safe -> SAFE
                            node.unsafe -> UNSAFE
                            else -> UNKNOWN
                        }
                    }

                    incrementCounter("unsafeSafetyReexpansion", safetyProof.reexpandedUnsafeStates)

                    when (safetyProof.status) {
                        SAFE -> {
                            // Safety proof was successful: mark everything safe
                            sourceNode.safe = true
                            sourceNode.safetyProof = safetyProof.safetyProof
                            safetyProof.safetyProof.forEach { getUninitializedNode(it).safe = true }
                        }
                        UNSAFE -> {
                            // Safety proof exhausted all possibilities and failed: mark everything unsafe
                            sourceNode.unsafe = true
                            safetyProof.discoveredStates.forEach { getUninitializedNode(it).unsafe = true }
                        }
                        UNKNOWN -> Unit
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
            openList.reorder(explorationComparator)

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
                    actionCost = successor.actionCost.toLong(),
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

