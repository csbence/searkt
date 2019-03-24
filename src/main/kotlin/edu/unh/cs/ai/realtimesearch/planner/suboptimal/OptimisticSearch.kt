package edu.unh.cs.ai.realtimesearch.planner.suboptimal

import edu.unh.cs.ai.realtimesearch.MetronomeConfigurationException
import edu.unh.cs.ai.realtimesearch.MetronomeException
import edu.unh.cs.ai.realtimesearch.environment.*
import edu.unh.cs.ai.realtimesearch.experiment.configuration.ExperimentConfiguration
import edu.unh.cs.ai.realtimesearch.experiment.terminationCheckers.TerminationChecker
import edu.unh.cs.ai.realtimesearch.planner.Planners
import edu.unh.cs.ai.realtimesearch.planner.classical.ClassicalPlanner
import edu.unh.cs.ai.realtimesearch.planner.exception.GoalNotReachableException
import edu.unh.cs.ai.realtimesearch.util.AbstractAdvancedPriorityQueue
import edu.unh.cs.ai.realtimesearch.util.Indexable
import edu.unh.cs.ai.realtimesearch.util.SearchQueueElement
import org.slf4j.LoggerFactory
import java.util.HashMap
import kotlin.Comparator

class OptimisticSearch<StateType : State<StateType>>(val domain: Domain<StateType>, val configuration: ExperimentConfiguration) : ClassicalPlanner<StateType>() {
    private val weight: Double = configuration.weight
            ?: throw MetronomeConfigurationException("Weight for optimistic search is not specified.")

    private val algorithmName: String = configuration.algorithmName

    var terminationChecker: TerminationChecker? = null

    var iteration = 0

    private var incumbentSolution: Node<StateType>? = null

    var aStarExpansions = 0
    var greedyExpansions = 0

    inner class Node<StateType : State<StateType>>(val state: StateType, var heuristic: Double, var cost: Double,
                                             var actionCost: Double, var action: Action,
                                             override var parent: Node<StateType>? = null):
            Indexable, SearchQueueElement<Node<StateType>> {
        var isClosed = false
        var expansionIteration = -1
        private val indexMap = Array(2) {-1}
        override val g: Double
            get() = cost
        override val depth: Double
            get() = cost
        override val h: Double
            get() = heuristic
        override val d: Double
            get() = cost
        override val hHat: Double
            get() = heuristic
        override val dHat: Double
            get() = heuristic

        val fHat: Double
            get() = if (weight < 1.8) cost + (weight * heuristic) else cost + (2*(weight - 1) + 1)*heuristic

        override fun setIndex(key: Int, index: Int) {
           indexMap[key] = index
        }

        override fun getIndex(key: Int): Int {
            return indexMap[key]
        }

        override var index: Int = -1

        override val f: Double
            get() = cost + heuristic

        override fun hashCode(): Int = state.hashCode()

        override fun equals(other: Any?): Boolean {
            return try {
                val otherCast = other as Node<*>
                otherCast.hashCode() == this.hashCode()
            } catch (exp: ClassCastException) {
                false
            }
        }

        override fun toString(): String =
                "Node: [State: $state h: $heuristic, g: $cost, actionCost: $actionCost, parent: ${parent?.state}, open: $open ]"
    }

    @Suppress("unused")
    private val logger = LoggerFactory.getLogger(OptimisticSearch::class.java)

    private val fValueComparator = Comparator<Node<StateType>> { lhs, rhs ->
        when {
            lhs.f < rhs.f -> -1
            lhs.f > rhs.f -> 1
            lhs.cost > rhs.cost -> -1 // Tie breaking on cost (g)
            lhs.cost < rhs.cost -> 1
            else -> 0
        }
    }

    private val fHatValueComparator = Comparator<Node<StateType>> { lhs, rhs ->
        when {
            lhs.fHat < rhs.fHat -> -1
            lhs.fHat > rhs.fHat -> 1
            lhs.cost > rhs.cost -> -1 // Tie breaking on cost (g)
            lhs.cost < rhs.cost -> 1
            else -> 0
        }
    }

    private val nodes: HashMap<StateType, Node<StateType>> = HashMap(1000000, 1.toFloat())

    inner class OpenListOnF : AbstractAdvancedPriorityQueue<Node<StateType>>(arrayOfNulls(1000000), fValueComparator) {
        override fun getIndex(item: Node<StateType>): Int = item.getIndex(0)
        override fun setIndex(item: Node<StateType>, index: Int) = item.setIndex(0, index)
    }

    private var fOpenList = OpenListOnF()

    inner class OpenListOnFHat : AbstractAdvancedPriorityQueue<Node<StateType>>(arrayOfNulls(1000000), fHatValueComparator) {
        override fun getIndex(item: Node<StateType>): Int = item.getIndex(1)
        override fun setIndex(item: Node<StateType>, index: Int) = item.setIndex(1, index)
    }

    private var fHatOpenList = OpenListOnFHat()

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
        if (sourceNode.isClosed) reexpansions++

        val currentGValue = sourceNode.cost
        for (successor in domain.successors(sourceNode.state)) {
            val successorState = successor.state
            val successorNode = getNode(sourceNode, successor)

            // skip if we have our parent as a successor
            if (successorState == sourceNode.parent?.state) {
                continue
            }

            // fresh undiscovered nodes have infinite cost if it doesn't
            // then the node must be a duplicate node from an earlier expansion
            val isDuplicate = successorNode.cost < Double.MAX_VALUE

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
                if (isDuplicate && successorNode.isClosed) {
                    // if duplicate and is closed add back to open
                    fOpenList.add(successorNode)
                    // only add duplicates back in with original optimistic search
                    if (algorithmName == Planners.OPTIMISTIC.toString()) {
                        fHatOpenList.add(successorNode)
                    }
                } else if (isDuplicate && !successorNode.isClosed){
                    // if duplicate and is open update within open
                    fOpenList.update(successorNode)
                    fHatOpenList.update(successorNode)
                } else {
                    // if a brand new node just add
                    fOpenList.add(successorNode)
                    fHatOpenList.add(successorNode)
                }
            }
        }
    }

    private fun selectNode(): Node<StateType> {
        val selectedNode: Node<StateType>

        val bestFHat = fHatOpenList.peek() ?: throw MetronomeException("Open list is empty")
        val bestF = fOpenList.peek() ?: throw MetronomeException("Open list is empty")
        val incumbentFHat = incumbentSolution?.fHat ?: Double.MAX_VALUE

        if (bestFHat.fHat < incumbentFHat) {
            greedyExpansions++
            selectedNode = bestFHat
            fHatOpenList.pop()
            fOpenList.remove(bestFHat)
        } else {
            aStarExpansions++
            selectedNode = bestF
            fOpenList.pop()
            fHatOpenList.remove(bestF)
        }

        return selectedNode
    }

    override fun plan(state: StateType, terminationChecker: TerminationChecker): List<Action> {
        this.terminationChecker = terminationChecker
        val node = Node(state, domain.heuristic(state), 0.0, 0.0, NoOperationAction)
        val startTime = System.nanoTime()
        nodes[state] = node
        fOpenList.add(node)
        fHatOpenList.add(node)
        generatedNodeCount++

        while (fOpenList.isNotEmpty() && !terminationChecker.reachedTermination()) {
            if (weight * fOpenList.peek()!!.f >= incumbentSolution?.f ?: Double.MAX_VALUE) {
                executionNanoTime = System.nanoTime() - startTime
                return extractPlan(incumbentSolution!!, state)
            }

            val topNode = selectNode()
            if (domain.isGoal(topNode.state)) {
                incumbentSolution = topNode
                if (weight < 1.8) {
                    return extractPlan(incumbentSolution!!, state)
                }
                if (weight * fOpenList.peek()!!.f >= incumbentSolution!!.f) {
                    executionNanoTime = System.nanoTime() - startTime
                    return extractPlan(incumbentSolution!!, state)
                }
            }
            expandFromNode(topNode)
            iteration++
            topNode.isClosed = true
            expandedNodeCount++
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