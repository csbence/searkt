package edu.unh.cs.ai.realtimesearch.planner.realtime

import edu.unh.cs.ai.realtimesearch.environment.*
import edu.unh.cs.ai.realtimesearch.experiment.measureInt
import edu.unh.cs.ai.realtimesearch.experiment.terminationCheckers.TerminationChecker
import edu.unh.cs.ai.realtimesearch.logging.debug
import edu.unh.cs.ai.realtimesearch.logging.trace
import edu.unh.cs.ai.realtimesearch.logging.warn
import edu.unh.cs.ai.realtimesearch.planner.RealTimePlanner
import edu.unh.cs.ai.realtimesearch.planner.exception.GoalNotReachableException
import edu.unh.cs.ai.realtimesearch.util.AdvancedPriorityQueue
import edu.unh.cs.ai.realtimesearch.util.Indexable
import edu.unh.cs.ai.realtimesearch.util.resize
import org.slf4j.LoggerFactory
import java.util.*
import kotlin.Long.Companion.MAX_VALUE
import kotlin.system.measureTimeMillis

/**
 * SZero, using Local Search Space Learning Real Time A* as a base, a type of RTS planner.
 *
 * Runs A* until out of resources, then selects action up till the most promising state.
 * While executing that plan, it will:
 * - update all the heuristic values along the path (dijkstra)
 * - Run A* from the expected destination state
 * - Choose actions which have a safe predecessor
 *
 * This loop continue until the goal has been found
 */
@Deprecated("Use SZero with parameters")
class SOnePlanner<StateType : State<StateType>>(domain: Domain<StateType>) : RealTimePlanner<StateType>(domain) {
    data class Edge<StateType : State<StateType>>(val node: Node<StateType>, val action: Action, val actionCost: Long)

    class Node<StateType : State<StateType>>(val state: StateType, var heuristic: Double, var cost: Long,
                                             var actionCost: Long, var action: Action,
                                             var iteration: Long,
                                             parent: Node<StateType>? = null,
                                             var safe: Boolean = false) : Indexable {

        /** Item index in the open list. */
        override var index: Int = -1

        /** Nodes that generated this Node as a successor in the current exploration phase. */
        var predecessors: MutableList<Edge<StateType>> = arrayListOf()

        /** Parent pointer that points to the min cost predecessor. */
        var parent: Node<StateType>

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

    val safeNodes = ArrayList<Node<StateType>>()

    private val fValueComparator = Comparator<Node<StateType>> { lhs, rhs ->
        when {
            lhs.f < rhs.f -> -1
            lhs.f > rhs.f -> 1
            lhs.cost > rhs.cost -> -1 // Tie braking on cost (g)
            lhs.cost < rhs.cost -> 1
            else -> 0
        }
    }

    private val heuristicComparator = Comparator<Node<StateType>> { lhs, rhs ->
        when {
            lhs.heuristic < rhs.heuristic -> -1
            lhs.heuristic > rhs.heuristic -> 1
            else -> 0
        }
    }

    private val nodes: HashMap<StateType, Node<StateType>> = HashMap<StateType, Node<StateType>>(100000000, 1.toFloat()).resize()

    // LSS stores heuristic values. Use those, but initialize them according to the domain heuristic
    // The cost values are initialized to infinity
    private var openList = AdvancedPriorityQueue<Node<StateType>>(10000000, fValueComparator)

    private var rootState: StateType? = null

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
    override fun selectAction(sourceState: StateType, terminationChecker: TerminationChecker): List<ActionBundle> {
        // Initiate for the first search

        if (rootState == null) {
            rootState = sourceState
        } else if (sourceState != rootState) {
            // The given sourceState should be the last target
            logger.debug { "Inconsistent world sourceState. Expected $rootState got $sourceState" }
        }

        if (domain.isGoal(sourceState)) {
            // The start sourceState is the goal sourceState
            logger.warn() { "selectAction: The goal sourceState is already found." }
            return emptyList()
        }

        logger.debug { "Root sourceState: $sourceState" }
        // Every turn learn then A* until time expires

        // Learning phase
        if (openList.isNotEmpty()) {
            dijkstraTimer += measureTimeMillis { dijkstra(terminationChecker) }
        }

        // Exploration phase
        var plan: List<ActionBundle>? = null
        aStarTimer += measureTimeMillis {
            val targetNode = aStar(sourceState, terminationChecker)

            updateSafeNodes()

            plan = extractPlan(targetNode, sourceState)
            rootState = targetNode.state
        }

        logger.debug { "AStar pops: $aStarPopCounter Dijkstra pops: $dijkstraPopCounter" }
        logger.debug { "AStar time: $aStarTimer Dijkstra pops: $dijkstraTimer" }

        return plan!!
    }

    /**
     * Runs AStar until termination and returns the path to the head of openList
     * Will just repeatedly expand according to A*.
     */
    private fun aStar(state: StateType, terminationChecker: TerminationChecker): Node<StateType> {
        // actual core steps of A*, building the tree
        initializeAStar()

        val node = Node(state, domain.heuristic(state), 0, 0, NoOperationAction, iterationCounter)
        nodes[state] = node
        var currentNode = node
        addToOpenList(node)
        logger.debug { "Starting A* from state: $state" }

        val expandedNodes = measureInt({ expandedNodeCount }) {
            while (!terminationChecker.reachedTermination() && !domain.isGoal(currentNode.state)) {
                aStarPopCounter++
                currentNode = popOpenList()
                expandFromNode(currentNode)
                terminationChecker.notifyExpansion()
            }
        }

        if (node == currentNode && !domain.isGoal(currentNode.state)) {
            //            throw InsufficientTerminationCriterionException("Not enough time to expand even one node")
        } else {
            logger.debug { "A* : expanded $expandedNodes nodes" }
        }

        logger.debug { "Done with AStar at $currentNode" }

        return currentNode
    }

    private fun initializeAStar() {
        iterationCounter++
        clearOpenList()
        safeNodes.forEach { it.safe = false }; safeNodes.clear() // reset and clear safe nodes
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

            // check for safety if safe add to the safe nodes
            if (domain.isSafe(successorNode.state)) {
                safeNodes.add(successorNode)
            }

            // Add the current state as the predecessor of the child state
            successorNode.predecessors.add(Edge(node = sourceNode, action = successor.action, actionCost = successor.actionCost))

            // If the node is outdated it should be updated.
            if (successorNode.iteration != iterationCounter) {
                successorNode.apply {
                    iteration = iterationCounter
                    predecessors.clear()
                    cost = MAX_VALUE
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
                    cost = MAX_VALUE,
                    iteration = iterationCounter
            )

            nodes[successorState] = undiscoveredNode
            undiscoveredNode
        } else {
            tempSuccessorNode
        }
    }

    /**
     * Backs up safety through the parent pointers
     *
     *
     */
    private fun updateSafeNodes() {
        while (safeNodes.isNotEmpty()) {
            val safeNode = safeNodes.first()
            var currentParent = safeNode.parent
            // always update the safe nodes
            // through the parent pointers
            while (currentParent !== currentParent.parent) {
                currentParent.safe = safeNode.safe
                currentParent = currentParent.parent
            }
            // update the safe nodes of predecessors also
            val predecessors = safeNode.predecessors
            (0 until predecessors.size).forEach {
                var currentPredecessor: Node<StateType>? = predecessors[it].node
                while (currentPredecessor !== currentPredecessor?.parent) {
                    currentPredecessor?.safe = safeNode.safe
                    currentPredecessor = currentPredecessor?.parent
                }
            }
            safeNodes.remove(safeNode)
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
            for ((predecessorNode, _, actionCost) in node.predecessors) {

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

                    predecessorNode.heuristic = currentHeuristicValue + actionCost
                    assert(predecessorNode.iteration == iterationCounter - 1)
                    predecessorNode.iteration = iterationCounter

                    addToOpenList(predecessorNode)
                } else if (predecessorHeuristicValue > currentHeuristicValue + actionCost) {
                    // This node was visited in this learning phase, but the current path is better then the previous
                    predecessorNode.heuristic = currentHeuristicValue + actionCost
                    openList.update(predecessorNode) // Update priority

                    // Frontier nodes could be also visited
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
     *
     */
    private fun safeNodeOnOpen(): Pair<Node<StateType>, Double> {
        val placeBackOnOpen = ArrayList<Node<StateType>?>()
        var topOfOpen = openList.pop()
        var hasSafeTopLevelActions = false
        placeBackOnOpen.add(topOfOpen)
        // pop off open until we find a safe node
        while (!hasSafeTopLevelActions && !topOfOpen!!.safe && openList.isNotEmpty()) {
            topOfOpen = openList.pop()
            // check if the top level action is safe
            var currentParent = topOfOpen?.parent
            if (currentParent !== currentParent?.parent) {
                while (currentParent?.parent?.parent !== currentParent?.parent?.parent) {
                    currentParent = currentParent?.parent
                    // find the top level action node
                }
            }
            if (currentParent!!.safe) {
                hasSafeTopLevelActions = true
            }
            placeBackOnOpen.add(topOfOpen)
        }

        // place all of our nodes back onto open
        while(placeBackOnOpen.isNotEmpty()) {
            val openTop: Node<StateType> = placeBackOnOpen.first()!!
            placeBackOnOpen.remove(openTop)
            openList.add(openTop)
        }
        // make sure open list is ordered appropriately
        openList.reorder(fValueComparator)
        // if the open list is empty there is no
        // safe node on open to travel up
        // return -1 and do what LSS does
        if (openList.isEmpty()) {
            return Pair(topOfOpen!!, -1.0)
        }
        // open list had something safe on it
        return Pair(topOfOpen!!, topOfOpen.f)
    }

    /**
     * Given a state, this function returns the path according to the tree pointers
     */
    private fun extractPlan(targetNode: Node<StateType>, sourceState: StateType): List<ActionBundle> {
        val actions = ArrayList<ActionBundle>(1000)
        var currentNode = targetNode

        logger.debug { "Extracting plan" }

        if (targetNode.state == sourceState) {
            return emptyList()
        }

        val safestNodeOnOpen: Pair<Node<StateType>, Double> = safeNodeOnOpen()

        if (safestNodeOnOpen.second != -1.0) {
            currentNode = safestNodeOnOpen.first
        }

        // keep on pushing actions to our queue until source state (our root) is reached
        do {
            actions.add(ActionBundle(currentNode.action, currentNode.actionCost))
            currentNode = currentNode.parent
        } while (currentNode.state != sourceState)

        logger.debug { "Plan extracted" }

        return actions.reversed()
    }

    private fun clearOpenList() {
        logger.debug { "Clear open list" }
        openList.clear()
    }

    private fun popOpenList(): Node<StateType> {
        return openList.pop() ?: throw GoalNotReachableException("Goal not reachable. Open list is empty.")
    }

    private fun addToOpenList(node: Node<StateType>) {
        openList.add(node)
    }

}
