package edu.unh.cs.ai.realtimesearch.planner.realtime

import edu.unh.cs.ai.realtimesearch.MetronomeException
import edu.unh.cs.ai.realtimesearch.environment.*
import edu.unh.cs.ai.realtimesearch.experiment.configuration.GeneralExperimentConfiguration
import edu.unh.cs.ai.realtimesearch.experiment.measureLong
import edu.unh.cs.ai.realtimesearch.experiment.terminationCheckers.StaticExpansionTerminationChecker
import edu.unh.cs.ai.realtimesearch.experiment.terminationCheckers.TerminationChecker
import edu.unh.cs.ai.realtimesearch.logging.debug
import edu.unh.cs.ai.realtimesearch.logging.trace
import edu.unh.cs.ai.realtimesearch.logging.warn
import edu.unh.cs.ai.realtimesearch.planner.*
import edu.unh.cs.ai.realtimesearch.planner.exception.GoalNotReachableException
import edu.unh.cs.ai.realtimesearch.planner.realtime.SafeRealTimeSearchConfiguration.SAFETY_EXPLORATION_RATIO
import edu.unh.cs.ai.realtimesearch.planner.realtime.SafeRealTimeSearchConfiguration.TARGET_SELECTION
import edu.unh.cs.ai.realtimesearch.planner.realtime.SafeRealTimeSearchTargetSelection.valueOf
import edu.unh.cs.ai.realtimesearch.util.AdvancedPriorityQueue
import edu.unh.cs.ai.realtimesearch.util.Indexable
import edu.unh.cs.ai.realtimesearch.util.resize
import org.slf4j.LoggerFactory
import java.util.*
import kotlin.system.measureTimeMillis

/**
 * @author Bence Cserna (bence@cserna.net)
 */
class SafeRealTimeSearch<StateType : State<StateType>>(domain: Domain<StateType>, val configuration: GeneralExperimentConfiguration) : RealTimePlanner<StateType>(domain) {
    // Configuration
    private val targetSelection: SafeRealTimeSearchTargetSelection = valueOf(configuration[TARGET_SELECTION] as? String ?: throw MetronomeException("Target selection strategy not found"))
    private val safetyExplorationRatio: Double = (configuration[SAFETY_EXPLORATION_RATIO] as? Double ?: throw MetronomeException("Safety-exploration ratio not found"))

    class Node<StateType : State<StateType>>(
            override val state: StateType,
            override var heuristic: Double,
            override var cost: Long,
            override var actionCost: Long,
            override var action: Action,
            var iteration: Long,
            parent: Node<StateType>? = null) : SearchNode<StateType, Node<StateType>>, Indexable, Safe {

        /** Item index in the open list. */
        override var index: Int = -1
        override var safe = false

        /** Nodes that generated this Node as a successor in the current exploration phase. */
        override var predecessors: MutableList<SearchEdge<Node<StateType>>> = arrayListOf()

        /** Parent pointer that points to the min cost predecessor. */
        override var parent: Node<StateType> = parent ?: this

        val f: Double
            get() = cost + heuristic

        override fun hashCode(): Int = state.hashCode()

        override fun equals(other: Any?): Boolean = when (other) {
            null -> false
            is Node<*> -> state == other.state
            is State<*> -> state == other
            else -> false
        }

        override fun toString(): String =
                "Node: [State: $state h: $heuristic, g: $cost, iteration: $iteration, actionCost: $actionCost, parent: ${parent.state}, open: $open safe: $safe]"
    }

    private val logger = LoggerFactory.getLogger(SafeRealTimeSearch::class.java)
    private var iterationCounter = 0L

    private val fValueComparator = java.util.Comparator<Node<StateType>> { lhs, rhs ->
        when {
            lhs.f < rhs.f -> -1
            lhs.f > rhs.f -> 1
            lhs.cost > rhs.cost -> -1 // Tie braking on cost (g)
            lhs.cost < rhs.cost -> 1
            else -> 0
        }
    }

    private val heuristicComparator = java.util.Comparator<Node<StateType>> { lhs, rhs ->
        when {
            lhs.heuristic < rhs.heuristic -> -1
            lhs.heuristic > rhs.heuristic -> 1
            else -> 0
        }
    }

    private val nodes: HashMap<StateType, Node<StateType>> = HashMap<StateType, Node<StateType>>(100000000).resize()

    // LSS stores heuristic values. Use those, but initialize them according to the domain heuristic
    // The cost values are initialized to infinity
    private var openList = AdvancedPriorityQueue(10000000, fValueComparator)

    private var safeNodes = mutableListOf<Node<StateType>>()

    private var rootState: StateType? = null

    private var continueSearch = false

    // Performance measurement
    private var aStarPopCounter = 0
    private var dijkstraPopCounter = 0
    var aStarTimer = 0L
        get
    var dijkstraTimer = 0L
        get


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
            dijkstraTimer += measureTimeMillis { dijkstra(terminationChecker) }
        }

        // Exploration phase
        var plan: List<RealTimePlanner.ActionBundle>? = null
        aStarTimer += measureTimeMillis {
            val (targetNode, lastSafeNode) = aStar(sourceState, terminationChecker)

            // Backup safety
            predecessorSafetyPropagation(safeNodes)
            safeNodes.clear()

            val currentSafeTarget = when (targetSelection) {
            // What the safe predecessors are on a dead-path (meaning not reachable by the parent pointers)
                SafeRealTimeSearchTargetSelection.SAFE_TO_BEST -> selectSafeToBest(openList)
                SafeRealTimeSearchTargetSelection.BEST_SAFE -> lastSafeNode
            }

            val targetSafeNode = currentSafeTarget
                    ?: attemptIdentityAction(sourceState)
                    ?: bestSafeChild(sourceState, domain, { state -> nodes[state]?.safe ?: false })?.let { nodes[it] }
                    ?: targetNode

            plan = extractPath(targetSafeNode, sourceState)
            rootState = targetSafeNode.state
        }

        logger.debug { "AStar pops: $aStarPopCounter Dijkstra pops: $dijkstraPopCounter" }
        logger.debug { "AStar time: $aStarTimer Dijkstra pops: $dijkstraTimer" }

        return plan!!
    }

    private fun attemptIdentityAction(sourceState: StateType): Node<StateType>? {
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
    private fun aStar(sourceState: StateType, terminationChecker: TerminationChecker): Pair<Node<StateType>, Node<StateType>?> {
        logger.debug { "Starting A* from sourceState: $sourceState" }
        initializeAStar(sourceState)

        var lastSafeNode: Node<StateType>? = null

        var currentNode = openList.peek()!!

        var totalExpansionDuration = 0L
        var currentExpansionDuration = 0L
        var totalSafetyDuration = 0L
        var costBucket = 10

        while (!terminationChecker.reachedTermination()) {
            aStarPopCounter++

            val topNode = openList.peek() ?: throw GoalNotReachableException("Open list is empty.")
            if (domain.isGoal(topNode.state)) return topNode to topNode

            currentNode = openList.pop()!!

            if (!currentNode.safe && domain.isSafe(currentNode.state)) {
                currentNode.safe = true
            }

            if (currentNode.safe && currentNode != sourceState) {
                safeNodes.add(currentNode)
                lastSafeNode = currentNode
            }

            //Explore
            currentExpansionDuration += measureLong(terminationChecker::elapsed) {
                expandFromNode(currentNode)
                terminationChecker.notifyExpansion()
            }

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

        logger.debug { "Done with AStar at $currentNode" }
        logger.debug { "Last safe node: $lastSafeNode" }

        return (openList.peek() ?: throw GoalNotReachableException("Open list is empty.")) to lastSafeNode
    }

    private fun proveSafety(sourceNode: Node<StateType>, terminationChecker: TerminationChecker): Long {
        return measureLong(terminationChecker::elapsed) {
            when {
                sourceNode.safe -> {}
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
            }

            nodes[state] = node
            openList.add(node)
        }
    }

    /**
     * Expands a node and add it to closed list. For each successor
     * it will add it to the open list and store it's g value, as long as the
     * state has not been seen before, or is found with a lower g value
     */
    private fun expandFromNode(sourceNode: Node<StateType>) {
        expandedNodeCount += 1

        for (successor in domain.successors(sourceNode.state)) {
            val successorState = successor.state
            logger.trace { "Considering successor $successorState" }

            val successorNode = getNode(sourceNode, successor)

            if (successorNode.heuristic == Double.POSITIVE_INFINITY
                    && successorNode.iteration != iterationCounter) {
                // Ignore this successor as it is a dead end
                continue
            }

            // check for safety if safe add to the safe nodes
            if (successorNode.safe || domain.isSafe(successorNode.state)) {
                safeNodes.add(successorNode)
                successorNode.safe = true
            }

            // If the node is outdated it should be updated.
            if (successorNode.iteration != iterationCounter) {
                successorNode.apply {
                    iteration = iterationCounter
                    predecessors.clear()
                    cost = Long.MAX_VALUE
                    // parent, action, and actionCost is outdated too, but not relevant.
                }
            }

            // Add the current state as the predecessor of the child state
            successorNode.predecessors.add(SearchEdge(node = sourceNode, action = successor.action, actionCost = successor.actionCost.toLong()))

            // Skip if we got back to the parent
            if (successorState == sourceNode.parent.state) {
                continue
            }

            // only generate those state that are not visited yet or whose cost value are lower than this path
            val successorGValueFromCurrent = sourceNode.cost + successor.actionCost
            if (successorNode.cost > successorGValueFromCurrent) {
                // here we generate a state. We store it's g value and remember how to get here via the treePointers
                successorNode.apply {
                    cost = successorGValueFromCurrent.toLong()
                    parent = sourceNode
                    action = successor.action
                    actionCost = successor.actionCost.toLong()
                }

                logger.debug { "Expanding from $sourceNode --> $successorState :: open list size: ${openList.size}" }
                logger.trace { "Adding it to to cost table with value ${successorNode.cost}" }

                if (!successorNode.open) {
                    openList.add(successorNode) // Fresh node not on the open yet
                } else {
                    openList.update(successorNode)
                }
            } else {
                logger.trace {
                    "Did not add, because it's cost is ${successorNode.cost} compared to cost of predecessor ( ${sourceNode.cost}), and action cost ${successor.actionCost}"
                }
            }
        }

        sourceNode.heuristic = Double.POSITIVE_INFINITY
    }

    private fun getUninitializedNode(state: StateType): Node<StateType> {
        val tempNode = nodes[state]

        return if (tempNode != null) {
            tempNode
        } else {
            generatedNodeCount++
            val node = Node(
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
    private fun getNode(parent: Node<StateType>, successor: SuccessorBundle<StateType>): Node<StateType> {
        val successorState = successor.state
        val tempSuccessorNode = nodes[successorState]

        return if (tempSuccessorNode == null) {
            generatedNodeCount++

            val undiscoveredNode = Node(
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

    /**
     * Performs Dijkstra updates until runs out of resources or done
     *
     * Updates the mode to SEARCH if done with DIJKSTRA
     *
     * Dijkstra updates repeatedly pop the state s according to their heuristic value, and then update
     * the cost values for all it's visited successors, based on the heuristic s.
     *
     * This increases the stored heuristic values, ensuring that A* won't go in circles, and in general generating
     * a better table of heuristics.
     *
     */
    private fun dijkstra(terminationChecker: TerminationChecker) {
        logger.debug { "Start: Dijkstra" }
        // Invalidate the current heuristic value by incrementing the counter
        iterationCounter++

        // change openList ordering to heuristic only`
        openList.reorder(heuristicComparator)

        // LSS-LRTA addition
        //        openList.toTypedArray().forEach {
        //            it.iteration = iterationCounter
        //        }

        while (!terminationChecker.reachedTermination() && openList.isNotEmpty()) {
            // Closed list should be checked
            val node = openList.pop() ?: throw GoalNotReachableException("Goal not reachable. Open list is empty.")

            node.iteration = iterationCounter

            val currentHeuristicValue = node.heuristic

            // update heuristic value for each predecessor
            for (predecessor in node.predecessors) {
                val predecessorNode = predecessor.node

                // Propagate safety
//                if (node.safe) {
//                    predecessorNode.safe = true
//                    if (predecessorNode.proofParent == null) {
//                        predecessorNode.proofParent = node
//                    }
//                }

                if (predecessorNode.iteration == iterationCounter && !predecessorNode.open) {
                    // This node was already learned and closed in the current iteration
                    continue
                }

                // Update if the node is outdated
                //                if (predecessorNode.iteration != iterationCounter) {
                //                    predecessorNode.heuristic = Double.POSITIVE_INFINITY
                //                    predecessorNode.iteration = iterationCounter
                //                }

                val predecessorHeuristicValue = predecessorNode.heuristic

                //                logger.debug { "Considering predecessor ${predecessor.node} with heuristic value $predecessorHeuristicValue" }
                //                logger.debug { "Node in closedList: ${predecessor.node in closedList}. Current heuristic: $predecessorHeuristicValue. Proposed new value: ${(currentHeuristicValue + predecessor.actionCost)}" }

                if (!predecessorNode.open) {
                    // This node is not open yet, because it was not visited in the current planning iteration

                    predecessorNode.heuristic = currentHeuristicValue + predecessor.actionCost
                    assert(predecessorNode.iteration == iterationCounter - 1)
                    predecessorNode.iteration = iterationCounter

                    openList.add(predecessorNode)
                } else if (predecessorHeuristicValue > currentHeuristicValue + predecessor.actionCost) {
                    // This node was visited in this learning phase, but the current path is better then the previous
                    predecessorNode.heuristic = currentHeuristicValue + predecessor.actionCost
                    openList.update(predecessorNode) // Update priority

                    // Frontier nodes could be also visited TODO
                    //                    assert(predecessorNode.iteration == iterationCounter) {
                    //                        "Expected iteration stamp $iterationCounter got ${predecessorNode.iteration}"
                    //                    }
                }
            }
        }

        // update mode if done
        if (openList.isEmpty()) {
            logger.debug { "Done with Dijkstra" }
        } else {
            logger.warn { "Incomplete learning step. Lists: Open(${openList.size})" }
        }
    }

    /**
     * Given a state, this function returns the path according to the tree pointers
     */
    private fun extractPlan(targetNode: Node<StateType>?, sourceState: StateType): List<RealTimePlanner.ActionBundle> {
        targetNode ?: return emptyList()

        val actions = java.util.ArrayList<RealTimePlanner.ActionBundle>(1000)
        var currentNode: Node<StateType> = targetNode

        logger.debug { "Extracting plan" }

        if (targetNode.state == sourceState) {
            return emptyList()
        }

        // keep on pushing actions to our queue until source state (our root) is reached
        do {
            actions.add(RealTimePlanner.ActionBundle(currentNode.action, currentNode.actionCost))
            // Propagate safety
//            if (currentNode.safe) {
//                currentNode.parent.safe = true
//
//                if (currentNode.parent.proofParent == null) {
//                    currentNode.parent.proofParent = currentNode
//                }
//
//            }
            currentNode = currentNode.parent
        } while (currentNode.state != sourceState)

        logger.debug { "Plan extracted" }

        return actions.reversed()
    }

}

enum class SafeRealTimeSearchConfiguration {
    TARGET_SELECTION,
    SAFETY_EXPLORATION_RATIO
}

/**
 * Selector to define how to pick the target node at the end of the planning iteration.
 * The planner returns a sequence of actions from the agent's current location to the selected target node.
 */
enum class SafeRealTimeSearchTargetSelection {
    /** Select the best safe predecessor of a the best node on open that has such predecessor. */
    SAFE_TO_BEST,
    /** Select the best safe node in LSS. */
    BEST_SAFE
}