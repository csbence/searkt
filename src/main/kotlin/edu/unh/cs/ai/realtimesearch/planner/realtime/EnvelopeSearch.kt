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
class EnvelopeSearch<StateType : State<StateType>>(override val domain: Domain<StateType>,
                                                   val configuration: ExperimentConfiguration) :
        RealTimePlanner<StateType>(),
        RealTimePlannerContext<StateType, EnvelopeSearch.EnvelopeSearchNode<StateType>> {

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


    }

    private val backupRatio = configuration.backlogRatio ?: 0.0
    private var backupCount = 0
    private var resourceLimit = 0L
    override var iterationCounter = 0L

    private val nodes: HashMap<StateType, EnvelopeSearchNode<StateType>> = HashMap<StateType, EnvelopeSearchNode<StateType>>(100000000, 1.toFloat()).resize()
    override var openList = AdvancedPriorityQueue<EnvelopeSearchNode<StateType>>(1000000, heuristicComparator)

    private var rootState: StateType? = null
    private var currentAgentState: StateType? = null

    private var goalNode: EnvelopeSearchNode<StateType>? = null
    private var newGoalDiscovered = false
    private var currentTarget: EnvelopeSearchNode<StateType>? = null
    private var goalReachedAgent = false

    private val pseudoFComparator = Comparator<EnvelopeSearchNode<StateType>> { lhs, rhs ->
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

        if (!goalReachedAgent) {
            // Exploration phase
            // TODO maybe on pseudo f not h
            val targetNode = explore(sourceState, terminationChecker)
            if (resourceLimit == 0L) resourceLimit = expandedNodeCount.toLong()

            // Backup phase
            flushBackups(sourceState, targetNode, resourceLimit)
        }


        return listOf(getBestAction(sourceState))
    }

    private fun explore(state: StateType, terminationChecker: TerminationChecker): EnvelopeSearchNode<StateType> {
        iterationCounter++

        val node = nodes[state] ?: EnvelopeSearchNode(state, domain.heuristic(state), 0, 0, NoOperationAction, 0)
        if (nodes[state] == null) {
            nodes[state] = node
            generatedNodeCount++
        }

        var currentNode = if (node.successors.size == 0) node else popFromOpen()

        while (!terminationChecker.reachedTermination()) {
            /* I don't think Envelope Search wants to stop when we reach the goal node.
             * TODO: Examine this assumption
             */
//            if (domain.isGoal(topNode.state)) return topNode
            if (domain.isGoal(currentNode.state)) {
                if (currentNode.isGoal) { //this is a reexamination, meaning the queue is empty
                    return currentNode
                } else {
                    newGoalDiscovered = true
                    goalNode = currentNode
                    currentNode.isGoal = true
                    //we want to expand the envelope closest to the agent now
                    openList.reorder(pseudoFComparator)

                    // Adding to backup since we do care about predecessors
                    currentNode.iteration = Long.MAX_VALUE //goal is important! should always be top priority!
                }
            } else {
                expandFromNode(currentNode)
            }

            terminationChecker.notifyExpansion()
            if (!terminationChecker.reachedTermination()) currentNode = popFromOpen()
        }

        return (if(newGoalDiscovered) goalNode else currentNode) ?: goalNode ?: throw GoalNotReachableException("Open list is empty.")
    }

    private fun popFromOpen() : EnvelopeSearchNode<StateType> {
        openList.peek() ?: return goalNode ?: throw GoalNotReachableException("Open list is empty.")
        return openList.pop()!!
    }

    private fun flushBackups(currentState: StateType, targetNode: EnvelopeSearchNode<StateType>, currentResourceLimit: Long) {
        val backupLimit = (currentResourceLimit * backupRatio).toLong()

        if (newGoalDiscovered && targetNode.isGoal) {
            newGoalDiscovered = false
            dStarQueue.clear()

            currentTarget = targetNode
            //add goal's predecessors to queue, not the goal state itself
            targetNode.predecessors.forEach {
                it.node.iteration = targetNode.iteration
                dStarQueue.add(it.node)
            }
        } else if (dStarQueue.isEmpty()) {
            dStarQueue.add(targetNode)
            currentTarget = targetNode
        } else {
            //resort for proximity to agent
            dStarQueue.reorder(pseudoFComparator)
        }

        for (i in 1..backupLimit) {
            //if backlog queue is fully flushed, return
            val topNode = dStarQueue.pop() ?: return
            backupNode(topNode)

            // if we reach the agent, back up all its successors so that the agent makes an informed decision,
            // then clear the queue
            // TODO: Force this to be within the backlog limit
            if (topNode.state == currentState) {
                topNode.successors.forEach{
                    if (it.node.successors.size > 0) backupNode(it.node)
                }

                if (currentTarget!!.isGoal) goalReachedAgent = true

                dStarQueue.clear()
                break
            }
        }
    }

    /**
     * Action chosen based on iteration counter. Agent prefers, in this order:
     * Is the Target Node;
     * Max Iteration;
     * Not on the backlog queue;
     * Best heuristic
     */
    private val nextActionComparator = Comparator<SearchEdge<EnvelopeSearchNode<StateType>>> { lhs, rhs ->
        val lhsNode = lhs.node
        val rhsNode = rhs.node

        val lhsCost = lhsNode.heuristic + lhs.actionCost
        val rhsCost = rhsNode.heuristic + rhs.actionCost

        when {
            lhsNode == currentTarget -> -1
            rhsNode == currentTarget -> 1
            lhsNode.iteration > rhsNode.iteration -> -1
            lhsNode.iteration < rhsNode.iteration -> 1
            !lhsNode.open && rhsNode.open -> -1
            lhsNode.open && !rhsNode.open -> 1
            lhsCost < rhsCost -> -1
            lhsCost > rhsCost -> 1
            else -> 0
        }
    }
    // Backup immediately after getting action.
    // Note: Inadmissible heuristic results!
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
        sourceNode.iteration = expandedNodeCount.toLong()

        domain.successors(sourceNode.state).forEach { successor ->
            val successorNode = getNode(sourceNode, successor)
            // The parent is random (last caller)

            val edge = SearchEdge(node = sourceNode, action = successor.action, actionCost = successor.actionCost)
            // Having a predecessor set would make this prettier, but probably slower
            if (!successorNode.predecessors.contains(edge)) {
                successorNode.predecessors.add(edge)
            }

            if (!successorNode.expanded) {
                openList.add(successorNode)
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

    /**
     * Backs up node by taking the best successor heursitic plus cost to get there from those successors with the
     * highest "iteration counter"
     * Adds all predecessors to backup queue (dStarQueue) and resets their iteration counter
     */
    private fun backupNode(node: EnvelopeSearchNode<StateType>) {
        if (node.isGoal) return

        val maxIteration = node.successors.map { it.node.iteration }.max() ?: 0L
        val bestSuccessorEdge =
            node.successors.filter {
                it.node.iteration == maxIteration
            } .minBy {
                it.node.heuristic + it.actionCost
            } ?: throw MetronomeException("No viable successor edges. The planner is confused")

        val tempH = node.heuristic
        node.heuristic = bestSuccessorEdge.node.heuristic + bestSuccessorEdge.actionCost
        node.rhsHeuristic = node.heuristic

        val hChanged = tempH != node.heuristic

        node.predecessors.forEach {
            if (it.node.iteration < node.iteration || hChanged) {
                it.node.iteration = node.iteration

                if (!it.node.open) dStarQueue.add(it.node)
            }
        }

        backupCount++
    }

    enum class EnvelopeConfigurations(private val configurationName: String) {
        BACKLOG_RATIO("backlogRatio");

        override fun toString() = configurationName
    }
}
