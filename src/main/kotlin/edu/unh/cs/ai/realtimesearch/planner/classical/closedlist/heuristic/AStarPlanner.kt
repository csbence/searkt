package edu.unh.cs.ai.realtimesearch.planner.classical.closedlist.heuristic

import edu.unh.cs.ai.realtimesearch.environment.Action
import edu.unh.cs.ai.realtimesearch.environment.Domain
import edu.unh.cs.ai.realtimesearch.environment.State
import edu.unh.cs.ai.realtimesearch.experiment.terminationCheckers.TerminationChecker
import edu.unh.cs.ai.realtimesearch.planner.classical.ClassicalPlanner
import edu.unh.cs.ai.realtimesearch.planner.exception.GoalNotReachableException
import edu.unh.cs.ai.realtimesearch.util.resize
import org.slf4j.LoggerFactory
import java.util.*

/**
 * Standalone A* implementation.
 *
 * Requires a domain with an admissible heuristic function.
 */
class AStarPlanner<StateType : State<StateType>>(val domain: Domain<StateType>) : ClassicalPlanner<StateType>() {
    private val logger = LoggerFactory.getLogger(AStarPlanner::class.java)

    private val openList = PriorityQueue { lhs: Node<StateType>, rhs: Node<StateType> ->
        when {
            lhs.f < rhs.f -> -1
            lhs.f > rhs.f -> 1
            lhs.cost > rhs.cost -> -1 // Tie braking on cost (g)
            lhs.cost < rhs.cost -> 1
            else -> 0
        }
    }

    /**
     * Hash table that represents the union of closed and the open list
     */
    private val nodes: HashMap<StateType, Node<StateType>> = HashMap<StateType, Node<StateType>>(100000000, 1F).resize()

    data class Node<StateType : State<StateType>>(val parent: Node<StateType>?, val state: StateType, val action: Action?, val cost: Double, val f: Double, var open: Boolean) {
        override fun hashCode() = state.hashCode()
        override fun equals(other: Any?) = other != null && other is Node<*> && state.equals(other.state)
    }

    override fun plan(state: StateType, terminationChecker: TerminationChecker): List<Action> {
        val startTime = System.nanoTime()
        openList.clear()
        nodes.clear()

        val startNode = Node(null, state, null, 0.0, 0.0, true)
        openList.add(startNode)
        nodes.put(state, startNode)
        generatedNodeCount++

        while (openList.isNotEmpty()) {
            expandedNodeCount++
            val node = openList.remove()

            if (!node.open) {
                continue // This node was disabled
            }

            if (domain.isGoal(node.state)) {
                executionNanoTime = System.nanoTime() - startTime
                return extractPlan(node)
            }

            // expand (only those not visited yet)
            successors@ for (successor in domain.successors(node.state)) {
                if (successor.state == node.state) {
                    continue // Don't consider the parent node
                }

                generatedNodeCount++

                val existingSuccessorNode = nodes[successor.state]

                val newCost = successor.actionCost + node.cost

                when {
                    existingSuccessorNode == null -> {
                        // New state discovered
                        val newSuccessorNode = Node(node, successor.state, successor.action, newCost, newCost + domain.heuristic(successor.state), true)
                        nodes.put(newSuccessorNode.state, newSuccessorNode) // Add to the node list
                        openList.add(newSuccessorNode)
                    }
                    existingSuccessorNode.open && existingSuccessorNode.cost > newCost -> {
                        // Rediscover with a better cost
                        println("Rediscover better cost")
                        val newSuccessorNode = Node(node, successor.state, successor.action, newCost, newCost + domain.heuristic(successor.state), true)
                        nodes.put(newSuccessorNode.state, newSuccessorNode) // Override the previous node for the state
                        openList.add(newSuccessorNode)
                        existingSuccessorNode.open = false // Disable the existing representative on the open
                    }
                    else -> continue@successors

                }
            }
        }

        throw GoalNotReachableException()
    }

    fun extractPlan(leave: Node<StateType>): List<Action> {
        val actions: MutableList<Action> = arrayListOf()

        var node = leave
        // root will have null action. So as long as the parent
        // is not null, we can take it's action and assume all is good
        while (node.parent != null) {
            actions.add(node.action!!)
            node = node.parent!!
        }

        return actions.reversed() // we are adding actions in wrong order, to return the reverser
    }
}