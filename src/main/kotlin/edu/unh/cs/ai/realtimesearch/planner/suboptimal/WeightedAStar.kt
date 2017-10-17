package edu.unh.cs.ai.realtimesearch.planner.suboptimal

import edu.unh.cs.ai.realtimesearch.environment.*
import edu.unh.cs.ai.realtimesearch.planner.classical.ClassicalPlanner
import edu.unh.cs.ai.realtimesearch.planner.exception.GoalNotReachableException
import edu.unh.cs.ai.realtimesearch.util.AdvancedPriorityQueue
import edu.unh.cs.ai.realtimesearch.util.Indexable
import edu.unh.cs.ai.realtimesearch.util.resize
import org.slf4j.LoggerFactory
import java.util.HashMap
import kotlin.Any
import kotlin.Boolean
import kotlin.Comparator
import kotlin.Double
import kotlin.Int
import kotlin.Long
import kotlin.String
import kotlin.apply
import kotlin.assert

class WeightedAStar<StateType : State<StateType>>(val domain: Domain<StateType>, val weight: Double = 1.0) : ClassicalPlanner<StateType>() {
    class Node<StateType : State<StateType>>(val state: StateType, var heuristic: Double, var cost: Long,
                                             var actionCost: Long, var action: Action,
                                             var parent: WeightedAStar.Node<StateType>? = null) : Indexable {

        override var index: Int = -1

        val f: Double
            get() = cost + heuristic

        override fun equals(other: Any?): Boolean {
            if (other != null && other is Node<*>) {
                return state == other.state
            }
            return false
        }

        override fun hashCode(): Int = state.hashCode()

        override fun toString(): String =
                "Node: [State: $state h: $heuristic, g: $cost, actionCost: $actionCost, parent: ${parent?.state}, open: $open ]"
    }

    var timesReplaced = 0

    private val logger = LoggerFactory.getLogger(WeightedAStar::class.java)

    private val fValueComparator = Comparator<WeightedAStar.Node<StateType>> { lhs, rhs ->
        when {
            lhs.f < rhs.f -> -1
            lhs.f > rhs.f -> 1
            lhs.cost > rhs.cost -> -1 // Tie braking on cost (g)
            lhs.cost < rhs.cost -> 1
            else -> 0
        }
    }

    private val nodes: HashMap<StateType, WeightedAStar.Node<StateType>> = HashMap<StateType, WeightedAStar.Node<StateType>>(100000000, 1.toFloat()).resize()
    private var openList = AdvancedPriorityQueue(100000000, fValueComparator)

    private fun initializeAStar(): Long = System.currentTimeMillis()

    private fun getNode(sourceNode: Node<StateType>, successorBundle: SuccessorBundle<StateType>): Node<StateType> {
        val successorState = successorBundle.state
        val tempSuccessorNode = nodes[successorState]
        return if (tempSuccessorNode == null) {
            generatedNodeCount++
            val undiscoveredNode = Node(
                    state = successorState,
                    heuristic = weight * domain.heuristic(successorState),
                    actionCost = successorBundle.actionCost,
                    action = successorBundle.action,
                    parent = sourceNode,
                    cost = Long.MAX_VALUE
            )
            nodes[successorState] = undiscoveredNode
            undiscoveredNode
        } else {
            tempSuccessorNode
        }
    }

    private fun expandFromNode(sourceNode: Node<StateType>) {
        expandedNodeCount++
        val currentGValue = sourceNode.cost
        for (successor in domain.successors(sourceNode.state)) {
            val successorState = successor.state
            val successorNode = getNode(sourceNode, successor)
//            if (successorNode.heuristic == Double.POSITIVE_INFINITY
//                    && successorNode.iteration != iterationCounter) {
//                continue
//            }
//
//            if (successorNode.iteration != iterationCounter) {
//                successorNode.apply {
//                    iteration = iterationCounter
//                    cost = Long.MAX_VALUE
//                }
//            }
//
            // skip if we have our parent as a successor
            if (successorState == sourceNode.parent?.state) {
                continue
            }

            // only generate states which have not been visited or with a cheaper cost
            val successorGValueFromCurrent = currentGValue + successor.actionCost
            if (successorNode.cost > successorGValueFromCurrent) {
                assert(successorNode.state == successor.state)
                successorNode.apply {
                    cost = successorGValueFromCurrent
                    parent = sourceNode
                    action = successor.action
                    actionCost = successor.actionCost
                }
                if (!successorNode.open) {
                    openList.add(successorNode)
                } else {
                    timesReplaced++
                    openList.update(successorNode)
                }
            }
        }
    }

    override fun plan(state: StateType): List<Action> {
        val startTime = initializeAStar()
        val node = Node(state, weight * domain.heuristic(state), 0, 0, NoOperationAction)
        var currentNode: Node<StateType>
        nodes[state] = node
        openList.add(node)
        generatedNodeCount++

        while (openList.isNotEmpty()) {
            val topNode = openList.peek() ?: throw GoalNotReachableException("Open list is empty")
            if (domain.isGoal(topNode.state)) {
                executionNanoTime = System.currentTimeMillis() - startTime
                return extractPlan(topNode, state)
            }
            currentNode = openList.pop() ?: throw GoalNotReachableException("Open list is empty")
            expandFromNode(currentNode)
        }
        throw GoalNotReachableException()
    }

    private fun extractPlan(solutionNode: Node<StateType>, startState: StateType): List<Action> {
        val actions = arrayListOf<Action>()
        var iterationNode = solutionNode
        while (iterationNode.parent != null) {
            actions.add(iterationNode.action)
            iterationNode = iterationNode.parent!!
        }
        assert(startState == iterationNode.state )
        actions.reverse()
        return actions
    }
}