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
import kotlin.math.max
import kotlin.math.min

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
    // Counter and attribute keys
    private val EXTRACT_TEMP_PATH_COUNTER = "tempPath"
    private val TEMP_PATH_LENGTH = "tempPathLength"
    private val CACHED_PATH_LENGTH = "cachedPathLength"
    private val ENVELOPE_RESETS = "envelopeResets"
    private val HOME_STRETCH_ITERATION = "goalPathExtracted"

    // Configuration
    private val weight = configuration.weight ?: 1.0
    private val lookaheadStrategy = configuration.lookaheadStrategy ?: LookaheadStrategy.PSEUDO_F
    private val envelopeStrategy = configuration.envelopeSearchStrategy ?: lookaheadStrategy // align w/ general lookahead
    private val frontierAdjustmentRatio = configuration.frontierAdjustmentRatio ?: 1.0
    private val generatePredecessors = configuration.generatePredecessors ?: false
    private val bidirectionalSearchStrategy = configuration.bidirectionalSearchStrategy ?: BidirectionalSearchStrategy.ROUND_ROBIN

    // Hard code expansion ratios while testing
    // r1 < 1.0
    private var initFrontierLimitRatio = 0.8
    private var goalsFoundFrontierLimitRatio = 0.1
    private var frontierLimitRatio = initFrontierLimitRatio

    // TODO: Configuration for traceback accounting

    class BiEnvelopeSearchNode<StateType : State<StateType>>(
            override val state: StateType,
            override var heuristic: Double, // frontier heuristic
            override var iteration: Long,
            var envelopeVersion: Long,
            var pseudoG: Long // for frontier using pseudoF
    ) : RealTimeSearchNode<StateType, BiEnvelopeSearchNode<StateType>> {
        // required to implement interface
        override var parent: BiEnvelopeSearchNode<StateType> = this

        /*
         * Global predecessor and successor sets
         * Necessary to distinguish from predecessor list as required by search node interface
         * Flag isExpanded is set only once. Saves computation - we just use cached successors since the world
         * is static
         */
        var globalSuccessors: MutableSet<BiSearchEdge<BiEnvelopeSearchNode<StateType>>> = mutableSetOf()
        var isExpanded = false

        var globalPredecessors: MutableSet<BiSearchEdge<BiEnvelopeSearchNode<StateType>>> = mutableSetOf()
        // Envelope-specific props
        override var index = -1
        override var cost: Long = MAX_VALUE
        override var actionCost: Long = -1
        override var action: Action = NoOperationAction
        override var closed: Boolean = false

        // Forward Search Properties
        var forwardParent: BiSearchEdge<BiEnvelopeSearchNode<StateType>>? = null
        var forwardOpenIndex = -1
        var forwardClosed = false
        var forwardG = 0L
        var forwardH = Double.POSITIVE_INFINITY
        /** If node is part of cached path, we store the parent of the cached path here */
        var cachedForwardParent: BiSearchEdge<BiEnvelopeSearchNode<StateType>>? = null
        var forwardSearchIteration = -1L

        // Properties for backward search
        var backwardOpenIndex = -1
        var backwardClosed = false
        var backwardG = 0L
        var backwardH = Double.POSITIVE_INFINITY
        var backwardParent: BiSearchEdge<BiEnvelopeSearchNode<StateType>>? = null
        var backwardSearchIteration = -1L

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

    private val weightedFComparator = Comparator<BiEnvelopeSearchNode<StateType>> { lhs, rhs ->
        val lhsPseudoF = (lhs.heuristic * weight) + lhs.cost
        val rhsPseudoF = (rhs.heuristic * weight) + rhs.cost

        //break ties on lower H -> this is better info!
        when {
            lhsPseudoF < rhsPseudoF -> -1
            lhsPseudoF > rhsPseudoF -> 1
            lhs.heuristic < rhs.heuristic -> -1
            lhs.heuristic > rhs.heuristic -> 1
            else -> 0
        }
    }

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

    // For the 2 greedy comparators, break ties on lower G representing shorter total paths
    private val greedyBackwardsComparator = Comparator<BiEnvelopeSearchNode<StateType>> { lhs, rhs ->
        // Still break ties on G
        when {
            lhs.backwardH < rhs.backwardH -> -1
            lhs.backwardH > rhs.backwardH -> 1
            lhs.backwardG < rhs.backwardG -> -1
            lhs.backwardG > rhs.backwardG -> 1
            else -> 0
        }
    }

    private val greedyLocalHComparator = Comparator<BiEnvelopeSearchNode<StateType>> { lhs, rhs ->
        // break ties on G
        when {
            lhs.forwardH < rhs.forwardH -> -1
            lhs.forwardH > rhs.forwardH -> 1
            lhs.forwardG < rhs.forwardG -> -1
            lhs.forwardG > rhs.forwardG -> 1
            else -> 0
        }
    }

    private val weightedLocalFComparator = Comparator<BiEnvelopeSearchNode<StateType>> { lhs, rhs ->
        val lhsF = (lhs.forwardH * weight) + lhs.forwardG
        val rhsF = (rhs.forwardH * weight) + rhs.forwardG

        // break ties on low H
        when {
            lhsF < rhsF -> -1
            lhsF > rhsF -> 1
            lhs.forwardH < rhs.forwardH -> -1
            lhs.forwardH > rhs.forwardH -> 1
            else -> 0
        }
    }

    /* SPECIALIZED PRIORITY QUEUES */

    inner class ForwardOpenList : AbstractAdvancedPriorityQueue<BiEnvelopeSearchNode<StateType>>(arrayOfNulls(1000000), weightedLocalFComparator) {
        override fun getIndex(item: BiEnvelopeSearchNode<StateType>): Int = item.forwardOpenIndex
        override fun setIndex(item: BiEnvelopeSearchNode<StateType>, index: Int) {
            item.forwardOpenIndex = index
        }

        override fun setClosed(item: BiEnvelopeSearchNode<StateType>, newValue: Boolean) {
            item.forwardClosed = newValue
        }

        override fun isClosed(item: BiEnvelopeSearchNode<StateType>): Boolean = item.forwardClosed
    }

    inner class BackwardOpenList : AbstractAdvancedPriorityQueue<BiEnvelopeSearchNode<StateType>>(arrayOfNulls(1000000), backwardsComparator) {
        override fun getIndex(item: BiEnvelopeSearchNode<StateType>): Int = item.backwardOpenIndex
        override fun setIndex(item: BiEnvelopeSearchNode<StateType>, index: Int) {
            item.backwardOpenIndex = index
        }
        override fun setClosed(item: BiEnvelopeSearchNode<StateType>, newValue: Boolean) {
            item.backwardClosed = newValue
        }
        override fun isClosed(item: BiEnvelopeSearchNode<StateType>): Boolean = item.backwardClosed
    }


    // Open lists
    private var forwardOpenList = ForwardOpenList()
    private var backwardOpenList = BackwardOpenList()
    // for the frontier
    override var openList = AdvancedPriorityQueue(1000000, pseudoFComparator)

    // Main Node-container data structures
    private val nodes: HashMap<StateType, BiEnvelopeSearchNode<StateType>> = HashMap<StateType, BiEnvelopeSearchNode<StateType>>(100_000_000, 1.toFloat()).resize()

    // Current and discovered states
    private var rootState: StateType? = null
    private lateinit var currentAgentState: StateType
    private lateinit var currentAgentNode: BiEnvelopeSearchNode<StateType>
    private var targetGoal: BiEnvelopeSearchNode<StateType>? = null
    private val foundGoals = mutableSetOf<BiEnvelopeSearchNode<StateType>>()
    private var goalPathExtractedIteration = -1L
    private var goalPathExtracted = false
    private var frontierTarget: BiEnvelopeSearchNode<StateType>? = null
    private var forwardRoot: BiEnvelopeSearchNode<StateType>? = null

    /* State of the algorithm */
    // Counters
    override var iterationCounter = 0L // used by global context
    var backwardIterationCounter = 0L // used by backward search
    var forwardIterationCounter = 0L // used by forward search
    var envelopeVersionCounter = 0L
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
                0.0, 0L, 0L, 0L)
        nodes[initialState] = primer
        nodes.remove(initialState)

        // reset comparators for different lookaheads
        when (lookaheadStrategy) {
            LookaheadStrategy.GBFS -> openList.reorder(heuristicComparator)
            LookaheadStrategy.A_STAR -> openList.reorder(weightedFComparator)
            LookaheadStrategy.PSEUDO_F -> {}// this is the default
        }

        if (envelopeStrategy == LookaheadStrategy.GBFS) {
            backwardOpenList.reorder(greedyBackwardsComparator)
            forwardOpenList.reorder(greedyLocalHComparator)
        }
    }

    @ImplicitReflectionSerializer
    override fun appendPlannerSpecificResults(results: ExperimentResult) {
        val avgCachedPathSize = this.attributes[CACHED_PATH_LENGTH]!!.average()

        results.attributes["avgCachedPathLength"] = avgCachedPathSize
        results.attributes["numForwardResets"] = this.counters[this.EXTRACT_TEMP_PATH_COUNTER] ?: 0
        results.attributes["envelopeResets"] = this.counters[this.ENVELOPE_RESETS] ?: 0
        results.attributes[HOME_STRETCH_ITERATION] = goalPathExtractedIteration
    }

    override fun selectAction(sourceState: StateType, terminationChecker: TerminationChecker): List<ActionBundle> {
        if (rootState == null) {
            rootState = sourceState

            val node = BiEnvelopeSearchNode(sourceState, domain.heuristic(sourceState), iterationCounter, envelopeVersionCounter, 0)
            node.cost = 0
            nodes[sourceState] = node

            openList.add(node)
            generatedNodeCount++ // the first node
        }

        currentAgentState = sourceState
        currentAgentNode = nodes[sourceState]!!
        expansionLimit = terminationChecker.remaining()

        if (domain.isGoal(sourceState)) {
            return emptyList()
        }

        // If we don't already have a path to the goal
        if (!(cachedPath.size > 0 && domain.isGoal(cachedPath.last.successor.state))) {
            var frontierTimeSlice = (terminationChecker.remaining() * frontierLimitRatio).toLong()
            val biSearchTimeSlice = (terminationChecker.remaining() - frontierTimeSlice)
            // adds carry-over from prior iteration to path selection time
            frontierTimeSlice += timeRemaining

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
        }

        // TODO: Make this fit into real time resource allocation
        if (cachedPath.size == 0) extractTemporaryPath()

        // Remove current state / step from cached path
        val nextEdge = cachedPath.removeFirst()
        assert(nextEdge.predecessor.state == currentAgentState){"Predecessor is current agent state"}
        pathStates.remove(sourceState)
        currentAgentNode.cachedForwardParent = null

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
            if (openList.isEmpty()) {
                if (foundGoals.isEmpty())
                    throw GoalNotReachableException("Frontier is empty without finding any goals")

                return targetGoal ?: throw MetronomeException("Goals found, but target not set")
            }

            // ensure we always expand the current target if it is not expanded yet

            val currentNode = openList.pop()!!
            if (domain.isGoal(currentNode.state)) {
                if (foundGoals.size == 0) {
                    // reduce the amount of time spent expanding the frontier starting next iteration
                    frontierLimitRatio = goalsFoundFrontierLimitRatio
                }

                foundGoals.add(currentNode)
                targetGoal = currentNode
                continue // we do not need to explore beyond a goal state
            }

            expandNode(currentNode)
            terminationChecker.notifyExpansion()
        }

        return openList.peek() ?:
            if (foundGoals.isEmpty()) throw GoalNotReachableException("Open list is empty.") else foundGoals.first()
    }

    private fun expandNode(node: BiEnvelopeSearchNode<StateType>) {
        // necessary b/c the forward local search expands nodes - we need to remove them from the frontier
        if (openList.isOpen(node)) openList.remove(node)

        if (!node.isExpanded) {
            domain.successors(node.state).forEach { successor ->
                getNode(node, successor)
            }
            node.isExpanded = true
        }

        var newH = Double.POSITIVE_INFINITY
        node.globalSuccessors.forEach { successorEdge ->
            val successorNode = successorEdge.successor
            if (successorNode.envelopeVersion != envelopeVersionCounter) {
                successorNode.apply{
                    envelopeVersion = envelopeVersionCounter
                    closed = false
                    pseudoG = domain.heuristic(currentAgentState, successorNode.state).toLong()
                    cost = MAX_VALUE
                }
            }

            val gFromCurrent = node.cost + successorEdge.actionCost
            // update cost if applicable
            if (successorNode.cost > gFromCurrent) {
                successorNode.cost = gFromCurrent

                // update open list regardless of whether cost is updated
                if (openList.isOpen(successorNode)) {
                    openList.update(successorNode)
                } else if (!successorNode.closed) {
                    openList.add(successorNode)
                }
            }

            newH = minOf(newH, successorEdge.actionCost + successorNode.heuristic)
        }

        // Updating heuristic. This is what makes us complete in directed domains!
        node.heuristic = newH

        expandedNodeCount++
        node.closed = true
    }

    /**
     * Search backward from the best frontier node (or goals) toward the agent
     */
    private fun bidirectionalSearch(forwardSeed: BiEnvelopeSearchNode<StateType>,
                                    frontierSeed: BiEnvelopeSearchNode<StateType>,
                                    terminationChecker: TerminationChecker) {
        // re-initialize search constructs if we're not in the middle of a search
        reSeedSearch(forwardSeed, frontierSeed)

        // search into envelope via already-generated predecessors
        var searchIntersection: BiEnvelopeSearchNode<StateType>? = null

        try {
            var backward = true
            while (searchIntersection == null && !terminationChecker.reachedTermination()){
                val backwardNode = backwardOpenList.peek() ?:
                    throw BiSearchOpenEmptyException("Backward Search Open list is empty without finding the Agent")
                val forwardNode = forwardOpenList.peek() ?:
                    throw GoalNotReachableException("Dead end in forward search")

                val backF = backwardNode.backwardG + (backwardNode.backwardH * weight)
                val forwardF = forwardNode.forwardG + (forwardNode.forwardH * weight)

                searchIntersection = when (currentAgentState) {
                    // this first condition ensures we always have at least 1-step lookahead for temporary path extraction
                    forwardNode.state -> {
                        backward = false // for ROUND ROBIN strategy
                        expandForwardNode()
                    }
                    else -> when (bidirectionalSearchStrategy) {
                        BidirectionalSearchStrategy.ROUND_ROBIN -> when {
                            backward -> expandBackwardNode()
                            else -> expandForwardNode()
                        }
                        BidirectionalSearchStrategy.FORWARD -> expandForwardNode()
                        BidirectionalSearchStrategy.BACKWARD -> expandBackwardNode()
                        BidirectionalSearchStrategy.MM -> when {
                            backF < forwardF -> expandBackwardNode()
                            backF > forwardF -> expandForwardNode()
                            backwardNode.backwardG > forwardNode.forwardG -> expandBackwardNode()
                            backwardNode.backwardG < forwardNode.forwardG -> expandForwardNode()
                            // arbitrarily break ties by searching forward
                            else -> expandForwardNode()
                        }
                    }

                }
                backward = !backward
                terminationChecker.notifyExpansion()
            }
        } catch (ex: BiSearchOpenEmptyException) {
            // clear envelope entirely including cached paths and bi-search.
            // select next action based on 1-step lookahead and seed envelope with that state
            printMessage(ex.message ?: "")
            resetEnvelope()
        }

        if (searchIntersection != null) extractPath(searchIntersection) // sets cached Path and path state set
    }

    private fun expandBackwardNode(): BiEnvelopeSearchNode<StateType>? {
        val node = backwardOpenList.pop() ?: throw MetronomeException("Bug - No backward node?")

        // Does not increment expanded node count
        if (generatePredecessors) expandPredecessors(node)

        for (edge in node.globalPredecessors) {
            val predecessorNode = edge.predecessor

            // ensuring the backward search stays within the current envelope
            if (predecessorNode.envelopeVersion != envelopeVersionCounter) continue

            // reset node
            if (predecessorNode.backwardSearchIteration != node.backwardSearchIteration) {
                predecessorNode.backwardSearchIteration = node.backwardSearchIteration
                predecessorNode.backwardG = MAX_VALUE
                predecessorNode.backwardClosed = false
                predecessorNode.backwardH = domain.heuristic(forwardRoot?.state ?: throw MetronomeException("No forward target set"),
                        predecessorNode.state)
                predecessorNode.backwardParent = null
            }

            // Perform goal check before cost checking. If we have a path, we want it now!
            // Must check for closed forward search nodes since successor lists may have been
            // increased by expansion, so a "closed" forward node may not be within the frontier
            if (forwardOpenList.isOpen(predecessorNode) ||
                    predecessorNode.forwardSearchIteration == forwardIterationCounter
                    || (!goalPathExtracted && pathStates.contains(predecessorNode.state))) {
                predecessorNode.backwardParent = edge // we need the edge set for path extraction
                return predecessorNode
            }


            val newG = node.backwardG + edge.actionCost
            if (predecessorNode.backwardG > newG) {
                predecessorNode.backwardG = newG
                predecessorNode.backwardParent = edge

                if (backwardOpenList.isOpen(predecessorNode)) {
                    backwardOpenList.update(predecessorNode)
                } else if (!predecessorNode.backwardClosed){
                    backwardOpenList.add(predecessorNode)
                }
            }
        }
        node.backwardClosed = true

        expandedNodeCount++
        return null
    }

    private fun expandForwardNode(): BiEnvelopeSearchNode<StateType>? {
        val node = forwardOpenList.pop() ?: throw MetronomeException("Bug - No forward node?")

        // increments expandedNodeCount
        expandNode(node)

        for (edge in node.globalSuccessors) {
            val successorNode = edge.successor

            if (successorNode.forwardSearchIteration != node.forwardSearchIteration) {
                successorNode.forwardSearchIteration = node.forwardSearchIteration
                successorNode.forwardG = MAX_VALUE
                successorNode.forwardClosed = false
                successorNode.forwardH = domain.heuristic(successorNode.state,
                        frontierTarget?.state ?: throw MetronomeException("No frontier target set"))
                successorNode.forwardParent = null
            }

            // Perform goal check before cost checking. If we have a path, we want it now!
            if (backwardOpenList.isOpen(successorNode) ||
                    successorNode.backwardSearchIteration == backwardIterationCounter) {
                successorNode.forwardParent = edge // we need the edge set for path extraction
                return successorNode
            }

            val newG = node.forwardG + edge.actionCost
            if (successorNode.forwardG > newG) {
                successorNode.forwardG = newG
                successorNode.forwardParent = edge

                if (forwardOpenList.isOpen(successorNode)) {
                    forwardOpenList.update(successorNode)
                } else if (!successorNode.forwardClosed){
                    forwardOpenList.add(successorNode)
                }
            }
        }
        node.forwardClosed = true
        return null
    }

    /**
     * Simple convenience function that expands predecessors. Call to getNode adds them to the appropriate
     * data structures
     */
    private fun expandPredecessors(node: BiEnvelopeSearchNode<StateType>) {
        for (predecessorBundle in domain.predecessors(node.state)) {
            getNode(node, predecessorBundle, true)
        }
    }

    private fun reSeedSearch(forwardSeed: BiEnvelopeSearchNode<StateType>,
                             frontierSeed: BiEnvelopeSearchNode<StateType>) {
        if (backwardOpenList.isEmpty()) {
            backwardIterationCounter++ // invalidate prior backward search

            if (foundGoals.size > 0) {
                foundGoals.forEach { node ->
                    node.backwardSearchIteration = backwardIterationCounter
                    node.backwardG = 0
                    node.backwardH = domain.heuristic(forwardSeed.state, node.state)
                    node.backwardParent = null
                    node.backwardClosed = false
                    backwardOpenList.add(node)
                }

                frontierTarget = backwardOpenList.peek()
            } else {
                frontierSeed.backwardSearchIteration = backwardIterationCounter
                frontierSeed.backwardG = 0
                frontierSeed.backwardH = domain.heuristic(forwardSeed.state, frontierSeed.state)
                frontierSeed.backwardParent = null
                frontierSeed.backwardClosed = false

                frontierTarget = frontierSeed
                backwardOpenList.add(frontierSeed)
            }
        }

        if (forwardOpenList.isEmpty()) {
            forwardIterationCounter++ //invalidate prior search

            forwardSeed.forwardG = 0L
            forwardSeed.forwardH = domain.heuristic(forwardSeed.state, frontierTarget!!.state)
            forwardSeed.forwardSearchIteration = forwardIterationCounter
            forwardSeed.forwardParent = null
            forwardSeed.forwardClosed = false

            forwardRoot = forwardSeed
            forwardOpenList.add(forwardSeed)
        }

        if (forwardRoot == frontierTarget)
            throw MetronomeException("Bidirectional search roots are the same")
    }

    /**
     * Extracts path from the given node to the target frontier node (or a goal)
     * Adds to existing path since the search anchored at the end of the cached path
     * TODO: Make this obey real time bounds
     */
    private fun extractPath(intersectionNode: BiEnvelopeSearchNode<StateType>) {
        val newPathStates = mutableSetOf(intersectionNode.state)
        val newPath = LinkedList<BiSearchEdge<BiEnvelopeSearchNode<StateType>>>()

        // start at the intersection and work backward toward root, then come back and work forward toward frontier
        var currentNode = intersectionNode
        var useCachedPointer = pathStates.contains(currentNode.state)
        var canAppend = false
        while (currentNode.state != currentAgentState) {
            if (currentNode == forwardRoot) {
                canAppend = true
                break
            }

            val nextEdge = if(useCachedPointer) {
                currentNode.cachedForwardParent
            } else {
                currentNode.cachedForwardParent = currentNode.forwardParent
                currentNode.forwardParent
            }

            newPath.addFirst(nextEdge)
            currentNode = nextEdge?.predecessor ?:
                    throw MetronomeException("Cannot trace path to forward root")


            if (!newPathStates.add(currentNode.state)) {
                throw MetronomeException("Cycle detected in extractPath toward root")
            }
            if (!useCachedPointer && pathStates.contains(currentNode.state)) useCachedPointer = true
        }

        currentNode = intersectionNode
        while (currentNode != frontierTarget && !domain.isGoal(currentNode.state)) {
            val edge = currentNode.backwardParent ?:
                throw MetronomeException("Cannot trace path to frontier target")

            newPath.addLast(edge)
            currentNode = edge.successor

            // set forward parent in case we hit this node in the cached path on a subsequent search and need to trace back
            currentNode.cachedForwardParent = edge

            if (!newPathStates.add(currentNode.state)) {
                throw MetronomeException("Cycle detected in extractPath toward frontier")
            }
        }

        if (canAppend) {
            pathStates.addAll(newPathStates)
            cachedPath.addAll(newPath) // appends to current path
        } else {
            pathStates = newPathStates
            cachedPath = newPath // replaces current path
        }

        this.appendAttribute(CACHED_PATH_LENGTH, cachedPath.size)

        if (goalPathExtractedIteration == -1L && domain.isGoal(cachedPath.last.successor.state)) {
            goalPathExtractedIteration = iterationCounter
            goalPathExtracted = true
        }

        clearSearch()
    }

    private fun extractTemporaryPath() {
        this.incrementCounter(EXTRACT_TEMP_PATH_COUNTER)

        val targetNode = forwardOpenList.peek() ?: throw GoalNotReachableException("Forward Open is empty - extract path")

        if (targetNode.state == currentAgentState) {
            throw MetronomeException("Current state is target? (extractTemporaryPath)")
        }

        pathStates = mutableSetOf(targetNode.state)
        cachedPath = LinkedList()

        var currentNode = targetNode
        while (currentNode.state != currentAgentState) {
            cachedPath.addFirst(currentNode.forwardParent)
            currentNode.cachedForwardParent = currentNode.forwardParent
            currentNode = currentNode.forwardParent?.predecessor ?:
                    throw MetronomeException("Cannot trace temporary path to forward root")

            if (!pathStates.add(currentNode.state)) throw MetronomeException("Cycle detected in extractTemporaryPath")
        }

        this.appendAttribute(TEMP_PATH_LENGTH, cachedPath.size)

        clearForwardSearch() // moving invalidates our current search
        // modify frontier limit ratio according to configured parameter. Keep within 0 and 1
        frontierLimitRatio = max(0.0, min(1.0, frontierLimitRatio * frontierAdjustmentRatio))
    }

    private fun clearSearch() {
        clearBackwardSearch()
        clearForwardSearch()

        frontierLimitRatio = if (foundGoals.isNotEmpty()) goalsFoundFrontierLimitRatio else initFrontierLimitRatio
    }

    private fun clearBackwardSearch() {
        backwardOpenList.clear()
        frontierTarget = null
    }

    private fun clearForwardSearch() {
        forwardOpenList.clear()
        forwardRoot = null
    }

    // Reset envelope by clearing searches and frontier. Choose the next move via a 1-step lookahead and seed
    // the frontier with the chosen successor
    private fun resetEnvelope() {
        this.incrementCounter(ENVELOPE_RESETS)

        clearSearch()

        val target = if (cachedPath.size > 0) {
            cachedPath.last
        } else {
            val nextEdge = currentAgentNode.globalSuccessors.minBy{ it.successor.heuristic }

            pathStates = mutableSetOf(nextEdge!!.predecessor.state, nextEdge.successor.state)
            cachedPath = LinkedList()
            cachedPath.add(nextEdge)

            nextEdge
        }

        // reset frontier
        openList.clear()
        envelopeVersionCounter++

        target.successor.closed = false
        target.successor.envelopeVersion = envelopeVersionCounter
        target.successor.pseudoG = 0L
        target.successor.cost = 0L
        openList.add(target.successor)

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
    override fun getNode(parent: BiEnvelopeSearchNode<StateType>, successor: SuccessorBundle<StateType>): BiEnvelopeSearchNode<StateType> = getNode(parent, successor, false)
    private fun getNode(parent: BiEnvelopeSearchNode<StateType>, successor: SuccessorBundle<StateType>, isPredecessor: Boolean): BiEnvelopeSearchNode<StateType> {
        val successorState = successor.state
        val tempSuccessorNode = nodes[successorState]

        val successorNode = if (tempSuccessorNode == null) {
            generatedNodeCount++

            // base pseudoF on where the agent is going rather than where it is
            val pseudoFRoot = if (cachedPath.size > 0) cachedPath.last.successor.state else currentAgentState

            val undiscoveredNode = BiEnvelopeSearchNode(
                    state = successorState,
                    heuristic = domain.heuristic(successorState),
                    iteration = iterationCounter,
                    //do not allow predecessor expansion to prevent normal envelope expansion
                    envelopeVersion = if(isPredecessor) -1 else envelopeVersionCounter,
                    pseudoG = domain.heuristic(pseudoFRoot, successorState).toLong()
            )

            nodes[successorState] = undiscoveredNode

            undiscoveredNode
        } else {
            tempSuccessorNode
        }

        val edge = if (isPredecessor) {
            when {
                successor.action == NoOperationAction -> BiSearchEdge(predecessor = successorNode, successor = parent,
                        action = successor.action,
                        actionCost = successor.actionCost.toLong())
                successor.action is Operator<*> -> BiSearchEdge(predecessor = successorNode, successor = parent,
                        action = (successor.action as Operator<StateType>).reverse(successorState),
                        actionCost = successor.actionCost.toLong())
                else -> throw MetronomeException("Actions must be reversible to generate predecessors")
            }
        } else {
            BiSearchEdge(predecessor = parent, successor = successorNode, action = successor.action, actionCost = successor.actionCost.toLong())
        }

        // add to parent's successor set
        parent.globalSuccessors.add(edge)
        // add parent to successor's predecessors set
        successorNode.globalPredecessors.add(edge)

        return successorNode
    }

    class BiSearchOpenEmptyException(override val message: String?=null, override val cause: Throwable?=null): MetronomeException(message, cause)

    @Suppress("UNUSED_PARAMETER")
    private fun printMessage(msg: String) = 0//println(msg)

    enum class BiESConfiguration(val key: String) {
        ENVELOPE_SEARCH_STRATEGY("envelopeSearchStrategy"),
        FRONTIER_ADJUSTMENT_RATIO("frontierAdjustmentRatio"),
        BIDIRECTIONAL_SEARCH_STRATEGY("bidirectionalSearchStrategy"),
        GENERATE_PREDECESSORS("generatePredecessors");

        override fun toString(): String = key
    }

    enum class BidirectionalSearchStrategy {
        ROUND_ROBIN, MM, FORWARD, BACKWARD;
    }
}


