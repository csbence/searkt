package edu.unh.cs.ai.realtimesearch.planner.realtime

import edu.unh.cs.ai.realtimesearch.MetronomeException
import edu.unh.cs.ai.realtimesearch.environment.*
import edu.unh.cs.ai.realtimesearch.experiment.terminationCheckers.TerminationChecker
import edu.unh.cs.ai.realtimesearch.planner.*
import edu.unh.cs.ai.realtimesearch.planner.exception.GoalNotReachableException
import edu.unh.cs.ai.realtimesearch.util.AdvancedPriorityQueue
import edu.unh.cs.ai.realtimesearch.util.Indexable
import edu.unh.cs.ai.realtimesearch.util.resize
import kotlin.Long.Companion.MAX_VALUE
import kotlin.math.min

class EnvelopeSearch<StateType : State<StateType>>(override val domain: Domain<StateType>) :
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

        /** Nodes that generated this SafeRealTimeSearchNode as a successor in the current exploration phase. */
        override var predecessors: MutableList<SearchEdge<EnvelopeSearchNode<StateType>>> = arrayListOf()

        /** Parent pointer that points to the min cost predecessor. */
        override var parent: EnvelopeSearchNode<StateType> = parent ?: this

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

    override var iterationCounter = 0L

    private val nodes: HashMap<StateType, EnvelopeSearchNode<StateType>> = HashMap<StateType, EnvelopeSearchNode<StateType>>(100000000, 1.toFloat()).resize()

    override var openList = AdvancedPriorityQueue<EnvelopeSearchNode<StateType>>(1000000, fValueComparator)

    // TODO add D*
    var dStarQueue = AdvancedPriorityQueue<EnvelopeSearchNode<StateType>>(1000000, fValueComparator)

    private var rootState: StateType? = null

    override fun selectAction(sourceState: StateType, terminationChecker: TerminationChecker): List<ActionBundle> {
        if (rootState == null) {
            rootState = sourceState
        }

        if (domain.isGoal(sourceState)) {
            return emptyList()
        }

        // Exploration phase
        // TODO maybe on pseudo f not h
        val targetNode = explore(sourceState, terminationChecker)

        // Backup phase


        return listOf(getBestAction(sourceState))
    }

    private fun getBestAction(sourceState: StateType): ActionBundle {
        val (_, action, actionCost) = domain.successors(sourceState).minBy {
            nodes[it.state]!!.heuristic + it.actionCost
        } ?: throw MetronomeException("Goal is not reachable from agent's current location")

        return ActionBundle(action, actionCost)
    }

    private fun explore(state: StateType, terminationChecker: TerminationChecker): EnvelopeSearchNode<StateType> {
        iterationCounter++
        openList.reorder(heuristicComparator)

        val node = EnvelopeSearchNode(state, domain.heuristic(state), 0, 0, NoOperationAction, 0)
        nodes[state] = node
        openList.add(node)

        while (!terminationChecker.reachedTermination()) {
            val topNode = openList.peek() ?: throw GoalNotReachableException("Open list is empty.")
            if (domain.isGoal(topNode.state)) return topNode

            val currentNode = openList.pop()
                    ?: throw GoalNotReachableException("Goal not reachable. Open list is empty.")

            expandFromNode(currentNode)
            terminationChecker.notifyExpansion()
        }

        return openList.peek() ?: throw GoalNotReachableException("Open list is empty.")
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

            if (!successorNode.open) {
                openList.add(successorNode)
            }

            rhsHeuristic = min(successorNode.heuristic, rhsHeuristic)
        }

        sourceNode.heuristic = rhsHeuristic
        sourceNode.rhsHeuristic = rhsHeuristic
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

            nodes[successorState] = undiscoveredNode
            undiscoveredNode
        } else {
            tempSuccessorNode
        }
    }

}
