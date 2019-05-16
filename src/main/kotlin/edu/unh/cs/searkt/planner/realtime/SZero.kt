package edu.unh.cs.searkt.planner.realtime

import edu.unh.cs.searkt.MetronomeConfigurationException
import edu.unh.cs.searkt.MetronomeException
import edu.unh.cs.searkt.environment.*
import edu.unh.cs.searkt.experiment.configuration.ExperimentConfiguration
import edu.unh.cs.searkt.experiment.terminationCheckers.TerminationChecker
import edu.unh.cs.searkt.planner.*
import edu.unh.cs.searkt.planner.exception.GoalNotReachableException
import edu.unh.cs.searkt.util.AdvancedPriorityQueue
import edu.unh.cs.searkt.util.resize
import java.util.*
import kotlin.Long.Companion.MAX_VALUE
import kotlin.system.measureTimeMillis

/**
 * SZero using Local Search Space Learning Real Time Search A* as a base, a type of RTS planner.
 *
 * Runs A* until out of resources, then selects action up till the most promising state.
 * While executing that plan, it will:
 * - update all the heuristic values along the path (dijkstra)
 * - Run A* from the expected destination state
 * - Choose actions which have a safe parent
 *
 * This loop continue until the goal has been found
 */
class SZeroPlanner<StateType : State<StateType>>(val domain: Domain<StateType>, configuration: ExperimentConfiguration) : RealTimePlanner<StateType>() {
    private val safetyBackup = configuration.safetyBackup
            ?: throw MetronomeConfigurationException("Safety backup strategy is not specified.")
    private val targetSelection = configuration.targetSelection
            ?: throw MetronomeConfigurationException("Target selection strategy is not specified.")

    class Node<StateType : State<StateType>>(override val state: StateType,
                                             override var heuristic: Double,
                                             override var cost: Long,
                                             override var actionCost: Long,
                                             override var action: Action,
                                             var iteration: Long,
                                             parent: Node<StateType>? = null,
                                             override var safe: Boolean = false,
                                             override var unsafe: Boolean = false) : Safe, SearchNode<StateType, Node<StateType>> {
        /** Item index in the open list. */
        override var index: Int = -1
        override var closed = false

        /** Nodes that generated this Node as a successor in the current exploration phase. */
        override var predecessors: MutableList<SearchEdge<Node<StateType>>> = arrayListOf()

        /** Parent pointer that points to the min cost predecessor. */
        override var parent: Node<StateType> = parent ?: this

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
            return "SafeRealTimeSearchNode: [State: $state h: $heuristic, g: $cost, iteration: $iteration, actionCost: $actionCost, parent: ${parent.state}, open: $open ]"
        }
    }

    private var iterationCounter = 0L

    private val safeNodes = ArrayList<Node<StateType>>()

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
    private var openList = AdvancedPriorityQueue<Node<StateType>>(100000000, fValueComparator)

    private var rootState: StateType? = null

    // Performance measurement
    private var aStarPopCounter = 0
    private var dijkstraPopCounter = 0
    var aStarTimer = 0L
    var dijkstraTimer = 0L

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
        }

        if (domain.isGoal(sourceState)) {
            // The start sourceState is the goal sourceState
            return emptyList()
        }

        // Every turn learn then A* until time expires

        // Learning phase
        if (openList.isNotEmpty()) {
            dijkstraTimer += measureTimeMillis { dijkstra(terminationChecker) }
        }

        // Exploration phase
        var plan: List<ActionBundle>? = null
        aStarTimer += measureTimeMillis {
            val targetNode = aStar(sourceState, terminationChecker)

            when (safetyBackup) {
                SafetyBackup.PARENT -> backUpSafetyThroughParents()
                SafetyBackup.PREDECESSOR -> predecessorSafetyPropagation(safeNodes)
            }

            val targetSafeNode = when (targetSelection) {
                SafeRealTimeSearchTargetSelection.SAFE_TO_BEST -> selectSafeToBest(openList)
                SafeRealTimeSearchTargetSelection.BEST_SAFE -> throw MetronomeException("Invalid configuration. S0 does not implement the BEST_SAFE strategy")
            }

            plan = extractPath(targetSafeNode ?: targetNode, sourceState)
            rootState = targetNode.state
        }


        return plan!!
    }

    /**
     * Runs AStar until termination and returns the path to the head of openList
     * Will just repeatedly expand according to A*.
     */
    private fun aStar(state: StateType, terminationChecker: TerminationChecker): Node<StateType> {
        // actual core steps of A*, building the tree
        initializeAStar()

        val node = Node(state, nodes[state]?.heuristic
                ?: domain.heuristic(state), 0, 0, NoOperationAction, iterationCounter)
        nodes[state] = node
        openList.add(node)

        while (!terminationChecker.reachedTermination()) {
            aStarPopCounter++

            val topNode = openList.peek() ?: throw GoalNotReachableException("Open list is empty.")
            if (domain.isGoal(topNode.state)) return topNode

            expandFromNode(openList.pop()!!)
            terminationChecker.notifyExpansion()
        }
        return openList.peek() ?: throw GoalNotReachableException("Open list is empty.")
    }

    private fun initializeAStar() {
        iterationCounter++
        openList.clear()
        safeNodes.clear()
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

            val successorNode = getNode(sourceNode, successor)

            if (successorNode.heuristic == Double.POSITIVE_INFINITY
                    && successorNode.iteration != iterationCounter) {
                // Ignore this successor as it is a dead end
                continue
            }

            // check for safety if safe add to the safe nodes
            if (domain.isSafe(successorNode.state)) {
                safeNodes.add(successorNode)
                successorNode.safe = true
            }

            // If the node is outdated it should be updated.
            if (successorNode.iteration != iterationCounter) {
                successorNode.apply {
                    iteration = iterationCounter
                    predecessors.clear()
                    cost = MAX_VALUE
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
            val successorGValueFromCurrent = currentGValue + successor.actionCost
            if (successorNode.cost > successorGValueFromCurrent) {
                // here we generate a state. We store it's g value and remember how to get here via the treePointers
                successorNode.apply {
                    cost = successorGValueFromCurrent.toLong()
                    parent = sourceNode
                    action = successor.action
                    actionCost = successor.actionCost.toLong()
                }

                if (!successorNode.open) {
                    openList.add(successorNode) // Fresh node not on the open yet
                } else {
                    openList.update(successorNode)
                }
            } else {
            }
        }

        sourceNode.heuristic = Double.POSITIVE_INFINITY
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
     */
    private fun backUpSafetyThroughParents() {
        while (safeNodes.isNotEmpty()) {
            val safeNode = safeNodes.first()
            safeNodes.remove(safeNode)

            var currentParent = safeNode.parent
            // always update the safe nodes
            // through the parent pointers
            while (currentParent !== currentParent.parent) {
                currentParent.safe = safeNode.safe
                currentParent = currentParent.parent
            }
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

                if (!predecessorNode.open) {
                    // This node is not open yet, because it was not visited in the current planning iteration

                    predecessorNode.heuristic = currentHeuristicValue + actionCost
                    assert(predecessorNode.iteration == iterationCounter - 1)
                    predecessorNode.iteration = iterationCounter

                    openList.add(predecessorNode)
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

    }

    /**
     *
     */
    private fun safeNodeOnOpen(): Node<StateType>? {
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
        while (placeBackOnOpen.isNotEmpty()) {
            val openTop: Node<StateType> = placeBackOnOpen.first()!!
            placeBackOnOpen.remove(openTop)
            openList.add(openTop)
        }
        // make sure open list is ordered appropriately
        openList.reorder(fValueComparator)
        // if the open list is empty there is no
        // safe node on open to travel up
        // return -1 and do what LSS does
        return if (openList.isEmpty()) topOfOpen!! else null
    }

}

enum class SafeZeroConfiguration {
    SAFETY_BACKUP,
    SAFETY
}

enum class SafeZeroSafetyBackup {
    PARENT, PREDECESSOR
}

enum class SafeZeroSafety {
    ABSOLUTE, PREFERRED
}