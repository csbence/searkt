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

    // TODO: Ensure this node type can handle both envelope and local search
    // TODO: Consider removing lss-specific props from RealTimeSearchNode and moving them into a new sub intf LocalSearchNode
    class BiEnvelopeSearchNode<StateType : State<StateType>>(
            override val state: StateType,
            override var heuristic: Double,
            override var cost: Long,
            override var actionCost: Long,
            override var action: Action,
            override var iteration: Long,
            override var closed: Boolean
    ) : RealTimeSearchNode<StateType, BiEnvelopeSearchNode<StateType>> {
        override var index = -1 // required to implement RealTimeSearchNode

        /** Nodes that generated this node as a successor */
        override var predecessors: MutableList<SearchEdge<BiEnvelopeSearchNode<StateType>>> = arrayListOf()

        /** Parent pointer is irrelevant in this algorithm, so always set to this */
        override var parent: BiEnvelopeSearchNode<StateType> = this

        override var lastLearnedHeuristic = heuristic
        override var minCostPathLength: Long = 0L

        // Envelope-specific props
        var frontierOpenIndex = -1
        var backwardOpenIndex = -1

        var expanded = -1
        var generated = -1

        override fun hashCode(): Int = state.hashCode()

        override fun equals(other: Any?): Boolean = when (other) {
            null -> false
            is SearchNode<*, *> -> state == other.state
            is State<*> -> state == other
            else -> false
        }

        override fun toString() =
                "RTSNode: [State: $state h: $heuristic, g: $cost, iteration: $iteration, actionCost: $actionCost, parent: ${parent.state}, open: $open]"

    }

    /* COMPARATORS */

    private val pseudoFComparator = Comparator<BiEnvelopeSearchNode<StateType>> { lhs, rhs ->
        //using heuristic function for pseudo-g
        val lhsPseudoG = domain.heuristic(currentAgentState, lhs.state) * weight
        val rhsPseudoG = domain.heuristic(currentAgentState, rhs.state) * weight
        val lhsPseudoF = lhs.heuristic + lhsPseudoG
        val rhsPseudoF = rhs.heuristic + rhsPseudoG

        //break ties on lower H -> this is better info!
        when {
            lhsPseudoF < rhsPseudoF -> -1
            lhsPseudoF > rhsPseudoF -> 1
            lhs.heuristic < rhs.heuristic -> -1
            lhs.heuristic > rhs.heuristic -> 1
            lhs.generated < rhs.generated -> -1
            lhs.generated > rhs.generated -> 1
            else -> 0
        }
    }

    private val backwardsComparator: Comparator<BiEnvelopeSearchNode<StateType>> = TODO("A comparator for searching backwards from the frontier toward the agent")

    /* SPECIALIZED PRIORITY QUEUES */

    inner class FrontierOpenList : AbstractAdvancedPriorityQueue<BiEnvelopeSearchNode<StateType>>(arrayOfNulls(1000000), pseudoFComparator) {
        override fun getIndex(item: BiEnvelopeSearchNode<StateType>): Int = item.frontierOpenIndex
        override fun setIndex(item: BiEnvelopeSearchNode<StateType>, index: Int) {
            item.frontierOpenIndex = index
        }
    }

    inner class BackwardOpenList : AbstractAdvancedPriorityQueue<BiEnvelopeSearchNode<StateType>>(arrayOfNulls(1000000), backwardsComparator) {
        override fun getIndex(item: BiEnvelopeSearchNode<StateType>): Int = item.backwardOpenIndex
        override fun setIndex(item: BiEnvelopeSearchNode<StateType>, index: Int) {
            item.backwardOpenIndex = index
        }
    }

    private var frontierOpenList = FrontierOpenList()
    private var backwardOpenList = BackwardOpenList()

    // Main Node-container data structures
    private val nodes: HashMap<StateType, BiEnvelopeSearchNode<StateType>> = HashMap<StateType, BiEnvelopeSearchNode<StateType>>(100_000_000, 1.toFloat()).resize()
    override var openList = AdvancedPriorityQueue(1000000, pseudoFComparator)

    // Current and discovered states
    private var rootState: StateType? = null
    private var currentAgentState: StateType
    private val foundGoals = mutableSetOf<BiEnvelopeSearchNode<StateType>>()

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
                0.0, 0L, 0L, NoOperationAction, 0L, false)
        nodes[initialState] = primer
        nodes.remove(initialState)
    }

    override fun selectAction(sourceState: StateType, terminationChecker: TerminationChecker): List<ActionBundle> {
        if (rootState == null) {
            rootState = sourceState

            val node = BiEnvelopeSearchNode(sourceState, domain.heuristic(sourceState), 0, 0, NoOperationAction, 0, false)
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

        var nextEdge = cachedPath.removeFirst()
        pathStates.remove(sourceState)

        iterationCounter++
        return listOf(ActionBundle(nextEdge.action, nextEdge.actionCost))
    }

    /**
     * Explore the state space by expanding the frontier
     */
    private fun explore(terminationChecker: TerminationChecker): BiEnvelopeSearchNode<StateType> = TODO()

    /**
     * Search backward from the best frontier node (or goals) toward the agent
     */
    private fun backwardSearch(seed: BiEnvelopeSearchNode<StateType>, terminationChecker: TerminationChecker): Boolean = TODO()
    private fun localSearch(root: BiEnvelopeSearchNode<StateType>, terminationChecker: TerminationChecker): BiEnvelopeSearchNode<StateType> = TODO()

    /**
     * Get a node for the state if exists, else create a new node.
     *
     * @return node corresponding to the given state.
     * TODO: Review this function
     */
    override fun getNode(parent: BiEnvelopeSearchNode<StateType>, successor: SuccessorBundle<StateType>): BiEnvelopeSearchNode<StateType> {
        val successorState = successor.state
        val tempSuccessorNode = nodes[successorState]

        return if (tempSuccessorNode == null) {
            generatedNodeCount++

            val undiscoveredNode = BiEnvelopeSearchNode(
                    state = successorState,
                    heuristic = domain.heuristic(successorState),
                    actionCost = successor.actionCost.toLong(),
                    action = successor.action,
                    cost = MAX_VALUE,
                    iteration = 0,
                    closed = false
            )

            undiscoveredNode.generated = generatedNodeCount

            nodes[successorState] = undiscoveredNode
            undiscoveredNode
        } else {
            tempSuccessorNode
        }
    }


    @Suppress("UNUSED_PARAMETER")
    private fun printMessage(msg: String) = 0//println(msg)
}


