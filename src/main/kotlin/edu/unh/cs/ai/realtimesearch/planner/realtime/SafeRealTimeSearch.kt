package edu.unh.cs.ai.realtimesearch.planner.realtime

import edu.unh.cs.ai.realtimesearch.MetronomeException
import edu.unh.cs.ai.realtimesearch.environment.*
import edu.unh.cs.ai.realtimesearch.experiment.configuration.GeneralExperimentConfiguration
import edu.unh.cs.ai.realtimesearch.experiment.measureLong
import edu.unh.cs.ai.realtimesearch.experiment.terminationCheckers.TerminationChecker
import edu.unh.cs.ai.realtimesearch.logging.debug
import edu.unh.cs.ai.realtimesearch.logging.trace
import edu.unh.cs.ai.realtimesearch.logging.warn
import edu.unh.cs.ai.realtimesearch.planner.RealTimePlanner
import edu.unh.cs.ai.realtimesearch.planner.exception.GoalNotReachableException
import edu.unh.cs.ai.realtimesearch.planner.realtime.SafeRealTimeSearchConfiguration.SAFETY_EXPLORATION_RATIO
import edu.unh.cs.ai.realtimesearch.planner.realtime.SafeRealTimeSearchConfiguration.TARGET_SELECTION
import edu.unh.cs.ai.realtimesearch.planner.realtime.SafeRealTimeSearchTargetSelection.BEST_SAFE
import edu.unh.cs.ai.realtimesearch.planner.realtime.SafeRealTimeSearchTargetSelection.SAFE_TO_BEST
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
    val targetSelection: SafeRealTimeSearchTargetSelection = SafeRealTimeSearchTargetSelection.valueOf(configuration[TARGET_SELECTION.toString()] as? String ?: throw MetronomeException("Target selection strategy not found"))
    val safetyExplorationRatio: Double = (configuration[SAFETY_EXPLORATION_RATIO.toString()] as? Double ?: throw MetronomeException("Safety-exploration ratio not found"))

    data class Edge<StateType : State<StateType>>(val node: Node<StateType>, val action: Action, val actionCost: Long)

    class Node<StateType : State<StateType>>(
            override val state: StateType,
            override var heuristic: Double,
            override var cost: Long,
            override var actionCost: Long,
            override var action: Action,
            var iteration: Long,
            override var open: Boolean = false,
            parent: Node<StateType>? = null) : SearchNode<StateType>, Indexable, Safe {

        /** Item index in the open list. */
        override var index: Int = -1
        override var safe = false

        /** Nodes that generated this Node as a successor in the current exploration phase. */
        var predecessors: MutableList<Edge<StateType>> = arrayListOf()

        /** Parent pointer that points to the min cost predecessor. */
        var parent: Node<StateType>

        override fun getParent(): SearchNode<StateType> = parent

        val f: Double
            get() = cost + heuristic

        init {
            this.parent = parent ?: this
        }

        override fun hashCode(): Int {
            return state.hashCode()
        }

        override fun equals(other: Any?): Boolean {
            if (other != null && other is Node<*>) {
                return state == other.state
            }
            return false
        }

        override fun toString(): String {
            return "Node: [State: $state h: $heuristic, g: $cost, iteration: $iteration, actionCost: $actionCost, parent: ${parent.state}, open: $open ]"
        }
    }

    private val logger = LoggerFactory.getLogger(LssLrtaStarPlanner::class.java)
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
    private var openList = AdvancedPriorityQueue<Node<StateType>>(10000000, fValueComparator)

    private var rootState: StateType? = null

    private var continueSearch = false
    private var safeActions: List<ActionBundle>? = null

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
            val targetNode = aStar(sourceState, terminationChecker)

            plan = extractPlan(targetNode, sourceState)

            rootState = targetNode?.state
        }

        logger.debug { "AStar pops: $aStarPopCounter Dijkstra pops: $dijkstraPopCounter" }
        logger.debug { "AStar time: $aStarTimer Dijkstra pops: $dijkstraTimer" }

        // Safety action, to make sure that no empty plan is returned
        if (plan!!.isEmpty()) {
            if (domain.isSafe(sourceState)) {
                // The current state is safe, attempt an identity action
                val actionBundle = domain.getIdentityAction(sourceState)

                if (actionBundle != null) {
                    continueSearch = true // The planner can continue the search in the next iteration since the state is not changed
                    safeActions = null // No further safe actions are available
                    return listOf(ActionBundle(actionBundle.action, actionBundle.actionCost))
                }
            }

            logger.info("Safe plan was used")
            // Return the next safe action from a previously generated plan if available
            safeActions = safeActions?.drop(1)
            val nextSafeAction = safeActions?.firstOrNull() ?:
                    bestSafeChild(sourceState, domain, { state -> nodes[state]?.safe ?: false }) ?:
                    throw MetronomeException("No safe successors are available.")
            return listOf(ActionBundle(nextSafeAction.action, nextSafeAction.duration))
        } else {
            // A safe plan is available
            safeActions = plan
        }

        return plan!!
    }

    /**
     * Runs AStar until termination and returns the path to the head of openList
     * Will just repeatedly expand according to A*.
     */
    private fun aStar(state: StateType, terminationChecker: TerminationChecker): Node<StateType>? {
        // actual core steps of A*, building the tree
        if (continueSearch) {
            continueSearch = false
        } else {
            initializeAStar()
            // Create and add initial node to open
            val node = Node(state, domain.heuristic(state), 0, 0, NoOperationAction, iterationCounter, false)
            nodes[state] = node
            addToOpenList(node)
        }
        var lastSafeNode: Node<StateType>? = null

        var currentNode = openList.peek()!!
        logger.debug { "Starting A* from state: $state" }

        var totalExpansionDuration = 0L
        var totalSafetyDuration = 0L

        while (!terminationChecker.reachedTermination() && !domain.isGoal(currentNode.state)) {
            aStarPopCounter++
            currentNode = popOpenList()

            totalExpansionDuration += measureLong(terminationChecker::elapsed) {
                expandFromNode(currentNode)
                terminationChecker.notifyExpansion()
            }

            // Update best safe node
            if (totalExpansionDuration * safetyExplorationRatio > totalSafetyDuration) {
                totalSafetyDuration += measureLong(terminationChecker::elapsed) {
                    val topNode = openList.peek() ?: throw MetronomeException("Open list is empty! Goal is not reachable")

                    lastSafeNode = if (topNode.safe) {
                        topNode
                    } else {
                        isComfortable(topNode.state, terminationChecker, domain, { state -> nodes[state]?.safe ?: false })?.run {
                            // Save safe states from proof
                            forEach { getUninitializedNode(it).safe = true }
                            topNode.safe = true
                            topNode
                        } ?: lastSafeNode
                    }
                }
            }
        }

//        if (node == currentNode && !domain.isGoal(currentNode.state)) {
        //            throw InsufficientTerminationCriterionException("Not enough time to expand even one node")
//        } else {
//            logger.debug { "A* : expanded $expandedNodes nodes" }
//        }

        logger.debug { "Done with AStar at $currentNode" }
        logger.debug { "Last safe node: $lastSafeNode" }

        return when (targetSelection) {
            SAFE_TO_BEST -> selectSafeToBest(openList)
            BEST_SAFE -> lastSafeNode
        }
    }


    private fun initializeAStar() {
        iterationCounter++
        clearOpenList()

        openList.reorder(fValueComparator)
    }

    /**
     * Expands a node and add it to closed list. For each successor
     * it will add it to the open list and store it's g value, as long as the
     * state has not been seen before, or is found with a lower g value
     */
    private fun expandFromNode(sourceNode: Node<StateType>) {
        expandedNodeCount += 1


        val currentGValue = sourceNode.cost
        for (successor in domain.successors(sourceNode.state)) {
            val successorState = successor.state
            logger.trace { "Considering successor $successorState" }

            val successorNode = getNode(sourceNode, successor)

            // Add the current state as the predecessor of the child state
            successorNode.predecessors.add(Edge(node = sourceNode, action = successor.action, actionCost = successor.actionCost))

            // If the node is outdated it should be updated.
            if (successorNode.iteration != iterationCounter) {
                successorNode.apply {
                    iteration = iterationCounter
                    predecessors.clear()
                    cost = Long.MAX_VALUE
                    open = false // It is not on the open list yet, but it will be
                    // parent, action, and actionCost is outdated too, but not relevant.
                }
            }

            // Skip if we got back to the parent
            if (successorState == sourceNode.parent.state) {
                continue
            }

            // only generate those state that are not visited yet or whose cost value are lower than this path
            val successorGValueFromCurrent = currentGValue + successor.actionCost
            if (successorNode.cost > successorGValueFromCurrent) {
                // here we generate a state. We store it's g value and remember how to get here via the treePointers
                successorNode.apply {
                    cost = successorGValueFromCurrent
                    parent = sourceNode
                    action = successor.action
                    actionCost = successor.actionCost
                }

                logger.debug { "Expanding from $sourceNode --> $successorState :: open list size: ${openList.size}" }
                logger.trace { "Adding it to to cost table with value ${successorNode.cost}" }

                if (!successorNode.open) {
                    addToOpenList(successorNode) // Fresh node not on the open yet
                } else {
                    openList.update(successorNode)
                }
            } else {
                logger.trace {
                    "Did not add, because it's cost is ${successorNode.cost} compared to cost of predecessor ( ${sourceNode.cost}), and action cost ${successor.actionCost}"
                }
            }
        }
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
                    iteration = 0,
                    open = false)

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
                    actionCost = successor.actionCost,
                    action = successor.action,
                    parent = parent,
                    cost = Long.MAX_VALUE,
                    iteration = iterationCounter,
                    open = false)

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
            val node = popOpenList()
            node.iteration = iterationCounter

            val currentHeuristicValue = node.heuristic

            // update heuristic value for each predecessor
            for (predecessor in node.predecessors) {
                val predecessorNode = predecessor.node

                // Propagate safety
                if (node.safe) predecessorNode.safe = true

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

                    addToOpenList(predecessorNode)
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
            if (currentNode.safe) currentNode.parent.safe = true
            currentNode = currentNode.parent
        } while (currentNode.state != sourceState)

        logger.debug { "Plan extracted" }

        return actions.reversed()
    }

    private fun clearOpenList() {
        logger.debug { "Clear open list" }
        openList.applyAndClear {
            it.open = false
        }
    }

    private fun popOpenList(): Node<StateType> {
        val node = openList.pop() ?: throw GoalNotReachableException("Goal not reachable. Open list is empty.")
        node.open = false
        return node
    }

    private fun addToOpenList(node: Node<StateType>) {
        openList.add(node)
        node.open = true
    }

}

/**
 * Prove the safety of a given state. A state is safe (more precisely comfortable) if the state itself is safe or a
 * safe state is reachable from it. The explicit safety of a state is defined by the domain.
 *
 * To prove the implicit safety of a state a best first search(BFS) algorithm is used prioritized on the safe distance of
 * the states. The safe distance of states is defined by the domain.
 *
 * @param state State to validate.
 * @param terminationChecker The termination checker is used to ensure the termination of the BFS algorithm. The
 *                           expansion of the during the search are also logged against the termination checker.
 * @param domain The domain is used to determine the safety distance and the explicit safety of states.
 * @param isSafe An optional secondary safety check can be provided to prove implicit safety.
 *
 * @return null if the given state is not safe, else a list of states that are proven to be safe.
 * Empty list if the state itself is safe.
 */
fun <StateType : State<StateType>> isComfortable(state: StateType, terminationChecker: TerminationChecker, domain: Domain<StateType>, isSafe: ((StateType) -> Boolean)? = null): List<StateType>? {
    data class Node(val state: StateType, val safeDistance: Pair<Int, Int>, val parent: Node? = null)

    // Return empty list if the original state is safe
    if (domain.isSafe(state) || (isSafe != null && isSafe(state))) return emptyList()

    val nodeComparator = java.util.Comparator<Node> { (_, lhsDistance), (_, rhsDistance) ->
        when {
            lhsDistance.first < rhsDistance.first -> -1
            lhsDistance.first > rhsDistance.first -> 1
            lhsDistance.second < rhsDistance.second -> -1
            lhsDistance.second > rhsDistance.second -> 1
            else -> 0
        }
    }

    val priorityQueue = PriorityQueue<Node>(nodeComparator)
    val discoveredStates = hashSetOf<StateType>()
    val comfortableStates = mutableListOf<StateType>()

    priorityQueue.add(Node(state, domain.safeDistance(state)))

    while (priorityQueue.isNotEmpty() && !terminationChecker.reachedTermination()) {
        val currentNode = priorityQueue.poll() ?: return null

        if (domain.isSafe(currentNode.state) || (isSafe != null && isSafe(currentNode.state))) {
            // Backtrack to the root and return all safe states
            // The parent of the safe state is comfortable
            var backTrackNode: Node? = currentNode
            while (backTrackNode != null) {
                comfortableStates.add(backTrackNode.state)
                backTrackNode = backTrackNode.parent
            }

            return comfortableStates
        }

        terminationChecker.notifyExpansion()
        domain.successors(currentNode.state)
                .filter { it.state !in discoveredStates } // Do not add add an item twice to the list
                .onEach { discoveredStates += it.state }
                .mapTo(priorityQueue, { Node(it.state, domain.safeDistance(it.state), currentNode) }) // Add successors to the queue
    }

    return null
}

/**
 * Find the best safe successor of a state if any.
 *
 * @return the best safe successor if available, else null.
 */
fun <StateType : State<StateType>> bestSafeChild(state: StateType, domain: Domain<StateType>, isSafe: ((StateType) -> Boolean)): RealTimePlanner.ActionBundle? {
    return domain.successors(state)
            .filter { domain.isSafe(it.state) || isSafe(it.state) }
            .minBy { it.actionCost + domain.heuristic(it.state) }
            ?.run { RealTimePlanner.ActionBundle(this.action, this.actionCost) }
}

interface SearchNode<StateType : State<StateType>> {
    val state: StateType
    var heuristic: Double
    var cost: Long
    var actionCost: Long
    var action: Action
    var open: Boolean

    fun getParent(): SearchNode<StateType>
}

interface Safe {
    var safe: Boolean
}


private fun <StateType : State<StateType>, Node> selectSafeToBest(queue: AdvancedPriorityQueue<Node>): Node?
        where Node : SearchNode<StateType>, Node : Indexable, Node : Safe {
    val nodes = MutableList(queue.size, { queue.backingArray[it]!! })
    nodes.sortBy { it.cost + it.heuristic }

    nodes.forEach {
        var currentNode = it
        while (currentNode.getParent() !== currentNode) {
            if (currentNode.safe) {
                return currentNode
            }
            @Suppress("UNCHECKED_CAST")
            currentNode = currentNode.getParent() as Node
        }
    }

    return null
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
    /** Select the best safe node on open. */
    BEST_SAFE
}