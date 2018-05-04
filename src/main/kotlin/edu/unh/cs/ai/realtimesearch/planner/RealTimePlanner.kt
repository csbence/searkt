package edu.unh.cs.ai.realtimesearch.planner

import edu.unh.cs.ai.realtimesearch.MetronomeException
import edu.unh.cs.ai.realtimesearch.environment.Action
import edu.unh.cs.ai.realtimesearch.environment.Domain
import edu.unh.cs.ai.realtimesearch.environment.State
import edu.unh.cs.ai.realtimesearch.environment.SuccessorBundle
import edu.unh.cs.ai.realtimesearch.experiment.terminationCheckers.TerminationChecker
import edu.unh.cs.ai.realtimesearch.planner.exception.GoalNotReachableException
import edu.unh.cs.ai.realtimesearch.util.AdvancedPriorityQueue
import edu.unh.cs.ai.realtimesearch.util.Indexable

/**
 * A planner for real time search environments, where a constraint is placed
 * on the the amount of time allowed to plan. A RTS planner requires to return
 * a action within a certain time limit
 *
 * @param domain: The domain to plan in
 */
abstract class RealTimePlanner<StateType : State<StateType>> : Planner<StateType>() {
    /**
     * Data class to store [Action]s along with their execution time.
     *
     * The [duration] is measured in nanoseconds.
     */
    data class ActionBundle(val action: Action, val duration: Long)

    /**
     * Returns an action while abiding the termination checker's criteria.
     *
     * @param sourceState is the sourceState to pick an action for
     * @param terminationChecker provides the termination criteria
     * @return an action for current sourceState
     */
    abstract fun selectAction(sourceState: StateType, terminationChecker: TerminationChecker): List<ActionBundle>

    /**
     * Called before the first [selectAction] call.
     *
     * This call does not count towards the planning time.
     */
    open fun init() {

    }

}

data class SearchEdge<out Node>(val node: Node, val action: Action, val actionCost: Long) {

    override fun hashCode(): Int = node!!.hashCode()

    override fun equals(other: Any?): Boolean = when (other) {
        null -> false
        is SearchEdge<*> -> node == other.node
        is SearchEdge<*> -> node == other.node
        else -> false
    }
}

interface SearchNode<StateType : State<StateType>, NodeType : SearchNode<StateType, NodeType>> : Indexable {
    val state: StateType
    var heuristic: Double
    var cost: Long
    var actionCost: Long
    var action: Action
    var parent: NodeType
    val predecessors: MutableList<SearchEdge<NodeType>>

    val f: Double
        get() = cost + heuristic
}

val fValueComparator: java.util.Comparator<SearchNode<*, *>> = Comparator { lhs, rhs ->
    when {
        lhs.f < rhs.f -> -1
        lhs.f > rhs.f -> 1
        lhs.cost > rhs.cost -> -1 // Tie braking on cost (g)
        lhs.cost < rhs.cost -> 1
        else -> 0
    }
}

val heuristicComparator: java.util.Comparator<SearchNode<*, *>> = Comparator { lhs, rhs ->
    when {
        lhs.heuristic < rhs.heuristic -> -1
        lhs.heuristic > rhs.heuristic -> 1
        else -> 0
    }
}


interface RealTimeSearchNode<StateType : State<StateType>, NodeType : SearchNode<StateType, NodeType>> : SearchNode<StateType, NodeType> {
    var iteration: Long
}

class PureRealTimeSearchNode<StateType : State<StateType>>(
        override val state: StateType,
        override var heuristic: Double,
        override var cost: Long,
        override var actionCost: Long,
        override var action: Action,
        override var iteration: Long,
        parent: PureRealTimeSearchNode<StateType>? = null
) : RealTimeSearchNode<StateType, PureRealTimeSearchNode<StateType>>, Indexable {

    /** Item index in the open list. */
    override var index: Int = -1

    /** Nodes that generated this SafeRealTimeSearchNode as a successor in the current exploration phase. */
    override var predecessors: MutableList<SearchEdge<PureRealTimeSearchNode<StateType>>> = arrayListOf()

    /** Parent pointer that points to the min cost predecessor. */
    override var parent: PureRealTimeSearchNode<StateType> = parent ?: this

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

interface RealTimePlannerContext<StateType : State<StateType>, NodeType : RealTimeSearchNode<StateType, NodeType>> {
    val domain: Domain<StateType>
    var expandedNodeCount: Int
    val openList: AdvancedPriorityQueue<NodeType>
    var iterationCounter: Long
    fun getNode(parent: NodeType, successor: SuccessorBundle<StateType>): NodeType
}

/**
 * Extracts an action sequence that leads from the start state to the target state.
 * The path follows the parent pointers from the target to the start in reversed order.
 *
 * @return path from source to target if exists.
 */
fun <StateType : State<StateType>, NodeType : SearchNode<StateType, NodeType>> extractPath(targetNode: NodeType?, sourceState: StateType): List<RealTimePlanner.ActionBundle> {
    targetNode ?: return emptyList()

    val actions = ArrayList<RealTimePlanner.ActionBundle>(1000)
    var currentNode: NodeType = targetNode

    if (targetNode.state == sourceState) {
        return emptyList()
    }

    // keep on pushing actions to our queue until source state (our root) is reached
    do {
        actions.add(RealTimePlanner.ActionBundle(currentNode.action, currentNode.actionCost))
        currentNode = currentNode.parent
    } while (currentNode.state != sourceState)

    return actions.reversed()
}


fun <StateType : State<StateType>> constructPath(statePath: Collection<StateType>, domain: Domain<StateType>): List<RealTimePlanner.ActionBundle> {
    if (statePath.isEmpty()) {
        throw MetronomeException("Cannot construct path from empty list")
    }

    return statePath
            .windowed(partialWindows = false, size = 2) {
                domain.transition(it[0], it[1])
                        ?: throw MetronomeException("Unable to construct path on the given state sequence")
            }
            .map { RealTimePlanner.ActionBundle(it.first, it.second) }
}

/**
 * Extracts an node sequence that leads from the boundary state(s) to the target state.
 * The path follows the parent pointers from the target to the start in reversed order.
 *
 * @return path from source to target if exists.
 */
fun <StateType : State<StateType>, NodeType : SearchNode<StateType, NodeType>> extractNodeChain(targetNode: NodeType?, boundaryChecker: (StateType) -> Boolean): List<NodeType> {
    targetNode ?: return emptyList()

    if (boundaryChecker(targetNode.state)) {
        return emptyList()
    }

    var currentNode: NodeType = targetNode
    val parentChain = mutableListOf(currentNode)

    do {
        parentChain.add(currentNode.parent)
        currentNode = currentNode.parent
    } while (!boundaryChecker(currentNode.state))

    return parentChain.reversed()
}


/**
 * Expands a node and add it to closed list. For each successor
 * it will add it to the open list and store it's g value, as long as the
 * state has not been seen before, or is found with a lower g value
 */
inline fun <StateType : State<StateType>, NodeType : RealTimeSearchNode<StateType, NodeType>> expandFromNode(
        context: RealTimePlannerContext<StateType, NodeType>,
        sourceNode: NodeType,
        nodeFound: ((NodeType) -> Unit)) {

    context.expandedNodeCount += 1

    for (successor in context.domain.successors(sourceNode.state)) {
        val successorState = successor.state
        val successorNode = context.getNode(sourceNode, successor)

        if (successorNode.heuristic == Double.POSITIVE_INFINITY
                && successorNode.iteration != context.iterationCounter) {
            // Ignore this successor as it is a dead end
            continue
        }

        // If the node is outdated it should be updated.
        if (successorNode.iteration != context.iterationCounter) {
            nodeFound(successorNode)
            successorNode.apply {
                iteration = context.iterationCounter
                predecessors.clear()
                cost = Long.MAX_VALUE
                // parent, action, and actionCost is outdated too, but not relevant.
            }
        }

        // Add the current state as the predecessor of the child state
        successorNode.predecessors.add(SearchEdge(node = sourceNode, action = successor.action, actionCost = successor.actionCost))

        // Skip if we got back to the parent
        if (successorState == sourceNode.parent.state) {
            continue
        }

        // only generate those state that are not visited yet or whose cost value are lower than this path
        val successorGValueFromCurrent = sourceNode.cost + successor.actionCost
        if (successorNode.cost > successorGValueFromCurrent) {
            // here we generate a state. We store it's g value and remember how to get here via the treePointers
            successorNode.apply {
                cost = successorGValueFromCurrent
                parent = sourceNode
                action = successor.action
                actionCost = successor.actionCost
            }

            if (!successorNode.open) {
                context.openList.add(successorNode) // Fresh node not on the open yet
            } else {
                context.openList.update(successorNode)
            }
        }
    }

    sourceNode.heuristic = Double.POSITIVE_INFINITY
}

/**
 * Performs Dijkstra updates until runs out of resources or done
 *
 * Updates the mode to SEARCH if done with DIJKSTRA
 *
 * Dijkstra updates repeatedly pop the state s according to their heuristic value, and then update
 * the cost values for all it's visited successors, based on the heuristic s.
 *
 * This increases the stored heuristic values, ensuring that A* won't go in circles, and in general generating
 * a better table of heuristics.
 *
 */
fun <StateType : State<StateType>, NodeType : RealTimeSearchNode<StateType, NodeType>> dijkstra(
        context: RealTimePlannerContext<StateType, NodeType>) {
    val openList = context.openList

    // Invalidate the current heuristic value by incrementing the counter
    context.iterationCounter++

    // change openList ordering to heuristic only
    openList.reorder(heuristicComparator)

    while (openList.isNotEmpty()) {
        val node = openList.pop() ?: throw GoalNotReachableException("Goal not reachable. Open list is empty.")

        node.iteration = context.iterationCounter

        val currentHeuristicValue = node.heuristic

        // update heuristic value for each predecessor
        for (predecessor in node.predecessors) {
            val predecessorNode = predecessor.node

            // This node was already learned and closed in the current iteration
            if (predecessorNode.iteration == context.iterationCounter && !predecessorNode.open) continue

            val predecessorHeuristicValue = predecessorNode.heuristic

            if (!predecessorNode.open) {
                // This node is not open yet, because it was not visited in the current planning iteration

                predecessorNode.heuristic = currentHeuristicValue + predecessor.actionCost
                assert(predecessorNode.iteration == context.iterationCounter - 1)
                predecessorNode.iteration = context.iterationCounter

                openList.add(predecessorNode)
            } else if (predecessorHeuristicValue > currentHeuristicValue + predecessor.actionCost) {
                // This node was visited in this learning phase, but the current path is better then the previous
                predecessorNode.heuristic = currentHeuristicValue + predecessor.actionCost
                openList.update(predecessorNode) // Update priority
            }
        }
    }
}

/**
 * Dynamic dijkstra operation which supports custom termination checker and ordering comparator
 */
fun <StateType : State<StateType>, NodeType : RealTimeSearchNode<StateType, NodeType>> dynamicDijkstra(
        context: RealTimePlannerContext<StateType, NodeType>,
        freshSearch: Boolean = true,
        openListComparator: Comparator<RealTimeSearchNode<StateType,NodeType>> = Comparator {lhs, rhs -> heuristicComparator.compare(lhs, rhs)},
        reachedTermination: (AdvancedPriorityQueue<NodeType>) -> Boolean = {openList -> openList.isEmpty()}) {
    val openList = context.openList

    // Invalidate the current heuristic value by incrementing the counter
    // Otherwise, we continue the previous iteration
    if (freshSearch) context.iterationCounter++

    // change openList ordering to heuristic only
    openList.reorder(openListComparator)

    while (!reachedTermination(openList)) {
        val node = openList.pop() ?: throw GoalNotReachableException("Goal not reachable. Open list is empty.")

        node.iteration = context.iterationCounter

        val currentHeuristicValue = node.heuristic

        // update heuristic value for each predecessor
        for (predecessor in node.predecessors) {
            val predecessorNode = predecessor.node

            // This node was already learned and closed in the current iteration
            if (predecessorNode.iteration == context.iterationCounter && !predecessorNode.open) continue

            val predecessorHeuristicValue = predecessorNode.heuristic

            if (!predecessorNode.open) {
                // This node is not open yet, because it was not visited in the current planning iteration

                predecessorNode.heuristic = currentHeuristicValue + predecessor.actionCost
                predecessorNode.iteration = context.iterationCounter

                openList.add(predecessorNode)
            } else if (predecessorHeuristicValue > currentHeuristicValue + predecessor.actionCost) {
                // This node was visited in this learning phase, but the current path is better then the previous
                predecessorNode.heuristic = currentHeuristicValue + predecessor.actionCost
                openList.update(predecessorNode) // Update priority
            }
        }
    }
}