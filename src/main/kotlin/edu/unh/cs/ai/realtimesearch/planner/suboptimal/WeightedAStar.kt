package edu.unh.cs.ai.realtimesearch.planner.suboptimal

import edu.unh.cs.ai.realtimesearch.MetronomeConfigurationException
import edu.unh.cs.ai.realtimesearch.environment.*
import edu.unh.cs.ai.realtimesearch.experiment.configuration.ExperimentConfiguration
import edu.unh.cs.ai.realtimesearch.experiment.terminationCheckers.TerminationChecker
import edu.unh.cs.ai.realtimesearch.planner.classical.ClassicalPlanner
import edu.unh.cs.ai.realtimesearch.planner.exception.GoalNotReachableException
import edu.unh.cs.ai.realtimesearch.util.AdvancedPriorityQueue
import edu.unh.cs.ai.realtimesearch.util.Indexable
import edu.unh.cs.ai.realtimesearch.util.resize
import org.slf4j.LoggerFactory
import java.util.HashMap
import kotlin.Comparator

class WeightedAStar<StateType : State<StateType>>(val domain: Domain<StateType>, val configuration: ExperimentConfiguration) : ClassicalPlanner<StateType>() {
    private val weight: Double = configuration.weight
            ?: throw MetronomeConfigurationException("Weight for weighted A* is not specified.")

    class Node<StateType : State<StateType>>(val state: StateType, var heuristic: Double, var cost: Long,
                                             var actionCost: Long, var action: Action,
                                             var parent: WeightedAStar.Node<StateType>? = null) : Indexable {

        override var index: Int = -1
        var closed = false

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
        sourceNode.closed = true
        logger.debug("Expanding $sourceNode.")
        val currentGValue = sourceNode.cost
        for (successor in domain.successors(sourceNode.state)) {
            val successorState = successor.state
            val successorNode = getNode(sourceNode, successor)

            // skip if we have our parent as a successor
            if (successorState == sourceNode.parent?.state) {
                continue
            }

            // only generate states which have not been visited or with a cheaper cost
            val successorGValueFromCurrent = currentGValue + successor.actionCost
            if (successorNode.cost > successorGValueFromCurrent && !successorNode.closed) {
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
                    openList.update(successorNode)
                }
            }
        }
    }

    override fun plan(state: StateType, terminationChecker: TerminationChecker): List<Action> {
        val startTime = initializeAStar()
        val node = Node(state, weight * domain.heuristic(state), 0, 0, NoOperationAction)
        var currentNode: Node<StateType>
        nodes[state] = node
        openList.add(node)
        generatedNodeCount++

        while (openList.isNotEmpty() && !terminationChecker.reachedTermination()) {
            val topNode = openList.peek() ?: throw GoalNotReachableException("Open list is empty")
            logger.debug("Top node is $topNode.")
            if (domain.isGoal(topNode.state)) {
                executionNanoTime = System.currentTimeMillis() - startTime
                return extractPlan(topNode, state)
            }
            currentNode = openList.pop() ?: throw GoalNotReachableException("Open list is empty")
            expandFromNode(currentNode)
            terminationChecker.notifyExpansion()
        }
        throw GoalNotReachableException("Goal not reachable")
    }

    private fun extractPlan(solutionNode: Node<StateType>, startState: StateType): List<Action> {
        val actions = arrayListOf<Action>()
        var iterationNode = solutionNode
        while (iterationNode.parent != null) {
            actions.add(iterationNode.action)
            iterationNode = iterationNode.parent!!
        }
        assert(startState == iterationNode.state)
        actions.reverse()
        return actions
    }
}