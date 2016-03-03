package edu.unh.cs.ai.realtimesearch.planner.realtime_

import edu.unh.cs.ai.realtimesearch.environment.*
import edu.unh.cs.ai.realtimesearch.experiment.TerminationChecker
import edu.unh.cs.ai.realtimesearch.experiment.measureInt
import edu.unh.cs.ai.realtimesearch.logging.*
import org.slf4j.LoggerFactory
import java.util.*
import kotlin.Double.Companion.POSITIVE_INFINITY
import kotlin.comparisons.compareBy
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
    data class Edge<StateType : State<StateType>>(val node: Node<StateType>, val action: Action, val actionCost: Double)

    class Node<StateType : State<StateType>>(val state: StateType, var heuristic: Double, var cost: Double,
                                             var actionCost: Double, var action: Action,
                                             var iteration: Long,
                                             parent: Node<StateType>? = null) {

        var predecessors: MutableList<Edge<StateType>> = arrayListOf()
        var parent: Node<StateType>

        init {
            this.parent = parent ?: this
        }

        override fun hashCode(): Int {
            return state.hashCode()
        }

        override fun equals(other: Any?): Boolean {
            if (other != null && other is Node<*>) {
                return state.equals(other.state)
            }
            return false
        }

        override fun toString(): String {
            return "Node: [State: $state h: $heuristic, g: $cost, iteration: $iteration, actionCost: $actionCost, parent: ${parent.state}  ]"
        }
    }

    private val logger = LoggerFactory.getLogger(LssLrtaStarPlanner::class.java)
    private var iterationCounter = 0L

    private val fValueComparator = compareBy<Node<StateType>> { node ->
        val state = node.state
        nodes[state]?.let { it.heuristic + it.cost } ?: domain.heuristic(state)
    }

    private val heuristicComparator = compareBy<Node<StateType>> { node ->
        val state = node.state
        nodes[state]?.heuristic ?: domain.heuristic(state) // TODO
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

    /**
     * Prepares LSS for a completely unrelated new search. Sets mode to init
     * When a new action is selected, all members that persist during selection action phase are cleared
     */
    override fun reset() {
        super.reset()

        // Ready to start new search!
        rootState = null
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
    override fun selectAction(state: StateType, terminationChecker: TerminationChecker): List<Action> {
        // Initiate for the first search

        if (rootState == null) {
            rootState = state
        } else if (state != rootState) {
            // The given state should be the last target
            logger.error { "Inconsistent world state. Expected $rootState got $state" }
        }

        // TODO check whether the given state is goal or not

        logger.info { "Root state: $state" }
        // Every turn learn then A* until time expires

        if (closedList.isNotEmpty()) {
            dijkstraTimer += measureTimeMillis { dijkstra(terminationChecker) }
        }

        var plan: List<Action>? = null
        aStarTimer += measureTimeMillis {
            val targetNode = aStar(state, terminationChecker)
            plan = extractPlan(targetNode, state)
            rootState = targetNode.state
        }

        logger.info { "AStar pops: $aStarPopCounter Dijkstra pops: $dijkstraPopCounter" }
        logger.info { "AStar time: $aStarTimer Dijkstra pops: $dijkstraTimer" }

        return plan!!
    }

    /**
     * Runs AStar until termination and returns the path to the head of openList
     * Will just repeatedly expand according to A*.
     */
    private fun aStar(state: StateType, terminationChecker: TerminationChecker): Node<StateType> {
        // actual core steps of A*, building the tree
        initializeAStar()

        val node = Node(state, domain.heuristic(state), 0.0, 0.0, NoOperationAction, iterationCounter)
        nodes[state] = node
        //        costTable.put(state, 0.0) TODO
        var currentNode = node
        addToOpenList(node)
        logger.debug { "Starting A* from state: $state" }

        val expandedNodes = measureInt({ expandedNodes }) {
            while (!terminationChecker.reachedTermination() && !domain.isGoal(currentNode.state)) {
                aStarPopCounter++
                currentNode = popOpenList()
                expandFromNode(currentNode)
            }
        }

        if (node == currentNode && !domain.isGoal(currentNode.state)) {
            //            throw InsufficientTerminationCriterionException("Not enough time to expand even one node")
        } else {
            logger.info { "A* : expanded $expandedNodes nodes" }
        }

        logger.info { "Done with AStar at $currentNode" }

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
    private fun expandFromNode(node: Node<StateType>) {
        expandedNodes += 1
        closedList.add(node)

        val currentGValue = node.cost
        for (successor in domain.successors(node.state)) {
            val successorState = successor.state
            logger.trace { "Considering successor $successorState" }

            val successorNode = getNode(node, successor)

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
            successorNode.predecessors.add(Edge(node = node, action = successor.action, actionCost = successor.actionCost))

            // only generate those state that are not visited yet or whose cost value are lower than this path
            val successorGValue = successorNode.cost
            val successorGValueFromCurrent = currentGValue + successor.actionCost
            if (successorGValue > successorGValueFromCurrent) {
                generatedNodes += 1

                // here we generate a state. We store it's g value and remember how to get here via the treePointers
                successorNode.apply {
                    cost = successorGValueFromCurrent
                    parent = node
                    action = successor.action
                    actionCost = successor.actionCost
                }

                logger.debug { "Expanding from $node --> $successorState :: open list size: ${openList.size}" }
                logger.trace { "Adding it to to cost table with value ${successorNode.cost}" }

                if (!inOpenList(successorNode)) {
                    addToOpenList(successorNode)
                }
            } else {
                logger.trace {
                    "Did not add, because it's cost is ${successorNode.cost} compared to cost of predecessor ( ${node.cost}), and action cost ${successor.actionCost}"
                }
            }
        }
    }

    private fun getNode(parent: Node<StateType>, successor: SuccessorBundle<StateType>): Node<StateType> {
        val successorState = successor.state
        val tempSuccessorNode = nodes[successorState]

        return if (tempSuccessorNode == null) {
            val undiscoveredNode = Node(
                    state = successorState,
                    heuristic = domain.heuristic(successorState),
                    cost = POSITIVE_INFINITY,
                    actionCost = successor.actionCost,
                    action = successor.action,
                    parent = parent,
                    iteration = iterationCounter)

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
     * When first called (mode == NEW_DIJKSTRA), this will set the cost of all states in the closed list to infinity.
     * We then update
     *
     */
    private fun dijkstra(terminationChecker: TerminationChecker) {
        logger.info { "Start: Dijkstra" }
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

        while (!terminationChecker.reachedTermination() && openList.isNotEmpty()) { // Closed list should be checked
            val node = popOpenList()
            node.iteration = iterationCounter
            dijkstraPopCounter++ // TODO remove

            val removed = closedList.remove(node)
            logger.debug { "Dijkstra step: ${counter++} :: open list size: ${openList.size} :: closed list size: ${closedList.size} :: #succ: ${node.predecessors.size} :: $node      Removed: $removed - $removedCounter" }

            val currentHeuristicValue = node.heuristic
            //            logger.debug { "Checking for predecessors of $node (h value: $currentHeuristicValue)" }

            // update heuristic value for each predecessor
            node.predecessors.forEach { predecessor ->
                if (predecessor.node.iteration != iterationCounter) {
                    predecessor.node.heuristic = POSITIVE_INFINITY
                    predecessor.node.iteration = iterationCounter
                    logger.debug { "Update node: Change heuristic to infinity." }
                }

                val predecessorHeuristicValue = predecessor.node.heuristic

                logger.debug { "Considering predecessor ${predecessor.node} with heuristic value $predecessorHeuristicValue" }
                logger.debug { "Node in closedList: ${predecessor.node in closedList}. Current heuristic: $predecessorHeuristicValue. Proposed new value: ${(currentHeuristicValue + predecessor.actionCost)}" }

                // only update those that we found in the closed list and whose are lower than new found heuristic
                if (predecessor.node in closedList && predecessorHeuristicValue > (currentHeuristicValue + predecessor.actionCost)) {

                    predecessor.node.heuristic = currentHeuristicValue + predecessor.actionCost
                    logger.debug { "Updated to ${predecessor.node.heuristic}" }

                    if (!inOpenList(predecessor.node))
                        addToOpenList(predecessor.node)
                }
            }
        }

        // update mode if done
        if (openList.isEmpty()) {
            logger.info { "Done with Dijkstra" }
        } else {
            logger.warn { "Incomplete learning step. Lists: Open(${openList.size}) Closed(${closedList.size}) " }
        }
    }

    /**
     * Given a state, this function returns the path according to the tree pointers
     */
    private fun extractPlan(targetNode: Node<StateType>, sourceState: StateType): List<Action> {
        val actions = arrayListOf<Action>()
        var currentNode = targetNode

        logger.debug() { "Extracting plan" }

        if (targetNode.state == sourceState) {
            return emptyList()
        }

        // keep on pushing actions to our queue until source state (our root) is reached
        do {
            actions.add(currentNode.action)
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