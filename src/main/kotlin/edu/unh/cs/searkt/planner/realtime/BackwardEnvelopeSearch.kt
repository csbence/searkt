package edu.unh.cs.searkt.planner.realtime

import edu.unh.cs.searkt.MetronomeException
import edu.unh.cs.searkt.environment.*
import edu.unh.cs.searkt.experiment.configuration.ExperimentConfiguration
import edu.unh.cs.searkt.experiment.result.ExperimentResult
import edu.unh.cs.searkt.experiment.terminationCheckers.TerminationChecker
import edu.unh.cs.searkt.experiment.terminationCheckers.getTerminationChecker
import edu.unh.cs.searkt.planner.*
import edu.unh.cs.searkt.planner.exception.GoalNotReachableException
import edu.unh.cs.searkt.util.AbstractAdvancedPriorityQueue
import edu.unh.cs.searkt.util.AdvancedPriorityQueue
import edu.unh.cs.searkt.util.resize
import kotlinx.serialization.ImplicitReflectionSerializer
import java.util.*
import kotlin.Long.Companion.MAX_VALUE

/**
 * Real time search algorithm which maintains an ever-expanding search envelope
 * and directs the agent toward the best node on the frontier
 *
 * Connects the agent to the frontier by conducting a backward search over multiple iterations. Directs the agent
 * locally using LSS-LRTA* when no other path exists
 *
 * @author Kevin C. Gall
 */
class BackwardEnvelopeSearch<StateType : State<StateType>>(override val domain: Domain<StateType>, val configuration: ExperimentConfiguration) :
        RealTimePlanner<StateType>(),
        RealTimePlannerContext<StateType, BackwardEnvelopeSearch.BackwardEnvelopeSearchNode<StateType>> {

    // keys
    private val ENVELOPE_RESETS = "envelopeResets"
    private val FORWARD_SEARCH_COUNTER = "forwardSearches"

    // Configuration
    private val weight = configuration.weight ?: 1.0
    private val lookaheadStrategy = configuration.lookaheadStrategy ?: LookaheadStrategy.A_STAR

    // Hard code expansion ratios while testing
    // r1 < 1.0
    private val initFrontierLimitRatio = 0.8
    private var frontierLimitRatio = initFrontierLimitRatio
    // r2 + r3 = 1.0
    private val backwardLimitRatio = 0.6
//    private val localLimitRatio = 0.4 // implied by above

    // TODO: Consider removing lss-specific props from RealTimeSearchNode and moving them into a new sub intf LocalSearchNode
    class BackwardEnvelopeSearchNode<StateType : State<StateType>>(
            override val state: StateType,
            override var heuristic: Double,
            override var iteration: Long,
            var envelopeVersion: Long,
            var pseudoG: Long
    ) : RealTimeSearchNode<StateType, BackwardEnvelopeSearchNode<StateType>> {
        override var index = -1 // required to implement RealTimeSearchNode

        /** Global predecessor and successor sets */
        var successors: MutableSet<BackSearchEdge<BackwardEnvelopeSearchNode<StateType>>> = mutableSetOf()
        var globalPredecessors: MutableSet<BackSearchEdge<BackwardEnvelopeSearchNode<StateType>>> = mutableSetOf()

        // Local Search properties. All initialized to default values
        override var cost: Long = -1
        override var actionCost: Long = -1
        override var action: Action = NoOperationAction
        override var parent: BackwardEnvelopeSearchNode<StateType> = this
        override var closed: Boolean = false
        override var lastLearnedHeuristic = heuristic
        override var minCostPathLength: Long = 0L
        override var predecessors: MutableList<SearchEdge<BackwardEnvelopeSearchNode<StateType>>> = arrayListOf()


        // Envelope-specific props
        var frontierOpenIndex = -1
        var frontierClosed = false

        // Properties for backward search
        var backwardOpenIndex = -1
        var backwardG = 0L
        var backwardH = Double.POSITIVE_INFINITY
        var backwardSearchIteration = -1L
        var backwardParent: BackSearchEdge<BackwardEnvelopeSearchNode<StateType>>? = null

        override fun hashCode(): Int = state.hashCode()

        override fun equals(other: Any?): Boolean = when (other) {
            null -> false
            is SearchNode<*, *> -> state == other.state
            is State<*> -> state == other
            else -> false
        }

        override fun toString() =
                "BackESNode: [State: $state h: $heuristic, g: $cost, iteration: $iteration, actionCost: $actionCost, parent: ${parent.state}, open: $open]"

        fun forEachSuccessor(domain: Domain<StateType>, fn: (SuccessorBundle<StateType>) -> Unit) {
            domain.successors(state).forEach(fn)
        }

    }

    data class BackSearchEdge<out Node>(val predecessor: Node, val successor: Node, val action: Action, val actionCost: Long) {
        override fun hashCode(): Int = (Math.pow(predecessor.hashCode().toDouble(), 7.0) + (successor.hashCode() * 31)).toInt()

        override fun equals(other: Any?): Boolean = when (other) {
            null -> false
            is BackSearchEdge<*> -> predecessor == other.predecessor && successor == other.successor && action == other.action && actionCost == other.actionCost
            else -> false
        }

        companion object {
            fun <Node>fromSearchEdge(successor: Node, edge: SearchEdge<Node>): BackSearchEdge<Node> =
                    BackSearchEdge(edge.node, successor, edge.action, edge.actionCost)
        }
    }

    /* COMPARATORS */

    private val pseudoFComparator = Comparator<BackwardEnvelopeSearchNode<StateType>> { lhs, rhs ->
        val lhsPseudoF = (lhs.heuristic * weight) + lhs.pseudoG
        val rhsPseudoF = (rhs.heuristic * weight) + rhs.pseudoG

        //break ties on lower H -> this is better info!
        when {
            lhsPseudoF < rhsPseudoF -> -1
            lhsPseudoF > rhsPseudoF -> 1
            lhs.heuristic < rhs.heuristic -> -1
            lhs.heuristic > rhs.heuristic -> 1
            else -> 0
        }
    }

    private val backwardsComparator = Comparator<BackwardEnvelopeSearchNode<StateType>> { lhs, rhs ->
        val lhsF = lhs.backwardG + (lhs.backwardH * weight)
        val rhsF = rhs.backwardG + (rhs.backwardH * weight)

        // G is relevant, so we can break ties on higher G
        when {
            lhsF < rhsF -> -1
            lhsF > rhsF -> 1
            lhs.backwardG > rhs.backwardG -> -1
            lhs.backwardG < rhs.backwardG -> 1
            else -> 0
        }
    }

    private val greedyBackwardsComparator = Comparator<BackwardEnvelopeSearchNode<StateType>> { lhs, rhs ->
        // Still break ties on G
        when {
            lhs.backwardH < rhs.backwardH -> -1
            lhs.backwardH > rhs.backwardH -> 1
            lhs.backwardG > rhs.backwardG -> -1
            lhs.backwardG < rhs.backwardG -> 1
            else -> 0
        }
    }

    /* SPECIALIZED PRIORITY QUEUES */

    inner class FrontierOpenList : AbstractAdvancedPriorityQueue<BackwardEnvelopeSearchNode<StateType>>(arrayOfNulls(1000000), pseudoFComparator) {
        override fun getIndex(item: BackwardEnvelopeSearchNode<StateType>): Int = item.frontierOpenIndex
        override fun setIndex(item: BackwardEnvelopeSearchNode<StateType>, index: Int) {
            item.frontierOpenIndex = index
        }

        override fun isClosed(item: BackwardEnvelopeSearchNode<StateType>): Boolean = item.frontierClosed
        override fun setClosed(item: BackwardEnvelopeSearchNode<StateType>, newValue: Boolean) {
            item.frontierClosed = newValue
        }
    }

    inner class BackwardOpenList : AbstractAdvancedPriorityQueue<BackwardEnvelopeSearchNode<StateType>>(arrayOfNulls(1000000), backwardsComparator) {
        override fun getIndex(item: BackwardEnvelopeSearchNode<StateType>): Int = item.backwardOpenIndex
        override fun setIndex(item: BackwardEnvelopeSearchNode<StateType>, index: Int) {
            item.backwardOpenIndex = index
        }

        override fun isClosed(item: BackwardEnvelopeSearchNode<StateType>): Boolean = false
        override fun setClosed(item: BackwardEnvelopeSearchNode<StateType>, newValue: Boolean) {} // no op
    }

    private var frontierOpenList = FrontierOpenList()
    private var backwardOpenList = BackwardOpenList()

    // Main Node-container data structures
    private val nodes: HashMap<StateType, BackwardEnvelopeSearchNode<StateType>> = HashMap<StateType, BackwardEnvelopeSearchNode<StateType>>(100_000_000, 1.toFloat()).resize()
    override var openList = AdvancedPriorityQueue<BackwardEnvelopeSearchNode<StateType>>(1000000, fValueComparator)

    // Current and discovered states
    private var rootState: StateType? = null
    private lateinit var currentAgentState: StateType
    private lateinit var currentAgentNode: BackwardEnvelopeSearchNode<StateType>
    private val foundGoals = mutableSetOf<BackwardEnvelopeSearchNode<StateType>>()
    private var firstDiscoveredGoal: BackwardEnvelopeSearchNode<StateType>? = null
    private var frontierTarget: BackwardEnvelopeSearchNode<StateType>? = null

    /* State of the algorithm */
    // Counters
    override var iterationCounter = 0L // used by local search
    var backwardIterationCounter = 0L // used by backward search
    var globalIterationCounter = 0L
    var envelopeVersionCounter = 0L
    // resource leftover
    var timeRemaining = 0L


    // Path Caching
    private var pathStates = mutableSetOf<StateType>()
    // The node in the edge is the successor achieved when executing the action
    private var cachedPath = LinkedList<BackSearchEdge<BackwardEnvelopeSearchNode<StateType>>>()


    /**
     * "Prime" the nodes hashmap. Necessary for real time bounds to avoid hashmap startup costs
     */
    override fun init(initialState: StateType) {
        val primer = BackwardEnvelopeSearchNode(initialState,
                0.0, 0L, 0L, 0L)
        nodes[initialState] = primer
        nodes.remove(initialState)

        // resort open lists for greedy strategy
        if (lookaheadStrategy == LookaheadStrategy.GBFS) {
            backwardOpenList.reorder(greedyBackwardsComparator)
            frontierOpenList.reorder(heuristicComparator)
        }
    }

    @ImplicitReflectionSerializer
    override fun appendPlannerSpecificResults(results: ExperimentResult) {
        results.attributes["forwardSearches"] = this.counters[this.FORWARD_SEARCH_COUNTER] ?: 0
        results.attributes["envelopeResets"] = this.counters[this.ENVELOPE_RESETS] ?: 0
    }

    override fun selectAction(sourceState: StateType, terminationChecker: TerminationChecker): List<ActionBundle> {
        if (rootState == null) {
            rootState = sourceState

            val node = BackwardEnvelopeSearchNode(sourceState, domain.heuristic(sourceState), -1, 0L, 0)
            nodes[sourceState] = node

            frontierOpenList.add(node)
            generatedNodeCount++ // the first node
        }

        currentAgentState = sourceState
        currentAgentNode = nodes[currentAgentState ]!!

        if (domain.isGoal(sourceState)) {
            return emptyList()
        }

        val frontierTimeSlice = (terminationChecker.remaining() * frontierLimitRatio).toLong()
        // adds carry-over from prior iteration to path selection time
        val pathSelectionTimeSlice = (terminationChecker.remaining() - frontierTimeSlice) + timeRemaining

        var backwardTimeSlice = (pathSelectionTimeSlice * backwardLimitRatio).toLong()
        var localTimeSlice = pathSelectionTimeSlice - backwardTimeSlice
        // adjust based on whether or not we have a cached path
        if (cachedPath.size > 0) {
            backwardTimeSlice += localTimeSlice
            localTimeSlice = 0
        }

        // Expand the frontier
        val bestCurrentNode = explore(getTerminationChecker(configuration, frontierTimeSlice))

        // backward search tries to connect a path to the agent
        val backwardTerminationChecker = getTerminationChecker(configuration, backwardTimeSlice)
        backwardSearch(bestCurrentNode, backwardTerminationChecker)

        timeRemaining = backwardTerminationChecker.remaining()

        // local search kicks in if we don't have a path yet - generates one with limited knowledge
        timeRemaining += if (cachedPath.size == 0) {
            val localTerminationChecker = getTerminationChecker(configuration, localTimeSlice)
            localSearch(currentAgentNode, localTerminationChecker)

            localTerminationChecker.remaining()
        } else {
            localTimeSlice
        }

        val nextEdge = cachedPath.removeFirst()
        pathStates.remove(sourceState)

        globalIterationCounter++
        return listOf(ActionBundle(nextEdge.action, nextEdge.actionCost))
    }

    /**
     * Explore the state space by expanding the frontier
     */
    private fun explore(terminationChecker: TerminationChecker): BackwardEnvelopeSearchNode<StateType> {
        while (!terminationChecker.reachedTermination()) {
            // expand from frontier open list

            // first handle empty open list
            if (frontierOpenList.isEmpty()) {
                if (foundGoals.isEmpty()) throw GoalNotReachableException("Frontier is empty without finding any goals")

                return firstDiscoveredGoal ?: throw MetronomeException("Goals found, but first discovered not set")
            }

            val currentNode = if (frontierOpenList.isOpen(currentAgentNode)) {
                frontierOpenList.remove(currentAgentNode)
                currentAgentNode
            } else {
                frontierOpenList.pop()!!
            }

            if (domain.isGoal(currentNode.state)) {
                if (foundGoals.isEmpty()) {
                    frontierLimitRatio = 0.1 // reduce the amount of time spent expanding the frontier starting next iteration
                }

                if (!foundGoals.contains(currentNode)) {
                    foundGoals.add(currentNode)
                }
                continue // we do not need to explore beyond a goal state
            }

            domain.successors(currentNode.state).forEach { successor ->
                val successorNode = getNode(currentNode, successor)
                if (successorNode.envelopeVersion != currentNode.envelopeVersion) {
                    successorNode.envelopeVersion = currentNode.envelopeVersion
                    successorNode.frontierClosed = false
                    successorNode.pseudoG = domain.heuristic(currentAgentState, successorNode.state).toLong()
                }

                // detect if we've seen this node before - no double expansions
                if (!successorNode.frontierClosed && !frontierOpenList.isOpen(successorNode)) {
                    frontierOpenList.add(successorNode)
                }
            }

            currentNode.frontierClosed = true
            terminationChecker.notifyExpansion()
            expandedNodeCount++
        }

        return frontierOpenList.peek() ?:
            if (foundGoals.isEmpty()) throw GoalNotReachableException("Open list is empty.") else foundGoals.first()

    }

    /**
     * Search backward from the best frontier node (or goals) toward the agent
     *
     * Note: We are not recalculating the heuristic between iterations. G is not admissible!
     * TODO: Consider D* Lite style min queue
     */
    private fun backwardSearch(seed: BackwardEnvelopeSearchNode<StateType>, terminationChecker: TerminationChecker) {
        // re-seed backward search if we're not in the middle of a search
        if (backwardOpenList.isEmpty()) {
            backwardIterationCounter++ // invalidate past iteration
            if (foundGoals.size > 0) {
                foundGoals.forEach { node ->
                    node.backwardSearchIteration = backwardIterationCounter
                    node.backwardG = 0
                    node.backwardH = domain.heuristic(currentAgentState, node.state)
                    node.backwardParent = null
                    backwardOpenList.add(node)
                }
                frontierTarget = foundGoals.first()
            } else {
                seed.backwardSearchIteration = backwardIterationCounter
                seed.backwardG = 0
                seed.backwardH = domain.heuristic(currentAgentState, seed.state)
                seed.backwardParent = null

                frontierTarget = seed
                backwardOpenList.add(seed)
            }
        }

        // search into envelope via already-generated predecessors
        backSearch@while (!terminationChecker.reachedTermination()) {
            if (backwardOpenList.isEmpty()) {
                resetEnvelope()
                break // cut this iteration short
            }
            val currentNode = backwardOpenList.pop()!!

            for (edge in currentNode.globalPredecessors) {
                val predecessorNode = edge.predecessor

                if (predecessorNode.backwardSearchIteration != currentNode.backwardSearchIteration) {
                    predecessorNode.backwardSearchIteration = currentNode.backwardSearchIteration
                    predecessorNode.backwardG = MAX_VALUE
                    predecessorNode.backwardH = domain.heuristic(currentAgentState, predecessorNode.state) * weight
                }

                val newG = currentNode.backwardG + edge.actionCost
                if (predecessorNode.backwardG > newG) {
                    predecessorNode.backwardParent = edge
                    predecessorNode.backwardG = newG

                    // Performing goal condition check now. Insight is that it's better for us to have
                    // a path than to have an "optimal" path since optimality is sacrificed anyway
                    if (predecessorNode.state == currentAgentState || pathStates.contains(predecessorNode.state)) {
                        // allowing path extraction to take place from current agent node
                        extractBackwardPath() // sets cached Path and path state set

                        break@backSearch
                    } else if (backwardOpenList.isOpen(predecessorNode)) {
                        backwardOpenList.update(predecessorNode)
                    } else {
                        backwardOpenList.add(predecessorNode)
                    }
                }
            }

            // TODO: Is this a full expansion op?
            terminationChecker.notifyExpansion()
            expandedNodeCount++
        }
    }

    /**
     * Extracts path from the given node to the target frontier node (or a goal)
     */
    private fun extractBackwardPath(startState: StateType = currentAgentState,
                                    stateSet: MutableSet<StateType> = mutableSetOf(),
                                    pathList: LinkedList<BackSearchEdge<BackwardEnvelopeSearchNode<StateType>>> = LinkedList()) {
        var currentNode = nodes[startState]!!
        while (currentNode != frontierTarget && !foundGoals.contains(currentNode)) {
            if (!stateSet.add(currentNode.state)) {
                throw MetronomeException("Cycle detected in extractBackwardPath")
            }
            pathList.add(currentNode.backwardParent ?: throw MetronomeException("Attempt to extract path failed"))

            currentNode = currentNode.backwardParent!!.successor
        }
        if (!stateSet.add(currentNode.state)) throw MetronomeException("Cycle detected in extractBackwardPath")

//        println("""Backtrack path size: ${pathList.size}""")

        clearBackwardSearch()
        pathStates = stateSet
        cachedPath = pathList
    }

    private fun clearBackwardSearch() {
        backwardOpenList.clear()
        frontierTarget = null
    }

    /**
     * Search forward from agent's current state to produce a path
     * Local search is used when we do not have a cached path
     *
     * Technique used is LSS-LRTA*, but this could theoretically be any LSS algorithm
     */
    private fun localSearch(root: BackwardEnvelopeSearchNode<StateType>, terminationChecker: TerminationChecker) {
        this.incrementCounter(FORWARD_SEARCH_COUNTER)

        iterationCounter++
        openList.clear()
        openList.reorder(fValueComparator)
        openList.add(root)

        root.parent = root
        root.iteration = iterationCounter
        root.cost = 0

        extractForwardPath(aStar(terminationChecker))

        // updates iteration counter and learns
        dijkstra(this)
    }

    /**
     * Returns target node
     */
    private fun aStar(terminationChecker: TerminationChecker):BackwardEnvelopeSearchNode<StateType> {
        while (!terminationChecker.reachedTermination()) {
            val currentNode = openList.pop()
                    ?: throw GoalNotReachableException("Local search terminated in dead end")

            if (domain.isGoal(currentNode.state) || currentNode.backwardSearchIteration == backwardIterationCounter) {
                return currentNode
            }

            // Make sure that if we are exploring the frontier, open lists are appropriately modified
            if (frontierOpenList.isOpen(currentNode)) {
                frontierOpenList.remove(currentNode)
            }
            expandFromNode(this, currentNode,  {
                if (it.envelopeVersion != envelopeVersionCounter ||
                        (!it.frontierClosed && !frontierOpenList.isOpen(it))) {
                    frontierOpenList.add(it)
                }
            })
            terminationChecker.notifyExpansion()
        }

        return openList.peek() ?: throw GoalNotReachableException("Local search open list empty")
    }

    /**
     * Extracts a path from the current agent state to the target using parent pointers.
     * Sets the "backwardParent" edge along the way to allow the backward search to use it for path extraction
     *
     * If it is detected that the target is on a backward search, extracts the full path to the frontier
     */
    private fun extractForwardPath(target: BackwardEnvelopeSearchNode<StateType>) {
        pathStates = mutableSetOf()
        cachedPath = LinkedList()

        var currentNode = target

        while (currentNode.state != currentAgentState) {
            val edge = BackSearchEdge.fromSearchEdge(currentNode,
                    currentNode.predecessors.find { pred -> pred.node == currentNode.parent }
                            ?: throw MetronomeException("Parent not among predecessors"))

            currentNode.parent.backwardParent = edge
            cachedPath.addFirst(edge)
            pathStates.add(currentNode.parent.state)

            currentNode = currentNode.parent
        }

        if (target.backwardSearchIteration == backwardIterationCounter) {
            extractBackwardPath(target.state, pathStates, cachedPath)
        } else {
            pathStates.add(target.state)
        }
    }

    private fun resetEnvelope() {
        this.incrementCounter(ENVELOPE_RESETS)

        clearBackwardSearch()

        val target = if (cachedPath.size > 0) {
            cachedPath.last
        } else {
            val nextEdge = nodes[currentAgentState]!!.successors.minBy{ it.successor.heuristic }

            pathStates = mutableSetOf(nextEdge!!.predecessor.state, nextEdge.successor.state)
            cachedPath = LinkedList()
            cachedPath.add(nextEdge)

            nextEdge
        }

        // reset frontier
        frontierOpenList.clear()
        envelopeVersionCounter++

        target.successor.frontierClosed = false
        target.successor.envelopeVersion = envelopeVersionCounter
        target.successor.pseudoG = 0
        frontierOpenList.add(target.successor)

        frontierLimitRatio = initFrontierLimitRatio // reset in case a goal had been found and this was changed
        foundGoals.clear() // sad face
    }

    /**
     * Get a node for the state if exists, else create a new node.
     * Populate global successor and predecessor sets as necessary
     *
     * pseudoG set at time of node generation and not changed after. This is to maintain some kind of invariant
     * in the min heap because otherwise pseudoG changes with every iteration and thus destroys properties of
     * minimum priority queue.
     *
     * @return node corresponding to the given state.
     */
    override fun getNode(parent: BackwardEnvelopeSearchNode<StateType>, successor: SuccessorBundle<StateType>): BackwardEnvelopeSearchNode<StateType> {
        val successorState = successor.state
        val tempSuccessorNode = nodes[successorState]

        val successorNode = if (tempSuccessorNode == null) {
            generatedNodeCount++

            val undiscoveredNode = BackwardEnvelopeSearchNode(
                    state = successorState,
                    heuristic = domain.heuristic(successorState),
                    iteration = -1, // using local search iteration counter
                    envelopeVersion = envelopeVersionCounter,
                    pseudoG = domain.heuristic(currentAgentState, successorState).toLong()
            )

            nodes[successorState] = undiscoveredNode

            undiscoveredNode
        } else {
            tempSuccessorNode
        }

        val edge = BackSearchEdge(predecessor = parent, successor = successorNode, action = successor.action, actionCost = successor.actionCost.toLong())

        // add to parent's successor set
        parent.successors.add(edge)
        // add parent to successor's predecessors set
        successorNode.globalPredecessors.add(edge)

        return successorNode
    }


    @Suppress("UNUSED_PARAMETER")
    private fun printMessage(msg: String) = 0//println(msg)
}


