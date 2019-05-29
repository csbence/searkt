package edu.unh.cs.searkt.planner.realtime

import edu.unh.cs.searkt.MetronomeException
import edu.unh.cs.searkt.environment.*
import edu.unh.cs.searkt.experiment.configuration.ExperimentConfiguration
import edu.unh.cs.searkt.experiment.terminationCheckers.TerminationChecker
import edu.unh.cs.searkt.experiment.terminationCheckers.getTerminationChecker
import edu.unh.cs.searkt.planner.*
import edu.unh.cs.searkt.planner.exception.GoalNotReachableException
import edu.unh.cs.searkt.util.AbstractAdvancedPriorityQueue
import edu.unh.cs.searkt.util.AdvancedPriorityQueue
import edu.unh.cs.searkt.util.resize
import java.util.*
import kotlin.Long.Companion.MAX_VALUE

/**
 * Real time search algorithm which maintains an ever-expanding search envelope
 * and directs the agent toward the best node on the frontier
 *
 * Connects the agent to the frontier by conducting a bidirectional search over multiple iterations. Conducts a forward
 * search from the end of the agent's current committed path and a backward search from the best frontier node. Once the
 * two meet, the resulting path is the new committed path
 *
 * Notes:
 * TODO: Remove this section for brevity once algorithm is implemented
 * - Algorithm is actually very similar to TBA*, but removes dependency on irrelevant "root" node by using backward
 *      searches
 * - Once a goal is found, spend more time searching backward than expanding the frontier
 * - If the backward search open list empties, throw out the envelope (?)
 *   + Alternative - we save backward search iterations that emptied the open list and avoid re-expanding those nodes.
 *      They are effectively pruned from the envelope search graph.
 * - If the agent reaches the end of the path, follow parent pointers to the best node on the forward open
 *      list to produce a "temporary" path and clear the forward open list
 *   + This is not a real-time op. Consider making this an "anytime" operation where the first forward search is always
 *          guaranteed to be able to backtrace to the root of the forward search
 * - If forward search reaches the frontier... end search and pick a new target?
 *
 * @author Kevin C. Gall
 */
class BidirectionalEnvelopeSearch<StateType : State<StateType>>(override val domain: Domain<StateType>, val configuration: ExperimentConfiguration) :
        RealTimePlanner<StateType>(),
        RealTimePlannerContext<StateType, BidirectionalEnvelopeSearch.BiEnvelopeSearchNode<StateType>> {

    // Configuration
    private val weight = configuration.weight ?: 1.0

    // Hard code expansion ratios while testing
    // r1 < 1.0
    private var frontierLimitRatio = 0.5

    // TODO: Configuration for traceback accounting

    class BiEnvelopeSearchNode<StateType : State<StateType>>(
            override val state: StateType,
            override var heuristic: Double,
            override var iteration: Long,
            var pseudoG: Long // for frontier using pseudoF
    ) : RealTimeSearchNode<StateType, BiEnvelopeSearchNode<StateType>> {
        // required to implement interface
        override var parent: BiEnvelopeSearchNode<StateType> = this

        /* Global predecessor and successor sets
         * Necessary to distinguish from predecessor list as required by search node interface
         */
        var globalSuccessors: MutableSet<BiSearchEdge<BiEnvelopeSearchNode<StateType>>> = mutableSetOf()

        var globalPredecessors: MutableSet<BiSearchEdge<BiEnvelopeSearchNode<StateType>>> = mutableSetOf()
        // Envelope-specific props
        var frontierOpenIndex = -1

        var frontierClosed = false
        // Forward Search Properties
        override var index = -1
        override var cost: Long = -1
        override var actionCost: Long = -1
        override var action: Action = NoOperationAction
        override var closed: Boolean = false
        var forwardParent: BiSearchEdge<BiEnvelopeSearchNode<StateType>>? = null

        // Properties for backward search
        var backwardOpenIndex = -1
        var backwardClosed = false
        var backwardG = 0L
        var backwardH = Double.POSITIVE_INFINITY
        var backwardParent: BiSearchEdge<BiEnvelopeSearchNode<StateType>>? = null


        var biSearchIteration = -1L

        override fun hashCode(): Int = state.hashCode()

        override fun equals(other: Any?): Boolean = when (other) {
            null -> false
            is SearchNode<*, *> -> state == other.state
            is State<*> -> state == other
            else -> false
        }

        override fun toString() =
                "BiESNode: [State: $state h: $heuristic, g: $cost, iteration: $iteration, actionCost: $actionCost, parent: ${parent.state}, open: $open]"

        // Unused properties.. required to implement interface
        override var predecessors: MutableList<SearchEdge<BiEnvelopeSearchNode<StateType>>> = arrayListOf()
        override var lastLearnedHeuristic = heuristic
        override var minCostPathLength: Long = 0L

    }

    data class BiSearchEdge<out Node>(val predecessor: Node, val successor: Node, val action: Action, val actionCost: Long) {
        override fun hashCode(): Int = (Math.pow(predecessor.hashCode().toDouble(), 7.0) + (successor.hashCode() * 31)).toInt()

        override fun equals(other: Any?): Boolean = when (other) {
            null -> false
            is BiSearchEdge<*> -> predecessor == other.predecessor && successor == other.successor && action == other.action && actionCost == other.actionCost
            else -> false
        }

        companion object {
            fun <Node>fromSearchEdge(successor: Node, edge: SearchEdge<Node>): BiSearchEdge<Node> =
                    BiSearchEdge(edge.node, successor, edge.action, edge.actionCost)
        }
    }

    /* COMPARATORS */

    private val pseudoFComparator = Comparator<BiEnvelopeSearchNode<StateType>> { lhs, rhs ->
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

    private val backwardsComparator = Comparator<BiEnvelopeSearchNode<StateType>> { lhs, rhs ->
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

    /* SPECIALIZED PRIORITY QUEUES */

    inner class FrontierOpenList : AbstractAdvancedPriorityQueue<BiEnvelopeSearchNode<StateType>>(arrayOfNulls(1000000), pseudoFComparator) {
        override fun getIndex(item: BiEnvelopeSearchNode<StateType>): Int = item.frontierOpenIndex
        override fun setIndex(item: BiEnvelopeSearchNode<StateType>, index: Int) {
            item.frontierOpenIndex = index
        }
        fun isOpen(item: BiEnvelopeSearchNode<StateType>): Boolean = item.frontierOpenIndex > -1
    }

    inner class BackwardOpenList : AbstractAdvancedPriorityQueue<BiEnvelopeSearchNode<StateType>>(arrayOfNulls(1000000), backwardsComparator) {
        override fun getIndex(item: BiEnvelopeSearchNode<StateType>): Int = item.backwardOpenIndex
        override fun setIndex(item: BiEnvelopeSearchNode<StateType>, index: Int) {
            item.backwardOpenIndex = index
        }
        fun isOpen(item: BiEnvelopeSearchNode<StateType>): Boolean = item.backwardOpenIndex > -1
    }

    // Open lists
    private var frontierOpenList = FrontierOpenList()
    private var backwardOpenList = BackwardOpenList()
    override var openList = AdvancedPriorityQueue<BiEnvelopeSearchNode<StateType>>(1000000, fValueComparator)

    // Main Node-container data structures
    private val nodes: HashMap<StateType, BiEnvelopeSearchNode<StateType>> = HashMap<StateType, BiEnvelopeSearchNode<StateType>>(100_000_000, 1.toFloat()).resize()

    // Current and discovered states
    private var rootState: StateType? = null
    private lateinit var currentAgentState: StateType
    private var targetGoal: BiEnvelopeSearchNode<StateType>? = null
    private val foundGoals = mutableSetOf<BiEnvelopeSearchNode<StateType>>()
    private var frontierTarget: BiEnvelopeSearchNode<StateType>? = null
    private var forwardRoot: BiEnvelopeSearchNode<StateType>? = null

    /* State of the algorithm */
    // Counters
    override var iterationCounter = 0L // used by global context
    var bidirectionalIterationCounter = 0L // used by bidirectional search
    // resource trackers
    var expansionLimit = 0L // used for traceback accounting in EXPANSION termination type contexts
    var timeRemaining = 0L


    // Path Caching
    private var pathStates = mutableSetOf<StateType>()
    private var cachedPath = LinkedList<BiSearchEdge<BiEnvelopeSearchNode<StateType>>>()


    /**
     * "Prime" the nodes hashmap. Necessary for real time bounds to avoid hashmap startup costs
     */
    override fun init(initialState: StateType) {
        val primer = BiEnvelopeSearchNode(initialState,
                0.0, 0L, 0L)
        nodes[initialState] = primer
        nodes.remove(initialState)
    }

    override fun selectAction(sourceState: StateType, terminationChecker: TerminationChecker): List<ActionBundle> {
        if (rootState == null) {
            rootState = sourceState

            val node = BiEnvelopeSearchNode(sourceState, domain.heuristic(sourceState), iterationCounter, 0)
            nodes[sourceState] = node

            frontierOpenList.add(node)
            generatedNodeCount++ // the first node
        }

        currentAgentState = sourceState
        expansionLimit = terminationChecker.remaining()

        if (domain.isGoal(sourceState)) {
            return emptyList()
        }

        val frontierTimeSlice = (terminationChecker.remaining() * frontierLimitRatio).toLong()
        // adds carry-over from prior iteration to path selection time
        val biSearchTimeSlice = (terminationChecker.remaining() - frontierTimeSlice) + timeRemaining

        // Expand the frontier
        val bestCurrentNode = explore(getTerminationChecker(configuration, frontierTimeSlice))


        // Alternating bidirectional search tries to connect a path to the agent
        val biSearchTerminationChecker = getTerminationChecker(configuration, biSearchTimeSlice)
        val forwardSeed = if(cachedPath.size > 0) {
            cachedPath.last.successor
        } else {
            nodes[currentAgentState] ?: throw MetronomeException("Agent is outside envelope")
        }
        bidirectionalSearch(forwardSeed, bestCurrentNode, biSearchTerminationChecker)

        timeRemaining = biSearchTerminationChecker.remaining()

        // TODO: We have not guaranteed a path yet! Make sure something happens when the bi-search hasn't finished
        if (cachedPath.size == 0) throw MetronomeException("No path to follow")
        val nextEdge = cachedPath.removeFirst()
        pathStates.remove(sourceState)

        iterationCounter++
        return listOf(ActionBundle(nextEdge.action, nextEdge.actionCost))
    }

    /**
     * Explore the state space by expanding the frontier
     */
    private fun explore(terminationChecker: TerminationChecker): BiEnvelopeSearchNode<StateType> {
        while (!terminationChecker.reachedTermination()) {
            // expand from frontier open list

            // first handle empty open list
            if (frontierOpenList.isEmpty()) {
                if (foundGoals.isEmpty()) throw GoalNotReachableException("Frontier is empty without finding any goals")

                return targetGoal ?: throw MetronomeException("Goals found, but target not set")
            }

            val currentNode = frontierOpenList.pop()!!
            if (domain.isGoal(currentNode.state)) {
                if (foundGoals.size == 0) {
                    frontierLimitRatio = 0.1 // reduce the amount of time spent expanding the frontier starting next iteration
                }

                foundGoals.add(currentNode)
                continue // we do not need to explore beyond a goal state
            }

            domain.successors(currentNode.state).forEach { successor ->
                val successorNode = getNode(currentNode, successor)

                // detect if we've seen this node before - no double expansions
                if (!successorNode.frontierClosed && !frontierOpenList.isOpen(successorNode)) {
                    frontierOpenList.add(successorNode)
                }
            }

            currentNode.frontierClosed = true
            terminationChecker.notifyExpansion()
            expandedNodeCount++
        }

        return frontierOpenList.peek() ?: foundGoals.first() ?:
        throw GoalNotReachableException("Open list is empty.")
    }

    /**
     * Search backward from the best frontier node (or goals) toward the agent
     */
    private fun bidirectionalSearch(forwardSeed: BiEnvelopeSearchNode<StateType>,
                                    frontierSeed: BiEnvelopeSearchNode<StateType>,
                                    terminationChecker: TerminationChecker) {
        // re-initialize search constructs if we're not in the middle of a search
        if (backwardOpenList.isEmpty() && openList.isEmpty()) {
            initSearch(forwardSeed, frontierSeed)
        }

        // search into envelope via already-generated predecessors
        var searchIntersection: BiEnvelopeSearchNode<StateType>? = null

        // TODO: We straight-up copy paste the search logic for forward vs backward. Consolidate this!
        try {
            biSearch@while (!terminationChecker.reachedTermination()) {
                // Backward expansion
                val backwardNode = backwardOpenList.pop() ?:
                throw BiSearchOpenEmptyException("Backward Search Open list is empty without finding the Agent")

                for (edge in backwardNode.globalPredecessors) {
                    val predecessorNode = edge.predecessor

                    if (predecessorNode.biSearchIteration != backwardNode.biSearchIteration) {
                        predecessorNode.biSearchIteration = backwardNode.biSearchIteration
                        predecessorNode.backwardG = MAX_VALUE
                        predecessorNode.backwardClosed = false
                        predecessorNode.backwardH = domain.heuristic(forwardRoot?.state ?: throw MetronomeException("No forward target set"),
                                predecessorNode.state) * weight
                    }

                    val newG = backwardNode.backwardG + edge.actionCost
                    if (predecessorNode.backwardG > newG) {
                        predecessorNode.backwardG = newG
                        predecessorNode.backwardParent = edge

                        // Performing goal condition check now. Insight is that it's better for us to have
                        // a path than to have an "optimal" path since optimality isn't guaranteed by the algorithm anyway
                        if (predecessorNode.open) { // on forward frontier
                            searchIntersection = predecessorNode

                            break@biSearch
                        } else if (backwardOpenList.isOpen(predecessorNode)) {
                            backwardOpenList.update(predecessorNode)
                        } else if (!predecessorNode.backwardClosed){
                            backwardOpenList.add(predecessorNode)
                        }
                    }
                }
                backwardNode.backwardClosed = true

                terminationChecker.notifyExpansion()
                expandedNodeCount++

                if (terminationChecker.reachedTermination()) break // don't cheat!

                // Forward Expansion
                val forwardNode = openList.pop() ?:
                throw BiSearchOpenEmptyException("Forward Search Open list is empty without finding the frontier / goal")

                for (edge in forwardNode.globalSuccessors) {
                    val successorNode = edge.successor

                    if (successorNode.biSearchIteration != forwardNode.biSearchIteration) {
                        successorNode.biSearchIteration = forwardNode.biSearchIteration
                        successorNode.cost = MAX_VALUE
                        successorNode.closed = false
                        successorNode.heuristic = domain.heuristic(successorNode.state,
                                frontierTarget?.state ?: throw MetronomeException("No frontier target set")) * weight
                    }

                    val newG = forwardNode.cost + edge.actionCost
                    if (successorNode.cost > newG) {
                        successorNode.cost = newG
                        successorNode.forwardParent = edge

                        // Perform goal check now for same reason
                        if (backwardOpenList.isOpen(successorNode)) {
                            searchIntersection = successorNode

                            break@biSearch
                        } else if (successorNode.open) {
                            openList.update(successorNode)
                        } else if (!successorNode.closed){
                            openList.add(successorNode)
                        }
                    }
                }
                terminationChecker.notifyExpansion()
                expandedNodeCount++
            }
        } catch (ex: BiSearchOpenEmptyException) {
            // TODO: Perform ops for when the an open list is empty - either restarting envelope or otherwise
        }

        if (searchIntersection != null) extractPath(searchIntersection) // sets cached Path and path state set
    }

    private fun initSearch(forwardSeed: BiEnvelopeSearchNode<StateType>,
                           frontierSeed: BiEnvelopeSearchNode<StateType>) {
        if (forwardSeed == frontierSeed) throw MetronomeException("Bidirectional search roots are the same")

        if (foundGoals.size > 0) {
            foundGoals.forEach { node ->
                node.biSearchIteration = bidirectionalIterationCounter
                node.backwardG = 0
                node.backwardH = domain.heuristic(forwardSeed.state, node.state)
                node.backwardParent = null
                backwardOpenList.add(node)
            }

            frontierTarget = backwardOpenList.peek()
        } else {
            frontierSeed.biSearchIteration = bidirectionalIterationCounter
            frontierSeed.backwardG = 0
            frontierSeed.backwardH = domain.heuristic(forwardSeed.state, frontierSeed.state)
            frontierSeed.backwardParent = null

            frontierTarget = frontierSeed
            backwardOpenList.add(frontierSeed)
        }


        forwardSeed.cost = 0
        forwardSeed.heuristic = domain.heuristic(forwardSeed.state, frontierTarget!!.state)
        forwardSeed.biSearchIteration = bidirectionalIterationCounter
        forwardSeed.forwardParent = null

        forwardRoot = forwardSeed
        openList.add(forwardSeed)
    }

    /**
     * Extracts path from the given node to the target frontier node (or a goal)
     * TODO: Make this obey real time bounds
     */
    private fun extractPath(intersectionNode: BiEnvelopeSearchNode<StateType>) {
        pathStates = mutableSetOf(intersectionNode.state)
        cachedPath = LinkedList()
        // start at the intersection and work backward toward root, then come back and work forward toward frontier
        var currentNode = intersectionNode
        while (currentNode != forwardRoot) {

            cachedPath.addFirst(currentNode.forwardParent)
            currentNode = currentNode.forwardParent?.predecessor ?: throw MetronomeException("Cannot trace path to forward root")

            if (!pathStates.add(currentNode.state)) throw MetronomeException("Cycle detected in extractPath")
        }

        currentNode = intersectionNode
        while (currentNode != frontierTarget) {

            cachedPath.addLast(currentNode.backwardParent)
            currentNode = currentNode.backwardParent?.successor ?: throw MetronomeException("Cannot trace path to frontier target")

            if (!pathStates.add(currentNode.state)) throw MetronomeException("Cycle detected in extractPath")
        }

        clearSearch()
    }

    private fun clearSearch() {
        backwardOpenList.clear()
        frontierTarget = null

        openList.clear()
        forwardRoot = null

        bidirectionalIterationCounter++ // invalidate prior iteration search
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
    override fun getNode(parent: BiEnvelopeSearchNode<StateType>, successor: SuccessorBundle<StateType>): BiEnvelopeSearchNode<StateType> {
        val successorState = successor.state
        val tempSuccessorNode = nodes[successorState]

        val successorNode = if (tempSuccessorNode == null) {
            generatedNodeCount++

            val undiscoveredNode = BiEnvelopeSearchNode(
                    state = successorState,
                    heuristic = domain.heuristic(successorState),
                    iteration = iterationCounter,
                    pseudoG = domain.heuristic(currentAgentState, successorState).toLong()
            )

            nodes[successorState] = undiscoveredNode

            undiscoveredNode
        } else {
            tempSuccessorNode
        }

        val edge = BiSearchEdge(predecessor = parent, successor = successorNode, action = successor.action, actionCost = successor.actionCost.toLong())

        // add to parent's successor set
        parent.globalSuccessors.add(edge)
        // add parent to successor's predecessors set
        successorNode.globalPredecessors.add(edge)

        return successorNode
    }

    class BiSearchOpenEmptyException(override val message: String?=null, override val cause: Throwable?=null): MetronomeException(message, cause)

    @Suppress("UNUSED_PARAMETER")
    private fun printMessage(msg: String) = 0//println(msg)
}


