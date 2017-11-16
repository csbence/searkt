package edu.unh.cs.ai.realtimesearch.planner.suboptimal

import edu.unh.cs.ai.realtimesearch.environment.*
import edu.unh.cs.ai.realtimesearch.experiment.configuration.Configurations
import edu.unh.cs.ai.realtimesearch.experiment.configuration.GeneralExperimentConfiguration
import edu.unh.cs.ai.realtimesearch.experiment.configuration.InvalidFieldException
import edu.unh.cs.ai.realtimesearch.planner.classical.ClassicalPlanner
import edu.unh.cs.ai.realtimesearch.planner.exception.GoalNotReachableException
import edu.unh.cs.ai.realtimesearch.util.BucketNode
import edu.unh.cs.ai.realtimesearch.util.BucketOpenList
import edu.unh.cs.ai.realtimesearch.util.Indexable
import edu.unh.cs.ai.realtimesearch.util.resize
import org.slf4j.LoggerFactory
import java.util.*

class DynamicPotentialSearch<StateType : State<StateType>>(val domain: Domain<StateType>, val configuration: GeneralExperimentConfiguration) : ClassicalPlanner<StateType>() {
    private val weight: Double = configuration.getTypedValue(Configurations.WEIGHT.toString()) ?: throw InvalidFieldException("\"${Configurations.WEIGHT}\" is not found. Please add it the the experiment configuration.")

    class Node<StateType : State<StateType>>(val state: StateType, var heuristic: Double, var cost: Long,
                                             var actionCost: Long, var action: Action,
                                             var parent: DynamicPotentialSearch.Node<StateType>? = null) : Indexable, BucketNode {
        override fun getNodeIndex(): Int {
            return index
        }

        override fun updateIndex(i: Int) {
            index = i
        }

        override val open: Boolean
            get() = index != -1

        override fun getFValue(): Double = f

        override fun getGValue(): Double = cost.toDouble()

        override fun getHValue(): Double = heuristic

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

    private val logger = LoggerFactory.getLogger(DynamicPotentialSearch::class.java)

    private val nodes: HashMap<StateType, DynamicPotentialSearch.Node<StateType>> = HashMap<StateType, DynamicPotentialSearch.Node<StateType>>(100000000, 1.toFloat()).resize()
    private var openList = BucketOpenList<Node<StateType>>(weight)

    private fun initializeAStar(): Long = System.currentTimeMillis()

    private fun getNode(sourceNode: Node<StateType>, successorBundle: SuccessorBundle<StateType>): Node<StateType> {
        val successorState = successorBundle.state
        val tempSuccessorNode = nodes[successorState]
        return if (tempSuccessorNode == null) {
            generatedNodeCount++
            val undiscoveredNode = Node(
                    state = successorState,
                    heuristic = domain.heuristic(successorState),
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
        val successors = domain.successors(sourceNode.state)
        for (successor in successors) {
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
                val successorReplacement = Node(
                        successorNode.state,
                        successorNode.heuristic,
                        successorGValueFromCurrent,
                        successor.actionCost,
                        successor.action,
                        sourceNode
                )
                if (!successorNode.open) {
                    successorNode.apply {
                        cost = successorGValueFromCurrent
                        parent = sourceNode
                        action = successor.action
                        actionCost = successor.actionCost
                    }
                    openList.add(successorNode)
                } else {
                    openList.replace(successorNode, successorReplacement)
                }
            }
        }
    }

    override fun plan(state: StateType): List<Action> {
        val startTime = initializeAStar()
        val node = Node(state, domain.heuristic(state), 0, 0, NoOperationAction)
        var currentNode: Node<StateType>
        nodes[state] = node
        openList.add(node)
        generatedNodeCount++

        while (openList.isNotEmpty()) {
            val topNode = openList.chooseNode() ?: throw GoalNotReachableException("Open list is empty")
            if (domain.isGoal(topNode.state)) {
                executionNanoTime = System.currentTimeMillis() - startTime
                return extractPlan(topNode, state)
            }
            currentNode = topNode
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