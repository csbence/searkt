package edu.unh.cs.ai.realtimesearch.planner.realtime

import edu.unh.cs.ai.realtimesearch.MetronomeException
import edu.unh.cs.ai.realtimesearch.environment.*
import edu.unh.cs.ai.realtimesearch.experiment.configuration.ExperimentConfiguration
import edu.unh.cs.ai.realtimesearch.experiment.configuration.realtime.TerminationType
import edu.unh.cs.ai.realtimesearch.experiment.terminationCheckers.TerminationChecker
import edu.unh.cs.ai.realtimesearch.experiment.terminationCheckers.getTerminationChecker
import edu.unh.cs.ai.realtimesearch.experiment.visualizerIsActive
import edu.unh.cs.ai.realtimesearch.planner.*
import edu.unh.cs.ai.realtimesearch.planner.exception.GoalNotReachableException
import edu.unh.cs.ai.realtimesearch.planner.realtime.EnvelopeSearch.EnvelopeSearchPhases.*
import edu.unh.cs.ai.realtimesearch.util.AbstractAdvancedPriorityQueue
import edu.unh.cs.ai.realtimesearch.util.AdvancedPriorityQueue
import edu.unh.cs.ai.realtimesearch.util.Indexable
import edu.unh.cs.ai.realtimesearch.util.resize
import java.util.*
import kotlin.Long.Companion.MAX_VALUE
import kotlin.math.max
import kotlin.system.measureNanoTime

class EnvelopeSearch<StateType : State<StateType>>(override val domain: Domain<StateType>, val configuration: ExperimentConfiguration) :
        RealTimePlanner<StateType>(),
        RealTimePlannerContext<StateType, EnvelopeSearch.EnvelopeSearchNode<StateType>> {

    private val backupInit = configuration.backupInit ?: BACKUP_INIT.ALL
    init {
        print(backupInit)
    }

    // Configuration - Hard Coded
    private val pseudoGWeight = 2.0

    private val greedyResourceRatio = 6.0 / 9.0 //r1
    private val pseudoFResourceRatio = 2.0 / 9.0 //r2
    //r3 is the remainder of the time

    //Top k nodes for backup initialization
    private val k = 0.15

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

        override var lastLearnedHeuristic = heuristic
        override var minCostPathLength: Long = 0L

        var waveCounter: Int = -1
        lateinit var waveParent: EnvelopeSearchNode<StateType>
        var waveHeuristic: Double = Double.POSITIVE_INFINITY
        var waveExpanded = false
        lateinit var frontierPointer: EnvelopeSearchNode<StateType>
        var backupIndex = -1

        var pseudoFOpenIndex = -1
        var heuristicOpenIndex = -1

        var expanded = -1
        var generated = -1
        
        /** LSS properties */
        var lssIteration: Long = 0L
        var lssParent = this

        override fun hashCode(): Int = state.hashCode()

        override fun equals(other: Any?): Boolean = when (other) {
            null -> false
            is SearchNode<*, *> -> state == other.state
            is State<*> -> state == other
            else -> false
        }

        override fun toString() =
                "RTSNode: [State: $state h: $heuristic, wave:$waveCounter, g: $cost, iteration: $iteration, actionCost: $actionCost, parent: ${parent.state}, open: $open]"

    }

    /* COMPARATORS */

    private val pseudoFComparator = Comparator<EnvelopeSearchNode<StateType>> { lhs, rhs ->
        //using heuristic function for pseudo-g
        val lhsPseudoG = domain.heuristic(currentAgentState, lhs.state) * pseudoGWeight
        val rhsPseudoG = domain.heuristic(currentAgentState, rhs.state) * pseudoGWeight
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

    private val waveGComparator = Comparator<EnvelopeSearchNode<StateType>> { lhs, rhs ->
        val lhsWaveG = domain.heuristic(lhs.state, currentAgentState)
        val rhsWaveG = domain.heuristic(rhs.state, currentAgentState)

        when {
            lhsWaveG < rhsWaveG -> -1
            lhsWaveG > rhsWaveG -> 1
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

    private val waveComparator = Comparator<EnvelopeSearchNode<StateType>> { lhs, rhs ->
        when {
            lhs.waveCounter > rhs.waveCounter -> -1
            lhs.waveCounter < rhs.waveCounter -> 1
            lhs.heuristic < rhs.heuristic -> -1
            lhs.heuristic > rhs.heuristic -> 1
            else -> 0
        }
    }
    
    private val lssComparator = Comparator<EnvelopeSearchNode<StateType>> { lhs, rhs ->
        val lhsLocalF = lhs.heuristic + lhs.cost
        val rhsLocalF = rhs.heuristic + rhs.cost
        
        when {
            lhs.waveCounter > rhs.waveCounter -> -1
            lhs.waveCounter < rhs.waveCounter -> 1
            lhsLocalF < rhsLocalF -> -1
            lhsLocalF > rhsLocalF -> 1
            lhs.heuristic < rhs.heuristic -> -1
            lhs.heuristic > rhs.heuristic -> 1
            else -> 0
        }
    }

    /* SPECIALIZED PRIORITY QUEUES */

    inner class HeuristicOpenList : AbstractAdvancedPriorityQueue<EnvelopeSearchNode<StateType>>(arrayOfNulls(1000000), heuristicComparator) {
        override fun getIndex(item: EnvelopeSearchNode<StateType>): Int = item.heuristicOpenIndex
        override fun setIndex(item: EnvelopeSearchNode<StateType>, index: Int) {
            item.heuristicOpenIndex = index
        }
    }

    inner class LssOpenList : AbstractAdvancedPriorityQueue<EnvelopeSearchNode<StateType>>(arrayOfNulls(1000000), lssComparator) {
        override fun getIndex(item: EnvelopeSearchNode<StateType>): Int = item.pseudoFOpenIndex
        override fun setIndex(item: EnvelopeSearchNode<StateType>, index: Int) {
            item.pseudoFOpenIndex = index
        }

        override fun pop(): EnvelopeSearchNode<StateType> {
            val node = super.pop() ?: throw GoalNotReachableException("Goal not reachable")
            if (isOpen(node)) heuristicOpenList.remove(node)

            return node
        }
    }

    private var heuristicOpenList = HeuristicOpenList()
    private var lssOpenList = LssOpenList()

    // Main Node-container data structures
    private val nodes: HashMap<StateType, EnvelopeSearchNode<StateType>> = HashMap<StateType, EnvelopeSearchNode<StateType>>(100_000_000, 1.toFloat()).resize()
    override var openList = AdvancedPriorityQueue(1000000, pseudoFComparator)
    private var waveFrontier = object : AbstractAdvancedPriorityQueue<EnvelopeSearchNode<StateType>>(arrayOfNulls(1000000), waveGComparator) {
        override fun getIndex(item: EnvelopeSearchNode<StateType>): Int = item.backupIndex
        override fun setIndex(item: EnvelopeSearchNode<StateType>, index: Int) {
            item.backupIndex = index
        }
    }

    // Visualizer properties
    private val expandedNodes = ArrayList<EnvelopeSearchNode<StateType>>(1000000)

    private val backedUpNodes = ArrayList<EnvelopeSearchNode<StateType>>(1000000)
    private var clearPreviousBackup = false

    // Current and discovered states
    private var rootState: StateType? = null
    private lateinit var currentAgentState: StateType
    private val foundGoals = mutableListOf<EnvelopeSearchNode<StateType>>()

    /* State of the algorithm */
    // Counters
    override var iterationCounter = 0L
    private var waveCounter = 0

    //Wave initialization properties
    private var initializationIndex: Int? = null
    private var heapifyIndex: Int? = null
    private val nodesToAdd: MutableSet<EnvelopeSearchNode<StateType>> = mutableSetOf()
    private val nodesToRemove: MutableSet<EnvelopeSearchNode<StateType>> = mutableSetOf()
    //convenience function to determine if initialization is in progress
    private val isWaveInitializationInProgress
        get() = initializationIndex != null || heapifyIndex != null || nodesToAdd.size > 0 || nodesToRemove.size > 0

    private var agentLastWaveFrontier = -1

    // Phase state
    private var searchPhase = GOAL_SEARCH
    private var lastPlannedPath = mutableSetOf<StateType>()

    /**
     * "Prime" the nodes hashmap. Necessary for real time bounds to avoid hashmap startup costs
     */
    override fun init(rootState: StateType) {
        val primer = EnvelopeSearchNode(rootState,
                0.0, 0L, 0L, NoOperationAction, 0L)
        nodes[rootState] = primer
        nodes.remove(rootState)
    }

    override fun selectAction(sourceState: StateType, terminationChecker: TerminationChecker): List<ActionBundle> {
        if (rootState == null) {
            rootState = sourceState

            val node = EnvelopeSearchNode(sourceState, domain.heuristic(sourceState), 0, 0, NoOperationAction, 0)
            node.waveParent = node
            nodes[sourceState] = node

            addToOpen(node)
        }

        currentAgentState = sourceState

        if (domain.isGoal(sourceState)) {
            return emptyList()
        }

        var greedyTimeSlice = (terminationChecker.remaining() * greedyResourceRatio).toLong()
        var pseudoFTimeSlice = (terminationChecker.remaining() * pseudoFResourceRatio).toLong()

        if (isWaveInitializationInProgress) {
            greedyTimeSlice = 0
            pseudoFTimeSlice = 0
        } else if (searchPhase == GOAL_BACKUP) {
            greedyTimeSlice = 0
        }

        var wavePropagationTimeSlice = terminationChecker.remaining() - (greedyTimeSlice + pseudoFTimeSlice)
        var nextState = currentAgentState

        iterationCounter++
        val searchTime = measureNanoTime {
            if (foundGoals.isEmpty() && !isWaveInitializationInProgress) {
                // searchPhase GOAL_SEARCH

                // Timing: number of expansions - no compensation is necessary
                explore(sourceState, getTerminationChecker(configuration, greedyTimeSlice), heuristicOpenList)
            }

            if (configuration.terminationType == TerminationType.TIME &&  pseudoFTimeSlice < configuration.terminationTimeEpsilon) {
                pseudoFTimeSlice += configuration.terminationTimeEpsilon
            }
            
            val targetNode = if (searchPhase == PATH_IMPROVEMENT) {
                exploreLss(sourceState, getTerminationChecker(configuration, greedyTimeSlice + pseudoFTimeSlice))
            } else {
                exploreLss(sourceState, getTerminationChecker(configuration, pseudoFTimeSlice))
            }

            val projectPathTime = measureNanoTime {
                projectPath(sourceState, targetNode).apply {
                    nextState = this.first
                    lastPlannedPath = this.second
                }
            }
            printMessage("""Project path time: $projectPathTime""")
        }
        printMessage("""Search Time: $searchTime""")

        val agentNode = nodes[currentAgentState]!!

        // If the agent has reached the wave frontier before it has been backed
        // up, start a new frontier backup!
        val safetyWaveClear = measureNanoTime {
            if (agentNode.waveCounter == waveCounter && !isWaveInitializationInProgress) {
                waveFrontier.quickClear()
                if (searchPhase == GOAL_BACKUP) {
                    searchPhase = PATH_IMPROVEMENT
                    waveFrontier.reorder(waveFComparator)
                }
            }
        }
        printMessage("""Clear wave time (agent reached wave frontier): $safetyWaveClear""")

        //To appease expansion termination checkers, we need to instantiate a final checker from the calculated remaining time
        val backupTerminationChecker = if (configuration.terminationType == TerminationType.TIME) terminationChecker
        else getTerminationChecker(configuration, wavePropagationTimeSlice)

        val wavePropagationTime = measureNanoTime {
            wavePropagation(currentAgentState, backupTerminationChecker, lastPlannedPath)
        }
        printMessage("""Wave Propagation Time: $wavePropagationTime""")

        //Don't use constructPath here! That method uses Kotlin collections which become bottlenecks
        val transition = domain.transition(sourceState, nextState)
                ?: throw GoalNotReachableException("Dead end found")
        return listOf(RealTimePlanner.ActionBundle(transition.first, transition.second))
    }

    private fun explore(state: StateType, terminationChecker: TerminationChecker, explorationQueue: AbstractAdvancedPriorityQueue<EnvelopeSearchNode<StateType>>): EnvelopeSearchNode<StateType> {
        val sourceNode = nodes[state]!!
        while (!terminationChecker.reachedTermination()) {
            // Must expand current node if not already expanded, meaning it is likely on the envelope frontier.
            // If we expand it now, we must remove from the open list rather than expand it again later (which doesn't make sense)
            val currentNode = if (sourceNode.expanded == -1) sourceNode
            else explorationQueue.pop() ?: break
            removeFromOpen(currentNode)

            // TODO when in domains with multiple goals, don't stop looking for goals
            if (domain.isGoal(currentNode.state)) {
                if (!foundGoals.contains(currentNode)) {
                    foundGoals.add(currentNode)
                }
                return currentNode
            }

            if (currentNode.expanded == -1) { // Expanded is -1 when not expanded yet
                expandFromNode(currentNode)
            }
            terminationChecker.notifyExpansion()
        }

        return explorationQueue.peek() ?: if (foundGoals.size > 0) foundGoals[0] else null
                ?: throw GoalNotReachableException("Open list is empty.")
    }
    
    private fun exploreLss(root: StateType, terminationChecker: TerminationChecker): EnvelopeSearchNode<StateType> {
        lssOpenList.quickClear()
        
        lssOpenList.add(nodes[root]!!)
        var currentNode = nodes[root]!!
        
        //reset cost now
        currentNode.cost = 0L
        currentNode.lssParent = currentNode

        do {
            val topNode = lssOpenList.peek() ?: throw GoalNotReachableException("No reachable goals from the agent's current location")
            if (domain.isGoal(topNode.state)) return topNode

            currentNode = lssOpenList.pop()
            expandLssNode(currentNode)

            terminationChecker.notifyExpansion()
        }
        while (!terminationChecker.reachedTermination())
        
        return lssOpenList.peek() ?: throw GoalNotReachableException("No reachable goals from the agent's current location")
    }

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

            val edge = SearchEdge(node = sourceNode, action = successor.action, actionCost = successor.actionCost)
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
    
    private fun expandLssNode(sourceNode: EnvelopeSearchNode<StateType>) {
        expandedNodeCount += 1
        sourceNode.expanded = expandedNodeCount
        
        sourceNode.lssIteration = iterationCounter
        
        domain.successors(sourceNode.state).forEach { successor -> 
            val successorNode = getNode(sourceNode, successor)
            if (successorNode.heuristic == Double.POSITIVE_INFINITY) {
                // Ignore this successor as it is a dead end
                return@forEach
            }

            val edge = SearchEdge(node = sourceNode, action = successor.action, actionCost = successor.actionCost)
            // Having a predecessor set would make this prettier, but probably slower
            if (!successorNode.predecessors.contains(edge)) {
                successorNode.predecessors.add(edge)
            }
            
            if (successorNode.lssIteration != iterationCounter) {
                successorNode.apply{
                    lssIteration = iterationCounter
                    cost = MAX_VALUE
                    pseudoFOpenIndex = -1 //reset open list index
                }
            }
            // Skip if we got back to the parent
            if (successor.state == sourceNode.lssParent.state) {
                return@forEach
            }

            val successorGValueFromCurrent = sourceNode.cost + successor.actionCost
            if (successorNode.cost > successorGValueFromCurrent) {
                // here we generate a state. We store it's g value and remember how to get here via the treePointers
                successorNode.apply {
                    cost = successorGValueFromCurrent
                    lssParent = sourceNode
                    minCostPathLength = sourceNode.minCostPathLength + 1
                    action = successor.action
                    actionCost = successor.actionCost
                }

                if (successorNode.pseudoFOpenIndex == -1) {
                    lssOpenList.add(successorNode)
                } else {
                    lssOpenList.update(successorNode)
                }
                
                //add to the h open list
                if (!isOpen(successorNode) && successorNode.expanded == -1) {
                    addToOpen(successorNode)
                }
            }
        }
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

                waveFrontier.quickClear()
                break
            }

            if (searchPhase == GOAL_SEARCH && waveFront.state in agentPath) {
                waveFrontier.quickClear()
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

            printMessage("""Open list size: ${heuristicOpenList.size}""")

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
            val currentK = heuristicOpenList.size * k
            var count = 0
            initializationIndex = waveFrontier.initializeFromQueue(heuristicOpenList, terminationChecker, initializationIndex!!) {
                initWaveNode(it)
                count++
                false
            }
        }

        if (beginHeapify) heapifyIndex = waveFrontier.heapify(terminationChecker)
        else if (heapifyIndex != null) heapifyIndex = waveFrontier.heapify(terminationChecker, heapifyIndex!!)

        if (backupInit == BACKUP_INIT.TOP_K) waveFrontier.keepTopK(0.15)

        //once init process is done, add / remove nodes from the pseudoFOpenList as necessary
        if (initializationIndex == null && heapifyIndex == null && (nodesToAdd.size > 0 || nodesToRemove.size > 0)) {
            while (!terminationChecker.reachedTermination()) {
                if (nodesToRemove.size > 0) {
                    heuristicOpenList.remove(nodesToRemove.first())
                    nodesToRemove.remove(nodesToRemove.first())
                } else if (nodesToAdd.size > 0) {
                    heuristicOpenList.add(nodesToAdd.first())
                    nodesToAdd.remove(nodesToAdd.first())
                } else {
                    break
                }
            }
        }
    }

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

            if (outdated) {
                waveFrontier.add(predecessorNode)
            } else {
                waveFrontier.update(predecessorNode)
            }
        }

    }

    /**
     * @return The set of states in the agent's current intended path
     */
    private fun projectPath(sourceState: StateType, targetNode: EnvelopeSearchNode<StateType>): Pair<StateType, MutableSet<StateType>> {
        val currentTrace = mutableSetOf<StateType>()

        var currentNode = targetNode
        var prevState = targetNode.state

        while (currentNode.state != sourceState) {
            currentTrace.add(currentNode.state)
            prevState = currentNode.state
            currentNode = currentNode.lssParent
        }
        
        return Pair(prevState, currentTrace)
    }

    /**
     *  Updates the heuristic value via a one-step lookahead
     *  Can only be called on an expanded node
     */
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

    private fun addToOpen(successorNode: EnvelopeSearchNode<StateType>) {
        // During path improvement the heuristic open list is not used
        if (searchPhase != PATH_IMPROVEMENT) {
            if (isWaveInitializationInProgress) nodesToAdd.add(successorNode)
            else heuristicOpenList.add(successorNode)
        }
    }

    private fun isOpen(node: EnvelopeSearchNode<StateType>): Boolean = node.heuristicOpenIndex > -1

    private fun removeFromOpen(node: EnvelopeSearchNode<StateType>) {
        if (node.heuristicOpenIndex > -1) {
            if (isWaveInitializationInProgress) {
                nodesToRemove.add(node)
                nodesToAdd.remove(node)
            }
            else heuristicOpenList.remove(node)
        }
    }

    enum class EnvelopeSearchPhases {
        GOAL_SEARCH,
        GOAL_BACKUP,
        PATH_IMPROVEMENT
    }

    private fun printMessage(msg: String) = 0//println(msg)

    override fun getIterationSummary(): IterationSummary<StateType> {
        val expandedNodeMap = mutableMapOf<StateType, Map<String, String>>()
        val backedUpNodeMap = mutableMapOf<StateType, Map<String, String>>()

        expandedNodes.forEach{
            expandedNodeMap[it.state] = mapOf("h" to it.heuristic.toString())
        }
        expandedNodes.clear() //reset

        backedUpNodes.forEach{

            backedUpNodeMap[it.state] = mapOf(
                    "h" to it.heuristic.toString(),
                    "\"Wave\" h" to it.waveHeuristic.toString(),
                    "Wave Counter" to it.waveCounter.toString(),
                    "Wave Parent" to it.waveParent.state.toString()
            )
        }
        backedUpNodes.clear() //reset

        val summary = IterationSummary(expandedNodeMap, false, backedUpNodeMap, clearPreviousBackup, lastPlannedPath)
        clearPreviousBackup = false

        return summary
    }
}

enum class BACKUP_INIT {
    ALL, TOP_K
}

enum class UpdateStrategy {
    PSEUDO, RTDP
}

enum class BackupComparator {
    H_VALUE, PSEUDO_F;
}

enum class EnvelopeConfigurations(private val configurationName: String) {
    BACKUP_INIT("backupInit"),
    COMPARATOR("backupComparator");

    override fun toString() = configurationName
}


