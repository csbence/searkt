package edu.unh.cs.ai.realtimesearch.planner

import edu.unh.cs.ai.realtimesearch.environment.Action
import edu.unh.cs.ai.realtimesearch.environment.Domain
import edu.unh.cs.ai.realtimesearch.environment.State
import edu.unh.cs.ai.realtimesearch.experiment.terminationCheckers.TerminationChecker
import edu.unh.cs.ai.realtimesearch.util.AdvancedPriorityQueue
import edu.unh.cs.ai.realtimesearch.util.Indexable
import java.util.*

/**
 * @author Bence Cserna (bence@cserna.net)
 */

interface Safe {
    var safe: Boolean
}

interface Depth {
    var depth: Int
}

class SafeRealTimeSearchNode<StateType : State<StateType>>(
        override val state: StateType,
        override var heuristic: Double,
        override var cost: Long,
        override var actionCost: Long,
        override var action: Action,
        override var iteration: Long,
        parent: SafeRealTimeSearchNode<StateType>? = null) : RealTimeSearchNode<StateType, SafeRealTimeSearchNode<StateType>>, Indexable, Safe {

    /** Item index in the open list. */
    override var index: Int = -1
    override var safe = false

    /** Nodes that generated this SafeRealTimeSearchNode as a successor in the current exploration phase. */
    override var predecessors: MutableList<SearchEdge<SafeRealTimeSearchNode<StateType>>> = arrayListOf()

    /** Parent pointer that points to the min cost predecessor. */
    override var parent: SafeRealTimeSearchNode<StateType> = parent ?: this

    override fun hashCode(): Int = state.hashCode()

    override fun equals(other: Any?): Boolean = when (other) {
        null -> false
        is SafeRealTimeSearchNode<*> -> state == other.state
        is State<*> -> state == other
        else -> false
    }

    override fun toString(): String =
            "SafeRealTimeSearchNode: [State: $state h: $heuristic, g: $cost, iteration: $iteration, actionCost: $actionCost, parent: ${parent.state}, open: $open safe: $safe]"
}

/**
 * Prove the safety of a given state. A state is safe (more precisely comfortable) if the state itself is safe or a
 * safe state is reachable from it. The explicit safety of a state is defined by the domain.
 *
 * To prove the implicit safety of a state a best first search(BFS) algorithm is used prioritized on the safe distance of
 * the states. The safe distance of states is defined by the domain.
 *
 * @param state State to validate.
 * @param terminationChecker The termination checker is used to ensure the termination of the BFS algorithm. The
 *                           expansion of the during the search are also logged against the termination checker.
 * @param domain The domain is used to determine the safety distance and the explicit safety of states.
 * @param isSafe An optional secondary safety check can be provided to prove implicit safety.
 *
 * @return null if the given state is not safe, else a list of states that are proven to be safe.
 * Empty list if the state itself is safe.
 */
fun <StateType : State<StateType>> isComfortable(state: StateType, terminationChecker: TerminationChecker, domain: Domain<StateType>, isSafe: ((StateType) -> Boolean)? = null): List<StateType>? {
    data class Node(val state: StateType, val safeDistance: Pair<Int, Int>, val parent: Node? = null)

    // Return empty list if the original state is safe
    if (domain.isSafe(state) || (isSafe != null && isSafe(state))) return emptyList()

    val nodeComparator = java.util.Comparator<Node> { (_, lhsDistance), (_, rhsDistance) ->
        when {
            lhsDistance.first < rhsDistance.first -> -1
            lhsDistance.first > rhsDistance.first -> 1
            lhsDistance.second < rhsDistance.second -> -1
            lhsDistance.second > rhsDistance.second -> 1
            else -> 0
        }
    }

    val priorityQueue = PriorityQueue<Node>(nodeComparator)
    val discoveredStates = hashSetOf<StateType>()
    val comfortableStates = mutableListOf<StateType>()

    priorityQueue.add(Node(state, domain.safeDistance(state)))

    while (priorityQueue.isNotEmpty() && !terminationChecker.reachedTermination()) {
        val currentNode = priorityQueue.poll() ?: return null

        if (domain.isSafe(currentNode.state) || (isSafe != null && isSafe(currentNode.state))) {

            // Backtrack to the root and return all safe states
            // The parent of the safe state is comfortable
            var backTrackNode: Node? = currentNode
//            println("Safe: ${backTrackNode!!.state} explicit: ${domain.isSafe(currentNode.state)}")
            while (backTrackNode != null) {
//                println("   Prove safety: ${backTrackNode.state}")
                comfortableStates.add(backTrackNode.state)
                backTrackNode = backTrackNode.parent
            }

            return comfortableStates
        }

        terminationChecker.notifyExpansion()
        domain.successors(currentNode.state)
                .filter { it.state !in discoveredStates } // Do not add add an item twice to the list
                .onEach { discoveredStates += it.state }
                .mapTo(priorityQueue, { Node(it.state, domain.safeDistance(it.state), currentNode) }) // Add successors to the queue
    }

    return null
}

/**
 * Find the best safe successor of a state if any.
 *
 * @return the best safe successor if available, else null.
 */
fun <StateType : State<StateType>> bestSafeChild(state: StateType, domain: Domain<StateType>, isSafe: ((StateType) -> Boolean)): StateType? {
    return domain.successors(state)
            .filter { domain.isSafe(it.state) || isSafe(it.state) }
            .minBy { it.actionCost + domain.heuristic(it.state) }
            ?.state
}

fun <StateType: State<StateType>, NodeType> parentLabelBackPropagation(openList: List<NodeType>)
        where NodeType: SearchNode<StateType, NodeType>, NodeType : Safe{
    openList.forEach { nodeOnOpen ->
        val label = nodeOnOpen

    }

}

fun <StateType : State<StateType>, NodeType> predecessorSafetyPropagation(safeNodes: List<NodeType>)
        where NodeType : SearchNode<StateType, NodeType>, NodeType : Safe {
    val backedUpNodes: HashSet<NodeType> = safeNodes.toHashSet()
    var nodesToBackup: List<NodeType> = safeNodes

    while (nodesToBackup.isNotEmpty()) {
        nodesToBackup = nodesToBackup
                .flatMap { it.predecessors.map { it.node } }
                .filter { !it.safe && it !in backedUpNodes }
                .onEach {
                    backedUpNodes.add(it)
                    it.safe = true
                }
    }
}

fun <StateType : State<StateType>, Node> selectSafeToBest(queue: AdvancedPriorityQueue<Node>, recordRank: (Int, Int) -> (Unit) = { _: Int, _: Int -> } ): Node?
        where Node : SearchNode<StateType, Node>, Node : Indexable, Node : Safe {
    val nodes = MutableList(queue.size, { queue.backingArray[it]!! })
    nodes.sortBy { it.cost + it.heuristic }

    var rank = 0
    nodes.forEach { frontierNode ->
        rank++
        var currentNode = frontierNode
        while (currentNode.parent != currentNode) {
            if (currentNode.safe) {
                recordRank(rank, frontierNode.cost.toInt() )
                return currentNode
            }
            currentNode = currentNode.parent
        }
    }

    return null
}

/**
 * Selector to define how to pick the target node at the end of the planning iteration.
 * The planner returns a sequence of actions from the agent's current location to the selected target node.
 */
enum class SafeRealTimeSearchTargetSelection {
    /** Select the best safe predecessor of a the best node on open that has such predecessor. */
    SAFE_TO_BEST,
    /** Select the best safe node in LSS. */
    BEST_SAFE
}

enum class SafetyProof {
    TOP_OF_OPEN,
    LOW_D_WINDOW,
    LOW_D_TOP_PREDECESSOR,
    LOW_D_LOW_H,
    LOW_D_LOW_H_OPEN
}

enum class SafeRealTimeSearchConfiguration(val key: String) {
    TARGET_SELECTION("targetSelection"),
    SAFETY_EXPLORATION_RATIO("safetyExplorationRatio"),
    SAFETY_PROOF("safetyProof");

    override fun toString() = key
}

enum class SafetyBackup {
    PARENT, PREDECESSOR
}
