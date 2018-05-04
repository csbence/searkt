package edu.unh.cs.ai.realtimesearch.planner.realtime

import edu.unh.cs.ai.realtimesearch.MetronomeException
import edu.unh.cs.ai.realtimesearch.environment.*
import edu.unh.cs.ai.realtimesearch.experiment.configuration.ExperimentConfiguration
import edu.unh.cs.ai.realtimesearch.experiment.terminationCheckers.TerminationChecker
import edu.unh.cs.ai.realtimesearch.planner.*
import edu.unh.cs.ai.realtimesearch.planner.exception.GoalNotReachableException
import edu.unh.cs.ai.realtimesearch.util.AdvancedPriorityQueue
import edu.unh.cs.ai.realtimesearch.util.Indexable
import edu.unh.cs.ai.realtimesearch.util.resize
import edu.unh.cs.ai.realtimesearch.visualizer
import kotlin.Long.Companion.MAX_VALUE

class EnvelopeSearch<StateType : State<StateType>>(override val domain: Domain<StateType>, val configuration: ExperimentConfiguration) :
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

        val consistent: Boolean
            get() = heuristic == rhsHeuristic
    }

    override var iterationCounter = 0L

    private val nodes: HashMap<StateType, EnvelopeSearchNode<StateType>> = HashMap<StateType, EnvelopeSearchNode<StateType>>(100000000, 1.toFloat()).resize()

    override var openList = AdvancedPriorityQueue<EnvelopeSearchNode<StateType>>(1000000, fValueComparator)

    private val expandedNodes = mutableListOf<EnvelopeSearchNode<StateType>>()

    private var rootState: StateType? = null

    private var firstIteration = true

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
        // First backtrack if we are on the best path.
        // If yes, then we don't have to do anything we follow it
        // If not, we should figure out which way to go.
        // Update current path (make sure we avoid loops, thus we probably have to reset first then update)
        // Follow the policy until we find a

        val sourceToTargetNodeChain = projectPolicy(sourceState)
        val path = constructPath(sourceToTargetNodeChain, domain)
        println("Iteration $iterationCounter is done <<< <<<")
        return path
    }

    private fun projectPolicy(sourceState: StateType): Collection<StateType> {
        val sourceToCurrentTrace = mutableListOf<StateType>()
        val currentTrace = mutableSetOf<StateType>()


        fun backpropagate() {
            sourceToCurrentTrace.asReversed().forEach { updateRhs(it) }
            sourceToCurrentTrace.clear()

            currentTrace.clear()
        }

        visualizer?.updateSearchEnvelope(expandedNodes)
        visualizer?.updateAgentLocation(nodes[sourceState]!!)

        var pathConsistent = true
        var currentState = sourceState
        while (true) {
            val currentNode = nodes[currentState] ?: throw MetronomeException("Projection exited the envelope")
            visualizer?.updateFocusedNode(currentNode)

            // Break if we the projection reaches the frontier
            if (currentNode.open && pathConsistent) break

            if (currentNode.open || currentNode.state in currentTrace) {
                // We either hit the frontier with an inconsistent path or found a loop
                println("backprop start")
                backpropagate()

                // Restart
                pathConsistent = true
                currentState = sourceState

                println("backprop end")
                continue
            }

            sourceToCurrentTrace.add(currentState)
            currentTrace.add(currentState)

            if (!currentNode.consistent) {
                pathConsistent = false
            }

            val bestSuccessor = updateRhs(currentState)
            currentState = bestSuccessor.state

            visualizer?.delay()
        }

        visualizer?.updateFocusedNode<StateType, EnvelopeSearchNode<StateType>>(null)

        return sourceToCurrentTrace
    }

    private fun updateRhs(sourceState: StateType): SuccessorBundle<StateType> {
        println("update rhs: $sourceState")

        val sourceNode = nodes[sourceState]!!

        val bestSuccessor = domain.successors(sourceState).minBy {
            getNode(sourceNode, it).heuristic + it.actionCost
        } ?: throw MetronomeException("Goal is not reachable from agent's current location")


        val bestSuccessorNode = nodes[bestSuccessor.state]!!
        val rhs = bestSuccessorNode.heuristic + bestSuccessor.actionCost
        println("  selected best: $bestSuccessor - rhs: $rhs")

        if (rhs < sourceNode.heuristic) throw MetronomeException("The heuristic is inconsistent")

        // Update heuristic
        if (rhs >= sourceNode.heuristic) {
            sourceNode.rhsHeuristic = rhs
            sourceNode.heuristic = rhs

            // If all successors were infinity we keep the previous pointer
            // In this case we might want to consider to set the parent to the best h successor
            if (rhs != Double.POSITIVE_INFINITY) {
                sourceNode.parent = bestSuccessorNode
            }

            // Heuristic was changed any node that was dependent should be updated
            resetRhsOfPredecessors(sourceNode)
        }

        return bestSuccessor
    }

    private fun resetRhsOfPredecessors(sourceNode: EnvelopeSearchNode<StateType>) {
        for (predecessorEdge in sourceNode.predecessors) {
            val predecessorNode = predecessorEdge.node
            if (predecessorNode.parent == sourceNode) {
                println("reset rhs: ${predecessorNode.state}")
                predecessorNode.rhsHeuristic = Double.POSITIVE_INFINITY
//                predecessorNode.parent = predecessorNode // We keep the optimistic pointer
            }
        }
    }


    private fun explore(state: StateType, terminationChecker: TerminationChecker): EnvelopeSearchNode<StateType> {
        iterationCounter++
        openList.reorder(heuristicComparator)

        if (firstIteration) {
            val node = EnvelopeSearchNode(state, domain.heuristic(state), 0, 0, NoOperationAction, 0)
            nodes[state] = node
            openList.add(node)
            firstIteration = false
        }

        while (!terminationChecker.reachedTermination()) {
            val topNode = openList.peek() ?: throw GoalNotReachableException("Open list is empty.")
            if (domain.isGoal(topNode.state)) return topNode

            val currentNode = openList.pop()
                    ?: throw GoalNotReachableException("Goal not reachable. Open list is empty.")

            if (currentNode.iteration == 0L) { // Iteration is 0 when the node have not been expanded yet
                expandFromNode(currentNode)
            }
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
        println("expand: ${sourceNode.state}")

        expandedNodeCount += 1
        expandedNodes.add(sourceNode)

        sourceNode.iteration = expandedNodeCount.toLong()

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

            val relativeHeuristic = successorNode.heuristic + successor.actionCost

            if (sourceNode.rhsHeuristic < relativeHeuristic) {
                sourceNode.rhsHeuristic = relativeHeuristic
                sourceNode.heuristic = relativeHeuristic
                sourceNode.parent = successorNode
            }
        }

        resetRhsOfPredecessors(sourceNode)
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
