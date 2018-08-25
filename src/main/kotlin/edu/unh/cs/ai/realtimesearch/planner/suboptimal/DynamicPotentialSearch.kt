package edu.unh.cs.ai.realtimesearch.planner.suboptimal

import edu.unh.cs.ai.realtimesearch.MetronomeConfigurationException
import edu.unh.cs.ai.realtimesearch.MetronomeException
import edu.unh.cs.ai.realtimesearch.environment.*
import edu.unh.cs.ai.realtimesearch.experiment.configuration.ExperimentConfiguration
import edu.unh.cs.ai.realtimesearch.experiment.terminationCheckers.TerminationChecker
import edu.unh.cs.ai.realtimesearch.planner.classical.ClassicalPlanner
import edu.unh.cs.ai.realtimesearch.planner.exception.GoalNotReachableException
import edu.unh.cs.ai.realtimesearch.util.*
import org.slf4j.LoggerFactory
import java.util.*

class DynamicPotentialSearch<StateType : State<StateType>>(val domain: Domain<StateType>, val configuration: ExperimentConfiguration) : ClassicalPlanner<StateType>() {

    private val weight: Double = configuration.weight
            ?: throw MetronomeConfigurationException("Weight for Dynamic Potential Search is not specified.")

    var terminationChecker: TerminationChecker? = null

    class Node<StateType : State<StateType>>(val state: StateType, var heuristic: Double, var cost: Double,
                                             var actionCost: Double, var action: Action,
                                             override var parent: DynamicPotentialSearch.Node<StateType>? = null) : BucketNode, SearchQueueElement<Node<StateType>> {
        override val g: Double
            get() = cost
        override val depth: Double
            get() = parent?.depth?.plus(1) ?: 0.0
        override val h: Double
            get() = heuristic
        override val d: Double
            get() = heuristic
        override val hHat: Double
            get() = heuristic
        override val dHat: Double
            get() = heuristic

        override fun setIndex(key: Int, index: Int) {
            this.index = index
        }

        override fun getIndex(key: Int): Int {
            return index
        }

        val open: Boolean
            get() = index != -1

        override fun isOpen(): Boolean {
            return this.open
        }

        override fun getFValue(): Double = f

        override fun getGValue(): Double = cost

        override fun getHValue(): Double = heuristic

        override fun setOpenLocation(value: Int) {
            index = value
        }

        var index: Int = -1

        override val f: Double
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

        fun copy(): Node<StateType> {
            val copyState = this.state.copy()
            return Node(copyState, this.heuristic, this.cost, this.actionCost, this.action, this.parent)
        }
    }

    private val logger = LoggerFactory.getLogger(DynamicPotentialSearch::class.java)

    private val nodes: HashMap<StateType, Node<StateType>> = HashMap(100000000, 1.toFloat())
    private var openList = BucketOpenList<Node<StateType>>(weight) //BucketOpenList<Node<StateType>>(weight)

    private fun getNode(sourceNode: Node<StateType>, successorBundle: SuccessorBundle<StateType>): Node<StateType> {
        val successorState = successorBundle.state
        val tempSuccessorNode = nodes[successorState]
        return if (tempSuccessorNode == null) {
            generatedNodeCount++
            terminationChecker!!.notifyExpansion()
            val undiscoveredNode = Node(
                    state = successorState,
                    heuristic = domain.heuristic(successorState),
                    actionCost = successorBundle.actionCost,
                    action = successorBundle.action,
                    parent = sourceNode,
                    cost = Double.MAX_VALUE
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

            if (successorState == sourceNode.parent?.state) {
                continue
            }

            // only generate states which have not been visited or with a cheaper cost
            val successorGValueFromCurrent = currentGValue + successor.actionCost
            if (successorNode.cost > successorGValueFromCurrent) {
                assert(successorNode.state == successor.state)
                val duplicateNode = successorNode.copy()
                successorNode.apply {
                    cost = successorGValueFromCurrent
                    parent = sourceNode
                    action = successor.action
                    actionCost = successor.actionCost
                }
                if (!successorNode.open) openList.add(successorNode)
                else updateNode(duplicateNode, successorNode)
            }
        }
    }

    private fun updateNode(oldNode: Node<StateType>, updatedNode: Node<StateType>) {
        openList.replace(oldNode, updatedNode)
        nodes[oldNode.state] = oldNode
    }

    override fun plan(state: StateType, terminationChecker: TerminationChecker): List<Action> {
        this.terminationChecker = terminationChecker
        val node = Node(state, domain.heuristic(state), 0.0, 0.0, NoOperationAction)
        var currentNode: Node<StateType>
        val startTime = System.nanoTime()
        nodes[state] = node
        openList.add(node)
        generatedNodeCount++

        while (openList.isNotEmpty() && !terminationChecker.reachedTermination()) {
            val topNode = openList.chooseNode() ?: throw GoalNotReachableException("Open list is empty")
            if (domain.isGoal(topNode.state)) {
                executionNanoTime = System.nanoTime() - startTime
                return extractPlan(topNode, state)
            }
            currentNode = topNode
            expandFromNode(currentNode)
        }
        if (terminationChecker.reachedTermination()) {
            throw MetronomeException("Reached termination condition, " +
                    "${terminationChecker.remaining() + 1} / ${terminationChecker.elapsed() - 1} remaining!")
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
        assert(startState == iterationNode.state)
        actions.reverse()
        return actions
    }
}