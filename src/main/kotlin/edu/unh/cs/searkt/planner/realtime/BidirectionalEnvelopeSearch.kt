package edu.unh.cs.searkt.planner.realtime

import edu.unh.cs.searkt.MetronomeException
import edu.unh.cs.searkt.environment.*
import edu.unh.cs.searkt.experiment.configuration.ExperimentConfiguration
import edu.unh.cs.searkt.experiment.configuration.realtime.TerminationType
import edu.unh.cs.searkt.experiment.terminationCheckers.TerminationChecker
import edu.unh.cs.searkt.experiment.terminationCheckers.getTerminationChecker
import edu.unh.cs.searkt.experiment.visualizerIsActive
import edu.unh.cs.searkt.planner.*
import edu.unh.cs.searkt.planner.exception.GoalNotReachableException
import edu.unh.cs.searkt.planner.realtime.EnvelopeSearch.EnvelopeSearchPhases.*
import edu.unh.cs.searkt.util.AbstractAdvancedPriorityQueue
import edu.unh.cs.searkt.util.AdvancedPriorityQueue
import edu.unh.cs.searkt.util.Indexable
import edu.unh.cs.searkt.util.resize
import java.util.*
import kotlin.Long.Companion.MAX_VALUE
import kotlin.system.measureNanoTime

/**
 * Real time search algorithm which maintains an ever-expanding search envelope
 * and directs the agent toward the best node on the frontier
 *
 * Note: Cannibalized from the original Envelope Search
 *
 * @author Kevin C. Gall
 */
class BidirectionalEnvelopeSearch<StateType : State<StateType>>(override val domain: Domain<StateType>, val configuration: ExperimentConfiguration) :
        RealTimePlanner<StateType>(),
        RealTimePlannerContext<StateType, BidirectionalEnvelopeSearch.BiEnvelopeSearchNode<StateType>> {

    // Configuration
    private val weight = configuration.weight ?: 1.0

    // Hard code expansion ratios while testing
    // private val frontierLimit = 0.5 // implied by below
    private val backwardLimit = 0.3
    private val localLimit = 0.2

    // TODO: Consider removing lss-specific props from RealTimeSearchNode and moving them into a new sub intf LocalSearchNode
    class BiEnvelopeSearchNode<StateType : State<StateType>>(
            override val state: StateType,
            override var heuristic: Double,
            override var iteration: Long,
            var pseudoG: Long
    ) : RealTimeSearchNode<StateType, BiEnvelopeSearchNode<StateType>> {
        override var index = -1 // required to implement RealTimeSearchNode

        /** Global predecessor and successor sets */
        var successors: MutableSet<SearchEdge<BiEnvelopeSearchNode<StateType>>> = mutableSetOf()
        var globalPredecessors: MutableSet<SearchEdge<BiEnvelopeSearchNode<StateType>>> = mutableSetOf()

        // Local Search properties. All initialized to default values
        override var cost: Long = -1
        override var actionCost: Long = -1
        override var action: Action = NoOperationAction
        override var parent: BiEnvelopeSearchNode<StateType> = this
        override var closed: Boolean = false
        override var lastLearnedHeuristic = heuristic
        override var minCostPathLength: Long = 0L
        override var predecessors: MutableList<SearchEdge<BiEnvelopeSearchNode<StateType>>> = arrayListOf()


        // Envelope-specific props
        var frontierOpenIndex = -1
        var frontierClosed = false

        // Properties for backward search
        var backwardOpenIndex = -1
        var backwardG = -1
        var backwardH = -1
        var backwardParent: SearchEdge<BiEnvelopeSearchNode<StateType>>? = null

        override fun hashCode(): Int = state.hashCode()

        override fun equals(other: Any?): Boolean = when (other) {
            null -> false
            is SearchNode<*, *> -> state == other.state
            is State<*> -> state == other
            else -> false
        }

        override fun toString() =
                "BESNode: [State: $state h: $heuristic, g: $cost, iteration: $iteration, actionCost: $actionCost, parent: ${parent.state}, open: $open]"

        fun forEachSuccessor(domain: Domain<StateType>, fn: (SuccessorBundle<StateType>) -> Unit) {
            domain.successors(state).forEach(fn)
        }

    }

    /* COMPARATORS */

    private val pseudoFComparator = Comparator<BiEnvelopeSearchNode<StateType>> { lhs, rhs ->
        val lhsPseudoF = lhs.heuristic + lhs.pseudoG
        val rhsPseudoF = rhs.heuristic + rhs.pseudoG

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
        val lhsF = lhs.backwardG + lhs.backwardH
        val rhsF = rhs.backwardG + rhs.backwardH

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

    private var frontierOpenList = FrontierOpenList()
    private var backwardOpenList = BackwardOpenList()

    // Main Node-container data structures
    private val nodes: HashMap<StateType, BiEnvelopeSearchNode<StateType>> = HashMap<StateType, BiEnvelopeSearchNode<StateType>>(100_000_000, 1.toFloat()).resize()
    override var openList = AdvancedPriorityQueue<BiEnvelopeSearchNode<StateType>>(1000000, fValueComparator)

    // Current and discovered states
    private var rootState: StateType? = null
    private lateinit var currentAgentState: StateType
    private val foundGoals = mutableSetOf<BiEnvelopeSearchNode<StateType>>()
    private var firstDiscoveredGoal: BiEnvelopeSearchNode<StateType>? = null

    /* State of the algorithm */
    // Counters
    override var iterationCounter = 0L


    // Path Caching
    private val pathStates = mutableSetOf<StateType>()
    // The node in the edge is the successor achieved when executing the action
    private val cachedPath = LinkedList<SearchEdge<BiEnvelopeSearchNode<StateType>>>()


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

            val node = BiEnvelopeSearchNode(sourceState, domain.heuristic(sourceState), 0, 0)
            nodes[sourceState] = node

            frontierOpenList.add(node)
            generatedNodeCount++ // the first node
        }

        val agentNode = nodes[sourceState]!!
        currentAgentState = sourceState

        if (domain.isGoal(sourceState)) {
            return emptyList()
        }

        var backwardTimeSlice = (terminationChecker.remaining() * backwardLimit).toLong()
        var localTimeSlice = (terminationChecker.remaining() * localLimit).toLong()
        // adjust based on whether or not we have a cached path
        if (cachedPath.size > 0) {
            backwardTimeSlice += localTimeSlice
            localTimeSlice = 0
        }

        val frontierTimeSlice = terminationChecker.remaining() - backwardTimeSlice - localTimeSlice

        // Expand the frontier
        val bestCurrentNode = explore(getTerminationChecker(configuration, frontierTimeSlice))

        // backward search tries to connect a path to the agent
        backwardSearch(bestCurrentNode, getTerminationChecker(configuration, backwardTimeSlice))

        // local search kicks in if we don't have a path yet - generates one with limited knowledge
        if (cachedPath.size == 0) {
            localSearch(agentNode, getTerminationChecker(configuration, localTimeSlice))
        }

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

                return firstDiscoveredGoal ?: throw MetronomeException("Goals found, but first discovered not set")
            }

            val currentNode = frontierOpenList.pop()!!
            if (domain.isGoal(currentNode.state)) {
                if (firstDiscoveredGoal == null) firstDiscoveredGoal = currentNode

                if (!foundGoals.contains(currentNode)) {
                    foundGoals.add(currentNode)
                }
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
        }

        return frontierOpenList.peek() ?: firstDiscoveredGoal ?:
            throw GoalNotReachableException("Open list is empty.")
    }

    /**
     * Search backward from the best frontier node (or goals) toward the agent
     */
    private fun backwardSearch(seed: BiEnvelopeSearchNode<StateType>, terminationChecker: TerminationChecker): Boolean = TODO()

    /**
     * Search forward from agent's current state to produce a path
     */
    private fun localSearch(root: BiEnvelopeSearchNode<StateType>, terminationChecker: TerminationChecker): BiEnvelopeSearchNode<StateType> = TODO()

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
            tempSuccessorNode!!
        }

        val edge = SearchEdge(node = successorNode, action = successor.action, actionCost = successor.actionCost.toLong())

        // add to parent's successor set
        parent.successors.add(edge)
        // add parent to successor's predecessors
        successorNode.globalPredecessors.add(SearchEdge(node = parent, action = edge.action, actionCost = edge.actionCost))

        return successorNode
    }


    @Suppress("UNUSED_PARAMETER")
    private fun printMessage(msg: String) = 0//println(msg)
}


