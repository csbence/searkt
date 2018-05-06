package edu.unh.cs.ai.realtimesearch.planner.realtime

import edu.unh.cs.ai.realtimesearch.MetronomeException
import edu.unh.cs.ai.realtimesearch.environment.*
import edu.unh.cs.ai.realtimesearch.experiment.configuration.ExperimentConfiguration
import edu.unh.cs.ai.realtimesearch.experiment.result.ExperimentResult
import edu.unh.cs.ai.realtimesearch.experiment.terminationCheckers.TerminationChecker
import edu.unh.cs.ai.realtimesearch.planner.*
import edu.unh.cs.ai.realtimesearch.planner.exception.GoalNotReachableException
import edu.unh.cs.ai.realtimesearch.util.AdvancedPriorityQueue
import edu.unh.cs.ai.realtimesearch.util.Indexable
import edu.unh.cs.ai.realtimesearch.util.resize
import kotlin.Long.Companion.MAX_VALUE
import kotlin.math.max
import kotlin.math.min

/**
 * @author Bence Cserna
 * @author Kevin C. Gall
 *
 * Planner expands an envelope on a greedy best first frontier and propagates heuristic information back
 * to the agent stamped with the "iteration number," a proxy for a time stamp, of when the propagation began.
 * Agent goes toward the nodes updated most recently breaking ties on best h.
 */
class AlternateEnvelopeSearch<StateType : State<StateType>>(override val domain: Domain<StateType>,
                                                            val configuration: ExperimentConfiguration) :
        RealTimePlanner<StateType>(),
        RealTimePlannerContext<StateType, AlternateEnvelopeSearch.EnvelopeSearchNode<StateType>> {

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

        /** Nodes that generated this EnvelopeSearchNode as a successor in the current exploration phase. */
        override var predecessors: MutableList<SearchEdge<EnvelopeSearchNode<StateType>>> = arrayListOf()
        /** Successor nodes generated when this node is expanded */
        var successors: MutableList<SearchEdge<EnvelopeSearchNode<StateType>>> = arrayListOf()

        /** Parent pointer that points to the min cost predecessor. */
        override var parent: EnvelopeSearchNode<StateType> = parent ?: this

        var isGoal = false
        var expanded = false

        var rhsHeuristic = Double.POSITIVE_INFINITY

        override fun hashCode(): Int = state.hashCode()

        override fun equals(other: Any?): Boolean = when (other) {
            null -> false
            is SearchNode<*, *> -> state == other.state
            is State<*> -> state == other
            else -> false
        }

        override fun toString() =
                "RTSNode: [State: $state h: $heuristic, g: $cost, iteration: $iteration, actionCost: $actionCost, parent: ${parent.state}, open: $open]"

        /**
         *  Shallow copy of node. This is so that we can put effectively the same node into two priority queues:
         *  the frontier and the backlog open list
         */
        fun copy() : EnvelopeSearchNode<StateType> {
            val nodeCopy= EnvelopeSearchNode(state, heuristic, cost, actionCost, action, iteration, parent)
            nodeCopy.predecessors = predecessors
            nodeCopy.successors = successors
            nodeCopy.rhsHeuristic = rhsHeuristic
            nodeCopy.isGoal = isGoal
            nodeCopy.expanded = expanded

            return nodeCopy
        }
    }

    //Not using backup ratio. TODO: examine and delete if necessary
    private val backupRatio = configuration.backlogRatio ?: 0.0
    private var backupCount = 0
    private var resourceLimit = 0L
    override var iterationCounter = 0L
    private val backupComparatorType = configuration.backupComparator ?: BackupComparator.H_VALUE
    private var dijkstraComparator: Comparator<RealTimeSearchNode<StateType, EnvelopeSearchNode<StateType>>>? = null

    private val nodes: HashMap<StateType, EnvelopeSearchNode<StateType>> = HashMap<StateType, EnvelopeSearchNode<StateType>>(100000000, 1.toFloat()).resize()
    private val envelopeFrontier = AdvancedPriorityQueue<EnvelopeSearchNode<StateType>>(1000000, heuristicComparator)
    override var openList = AdvancedPriorityQueue<EnvelopeSearchNode<StateType>>(1000000, heuristicComparator)

    private var rootState: StateType? = null
    private var currentAgentState: StateType? = null
    private var goalNode: EnvelopeSearchNode<StateType>? = null

    private var executeNewBackup = true
    private var foundGoal = false
    private var goalBackedUp = false

    private val pseudoFComparator = Comparator<RealTimeSearchNode<StateType, EnvelopeSearchNode<StateType>>> { lhs, rhs ->
        //using heuristic function for pseudo-g
        val lhsPseudoG = domain.heuristic(currentAgentState!!, lhs.state)
        val rhsPseudoG = domain.heuristic(currentAgentState!!, rhs.state)
        val lhsPseudoF = lhs.heuristic + lhsPseudoG
        val rhsPseudoF = rhs.heuristic + rhsPseudoG

        //break ties on lower H -> this is better info!
        when {
            lhsPseudoF < rhsPseudoF -> -1
            lhsPseudoF > rhsPseudoF -> 1
            lhs.heuristic < rhs.heuristic -> -1
            rhs.heuristic > lhs.heuristic -> 1
            else -> 0
        }
    }

    init {
        dijkstraComparator =
                when (backupComparatorType) {
                    BackupComparator.H_VALUE -> Comparator{ lhs, rhs -> heuristicComparator.compare(lhs, rhs) }
                    BackupComparator.PSEUDO_F -> pseudoFComparator
                }

    }

    // TODO add D*
    var dStarQueue = AdvancedPriorityQueue<EnvelopeSearchNode<StateType>>(1000000, pseudoFComparator)

    override fun appendPlannerSpecificResults(results: ExperimentResult) {
        results.backupCount = this.backupCount
    }

    override fun selectAction(sourceState: StateType, terminationChecker: TerminationChecker): List<ActionBundle> {
        currentAgentState = sourceState

        if (rootState == null) {
            rootState = sourceState
        }

        if (domain.isGoal(sourceState)) {
            return emptyList()
        }

        // Exploration phase
        // TODO maybe on pseudo f not h
        val agentNode = nodes[sourceState] ?: EnvelopeSearchNode(sourceState, domain.heuristic(sourceState), 0, 0, NoOperationAction, 0)
        if (nodes[sourceState] == null) {
            nodes[sourceState] = agentNode
            generatedNodeCount++
        }

        explore(agentNode, terminationChecker)
        if (resourceLimit == 0L) resourceLimit = expandedNodeCount.toLong()

        // Backup phase

        // Note: Need to ensure that goal backups don't get overwritten
        // Therefore, we keep backing up from goal node. This is a bandaid, not
        // a real solution. TODO: come up with something better

        var freshBackup = false
        if (openList.isEmpty() || executeNewBackup) {
            openList.clear() //in case we are clearing the way for the goal

            freshBackup = true
            executeNewBackup = false
            // Refill open list with current frontier
//            envelopeFrontier.backingArray.forEach {
//                //Copying nodes to add to open list. These are frontier nodes as
//                //well, so we need to maintain their envelopeFrontier pointers!
//                if (it != null) openList.add(it.copy())
//            }

            // Seed the goal
            if (goalNode != null) openList.add(goalNode!!)
            else {
                envelopeFrontier.backingArray.forEach {
                    //Copying nodes to add to open list. These are frontier nodes as
                    //well, so we need to maintain their envelopeFrontier pointers!
                    if (it != null) openList.add(it.copy())
                }
            }
        }

        // Execute backups until we envelope the agent, run out of time, or flush the queue
        var currentBackupCount = 0
        dynamicDijkstra(this, freshBackup, dijkstraComparator!!) {
            //check if all successors of the agent have been backed up in the current iteration
            val agentEnveloped = agentNode.iteration == iterationCounter && agentNode.successors.all {successor ->
                successor.node.iteration == iterationCounter
            }
            if (agentEnveloped) executeNewBackup = true

            if (currentBackupCount >= resourceLimit || agentEnveloped) true
            else {
                currentBackupCount++
                it.isEmpty()
            }
        }

        backupCount += currentBackupCount


        return listOf(getBestAction(sourceState))
    }

    /**
     * @return The node of the agent's current location
     */
    private fun explore(sourceNode: EnvelopeSearchNode<StateType>, terminationChecker: TerminationChecker) {
        var currentNode = if (sourceNode.successors.size == 0) sourceNode else popFromFrontier()

        while (!terminationChecker.reachedTermination()) {
            /* I don't think Envelope Search wants to stop when we reach the goal node.
             * TODO: Examine this assumption
             */
//            if (domain.isGoal(topNode.state)) return topNode
            if (domain.isGoal(currentNode.state)) {
                if (currentNode.isGoal) { //this is a reexamination, meaning the queue is empty
                    break
                } else {
                    currentNode.isGoal = true
                    foundGoal = true
                    goalNode = currentNode
                }
            } else {
                expandFromNode(currentNode)
            }

            terminationChecker.notifyExpansion()
            if (!terminationChecker.reachedTermination()) currentNode = popFromFrontier()
        }
    }

    private fun popFromFrontier() : EnvelopeSearchNode<StateType> {
        envelopeFrontier.peek() ?: return goalNode ?: throw GoalNotReachableException("Open list is empty.")
        return envelopeFrontier.pop()!!
    }

    /**
     * Action chosen based on iteration counter. Agent prefers, in this order:
     * Max Iteration;
     * Best heuristic
     */
    private val nextActionComparator = Comparator<SearchEdge<EnvelopeSearchNode<StateType>>> { lhs, rhs ->
        val lhsNode = lhs.node
        val rhsNode = rhs.node

        val lhsCost = lhsNode.heuristic + lhs.actionCost
        val rhsCost = rhsNode.heuristic + rhs.actionCost

        when {
            lhsNode.iteration > rhsNode.iteration -> -1
            lhsNode.iteration < rhsNode.iteration -> 1
            lhsCost < rhsCost -> -1
            lhsCost > rhsCost -> 1
            else -> 0
        }
    }
    // Backup from best successor immediately after getting action.
    private fun getBestAction(sourceState: StateType): ActionBundle {
        val thisNode = nodes[sourceState] ?: throw MetronomeException("Agent's current state not generated. The Planner is confused")

        val actionSuccessors = thisNode.successors
        val (bestSuccessor, action, actionCost) = actionSuccessors.minWith(nextActionComparator)!!

        val newH = bestSuccessor.heuristic + actionCost
        assert(thisNode.heuristic <= newH)
        thisNode.heuristic = newH

        return ActionBundle(action, actionCost)
    }


    /**
     * Expands a node and add it to closed list. For each successor
     * it will add it to the open list and store it's g value, as long as the
     * state has not been seen before, or is found with a lower g value
     */
    fun expandFromNode(sourceNode: EnvelopeSearchNode<StateType>) {
        expandedNodeCount += 1

        var rhsHeuristic = Double.POSITIVE_INFINITY

        domain.successors(sourceNode.state).forEach { successor ->
            val successorNode = getNode(sourceNode, successor)
            // The parent is random (last caller)

            val edge = SearchEdge(node = sourceNode, action = successor.action, actionCost = successor.actionCost)
            // Having a predecessor set would make this prettier, but probably slower
            if (!successorNode.predecessors.contains(edge)) {
                successorNode.predecessors.add(edge)
            }

            if (!successorNode.expanded && !successorNode.open) {
                envelopeFrontier.add(successorNode)
            }

            rhsHeuristic = min(successorNode.heuristic + successor.actionCost, rhsHeuristic)

            sourceNode.successors.add(SearchEdge(node = successorNode, action = successor.action, actionCost = successor.actionCost))

            //if successor has been touched by goal, take the top iteration (i.e. priority) from it
            sourceNode.iteration = max(sourceNode.iteration, successorNode.iteration)
        }

        sourceNode.heuristic = rhsHeuristic
        sourceNode.rhsHeuristic = rhsHeuristic
        sourceNode.expanded = true
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
                    iteration = -1
            )

            nodes[successorState] = undiscoveredNode
            undiscoveredNode
        } else {
            tempSuccessorNode
        }
    }
}

enum class BackupComparator {
    H_VALUE, PSEUDO_F;
}

enum class EnvelopeConfigurations(private val configurationName: String) {
    BACKLOG_RATIO("backlogRatio"),
    COMPARATOR("backupComparator");

    override fun toString() = configurationName
}
