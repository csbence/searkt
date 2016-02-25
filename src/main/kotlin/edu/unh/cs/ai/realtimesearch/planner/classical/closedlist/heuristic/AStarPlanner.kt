package edu.unh.cs.ai.realtimesearch.planner.classical.closedlist.heuristic

import edu.unh.cs.ai.realtimesearch.environment.Action
import edu.unh.cs.ai.realtimesearch.environment.Domain
import edu.unh.cs.ai.realtimesearch.environment.State
import edu.unh.cs.ai.realtimesearch.planner.Exception.GoalNotReachableException
import edu.unh.cs.ai.realtimesearch.planner.classical.ClassicalPlanner
import edu.unh.cs.ai.realtimesearch.planner.classical.ClassicalPlannerBase
import org.slf4j.LoggerFactory
import java.util.*

/**
 * Standalone A* implementation.
 *
 * Requires a domain with an admissible heuristic function.
 */
class AStarPlanner<StateType : State<StateType>>(val domain: Domain<StateType>) : ClassicalPlanner<StateType> {

    private val logger = LoggerFactory.getLogger(ClassicalPlannerBase::class.java)

    private val openList = PriorityQueue { lhs: Node, rhs: Node ->
        if (lhs.f == rhs.f) {
            when {
                lhs.g > rhs.g -> -1
                lhs.g < rhs.g -> 1
                else -> 0
            }
        } else {
            when {
                lhs.f < rhs.f -> -1
                lhs.f > rhs.f -> 1
                else -> 0
            }
        }
    }

    private val closedList: HashSet<StateType> = hashSetOf()

    var generatedNodes = 0
    var expandedNodes = 0

    inner class Node(val parent: Node?, val state: StateType, val action: Action?, val cost: Double) {
        internal val f: Double
        internal val g: Double

        init {
            g = if (parent != null) {
                parent.g + cost
            } else {
                cost
            }

            f = g + domain.heuristic(state)
        }
    }

    override fun plan(state: StateType): List<Action> {
        generatedNodes = 0
        expandedNodes = 0
        openList.clear();
        closedList.clear();
        generatedNodes = 0

        openList.add(Node(null, state, null, 0.0))
        while (openList.isNotEmpty()) {
            val node = openList.poll()
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


            if (expandedNodes % 100000 == 0) {
                println(System.currentTimeMillis() - timestamp)
                timestamp = System.currentTimeMillis()
            }
        }

        throw GoalNotReachableException()
    }

    var timestamp = 0L

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

    override fun getExpandedNodeCount(): Int {
        return expandedNodes
    }

    override fun getGeneratedNodeCount(): Int {
        return generatedNodes
    }

}