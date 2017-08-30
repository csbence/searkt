package edu.unh.cs.ai.realtimesearch.planner.realtime

import edu.unh.cs.ai.realtimesearch.MetronomeException
import edu.unh.cs.ai.realtimesearch.environment.*
import edu.unh.cs.ai.realtimesearch.experiment.configuration.Configurations
import edu.unh.cs.ai.realtimesearch.experiment.configuration.GeneralExperimentConfiguration
import edu.unh.cs.ai.realtimesearch.experiment.terminationCheckers.TerminationChecker
import edu.unh.cs.ai.realtimesearch.logging.debug
import edu.unh.cs.ai.realtimesearch.logging.trace
import edu.unh.cs.ai.realtimesearch.logging.warn
import edu.unh.cs.ai.realtimesearch.planner.RealTimePlanner
import edu.unh.cs.ai.realtimesearch.planner.exception.GoalNotReachableException
import edu.unh.cs.ai.realtimesearch.planner.extractSourceToTargetPath
import edu.unh.cs.ai.realtimesearch.util.AdvancedPriorityQueue
import edu.unh.cs.ai.realtimesearch.util.Indexable
import edu.unh.cs.ai.realtimesearch.util.resize
import org.slf4j.LoggerFactory
import java.util.*
import kotlin.system.measureTimeMillis
import kotlin.Long.Companion.MAX_VALUE

/**
 * SimpleSafe as described in "Avoiding Dead Ends in Real-time Heuristic Search"
 *
 * Planning phase - performs a breadth-first search to depth k, noting any
 * safe states that are generated.
 *
 * The tree is then cleared and restarts search back at the initial state and
 * behaves like LSS-LRTA*, using its remaining expansion budget to perform
 * best-first search on f.
 *
 * After the learning phase marks the ancestors (via predecessors) of all generated
 * safe states (from both the breadth-first and best-first searches) as comfortable
 * and all top-level actions leading from the initial state to a comfortable state as
 * safe.
 *
 * If there are safe actions, it commits to one whose successors state has lowest f.
 *
 * If no actions are safe, it just commits to the best action.
 *
 * This look continues until the goal has been found.
 *
 */

class SimpleSafePlanner<StateType : State<StateType>>(domain: Domain<StateType>, configuration: GeneralExperimentConfiguration) : RealTimePlanner<StateType>(domain) {
    private val safetyBackup = SimpleSafeSafetyBackup.valueOf(configuration[SimpleSafeConfiguration.SAFETY_BACKUP] as? String ?: throw MetronomeException("Safety backup strategy not found"))
    private val targetSelection : SafeRealTimeSearchTargetSelection = SafeRealTimeSearchTargetSelection.valueOf(configuration[SafeRealTimeSearchConfiguration.TARGET_SELECTION] as? String ?: throw MetronomeException("Target selection strategy not found"))

    private val versionNumber = SimpleSafeVersion.valueOf(configuration[SimpleSafeConfiguration.VERSION] as? String ?: throw MetronomeException("Version number not found"))

    class Node<StateType : State<StateType>>(override val state: StateType,
                                             override var heuristic: Double,
                                             override var cost: Long,
                                             override var actionCost: Long,
                                             override var action: Action,
                                             var iteration: Long,
                                             parent: Node<StateType>? = null,
                                             override var safe: Boolean = false,
                                             override var depth: Int)
        : Indexable, Safe, SearchNode<StateType, Node<StateType>>, Depth{
        /** Item index in the open list */
        override var index: Int = -1

        /** Nodes that generated this Node as a successor in the current exploration phase */
        override var predecessors: MutableList<SearchEdge<Node<StateType>>> = arrayListOf()

        /** Parent pointer that points to the min cost predecessor */
        override var parent: Node<StateType> = parent ?: this

        val f: Double
            get() = cost + heuristic

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
            return "Node: [State: $state, h: $heuristic, g: $cost, iteration: $iteration, actionCost: $actionCost, parent: ${parent.state}, open: $open]"
        }
    }

    private val logger = LoggerFactory.getLogger(LssLrtaStarPlanner::class.java)

    private var iterationCounter = 0L

    private val safeNodes = ArrayList<Node<StateType>>()

    private val depthBound: Int = configuration[Configurations.LOOKAHEAD_DEPTH_LIMIT] as? Int ?: throw MetronomeException("Lookahead depth limit not found")

    private val fValueComparator = Comparator<Node<StateType>> {lhs, rhs ->
        when {
            lhs.f < rhs.f -> -1
            lhs.f > rhs.f -> 1
            lhs.cost > rhs.cost -> -1 // break ties on g
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

    private val nodes: HashMap<StateType, Node<StateType>> = HashMap<StateType, Node<StateType>> (100000000, 1.toFloat()).resize()

    private var openList = AdvancedPriorityQueue<Node<StateType>>(100000000, fValueComparator)

    private var rootState: StateType? = null

    private var aStarPopCounter = 0
    private var dijkstraPopCounter = 0
    var aStarTimer = 0L
        get
    var dijkstraTimer = 0L
        get

    override fun selectAction(sourceState: StateType, terminationChecker: TerminationChecker): List<ActionBundle> {
        // first search iteration check


        if (rootState == null) {
            rootState = sourceState
        } else if (sourceState != rootState) {
            logger.debug {"Inconsistent world sourceState. Expected $rootState got $sourceState"}
        }

        if (domain.isGoal(sourceState)) {
            logger.warn {"selectAction: The goal sourceState is already found."}
        }

        logger.debug{ "Root sourceState: $sourceState" }

        // Every turn do k-breadth-first search to learn safe states
        // then A* until time expires

        val nodesGenerated = breadthFirstSearch(sourceState, terminationChecker, depthBound)

        if(versionNumber == SimpleSafeVersion.ONE) {
            resetSearchTree(nodesGenerated)
        }

//        logger.debug { "Last BFS node $lastBreadthFirstNode" }

        if (openList.isNotEmpty()) {
            dijkstraTimer += measureTimeMillis { dijkstra(terminationChecker) }
        }

        var plan: List<ActionBundle>? = null
        aStarTimer += measureTimeMillis {
            val targetNode = aStar(sourceState, terminationChecker)

            when (safetyBackup) {
                SimpleSafeSafetyBackup.PARENT -> throw MetronomeException("Invalid configuration. SimpleSafe does not implement the BEST_SAFE strategy")
                SimpleSafeSafetyBackup.PREDECESSOR -> predecessorSafetyPropagation(safeNodes)
            }

            val targetSafeNode = when (targetSelection) {
                SafeRealTimeSearchTargetSelection.SAFE_TO_BEST -> selectSafeToBest(openList)
                SafeRealTimeSearchTargetSelection.BEST_SAFE -> throw MetronomeException("Invalid configuration. SimpleSafe does not implement the BEST_SAFE strategy")
            }

            plan = extractSourceToTargetPath(targetSafeNode ?: targetNode, sourceState)
            rootState = targetNode.state
        }

        logger.debug { "AStar pops: $aStarPopCounter Dijkstra pops: $dijkstraPopCounter" }
        logger.debug { "AStar time: $aStarTimer Dijkstra time: $dijkstraTimer" }
        logger.debug { "Termination checker remaining resources: ${terminationChecker.remaining()}" }
        return plan!!
    }

    /**
     * Runs local breadth-first search then clears the search tree
     * except for the safe nodes we learn about up to a depth k
     */
    private fun breadthFirstSearch(state: StateType, terminationChecker: TerminationChecker, depthBound: Int): Queue<Node<StateType>> {
        val openListQueue = LinkedList<Node<StateType>>()
        val node = Node(state, nodes[state]?.heuristic ?: domain.heuristic(state), 0, 0, NoOperationAction, iterationCounter, null, false, 0)
        nodes[state] = node
        openListQueue.add(node)

        var currentIteration = 0
        logger.debug { "Starting BFS from state: $state" }

        while (!terminationChecker.reachedTermination() && currentIteration < depthBound) {
            openListQueue.peek()?.let {
                if (domain.isGoal(it.state)) return openListQueue
            } ?: throw GoalNotReachableException ("Open list is empty during k-BFS")

            val foundSafeNode = expandFromNode(openListQueue.pop()!!, openListQueue)
            terminationChecker.notifyExpansion()
            currentIteration = openListQueue.peek().depth

            if(versionNumber == SimpleSafeVersion.TWO) {
                if(foundSafeNode) {
                    currentIteration = depthBound + 1
                }
            }
        }

        openListQueue.peek()?.let {
            return openListQueue
        } ?: throw GoalNotReachableException("Open list is empty during k-BFS")
    }

    /**
     * Expands a node and add it to closed list, similar to expandFromNode
     * but uses the queue implementation being passed in, for each successor
     * it will add it to the open list passed and store its g value as long as the
     * state has not been seen before, or is found with a lower g value.
     */
    private fun expandFromNode(sourceNode: Node<StateType>, openListQueue: Queue<Node<StateType>>) : Boolean {
        expandedNodeCount++
        var foundSafeNode = false
        val currentGValue = sourceNode.cost
        for (successor in domain.successors(sourceNode.state)) {
            val successorState = successor.state
            logger.trace {"Considering successor $successorState" }

            val successorNode = getNode(sourceNode, successor)

            // update the node depth to be one mor than the parent
            successorNode.depth = sourceNode.depth + 1

            // do not need to worry about predecessors because we are dumping the nodes after
            // but care about safety still
            if (domain.isSafe(successorNode.state)) {
                safeNodes.add(successorNode)
                successorNode.safe = true
                foundSafeNode = true
            }

            if (successorNode.iteration != iterationCounter) {
                successorNode.apply {
                    iteration = iterationCounter
                    predecessors.clear()
                    cost = MAX_VALUE
                }
            }

            // skip if we circle back to the parent
            if (successorState == sourceNode.parent.state) {
                continue
            }

            val successorGValueFromCurrent = currentGValue + successor.actionCost
            // always add the successor doing a BFS
            successorNode.apply {
                cost = successorGValueFromCurrent
                parent = sourceNode
                action = successor.action
                actionCost = successor.actionCost
                depth = parent.depth + 1
            }

            logger.debug { "Expanding from $sourceNode -> $successorNode :: open list size ${openListQueue.size}" }
            logger.trace { "Adding it to the cost table with value ${successorNode.cost}" }
            // we always add the node doing a BFS
            openListQueue.add(successorNode)
        }
        return foundSafeNode
    }

    /**
     * Runs AStar until termination and returns the path to the head of openlist
     * Will just repeatedly expand according to A*.
     */
    private fun aStar(state: StateType, terminationChecker: TerminationChecker): Node<StateType> {
        // build the fabulous (and glorious) A* tree / essential
        initializeAStar()

        val node = Node(state, nodes[state]?.heuristic ?: domain.heuristic(state), 0 , 0, NoOperationAction, iterationCounter, null, false, 0)
        nodes[state] = node
        openList.add(node)
        logger.debug { "Starting A* from state: $state" }

        while (!terminationChecker.reachedTermination()) {
            aStarPopCounter++

            openList.peek()?.let {
                if (domain.isGoal(it.state)) return it
            } ?: throw GoalNotReachableException( "Open list is empty")

            expandFromNode(openList.pop()!!)
            terminationChecker.notifyExpansion()

        }
        return openList.peek() ?: throw GoalNotReachableException ("Open list is empty.")
    }

    private fun initializeAStar() {
        iterationCounter++
        openList.clear()
        safeNodes.clear()
        openList.reorder(fValueComparator)
    }

    private fun resetSearchTree(openListQueue: Queue<Node<StateType>>) {
        openList.clear()
        nodes.clear()
        openListQueue.clear()
        openList.reorder(fValueComparator)
    }

    /**
     * Expands a node and add it to the closed list. For each successor
     * it will add it to the open list and store it's g value, as long as the
     * state has not been seen before, or is found with a lower g value
     */
    private fun expandFromNode(sourceNode: Node<StateType>) {
        expandedNodeCount++

        val currentGValue = sourceNode.cost
        for (successor in domain.successors(sourceNode.state)) {
            val successorState = successor.state
            logger.trace {"Considering successor $successorState" }

            val successorNode = getNode(sourceNode, successor)
            successorNode.depth = sourceNode.depth + 1

            // safety check
            if (domain.isSafe(successorNode.state)) {
                safeNodes.add(successorNode)
                successorNode.safe = true
            }

            successorNode.predecessors.add(SearchEdge(node = sourceNode, action = successor.action, actionCost = successor.actionCost))

            // out dated nodes are updated
            if (successorNode.iteration != iterationCounter) {
                successorNode.apply {
                    iteration = iterationCounter
                    predecessors.clear()
                    cost = MAX_VALUE
                }
            }

            // skip is we got back to the parent
            if (successorState == sourceNode.parent.state) {
                continue
            }

            // only generated states that are not yet visited or whose cost values are lower than this path
            val successorGValueFromCurrent = currentGValue + successor.actionCost
            if (successorNode.cost > successorGValueFromCurrent) {
                successorNode.apply {
                    cost = successorGValueFromCurrent
                    parent = sourceNode
                    action = successor.action
                    actionCost = successor.actionCost
                    depth = parent.depth + 1
                }

                logger.debug { "Expanding from $sourceNode -> $successorNode :: open list size : ${openList.size}" }
                logger.trace { "Adding it the cost table with value ${successorNode.cost}" }

                if (!successorNode.open) {
                    openList.add(successorNode)
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
     * Get a node for the state if it exists, else create new node.
     *
     * @return node corresponding to the given state.
     */
    private fun getNode(parent: Node<StateType>, successor: SuccessorBundle<StateType>) : Node<StateType> {
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
                    iteration = iterationCounter,
                    depth = parent.depth + 1
            )

            nodes[successorState] = undiscoveredNode
            undiscoveredNode
        } else {
            parent.depth += 1
            tempSuccessorNode.depth = parent.depth + 1
            tempSuccessorNode
        }
    }

    /**
     * Performs Dikjatra until runs out of resources or done
     *
     * Updates the mode to SEARCH if done with DIJKSTRA
     *
     * Dijkstra updates repeatedly pop the state s according to their heuristic value, and then update
     * the cost values for all it's visited successors, based on the heuristic s.
     *
     * This increases the stored heuristic value, ensuring that A* won't go in circles, and in general generating
     * a better table of heuristics.
     */
    private fun dijkstra(terminationChecker: TerminationChecker) {
        logger.debug { "Start: Dijkstra" }

        iterationCounter++

        openList.reorder(heuristicComparator)

        while (!terminationChecker.reachedTermination() && openList.isNotEmpty()) {
            // check closed list
            val node = openList.pop() ?: throw GoalNotReachableException("Goal not reachable. Open list is empty.")
            node.iteration = iterationCounter

            val currentHeuristicValue = node.heuristic

            for ((predecessorNode, _, actionCost) in node.predecessors) {
                if (predecessorNode.iteration == iterationCounter && !predecessorNode.open) {
                    // the node was already learned and closed in current iteration
                    continue
                }

                val predecessorHeuristicValue = predecessorNode.heuristic

                if (!predecessorNode.open){
                    // node is not open yet because it was not visited in the current planning iteration

                    predecessorNode.heuristic = currentHeuristicValue + actionCost

                    assert(predecessorNode.iteration == iterationCounter - 1)

                    predecessorNode.iteration = iterationCounter
                    openList.add(predecessorNode)
                } else if (predecessorHeuristicValue > currentHeuristicValue + actionCost) {
                    predecessorNode.heuristic = currentHeuristicValue + actionCost
                    openList.update(predecessorNode)
                }
            }
        }
        if (openList.isEmpty()) {
            logger.debug { "Done with Dijkstra" }
        } else {
            logger.warn { "Incomplete learning step. Lists: Open(${openList.size})" }
        }
    }
}

enum class SimpleSafeConfiguration {
    SAFETY_BACKUP, SAFETY, VERSION
}

enum class SimpleSafeSafetyBackup {
    PARENT, PREDECESSOR
}

enum class SimpleSafeSafety {
    ABSOLUTE, PREFERRED
}

enum class SimpleSafeVersion {
    ONE, TWO
}