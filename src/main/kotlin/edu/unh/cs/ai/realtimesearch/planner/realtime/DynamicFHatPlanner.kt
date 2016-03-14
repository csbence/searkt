package edu.unh.cs.ai.realtimesearch.planner.realtime

import edu.unh.cs.ai.realtimesearch.environment.*
import edu.unh.cs.ai.realtimesearch.experiment.measureInt
import edu.unh.cs.ai.realtimesearch.experiment.terminationCheckers.TimeTerminationChecker
import edu.unh.cs.ai.realtimesearch.logging.*
import edu.unh.cs.ai.realtimesearch.planner.RealTimePlanner
import org.slf4j.LoggerFactory
import java.lang.Math.max
import java.util.*
import kotlin.Double.Companion.POSITIVE_INFINITY
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
class DynamicFHatPlanner<StateType : State<StateType>>(domain: Domain<StateType>) : RealTimePlanner<StateType>(domain) {
    data class Edge<StateType : State<StateType>>(val node: Node<StateType>, val action: Action, val actionCost: Double)

    class Node<StateType : State<StateType>>(val state: StateType, var heuristic: Double, var cost: Double,
                                             var distance: Double, var correctedDistance: Double,
                                             var actionCost: Double, var action: Action,
                                             var iteration: Long,
                                             var correctedHeuristic: Double,
                                             var open: Boolean = false,
                                             parent: Node<StateType>? = null) {

        var predecessors: MutableList<Edge<StateType>> = arrayListOf()
        var parent: Node<StateType>
        val f: Double
            get() = cost + heuristic

        val fHat: Double
            get() = cost + correctedHeuristic

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

    private val logger = LoggerFactory.getLogger(DynamicFHatPlanner::class.java)
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

    private val fHatComparator = Comparator<Node<StateType>> { lhs, rhs ->
        if (lhs.fHat == rhs.fHat) {
            when {
                lhs.cost > rhs.cost -> -1
                lhs.cost < rhs.cost -> 1
                else -> 0
            }
        } else {
            when {
                lhs.fHat < rhs.fHat -> -1
                lhs.fHat > rhs.fHat -> 1
                else -> 0
            }
        }
    }

    private val correctedHeuristicComparator = Comparator<Node<StateType>> { lhs, rhs ->
        when {
            lhs.correctedHeuristic < rhs.correctedHeuristic -> -1
            lhs.correctedHeuristic > rhs.correctedHeuristic -> 1
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

    private var heuristicError = 0.0
    private var distanceError = 0.0

    private var nextHeuristicError = 0.0
    private var nextDistanceError = 0.0

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
        heuristicError = 0.0
        distanceError = 0.0

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

        logger.info { "Root state: $state" }

        // Learning phase
        if (closedList.isNotEmpty()) {
            dijkstraTimer += measureTimeMillis { dijkstra(terminationChecker) }
        }

        // Exploration phase
        var plan: List<ActionBundle>? = null
        aStarTimer += measureTimeMillis {
            val targetNode = aStar(state, terminationChecker)
            // Update error estimates
            distanceError = nextDistanceError
            heuristicError = nextHeuristicError

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
    private fun aStar(state: StateType, terminationChecker: TimeTerminationChecker): Node<StateType> {
        // actual core steps of A*, building the tree
        initializeAStar()

        val node = Node(state, domain.heuristic(state), 0.0, domain.distance(state), domain.distance(state), 0.0, NoOperationAction, iterationCounter, domain.heuristic(state)) // Create root node
        nodes[state] = node // Add root node to the node table
        var currentNode = node
        addToOpenList(node)
        logger.debug { "Starting A* from state: $state" }

        val expandedNodes = measureInt({ expandedNodeCount }) {
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
     * state has not been seen before, or is found with a lower g value.
     *
     * During the expansion the child with the minimum f value is selected and used to update the distance and heuristic error.
     *
     */
    private fun expandFromNode(sourceNode: Node<StateType>) {
        expandedNodeCount += 1
        closedList.add(sourceNode)

        // Select the best children to update the distance and heuristic error
        var bestChildNode: Node<StateType>? = null

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
                    cost = POSITIVE_INFINITY
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
                // Initially the cost is going to be higher, thus we encounter each (local) state at least once in the LSS
                successorNode.apply {
                    val currentDistanceEstimate = correctedDistance / (1.0 - distanceError) // Dionne 2011 (3.8)

                    cost = successorGValueFromCurrent
                    parent = sourceNode
                    action = successor.action
                    actionCost = successor.actionCost
                    correctedHeuristic = heuristicError * currentDistanceEstimate + heuristic
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

        if (bestChildNode != null) {
            // Local error values (min 0.0)
            val localHeuristicError = max(0.0, bestChildNode.f - sourceNode.f)
            val localDistanceError = max(0.0, bestChildNode.distance - sourceNode.distance + 1)

            // The next error values are the weighted average of the local error and the previous error
            nextHeuristicError += (localHeuristicError - nextHeuristicError) / expandedNodeCount
            nextDistanceError += (localDistanceError - nextDistanceError) / expandedNodeCount
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

            val distance = domain.distance(successorState)
            val heuristic = domain.heuristic(successorState)

            val undiscoveredNode = Node(
                    state = successorState,
                    heuristic = heuristic,
                    distance = distance,
                    cost = POSITIVE_INFINITY,
                    actionCost = successor.actionCost,
                    action = successor.action,
                    parent = parent,
                    iteration = iterationCounter,
                    correctedDistance = distance,
                    correctedHeuristic = distance * heuristicError + heuristic)

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
    private fun dijkstra(terminationChecker: TimeTerminationChecker) {
        logger.info { "Start: Dijkstra" }
        // Invalidate the current heuristic value by incrementing the counter
        iterationCounter++

        // change openList ordering to heuristic only`
        reorderOpenListBy(heuristicComparator)

        while (!terminationChecker.reachedTermination() && openList.isNotEmpty()) {
            // Closed list should be checked
            val node = popOpenList()
            node.iteration = iterationCounter

            closedList.remove(node)

            val currentHeuristicValue = node.heuristic

            // update heuristic value for each predecessor
            node.predecessors.forEach { predecessor ->
                // Update if the node is outdated
                if (predecessor.node.iteration != iterationCounter) {
                    predecessor.node.heuristic = POSITIVE_INFINITY
                    predecessor.node.iteration = iterationCounter
                }

                val predecessorHeuristicValue = predecessor.node.heuristic

                //                logger.debug { "Considering predecessor ${predecessor.node} with heuristic value $predecessorHeuristicValue" }
                //                logger.debug { "Node in closedList: ${predecessor.node in closedList}. Current heuristic: $predecessorHeuristicValue. Proposed new value: ${(currentHeuristicValue + predecessor.actionCost)}" }

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
    }

    private fun inOpenList(node: Node<StateType>) = node.open

    private fun popOpenList(): Node<StateType> {
        val node = openList.remove()
        node.open = false
        return node
    }

    private fun addToOpenList(node: Node<StateType>) {
        openList.add(node)
        node.open = true
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