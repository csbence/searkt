package edu.unh.cs.ai.realtimesearch.planner.suboptimal

import edu.unh.cs.ai.realtimesearch.MetronomeConfigurationException
import edu.unh.cs.ai.realtimesearch.MetronomeException
import edu.unh.cs.ai.realtimesearch.environment.*
import edu.unh.cs.ai.realtimesearch.experiment.configuration.ExperimentConfiguration
import edu.unh.cs.ai.realtimesearch.experiment.terminationCheckers.TerminationChecker
import edu.unh.cs.ai.realtimesearch.planner.classical.ClassicalPlanner
import edu.unh.cs.ai.realtimesearch.planner.exception.GoalNotReachableException
import edu.unh.cs.ai.realtimesearch.util.AbstractAdvancedPriorityQueue
import edu.unh.cs.ai.realtimesearch.util.Indexable
import edu.unh.cs.ai.realtimesearch.util.SearchQueueElement
import org.slf4j.LoggerFactory
import java.util.HashMap
import kotlin.Comparator
import kotlin.math.sqrt

class OptimisticXDPSearch<StateType : State<StateType>>(val domain: Domain<StateType>, val configuration: ExperimentConfiguration) : ClassicalPlanner<StateType>() {
    private val weight: Double = configuration.weight
            ?: throw MetronomeConfigurationException("Weight for optimistic search is not specified.")

    private val algorithmName: String = configuration.algorithmName

    private val wOpt = 2.0 * (weight - 1.0) + 1.0

    private var maxFwValue = Double.MIN_VALUE

    var terminationChecker: TerminationChecker? = null

    var iteration = 0

    private var incumbentSolution: Node<StateType>? = null

    var aStarExpansions = 0
    var greedyExpansions = 0
    var proofExpansions = 0

    inner class Node<StateType : State<StateType>>(val state: StateType, var heuristic: Double, var cost: Double,
                                                   var actionCost: Double, var action: Action,
                                                   override var parent: Node<StateType>? = null) :
            Indexable, SearchQueueElement<Node<StateType>> {
        var isClosed = false
        private val indexMap = Array(2) { -1 }
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

        val xdp: Double
            get() = (1 / (2 * wOpt)) * (cost + (((2 * wOpt) - 1) * heuristic) +
                    sqrt(Math.pow(cost - heuristic, 2.0) + (4 * wOpt * cost * heuristic)))

        val fw: Double
            get() = (cost / weight) + heuristic

        val fHat: Double
            get() = cost + wOpt * heuristic

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
    private val logger = LoggerFactory.getLogger(OptimisticXDPSearch::class.java)

    private val xdpComparator = Comparator<Node<StateType>> { lhs, rhs ->
        when {
            lhs.xdp < rhs.xdp -> -1
            lhs.xdp > rhs.xdp -> 1
            lhs.cost > rhs.cost -> -1 // Tie breaking on cost (g)
            lhs.cost < rhs.cost -> 1
            else -> 0
        }
    }

    private val fValueComparator = Comparator<Node<StateType>> { lhs, rhs ->
        when {
            lhs.f < rhs.f -> -1
            lhs.f > rhs.f -> 1
            lhs.cost > rhs.cost -> -1 // Tie breaking on cost (g)
            lhs.cost < rhs.cost -> 1
            else -> 0
        }
    }

    private val nodes: HashMap<StateType, Node<StateType>> = HashMap(1000000, 1.toFloat())

    inner class OpenListOnXDP : AbstractAdvancedPriorityQueue<Node<StateType>>(arrayOfNulls(1000000), xdpComparator) {
        override fun getIndex(item: Node<StateType>): Int = item.getIndex(0)
        override fun setIndex(item: Node<StateType>, index: Int) = item.setIndex(0, index)
    }

    private var openListOnXDP = OpenListOnXDP()

    inner class OpenListOnF : AbstractAdvancedPriorityQueue<Node<StateType>>(arrayOfNulls(1000000), fValueComparator) {
        override fun getIndex(item: Node<StateType>): Int = item.getIndex(1)
        override fun setIndex(item: Node<StateType>, index: Int) = item.setIndex(1, index)
    }

    private var openListOnF = OpenListOnF()

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
                    // only add to A* heap do not need to reopen
                    // for the greedy search
                    openListOnF.add(successorNode)
                } else if (isDuplicate && !successorNode.isClosed) {
                    // if duplicate and is open update within open
                    openListOnXDP.update(successorNode)
                    openListOnF.update(successorNode)
                } else {
                    // if a brand new node just add
                    openListOnXDP.add(successorNode)
                    openListOnF.add(successorNode)
                }
            }

            // keep track of max fw value
            maxFwValue = if (successorNode.fw > maxFwValue) successorNode.fw else maxFwValue
        }
    }

    private fun selectNode(): Node<StateType> {
        val selectedNode: Node<StateType>

        val bestF = openListOnF.peek() ?: throw MetronomeException("Open list is empty")
        val bestXDP = openListOnXDP.peek() ?: throw MetronomeException("Open list is empty")
        val incumbentXDP = incumbentSolution?.xdp ?: Double.MAX_VALUE

        if (bestXDP.xdp < incumbentXDP) {
            greedyExpansions++
            selectedNode = bestXDP
            openListOnXDP.pop()
            openListOnF.remove(bestXDP)
        } else {
            aStarExpansions++
            selectedNode = bestF
            openListOnF.pop()
            openListOnXDP.remove(bestF)
        }

        return selectedNode
    }

    private fun proveSolution(incumbentSolution: Node<StateType>): Node<StateType> {
        var currentSolution = incumbentSolution
        while (weight * openListOnF.peek()!!.f < incumbentSolution.f) {
            ++aStarExpansions
            ++proofExpansions
            val topF = openListOnF.pop()!!
            openListOnXDP.remove(topF)

            if (domain.isGoal(topF.state)) {
                currentSolution = topF
            }
            expandFromNode(topF)
        }
        return currentSolution
    }

    override fun plan(state: StateType, terminationChecker: TerminationChecker): List<Action> {
        this.terminationChecker = terminationChecker
        val node = Node(state, domain.heuristic(state), 0.0, 0.0, NoOperationAction)
        val startTime = System.nanoTime()
        nodes[state] = node
        openListOnXDP.add(node)
        openListOnF.add(node)
        generatedNodeCount++

        while (openListOnXDP.isNotEmpty() && !terminationChecker.reachedTermination()) {
            if (weight * maxFwValue >= incumbentSolution?.f ?: Double.MAX_VALUE) {
                return if (weight * openListOnF.peek()!!.f >= incumbentSolution?.f ?: Double.MAX_VALUE) {
                    executionNanoTime = System.nanoTime() - startTime
                    extractPlan(incumbentSolution!!, state)
                } else {
                    // prove solution with A* expansions
                    incumbentSolution = proveSolution(incumbentSolution!!)
                    extractPlan(incumbentSolution!!, state)
                }
            }

            val topNode = selectNode()
            // goal check
            if (domain.isGoal(topNode.state)) {
                incumbentSolution = topNode
                if (weight * maxFwValue >= incumbentSolution!!.f) {
                    return if (weight * openListOnF.peek()!!.f >= incumbentSolution!!.f) {
                        executionNanoTime = System.nanoTime() - startTime
                        extractPlan(incumbentSolution!!, state)
                    } else {
                        // prove solution with A* expansions
                        incumbentSolution = proveSolution(incumbentSolution!!)
                        extractPlan(incumbentSolution!!, state)
                    }
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