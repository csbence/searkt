package edu.unh.cs.ai.realtimesearch.planner.suboptimal

import edu.unh.cs.ai.realtimesearch.environment.*
import edu.unh.cs.ai.realtimesearch.environment.Domain
import edu.unh.cs.ai.realtimesearch.planner.classical.ClassicalPlanner
import edu.unh.cs.ai.realtimesearch.planner.realtime.LssLrtaStarPlanner
import edu.unh.cs.ai.realtimesearch.util.AdvancedPriorityQueue
import edu.unh.cs.ai.realtimesearch.util.Indexable
import edu.unh.cs.ai.realtimesearch.util.resize
import org.slf4j.LoggerFactory
import java.util.HashMap

class WeightedAStar<StateType : State<StateType>>(val domain: Domain<StateType>, val weight: Double = 1.0) : ClassicalPlanner<StateType>() {
    class Node<StateType : State<StateType>>(val state: StateType, var heuristic: Double, var cost: Long,
                                             var actionCost: Long, var action: Action,
                                             var iteration: Long,
                                             parent: WeightedAStar.Node<StateType>? = null) : Indexable {

        override var index: Int = -1

        var parent = parent ?: this

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
                "Node: [State: $state h: $heuristic, g: $cost, iteration: $iteration, actionCost: $actionCost, parent: ${parent.state}, open: $open ]"
    }

    private val logger = LoggerFactory.getLogger(WeightedAStar::class.java)
    private var iterationCounter = 0L

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
    private var openList = AdvancedPriorityQueue(10000000, fValueComparator)


    override fun plan(state: StateType): List<Action> {
        val startTime = System.currentTimeMillis()
        openList.clear()
        nodes.clear()

        val startNode = Node(state,domain.heuristic(state),0L,0L, NoOperationAction,iterationCounter)
        openList.add(startNode)
        nodes[state] = startNode
        generatedNodeCount++

        while (openList.isNotEmpty()) {
            expandedNodeCount++

            val currentNode = openList.pop()
            if(!currentNode.open)
        }

    }

    fun extractPlan(solutionNode: Node<StateType>): List<Action> {
        val actions = arrayListOf<Action>()
        var iterationNode = solutionNode
        while(iterationNode.parent != iterationNode) {
            actions.add(iterationNode.action)
            iterationNode = iterationNode.parent
        }
        return actions.reversed()
    }
}