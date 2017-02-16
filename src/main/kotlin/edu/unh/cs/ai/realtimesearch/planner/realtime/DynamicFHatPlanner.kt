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
import java.lang.Math.max
import java.util.*
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
    data class Edge<StateType : State<StateType>>(val node: Node<StateType>, val action: Action, val actionCost: Long)

    class Node<StateType : State<StateType>>(val state: StateType, var heuristic: Double, var cost: Long,
                                             var distance: Double, var distanceError: Double,
                                             var actionCost: Long, var action: Action,
                                             var iteration: Long,
                                             var correctedHeuristic: Double,
                                             parent: Node<StateType>? = null) : Indexable {

        /** Item index in the open list. */
        override var index: Int = -1

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

    private val nodes: HashMap<StateType, Node<StateType>> = HashMap<StateType, Node<StateType>>(100000000).resize()

    // LSS stores heuristic values. Use those, but initialize them according to the domain heuristic
    // The cost values are initialized to infinity
    private var openList = AdvancedPriorityQueue<Node<StateType>>(10000000, fHatComparator)

    private var rootState: StateType? = null

    // Performance measurement
    private var aStarPopCounter = 0
    private var dijkstraPopCounter = 0
    var aStarTimer = 0L
        get
    var dijkstraTimer = 0L
        get

    // Global error correction
    private var heuristicError = 0.0
    private var distanceError = 0.0

    private var nextHeuristicError = 0.0
    private var nextDistanceError = 0.0

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
            logger.warn { "selectAction: The goal sourceState is already found." }
            return emptyList()
        }

        logger.debug { "Root sourceState: $sourceState" }

        // Learning phase
        if (openList.isNotEmpty()) {
            dijkstraTimer += measureTimeMillis { dijkstra(terminationChecker) }
        }

        // Exploration phase
        var plan: List<ActionBundle>? = null
        aStarTimer += measureTimeMillis {
            val targetNode = aStar(sourceState, terminationChecker)
            // Update error estimates
            distanceError = nextDistanceError
            heuristicError = nextHeuristicError

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

        val node = Node(state, domain.heuristic(state), 0, domain.distance(state), domain.distance(state), 0, NoOperationAction, iterationCounter, domain.heuristic(state)) // Create root node
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
            logger.debug { "A* : expanded $expandedNodes nodes" }
        }

        logger.debug { "Done with AStar at $currentNode" }

        return currentNode
    }

    private fun initializeAStar() {
        iterationCounter++
        clearOpenList()

        openList.reorder(fHatComparator)
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
                    cost = Long.MAX_VALUE
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
                    val currentDistanceEstimate = distanceError / (1.0 - distanceError) // Dionne 2011 (3.8)

                    cost = successorGValueFromCurrent
                    parent = sourceNode
                    action = successor.action
                    actionCost = successor.actionCost
                    correctedHeuristic = heuristicError * currentDistanceEstimate + heuristic
                }

                logger.debug { "Expanding from $sourceNode --> $successorState :: open list size: ${openList.size}" }
                logger.trace { "Adding it to to cost table with value ${successorNode.cost}" }

                if (!successorNode.open) {
                    addToOpenList(successorNode)
                } else {
                    openList.update(successorNode)
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
                    cost = Long.MAX_VALUE,
                    actionCost = successor.actionCost,
                    action = successor.action,
                    parent = parent,
                    iteration = iterationCounter,
                    distanceError = distance,
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
    private fun dijkstra(terminationChecker: TerminationChecker) {
        logger.debug { "Start: Dijkstra" }
        // Invalidate the current heuristic value by incrementing the counter
        iterationCounter++

        // change openList ordering to heuristic only`
        openList.reorder(heuristicComparator)

        while (!terminationChecker.reachedTermination() && openList.isNotEmpty()) {
            // Closed list should be checked
            val node = popOpenList()
            node.iteration = iterationCounter

            val currentHeuristicValue = node.heuristic

            // update heuristic value for each predecessor
            for (predecessor in node.predecessors) {
                val predecessorNode = predecessor.node

                if (predecessorNode.iteration == iterationCounter && !predecessorNode.open) {
                    // This node was already learned and closed in the current iteration
                    continue
                }

                // Update if the node is outdated
                //                if (predecessorNode.iteration != iterationCounter) {
                //                    predecessorNode.heuristic = POSITIVE_INFINITY
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

                    predecessorNode.distanceError = node.distanceError
                    predecessorNode.distance = node.distance + 1

                    addToOpenList(predecessorNode)
                } else if (predecessorHeuristicValue > currentHeuristicValue + predecessor.actionCost) {
                    // This node was visited in this learning phase, but the current path is better then the previous

                    predecessorNode.heuristic = currentHeuristicValue + predecessor.actionCost
                    openList.update(predecessorNode)
                }
            }
        }

        // update mode if done
        if (openList.isEmpty()) {
            logger.debug { "Done with Dijkstra" }
        } else {
            logger.warn() { "Incomplete learning step. Lists: Open(${openList.size})) " }
        }
    }

    /**
     * Given a state, this function returns the path according to the tree pointers
     */
    private fun extractPlan(targetNode: Node<StateType>, sourceState: StateType): List<ActionBundle> {
        val actions = ArrayList<ActionBundle>(1000)
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

    private fun popOpenList(): Node<StateType> {
        val node = openList.pop() ?: throw GoalNotReachableException("Goal not reachable. Open list is empty.")
        return node
    }

    private fun addToOpenList(node: Node<StateType>) {
        openList.add(node)
    }
}