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
 * Note: Cannibalized from the original Envelope Search which did not perform well
 * enough to keep around
 *
 * @author Kevin C. Gall
 */
class EnvelopeSearch<StateType : State<StateType>>(override val domain: Domain<StateType>, val configuration: ExperimentConfiguration) :
        RealTimePlanner<StateType>(),
        RealTimePlannerContext<StateType, EnvelopeSearch.EnvelopeSearchNode<StateType>> {

    // Configuration
    private val weight = configuration.weight ?: 1.0

    // TODO: Decide what ratio to use for exploration vs. backward searching vs. local expansion
    // Hard code expansion ratios while testing
    // private val frontierLimit = 0.5 // implied by below
    private val backwardLimit = 0.3
    private val localLimit = 0.2

    // TODO: Ensure this node type can handle both envelope and local search
    // TODO: Consider removing lss-specific props from RealTimeSearchNode and moving them into a new sub intf LocalSearchNode
    class EnvelopeSearchNode<StateType : State<StateType>>(
            override val state: StateType,
            override var heuristic: Double,
            override var cost: Long,
            override var actionCost: Long,
            override var action: Action,
            override var iteration: Long,
            override var closed: Boolean
    ) : RealTimeSearchNode<StateType, EnvelopeSearchNode<StateType>> {
        override var index = -1 // required to implement RealTimeSearchNode

        /** Nodes that generated this node as a successor */
        override var predecessors: MutableList<SearchEdge<EnvelopeSearchNode<StateType>>> = arrayListOf()

        /** Parent pointer is irrelevant in this algorithm, so always set to this */
        override var parent: EnvelopeSearchNode<StateType> = this

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

    private val pseudoFComparator = Comparator<EnvelopeSearchNode<StateType>> { lhs, rhs ->
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

    private val backwardsComparator: Comparator<EnvelopeSearchNode<StateType>> = TODO("A comparator for searching backwards from the frontier toward the agent")

    /* SPECIALIZED PRIORITY QUEUES */

    inner class FrontierOpenList : AbstractAdvancedPriorityQueue<EnvelopeSearchNode<StateType>>(arrayOfNulls(1000000), pseudoFComparator) {
        override fun getIndex(item: EnvelopeSearchNode<StateType>): Int = item.frontierOpenIndex
        override fun setIndex(item: EnvelopeSearchNode<StateType>, index: Int) {
            item.frontierOpenIndex = index
        }
    }

    inner class BackwardOpenList : AbstractAdvancedPriorityQueue<EnvelopeSearchNode<StateType>>(arrayOfNulls(1000000), backwardsComparator) {
        override fun getIndex(item: EnvelopeSearchNode<StateType>): Int = item.backwardOpenIndex
        override fun setIndex(item: EnvelopeSearchNode<StateType>, index: Int) {
            item.backwardOpenIndex = index
        }
    }

    private var frontierOpenList = FrontierOpenList()
    private var backwardOpenList = BackwardOpenList()

    // Main Node-container data structures
    private val nodes: HashMap<StateType, EnvelopeSearchNode<StateType>> = HashMap<StateType, EnvelopeSearchNode<StateType>>(100_000_000, 1.toFloat()).resize()
    override var openList = AdvancedPriorityQueue(1000000, pseudoFComparator)

    // Current and discovered states
    private var rootState: StateType? = null
    private val foundGoals = mutableSetOf<EnvelopeSearchNode<StateType>>()

    /* State of the algorithm */
    // Counters
    override var iterationCounter = 0L


    // Path Caching
    private val pathStates = mutableSetOf<StateType>()
    // The node in the edge is the successor achieved when executing the action
    private val cachedPath = LinkedList<SearchEdge<EnvelopeSearchNode<StateType>>>()


    /**
     * "Prime" the nodes hashmap. Necessary for real time bounds to avoid hashmap startup costs
     */
    override fun init(initialState: StateType) {
        val primer = EnvelopeSearchNode(initialState,
                0.0, 0L, 0L, NoOperationAction, 0L, false)
        nodes[initialState] = primer
        nodes.remove(initialState)
    }

    override fun selectAction(sourceState: StateType, terminationChecker: TerminationChecker): List<ActionBundle> {
        if (rootState == null) {
            rootState = sourceState

            val node = EnvelopeSearchNode(sourceState, domain.heuristic(sourceState), 0, 0, NoOperationAction, 0, false)
            nodes[sourceState] = node

            frontierOpenList.add(node)
        }

        val agentNode = nodes[sourceState]!!

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
    private fun explore(terminationChecker: TerminationChecker): EnvelopeSearchNode<StateType> = TODO()

    /**
     * Search backward from the best frontier node (or goals) toward the agent
     */
    private fun backwardSearch(seed: EnvelopeSearchNode<StateType>, terminationChecker: TerminationChecker): Boolean = TODO()
    private fun localSearch(root: EnvelopeSearchNode<StateType>, terminationChecker: TerminationChecker): EnvelopeSearchNode<StateType> = TODO()
//    private fun explore(state: StateType, terminationChecker: TerminationChecker, explorationQueue: AbstractAdvancedPriorityQueue<EnvelopeSearchNode<StateType>>): EnvelopeSearchNode<StateType> {
//
//        val sourceNode = nodes[state]!!
//        // TODO: Use a buffer in the termination checker based on the size of the open list since we will initialize the wave frontier from the open list(?)
//        while (!terminationChecker.reachedTermination()) {
//            // Must expand current node if not already expanded, meaning it is likely on the envelope frontier.
//            // If we expand it now, we must remove from the open list rather than expand it again later (which doesn't make sense)
//            val currentNode = if (sourceNode.expanded == -1) sourceNode
//            else explorationQueue.pop() ?: break
//            removeFromOpen(currentNode)
//
//            // TODO when in domains with multiple goals, don't stop looking for goals
//            if (domain.isGoal(currentNode.state)) {
//                if (!foundGoals.contains(currentNode)) {
//                    foundGoals.add(currentNode)
//                }
//                return currentNode
//            }
//
//            if (currentNode.expanded == -1) { // Expanded is -1 when not expanded yet
//                expandFromNode(currentNode)
//            }
//            terminationChecker.notifyExpansion()
//        }
//
//        return explorationQueue.peek() ?: if (foundGoals.size > 0) foundGoals[0] else null
//                ?: throw GoalNotReachableException("Open list is empty.")
//    }

    /**
     * Expands a node and add it to closed list. For each successor
     * it will add it to the open list and store it's g value, as long as the
     * state has not been seen before, or is found with a lower g value
     */
    fun expandFromNode(sourceNode: EnvelopeSearchNode<StateType>) {
        expandedNodeCount += 1
        if (visualizerIsActive) expandedNodes.add(sourceNode) //for visualizer

        sourceNode.iteration = iterationCounter
        sourceNode.expanded = expandedNodeCount
        // We set to infinity because it has not yet been "backed up," so we avoid it until it is backed up
        // or we go greedy until we find backed up states
//        sourceNode.heuristic = Double.POSITIVE_INFINITY

        domain.successors(sourceNode.state).forEach { successor ->
            val successorNode = getNode(sourceNode, successor)

            val edge = SearchEdge(node = sourceNode, action = successor.action, actionCost = successor.actionCost.toLong())
            // Having a predecessor set would make this prettier, but probably slower
            if (!successorNode.predecessors.contains(edge)) {
                successorNode.predecessors.add(edge)
            }

            if (!isOpen(successorNode) && successorNode.iteration == 0L) {
                addToOpen(successorNode)
            }
        }

        removeFromOpen(sourceNode)
    }

    private fun wavePropagation(agentState: StateType, terminationChecker: TerminationChecker, agentPath: Set<StateType>) {
        val initFrontierTime = measureNanoTime {
            checkForWaveRefresh(terminationChecker)
        }
        printMessage("""Initialize wave frontier: $initFrontierTime""")

        while (waveFrontier.isNotEmpty() && !terminationChecker.reachedTermination()) {
            val waveFront = waveFrontier.pop()!!

            backupNode(waveFront)
            terminationChecker.notifyExpansion()

            // TODO: Reconsider wiping the frontier immediately. Need to profile cpu time to find out if this sucks
            if (waveFront == agentState) {
                if (searchPhase == GOAL_BACKUP) searchPhase = PATH_IMPROVEMENT

                waveFrontier.clear()
                break
            }

            if (searchPhase == GOAL_SEARCH && waveFront.state in agentPath) {
                waveFrontier.clear()
                break
            }

        }
    }

    private fun initWaveNode(node: EnvelopeSearchNode<StateType>) {
        node.waveHeuristic = node.heuristic
        node.waveCounter = waveCounter
        node.frontierPointer = node
        node.waveParent = node
    }

    private fun checkForWaveRefresh(terminationChecker: TerminationChecker) {
        //check for in-progress initialization
        if (isWaveInitializationInProgress) {
            initializeWaveFrontier(terminationChecker)
            return
        }

        val waveInProgress = waveFrontier.size > 0

        if (foundGoals.isNotEmpty() && searchPhase == GOAL_SEARCH) {
            // A current backup is in progress not from the goal, while we know where the goal is
            // We should terminate this and start a backup from the goal
            searchPhase = GOAL_BACKUP

            waveCounter++
            waveFrontier.clear()
            clearPreviousBackup = true

            foundGoals.forEach {
                initWaveNode(it)
                waveFrontier.add(it)
            }

        } else if (!waveInProgress && searchPhase == GOAL_SEARCH) {
            // Initialize wave
            waveCounter++
            clearPreviousBackup = true

            printMessage("""Open list size: ${pseudoFOpenList.size}""")
            initializationIndex = 0
            initializeWaveFrontier(terminationChecker)
        } else if (!waveInProgress && searchPhase == PATH_IMPROVEMENT) {
            waveCounter++
            clearPreviousBackup = true

            foundGoals.forEach {
                initWaveNode(it)
                waveFrontier.add(it)
            }
        }
    }

    private fun initializeWaveFrontier(terminationChecker: TerminationChecker) {
        var beginHeapify = false

        if (initializationIndex != null) {
            initializationIndex = waveFrontier.initializeFromQueue(pseudoFOpenList, terminationChecker, initializationIndex!!, this::initWaveNode)
            beginHeapify = initializationIndex == null
        }

        if (beginHeapify) heapifyIndex = waveFrontier.heapify(terminationChecker)
        else if (heapifyIndex != null) heapifyIndex = waveFrontier.heapify(terminationChecker, heapifyIndex!!)

        //once init process is done, add / remove nodes from the pseudoFOpenList as necessary
        if (initializationIndex == null && heapifyIndex == null && (nodesToAdd.size > 0 || nodesToRemove.size > 0)) {
            while (!terminationChecker.reachedTermination()) {
                if (nodesToRemove.size > 0) {
                    pseudoFOpenList.remove(nodesToRemove.first())
                    nodesToRemove.remove(nodesToRemove.first())
                } else if (nodesToAdd.size > 0) {
                    pseudoFOpenList.add(nodesToAdd.first())
                    nodesToAdd.remove(nodesToAdd.first())
                } else {
                    break
                }
            }
        }
    }

    private fun bestExpandedWaveSuccessor(state: StateType = currentAgentState) = domain.successors(state)
            .mapNotNull { nodes[it.state] }
            .filter { it.expanded != -1 && (!it.open || domain.isGoal(it.state)) }
            .minWith(waveComparator)
            ?: throw MetronomeException("No successors available from the agent's current location.")

    private fun bestWaveSuccessor(state: StateType = currentAgentState) = domain.successors(state)
            .mapNotNull { nodes[it.state] }
            .minWith(waveComparator)
            ?: throw MetronomeException("No successors available from the agent's current location.")

    private fun backupNode(sourceNode: EnvelopeSearchNode<StateType>) {
        sourceNode.waveExpanded = true
        if (visualizerIsActive) backedUpNodes.add(sourceNode)

        for ((predecessorNode, _, actionCost) in sourceNode.predecessors) {
            val outdated = predecessorNode.waveCounter != sourceNode.waveCounter

            if (outdated) {
                predecessorNode.waveExpanded = false
                predecessorNode.waveHeuristic = Double.POSITIVE_INFINITY
            }

            // Skip if it was updated in this iteration
            if (predecessorNode.waveExpanded) continue

            val valueFromSource = sourceNode.waveHeuristic + actionCost
            val currentValue = predecessorNode.waveHeuristic

            if (currentValue < valueFromSource) {
                // The node was already updated in this iteration and has a better value
                continue
            }

            predecessorNode.waveCounter = waveCounter
            predecessorNode.waveParent = sourceNode
            predecessorNode.frontierPointer = sourceNode.frontierPointer
            predecessorNode.heuristic = valueFromSource
            predecessorNode.waveHeuristic = valueFromSource

            if (predecessorNode.backupIndex == -1) {
                waveFrontier.add(predecessorNode)
            } else {
                waveFrontier.update(predecessorNode)
            }
        }

    }

    /**
     * @return The set of states in the agent's current intended path
     */
    private fun projectPath(sourceState: StateType, terminationChecker: TerminationChecker): MutableSet<StateType> {
        val currentTrace = mutableSetOf<StateType>()

        var currentState = sourceState

        var checkTerm = 1L
        while (true) {
            if (checkTerm.rem(1000L) == 0L && terminationChecker.reachedTermination()) return currentTrace

            val currentNode = nodes[currentState] ?: throw MetronomeException("Projection exited the envelope")

            if (currentNode.open || domain.isGoal(currentNode.state)) {
                currentTrace.add(currentState)
                return currentTrace
            }

            if (currentState in currentTrace) {
                return currentTrace
//                if (firstIteration) return sourceToCurrentTrace
//                else throw MetronomeException("Policy does not lead to the frontier.")
            }

            currentTrace.add(currentState)

            currentState = if (currentState != currentNode.waveParent.state) {
                currentNode.waveParent.state
            } else {
                bestWaveSuccessor(currentState).state
            }

            checkTerm++
        }
    }

    //Can only be called on an expanded node
    private fun updateLocalHeuristic(currentNode: EnvelopeSearchNode<StateType>): EnvelopeSearchNode<StateType> {
        val bestNode = domain.successors(currentNode.state)
                .map { nodes[it.state]!! }
                .minWith(Comparator { lhs, rhs ->
                    val lhsH = lhs.heuristic + lhs.actionCost
                    val rhsH = rhs.heuristic + rhs.actionCost
                    when {
                        rhs.heuristic == Double.POSITIVE_INFINITY -> -1
                        lhs.heuristic == Double.POSITIVE_INFINITY -> 1
                        lhs.waveCounter == waveCounter && rhs.waveCounter != waveCounter -> -1
                        rhs.waveCounter == waveCounter && lhs.waveCounter != waveCounter -> 1
                        lhsH < rhsH -> -1
                        rhsH < lhsH -> 1
                        else -> 0
                    }
                })!!
        currentNode.heuristic = bestNode.heuristic + bestNode.actionCost

        return bestNode
    }

    /**
     * Get a node for the state if exists, else create a new node.
     *
     * @return node corresponding to the given state.
     */
    override fun getNode(parent: EnvelopeSearchNode<StateType>, successor: SuccessorBundle<StateType>): EnvelopeSearchNode<StateType> {
        val successorState = successor.state
        val tempSuccessorNode = nodes[successorState]

        return if (tempSuccessorNode == null) {
            generatedNodeCount++

            val undiscoveredNode = EnvelopeSearchNode(
                    state = successorState,
                    heuristic = domain.heuristic(successorState),
                    actionCost = successor.actionCost.toLong(),
                    action = successor.action,
                    parent = parent,
                    cost = MAX_VALUE,
                    iteration = 0,
                    closed = false
            )

            undiscoveredNode.generated = generatedNodeCount
            undiscoveredNode.waveParent = undiscoveredNode
//            undiscoveredNode.frontierPointer = undiscoveredNode

            nodes[successorState] = undiscoveredNode
            undiscoveredNode
        } else {
            tempSuccessorNode
        }
    }

    enum class EnvelopeSearchPhases {
        GOAL_SEARCH,
        GOAL_BACKUP,
        PATH_IMPROVEMENT
    }

    @Suppress("UNUSED_PARAMETER")
    private fun printMessage(msg: String) = 0//println(msg)
}

enum class ExpansionStrategy {
    H_VALUE, PSEUDO_F
}

enum class EnvelopeConfigurations(private val configurationName: String) {
    BACKUP_RATIO("backupRatio");

    override fun toString() = configurationName
}


