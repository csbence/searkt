package edu.unh.cs.ai.realtimesearch.planner.classical.closedlist.heuristic

import edu.unh.cs.ai.realtimesearch.environment.Action
import edu.unh.cs.ai.realtimesearch.environment.Domain
import edu.unh.cs.ai.realtimesearch.environment.State
import edu.unh.cs.ai.realtimesearch.planner.classical.ClassicalPlanner
import edu.unh.cs.ai.realtimesearch.planner.classical.ClassicalPlannerBase
import edu.unh.cs.ai.realtimesearch.planner.exception.GoalNotReachableException
import org.slf4j.LoggerFactory
import java.util.*

/**
 * Standalone A* implementation.
 *
 * Requires a domain with an admissible heuristic function.
 */
class AStarPlanner<StateType : State<StateType>>(val domain: Domain<StateType>, val weight: Double = 1.0) : ClassicalPlanner<StateType> {

    private val logger = LoggerFactory.getLogger(ClassicalPlannerBase::class.java)

    override var generatedNodeCount = 0
    override var expandedNodeCount = 0

    private val openList = PriorityQueue { lhs: Node, rhs: Node ->
        when {
            lhs.f < rhs.f -> -1
            lhs.f > rhs.f -> 1
            lhs.cost > rhs.cost -> -1// Tie braking on cost (g)
            lhs.cost < rhs.cost -> 1
            else -> 0
        }
    }

    private val closedList: HashSet<StateType> = hashSetOf()
    private var generatedNodes = 0
    private var expandedNodes = 0

    inner class Node(val parent: Node?, val state: StateType, val action: Action?, val cost: Double) {
        internal val f: Double

        init {
            f = cost + (domain.heuristic(state) * weight)
        }
    }

    override fun plan(state: StateType): List<Action> {
        generatedNodes = 0
        expandedNodes = 0
        openList.clear();
        closedList.clear();

        openList.add(Node(null, state, null, 0.0))
        closedList.add(state)
        generatedNodes += 1

        while (openList.isNotEmpty()) {
            val node = openList.remove()
            expandedNodes += 1

            if (domain.isGoal(node.state)) {
                return extractPlan(node)
            }

            // expand (only those not visited yet)
            for (successor in domain.successors(node.state)) {
                if (successor.state !in closedList) {
                    generatedNodes += 1

                    // generate the node with correct cost
                    val nodeCost = successor.actionCost + node.cost

                    val successorNode = Node(node, successor.state,
                            successor.action, nodeCost)
                    openList.add(successorNode)
                    closedList.add(successorNode.state)
                }
            }

        }

        throw GoalNotReachableException()
    }

    protected fun extractPlan(leave: Node): List<Action> {
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