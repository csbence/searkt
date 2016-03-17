package edu.unh.cs.ai.realtimesearch.planner.realtime

import edu.unh.cs.ai.realtimesearch.environment.*
import edu.unh.cs.ai.realtimesearch.experiment.measureInt
import edu.unh.cs.ai.realtimesearch.experiment.terminationCheckers.TimeTerminationChecker
import edu.unh.cs.ai.realtimesearch.logging.*
import edu.unh.cs.ai.realtimesearch.planner.RealTimePlanner
import edu.unh.cs.ai.realtimesearch.util.OneWayObjectPool
import org.slf4j.LoggerFactory
import java.util.*
import java.util.concurrent.TimeUnit.MILLISECONDS
import java.util.concurrent.TimeUnit.NANOSECONDS
import kotlin.Double.Companion.POSITIVE_INFINITY
import kotlin.system.measureNanoTime
import kotlin.system.measureTimeMillis

/**
 * Local Search Space Learning Real Time Search A*, a type of RTS planner.
 *
 * Runs A* until out of resources, then selects action up till the most promising state.
 * While executing that plan, it will:
 * - update all the heuristic values along the path (dijkstra)
 * - Run A* from the expected destination state
 *
 * This loop continue until the goal has been found
 */
class LssLrtaStarPlanner<StateType : State<StateType>>(domain: Domain<StateType>) : RealTimePlanner<StateType>(domain) {
    data class Edge<StateType : State<StateType>>(var node: Node<StateType>?, var action: Action?, var actionCost: Double?)

    class Node<StateType : State<StateType>>(var state: StateType?, var heuristic: Double, var cost: Double,
                                             var actionCost: Double, var action: Action,
                                             var iteration: Long,
                                             var open: Boolean = false,
                                             parent: Node<StateType>? = null) {

        var predecessors: MutableList<Edge<StateType>> = arrayListOf()
        var parent: Node<StateType>
        val f: Double
            get() = cost + heuristic

        init {
            this.parent = parent ?: this
        }

        override fun hashCode(): Int {
            return state!!.hashCode()
        }

        override fun equals(other: Any?): Boolean {
            if (other != null && other is Node<*>) {
                return state!!.equals(other.state)
            }
            return false
        }

        override fun toString(): String {
            return "Node: [State: $state h: $heuristic, g: $cost, iteration: $iteration, actionCost: $actionCost, parent: ${parent.state}  ]"
        }
    }

    private val logger = LoggerFactory.getLogger(LssLrtaStarPlanner::class.java)
    private var iterationCounter = 0L

    private val fValueComparator = Comparator<Node<StateType>> { lhs, rhs ->
        if (lhs.f == rhs.f) {
            when {
                lhs.cost > rhs.cost -> -1
                lhs.cost < rhs.cost -> 1
                else -> 0
            }
        } else {
            when {
                lhs.f < rhs.f -> -1
                lhs.f > rhs.f -> 1
                else -> 0
            }
        }
    }

    private val heuristicComparator = Comparator<Node<StateType>> { lhs, rhs ->
        when {
            lhs.heuristic < rhs.heuristic -> -1
            lhs.heuristic > rhs.heuristic -> 1
            else -> 0
        }
    }

    private val nodes: MutableMap<StateType, Node<StateType>> = hashMapOf()
    private val closedList: MutableSet<Node<StateType>> = hashSetOf()

    // LSS stores heuristic values. Use those, but initialize them according to the domain heuristic
    // The cost values are initialized to infinity
    private var openList = PriorityQueue<Node<StateType>>(fValueComparator)

    // for fast lookup we maintain a set in parallel
    private val openSet = hashSetOf<Node<StateType>>()

    private var rootState: StateType? = null

    private var aStarPopCounter = 0
    private var dijkstraPopCounter = 0
    private var aStarTimer = 0L
    private var dijkstraTimer = 0L

    // Object pools
    private lateinit var nodePool: OneWayObjectPool<Node<StateType>>
    private lateinit var edgePool: OneWayObjectPool<Edge<StateType>>

    /**
     * Allocate memory for nodes.
     */
    override fun init() {
        super.init()

        val maxNodeCount = 10000000
        val expectedBranchingFactor = 5

        val nanoInitTime = measureNanoTime {
            val nodeFactory: () -> Node<StateType> = { Node(null, 0.0, 0.0, 0.0, NoOperationAction, 0, false) }
            nodePool = OneWayObjectPool(maxNodeCount, nodeFactory)

            val edgeFactory: () -> Edge<StateType> = { Edge(null, null, 0.0) }
            edgePool = OneWayObjectPool(maxNodeCount * expectedBranchingFactor, edgeFactory)
        }

        logger.info { "Initialization time: ${MILLISECONDS.convert(nanoInitTime, NANOSECONDS)}ms" }
        System.gc()
    }

    /**
     * Prepares LSS for a completely unrelated new search. Sets mode to init
     * When a new action is selected, all members that persist during selection action phase are cleared
     */
    override fun reset() {
        super.reset()

        rootState = null

        aStarPopCounter = 0
        dijkstraPopCounter = 0
        aStarTimer = 0L
        dijkstraTimer = 0L

        clearOpenList()
        closedList.clear()
    }

    /**
     * Selects a action given current state.
     *
     * LSS_LRTA* will generate a full plan to some frontier, and stick to that plan. So the action returned will
     * always be the first on in the current plan.
     *
     * LSS-LRTAStar will plan to a specific frontier, and continue
     * to plan from there. This planning abides a termination criteria, meaning that it plans under constraints
     *
     * @param state is the current state
     * @param terminationChecker is the constraint
     * @return a current action
     */
    override fun selectAction(state: StateType, terminationChecker: TimeTerminationChecker): List<ActionBundle> {
        // Initiate for the first search

        if (rootState == null) {
            rootState = state
        } else if (state != rootState) {
            // The given state should be the last target
            logger.error { "Inconsistent world state. Expected $rootState got $state" }
        }

        if (domain.isGoal(state)) {
            // The start state is the goal state
            logger.warn { "selectAction: The goal state is already found." }
            return emptyList()
        }

        logger.debug() { "Root state: $state" }
        // Every turn learn then A* until time expires

        if (closedList.isNotEmpty()) {
            dijkstraTimer += measureTimeMillis { dijkstra(terminationChecker) }
        }

        var plan: List<ActionBundle>? = null
        aStarTimer += measureTimeMillis {
            val targetNode = aStar(state, terminationChecker)
            plan = extractPlan(targetNode, state)
            rootState = targetNode.state
        }

        logger.debug() { "AStar pops: $aStarPopCounter Dijkstra pops: $dijkstraPopCounter" }
        logger.debug() { "AStar time: $aStarTimer Dijkstra pops: $dijkstraTimer" }

        return plan!!
    }

    /**
     * Runs AStar until termination and returns the path to the head of openList
     * Will just repeatedly expand according to A*.
     */
    private fun aStar(state: StateType, terminationChecker: TimeTerminationChecker): Node<StateType> {
        // actual core steps of A*, building the tree
        initializeAStar()

        val node = Node(state, domain.heuristic(state), 0.0, 0.0, NoOperationAction, iterationCounter)
        nodes[state] = node
        //        costTable.put(state, 0.0) TODO
        var currentNode = node
        addToOpenList(node)
        logger.debug { "Starting A* from state: $state" }

        val expandedNodes = measureInt({ expandedNodeCount }) {
            while (!terminationChecker.reachedTermination() && !domain.isGoal(currentNode.state!!)) {
                aStarPopCounter++
                if (openList.isEmpty()) {
                    logger.error("Solution not found! (Open list is empty)")
                }
                currentNode = popOpenList()
                expandFromNode(currentNode)
            }
        }

        if (node == currentNode && !domain.isGoal(currentNode.state!!)) {
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
        closedList.clear()

        reorderOpenListBy(fValueComparator)
    }

    /**
     * Expands a node and add it to closed list. For each successor
     * it will add it to the open list and store it's g value, as long as the
     * state has not been seen before, or is found with a lower g value
     */
    private fun expandFromNode(sourceNode: Node<StateType>) {
        expandedNodeCount += 1
        closedList.add(sourceNode)

        val currentGValue = sourceNode.cost
        for (successor in domain.successors(sourceNode.state!!)) {
            val successorState = successor.state
            logger.trace { "Considering successor $successorState" }

            val successorNode = getNode(sourceNode, successor)

            // Add the current state as the predecessor of the child state
            val edge = edgePool.getObject()
            edge.apply {
                node = sourceNode
                action = successor.action
                actionCost = successor.actionCost
            }
            successorNode.predecessors.add(edge)

            // If the node is outdated it should be updated.
            if (successorNode.iteration != iterationCounter) {
                successorNode.apply {
                    iteration = iterationCounter
                    predecessors.clear()
                    cost = POSITIVE_INFINITY
                    // parent, action, and actionCost is outdated too, but not relevant.
                }
            }

            // Add the current state as the predecessor of the child state
            successorNode.predecessors.add(Edge(node = sourceNode, action = successor.action, actionCost = successor.actionCost))

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

                if (!inOpenList(successorNode)) {
                    addToOpenList(successorNode)
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

            val node = nodePool.getObject()

            node.apply {
                state = successorState
                heuristic = domain.heuristic(successorState)
                cost = POSITIVE_INFINITY
                actionCost = successor.actionCost
                action = successor.action
                node.parent = parent
                iteration = iterationCounter
                open = false
            }

            nodes[successorState] = node
            node
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
     * When first called (mode == NEW_DIJKSTRA), this will set the cost of all states in the closed list to infinity.
     * We then update
     *
     */
    private fun dijkstra(terminationChecker: TimeTerminationChecker) {
        logger.debug { "Start: Dijkstra" }
        // Invalidate the current heuristic value by incrementing the counter
        iterationCounter++

        // change openList ordering to heuristic only`
        reorderOpenListBy(heuristicComparator)

        var counter = 0 // TODO
        var removedCounter = 0;
        logger.info { "\nOpen list: ${openSet.size} - ${openList.size}" }
        openSet.forEach { logger.debug("$it") }

        logger.info { "\nClosed list: ${closedList.size}" }
        closedList.forEach { logger.debug("$it") }

        while (!terminationChecker.reachedTermination() && openList.isNotEmpty()) {
            // Closed list should be checked
            val node = popOpenList()
            node.iteration = iterationCounter
            dijkstraPopCounter++ // TODO remove

            val removed = closedList.remove(node)
            logger.debug { "Dijkstra step: ${counter++} :: open list size: ${openList.size} :: closed list size: ${closedList.size} :: #succ: ${node.predecessors.size} :: $node      Removed: $removed - $removedCounter" }

            val currentHeuristicValue = node.heuristic
            //            logger.debug { "Checking for predecessors of $node (h value: $currentHeuristicValue)" }

            // update heuristic value for each predecessor
            node.predecessors.forEach { predecessor ->
                // Update if the node is outdated
                if (predecessor.node!!.iteration != iterationCounter) {
                    predecessor.node!!.heuristic = POSITIVE_INFINITY
                    predecessor.node!!.iteration = iterationCounter
                }

                val predecessorHeuristicValue = predecessor.node!!.heuristic

                logger.debug { "Considering predecessor ${predecessor.node} with heuristic value $predecessorHeuristicValue" }
//                logger.debug { "Node in closedList: ${predecessor.node in closedList}. Current heuristic: $predecessorHeuristicValue. Proposed new value: ${(currentHeuristicValue + predecessor.actionCost)}" }

                // only update those that we found in the closed list and whose are lower than new found heuristic
                if (predecessor!!.node in closedList ) {

                    if (predecessorHeuristicValue > (currentHeuristicValue + predecessor.actionCost!!)) {
                        predecessor.node!!.heuristic = currentHeuristicValue + predecessor.actionCost!!
                    }

                    if (!predecessor.node!!.open)
                        addToOpenList(predecessor.node!!)
                }
            }
        }

        // update mode if done
        if (openList.isEmpty()) {
            logger.debug { "Done with Dijkstra" }
        } else {
            logger.warn { "Incomplete learning step. Lists: Open(${openList.size}) Closed(${closedList.size}) " }
        }
    }

    /**
     * Given a state, this function returns the path according to the tree pointers
     */
    private fun extractPlan(targetNode: Node<StateType>, sourceState: StateType): List<ActionBundle> {
        val actions = arrayListOf<ActionBundle>()
        var currentNode = targetNode

        logger.debug() { "Extracting plan" }

        if (targetNode.state == sourceState) {
            return emptyList()
        }

        // keep on pushing actions to our queue until source state (our root) is reached
        do {
            actions.add(ActionBundle(currentNode.action, currentNode.actionCost))
            currentNode = currentNode.parent
        } while (currentNode.state != sourceState)

        logger.debug() { "Plan extracted" }

        return actions.reversed()
    }

    private fun clearOpenList() {
        logger.debug { "Clear open list" }
        openList.clear()
        openSet.clear()
    }

    private fun inOpenList(node: Node<StateType>) = openSet.contains(node)

    private fun popOpenList(): Node<StateType> {
        val state = openList.remove()
        openSet.remove(state)

        assert(openList.size == openSet.size)
        return state
    }

    private fun addToOpenList(node: Node<StateType>) {
        openList.add(node)
        openSet.add(node)

        assert(openList.size == openSet.size)
    }

    private fun reorderOpenListBy(comparator: Comparator<Node<StateType>>) {
        val tempOpenList = openList.toTypedArray() // O(1)
        if (tempOpenList.size >= 1) {
            openList = PriorityQueue<Node<StateType>>(tempOpenList.size, comparator) // O(1)
        } else {
            openList = PriorityQueue<Node<StateType>>(comparator) // O(1)
        }

        openList.addAll(tempOpenList) // O(n * log(n))
    }
}
