package edu.unh.cs.ai.realtimesearch.planner.realtime

import edu.unh.cs.ai.realtimesearch.MetronomeException
import edu.unh.cs.ai.realtimesearch.environment.*
import edu.unh.cs.ai.realtimesearch.experiment.configuration.ExperimentConfiguration
import edu.unh.cs.ai.realtimesearch.experiment.terminationCheckers.StaticExpansionTerminationChecker
import edu.unh.cs.ai.realtimesearch.experiment.terminationCheckers.TerminationChecker
import edu.unh.cs.ai.realtimesearch.planner.*
import edu.unh.cs.ai.realtimesearch.planner.exception.GoalNotReachableException
import edu.unh.cs.ai.realtimesearch.planner.realtime.EnvelopeSearch.EnvelopeSearchPhases.*
import edu.unh.cs.ai.realtimesearch.util.AbstractAdvancedPriorityQueue
import edu.unh.cs.ai.realtimesearch.util.AdvancedPriorityQueue
import edu.unh.cs.ai.realtimesearch.util.Indexable
import edu.unh.cs.ai.realtimesearch.util.resize
import edu.unh.cs.ai.realtimesearch.visualizer
import kotlin.Long.Companion.MAX_VALUE

class EnvelopeSearch<StateType : State<StateType>>(override val domain: Domain<StateType>, val configuration: ExperimentConfiguration) :
        RealTimePlanner<StateType>(),
        RealTimePlannerContext<StateType, EnvelopeSearch.EnvelopeSearchNode<StateType>> {

    private val expansionStrategy = ExpansionStrategy.PSEUDO_F
    private val updateStrategy = UpdateStrategy.PSEUDO

    class EnvelopeSearchNode<StateType : State<StateType>>(
            override val state: StateType,
            override var heuristic: Double,
            override var cost: Long,
            override var actionCost: Long,
            override var action: Action,
            override var iteration: Long,
            parent: EnvelopeSearchNode<StateType>? = null
    ) : RealTimeSearchNode<StateType, EnvelopeSearchNode<StateType>>, Indexable {

        /** Item index in the open list. */
        override var index: Int = -1

        /** Nodes that generated this SafeRealTimeSearchNode as a successor in the current exploration phase. */
        override var predecessors: MutableList<SearchEdge<EnvelopeSearchNode<StateType>>> = arrayListOf()

        /** Parent pointer that points to the min cost predecessor. */
        override var parent: EnvelopeSearchNode<StateType> = parent ?: this

        var rhsHeuristic = Double.POSITIVE_INFINITY

        var waveCounter: Int = -1
        lateinit var waveParent: EnvelopeSearchNode<StateType>
        var waveHeuristic: Double = Double.POSITIVE_INFINITY
        var waveExpanded = false
        lateinit var frontierPointer: EnvelopeSearchNode<StateType>
        var backupIndex = -1

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
                "RTSNode: [State: $state h: $heuristic, wave:$waveCounter, g: $cost, iteration: $iteration, actionCost: $actionCost, parent: ${parent.state}, open: $open]"

        val consistent: Boolean
            get() = heuristic == rhsHeuristic
    }

    private val pseudoFComparator = Comparator<EnvelopeSearchNode<StateType>> { lhs, rhs ->
        //using heuristic function for pseudo-g
        val lhsPseudoG = domain.heuristic(currentAgentState, lhs.state) * 2
        val rhsPseudoG = domain.heuristic(currentAgentState, rhs.state) * 2
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

    val waveHeuristicComparator = Comparator<EnvelopeSearchNode<StateType>> { lhs, rhs ->
        when {
            lhs.waveHeuristic < rhs.waveHeuristic -> -1
            lhs.waveHeuristic > rhs.waveHeuristic -> 1
            else -> 0
        }
    }

    private val waveFComparator = Comparator<EnvelopeSearchNode<StateType>> { lhs, rhs ->
        val lhsWaveF = lhs.waveHeuristic + domain.heuristic(lhs.state, currentAgentState)
        val rhsWaveF = rhs.waveHeuristic + domain.heuristic(rhs.state, currentAgentState)

        when {
            lhsWaveF < rhsWaveF -> -1
            lhsWaveF > rhsWaveF -> 1
            lhs.waveHeuristic < rhs.waveHeuristic -> -1
            lhs.waveHeuristic > rhs.waveHeuristic -> 1
            else -> 0
        }
    }

    private val waveComparator = Comparator<EnvelopeSearchNode<StateType>>({ lhs, rhs ->
        when {
            lhs.waveCounter > rhs.waveCounter -> -1
            lhs.waveCounter < rhs.waveCounter -> 1
            lhs.heuristic < rhs.heuristic -> -1
            lhs.heuristic > rhs.heuristic -> 1

            else -> 0
        }
    })

    override var iterationCounter = 0L

    private var waveCounter = 0

    private val nodes: HashMap<StateType, EnvelopeSearchNode<StateType>> = HashMap<StateType, EnvelopeSearchNode<StateType>>(100000000, 1.toFloat()).resize()

    override var openList = AdvancedPriorityQueue<EnvelopeSearchNode<StateType>>(1000000, pseudoFComparator)

    private var waveFrontier = object : AbstractAdvancedPriorityQueue<EnvelopeSearchNode<StateType>>(arrayOfNulls(1000000), waveHeuristicComparator) {
        override fun getIndex(item: EnvelopeSearchNode<StateType>): Int = item.backupIndex
        override fun setIndex(item: EnvelopeSearchNode<StateType>, index: Int) {
            item.backupIndex = index
        }
    }

    private val expandedNodes = mutableListOf<EnvelopeSearchNode<StateType>>()
    private val backedUpNodes = mutableListOf<EnvelopeSearchNode<StateType>>()

    private var rootState: StateType? = null

    private val foundGoals = mutableListOf<EnvelopeSearchNode<StateType>>()
    private var backupInProgress = false
    private var endOfPathReached = false

    private var firstIteration = true
    private lateinit var currentAgentState: StateType

    private var lastPlannedPath = mutableSetOf<StateType>()

    private var searchPhase = GOAL_SEARCH

    override fun selectAction(sourceState: StateType, terminationChecker: TerminationChecker): List<ActionBundle> {
        if (rootState == null) {
            rootState = sourceState
        }

        currentAgentState = sourceState

        val expansionTerminationChecker = StaticExpansionTerminationChecker(terminationChecker.remaining() / 4)

        if (domain.isGoal(sourceState)) {
            return emptyList()
        }

        if (foundGoals.isEmpty()) {
            val expansionTerminationChecker = StaticExpansionTerminationChecker(terminationChecker.remaining() / 4)

            openList.reorder(heuristicComparator)
            explore(sourceState, expansionTerminationChecker)

            expansionTerminationChecker.resetTo(0)

            openList.reorder(pseudoFComparator)
            explore(sourceState, expansionTerminationChecker)
        }

        if (searchPhase == PATH_IMPROVEMENT) {
            val expansionTerminationChecker = StaticExpansionTerminationChecker(terminationChecker.remaining() / 2)
            openList.reorder(pseudoFComparator)
            explore(sourceState, expansionTerminationChecker)
        }


        lastPlannedPath.add(currentAgentState)
        val agentNode = nodes[currentAgentState]!!

        //If the agent has reached the wave frontier before it has been backed
        //up, start a new frontier backup!
        if (agentNode.waveCounter == waveCounter) {
            backupInProgress = false
            endOfPathReached = false
        }

        val backupTerminationChecker = StaticExpansionTerminationChecker(terminationChecker.remaining() / 2)
        backupInProgress = wavePropagation(currentAgentState, backupTerminationChecker, backupInProgress, lastPlannedPath)

        val agentNextNode = if ((agentNode.waveParent == agentNode && backupInProgress) || endOfPathReached) {
            endOfPathReached = true
            updateLocalHeuristic(agentNode)
        } else {
            agentNode.waveParent
        }
        val agentToFrontier = if (endOfPathReached) {
            listOf(agentNode.state, agentNextNode.state)
        } else {
            projectPath(currentAgentState)
        }

        lastPlannedPath = agentToFrontier.toMutableSet()

        //Stealing 1 expansions here. TODO: Bookkeeping for these expansions
//
//                if (lastWaveNode.frontierPointer.expanded == -1 && !domain.isGoal(lastWaveNode.frontierPointer.state)) {
//                    expandFromNode(lastWaveNode.frontierPointer)
//                    openList.remove(lastWaveNode.frontierPointer)
//                }

        visualizer?.updateRootToBest(agentToFrontier.map { nodes[it]!! })
//                visualizer?.updateCommonAncestorToAgentChain(pointerProjection)

//        visualizer?.updateSearchEnvelope(expandedNodes)
//        visualizer?.updateBackpropagation(backedUpNodes)
//        visualizer?.updateAgentLocation(nodes[sourceState]!!)
//        visualizer?.delay()

        val path =
                constructPath(listOf(sourceState, agentNextNode.state), domain)

        return path
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


    private fun wavePropagation(agentState: StateType, terminationChecker: TerminationChecker, continueWave: Boolean = false, agentPath: Set<StateType>): Boolean {

        if (foundGoals.isNotEmpty() && searchPhase == GOAL_SEARCH) {
            // A current backup is in progress not from the goal, while we know where the goal is
            // We should terminate this and start a backup from the goal

            searchPhase = GOAL_BACKUP

            waveCounter++
            waveFrontier.clear()
            backedUpNodes.clear()

            waveFrontier.reorder(waveFComparator)

            foundGoals.forEach {
                it.heuristic = 0.0
                it.waveHeuristic = 0.0
                it.waveCounter = waveCounter
                it.frontierPointer = it
                it.waveParent = it

                waveFrontier.add(it)
            }

        } else if (!continueWave && searchPhase == GOAL_SEARCH) {
            // Initialize wave
            waveCounter++
            waveFrontier.clear()
            backedUpNodes.clear()

            openList.forEach {
                it.waveCounter = waveCounter
                it.waveHeuristic = if (domain.isGoal(it.state)) 0.0 else getOutsideHeuristic(it)
                it.frontierPointer = it
                it.waveParent = it

                waveFrontier.add(it)
            }
        } else if (!continueWave && searchPhase == PATH_IMPROVEMENT) {
            waveCounter++
            waveFrontier.clear()
            backedUpNodes.clear()

            foundGoals.forEach {
                it.heuristic = 0.0
                it.waveHeuristic = 0.0
                it.waveCounter = waveCounter
                it.frontierPointer = it
                it.waveParent = it

                waveFrontier.add(it)
            }
        }

        while (waveFrontier.isNotEmpty() && !terminationChecker.reachedTermination()) {
            val waveFront = waveFrontier.pop()!!

            backupNode(waveFront)

            if (searchPhase == GOAL_BACKUP && waveFront == agentState) {
                searchPhase = PATH_IMPROVEMENT
                return false
            }

            if (searchPhase == PATH_IMPROVEMENT && waveFront == agentState) {
                return false
            }

            if (searchPhase == GOAL_SEARCH && (waveFront.state in agentPath || waveFront == agentState)) {
                endOfPathReached = false
                return false
            }

            terminationChecker.notifyExpansion()
        }

        return true
    }

    private fun backupNode(sourceNode: EnvelopeSearchNode<StateType>) {
        sourceNode.waveExpanded = true
        backedUpNodes.add(sourceNode)
        visualizer?.updateBackpropagation(backedUpNodes)
        visualizer?.delay()

        for ((predecessorNode, _, actionCost) in sourceNode.predecessors) {
            val outdated = predecessorNode.waveCounter != sourceNode.waveCounter

            if (outdated) {
                predecessorNode.waveExpanded = false
                predecessorNode.waveHeuristic = Double.POSITIVE_INFINITY
                predecessorNode.heuristic = Double.POSITIVE_INFINITY
            }

            // Skip if it was updated in this iteration
            if (predecessorNode.waveExpanded) continue

            val valueFromSource = sourceNode.waveHeuristic + actionCost
            val currentValue = predecessorNode.waveHeuristic

            if (currentValue < valueFromSource) {
                // The node was already updated in this iteration and has a better value
                continue
            }

//            val currentTarget = predecessorNode.frontierPointer.heuristic
//            val sourceTarget = sourceNode.frontierPointer.heuristic
//            if (currentValue == valueFromSource && currentTarget < sourceTarget) {
//                // We want to pick the target that goes deeper
//                continue
//            }

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

    private fun projectPath(sourceState: StateType): Collection<StateType> {
        val sourceToCurrentTrace = mutableListOf<StateType>()
        val currentTrace = mutableSetOf<StateType>()

        var currentState = sourceState

        while (true) {
            val currentNode = nodes[currentState] ?: throw MetronomeException("Projection exited the envelope")

            if (currentNode.open || domain.isGoal(currentNode.state)) {
                sourceToCurrentTrace.add(currentState)
                return sourceToCurrentTrace
            }

            if (currentState in currentTrace) {
                return sourceToCurrentTrace
//                if (firstIteration) return sourceToCurrentTrace
//                else throw MetronomeException("Policy does not lead to the frontier.")
            }

            sourceToCurrentTrace.add(currentState)
            currentTrace.add(currentState)

            currentState = currentNode.waveParent.state
//            currentState = bestExpandedWaveSuccessor(currentState).state
        }
    }

    //Can only be called on an expanded node
    private fun updateLocalHeuristic(currentNode: EnvelopeSearchNode<StateType>) : EnvelopeSearchNode<StateType> {
        val bestNode = domain.successors(currentNode.state)
                .map{ nodes[it.state]!! }
                .filter{ it.waveCounter == waveCounter }
                ?.minBy{ it.heuristic + it.actionCost }
            ?:
                domain.successors((currentNode.state))
                        .map{ nodes[it.state]!! }
                        .minBy{ it.heuristic + it.actionCost }!!
        currentNode.heuristic = bestNode.heuristic + bestNode.actionCost

        return bestNode
    }



    private fun getOutsideHeuristic(sourceNode: EnvelopeSearchNode<StateType>) = domain.successors(sourceNode.state)
            .map { getNode(sourceNode, it) }
            .filter { it.expanded == -1 && !it.open } // Outside of envelope
            .map { it.heuristic + it.actionCost }
            .min() ?: Double.POSITIVE_INFINITY

    private fun explore(state: StateType, terminationChecker: TerminationChecker): EnvelopeSearchNode<StateType> {
        iterationCounter++

        if (firstIteration) {
            val node = EnvelopeSearchNode(state, domain.heuristic(state), 0, 0, NoOperationAction, 0)
            node.waveParent = node
            nodes[state] = node
            openList.add(node)
            firstIteration = false
        }

        val sourceNode = nodes[state]!!
        while (!terminationChecker.reachedTermination()) {
            val currentNode = if (sourceNode.expanded == -1) {
                if (sourceNode.open) openList.remove(sourceNode)
                sourceNode
            } else openList.pop() ?: break
            // TODO don't stop
            if (domain.isGoal(currentNode.state)) {
                if (!foundGoals.contains(currentNode)) {
                    foundGoals.add(currentNode)
                }
                return currentNode
            }

            if (currentNode.iteration == 0L) { // Iteration is 0 when the node have not been expanded yet
                expandFromNode(currentNode)
            }
            terminationChecker.notifyExpansion()
        }

        return openList.peek() ?: if (foundGoals.size > 0) foundGoals[0] else null
                ?: throw GoalNotReachableException("Open list is empty.")
    }

    /**
     * Expands a node and add it to closed list. For each successor
     * it will add it to the open list and store it's g value, as long as the
     * state has not been seen before, or is found with a lower g value
     */
    fun expandFromNode(sourceNode: EnvelopeSearchNode<StateType>) {
        expandedNodeCount += 1
        expandedNodes.add(sourceNode)
        visualizer?.updateSearchEnvelope(expandedNodes)
        visualizer?.delay()

        sourceNode.iteration = expandedNodeCount.toLong()
        sourceNode.expanded = expandedNodeCount
        sourceNode.heuristic = Double.POSITIVE_INFINITY

        domain.successors(sourceNode.state).forEach { successor ->
            val successorNode = getNode(sourceNode, successor)

            val edge = SearchEdge(node = sourceNode, action = successor.action, actionCost = successor.actionCost)
            // Having a predecessor set would make this prettier, but probably slower
            if (!successorNode.predecessors.contains(edge)) {
                successorNode.predecessors.add(edge)
            }

            if (!successorNode.open && successorNode.iteration == 0L) {
                openList.add(successorNode)
            }

            //Init waveParent and frontier pointer so that the agent has some direction
            //even if the backups haven't reached it by the time it gets to this node
//            if (sourceNode.heuristic > successorNode.heuristic + successorNode.actionCost) {
//                sourceNode.heuristic = successorNode.heuristic + successorNode.actionCost
//                sourceNode.waveParent = successorNode
//                sourceNode.frontierPointer = successorNode
//            }

//            val relativeHeuristic = successorNode.heuristic + successor.actionCost

//            // This is a backup // TODO consider to remove
//            if (sourceNode.rhsHeuristic > relativeHeuristic) {
//                sourceNode.rhsHeuristic = relativeHeuristic
//                sourceNode.heuristic = relativeHeuristic
//                sourceNode.parent = successorNode
//            }
        }

//        updateRhs(sourceNode, propagate = false)
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
                    actionCost = successor.actionCost,
                    action = successor.action,
                    parent = parent,
                    cost = MAX_VALUE,
                    iteration = 0
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
}

enum class ExpansionStrategy {
    H_VALUE, PSEUDO_F
}

enum class UpdateStrategy {
    PSEUDO, RTDP
}

enum class BackupComparator {
    H_VALUE, PSEUDO_F;
}

enum class EnvelopeConfigurations(private val configurationName: String) {
    BACKLOG_RATIO("backlogRatio"),
    COMPARATOR("backupComparator");

    override fun toString() = configurationName
}


