package edu.unh.cs.ai.realtimesearch.planner.suboptimal

import edu.unh.cs.ai.realtimesearch.MetronomeConfigurationException
import edu.unh.cs.ai.realtimesearch.MetronomeException
import edu.unh.cs.ai.realtimesearch.environment.*
import edu.unh.cs.ai.realtimesearch.experiment.configuration.ExperimentConfiguration
import edu.unh.cs.ai.realtimesearch.experiment.terminationCheckers.TerminationChecker
import edu.unh.cs.ai.realtimesearch.planner.classical.ClassicalPlanner
import edu.unh.cs.ai.realtimesearch.planner.exception.GoalNotReachableException
import edu.unh.cs.ai.realtimesearch.util.*
import java.util.*
import kotlin.Comparator

class ExplicitEstimationConservativeSearch<StateType : State<StateType>>(val domain: Domain<StateType>, val configuration: ExperimentConfiguration) : ClassicalPlanner<StateType>() {
    private val weight: Double = configuration.weight
            ?: throw MetronomeConfigurationException("Weight for Explicit Estimation Search is not specified.")

    val errorEstimator = ErrorEstimator<Node<StateType>>()

    var terminationChecker: TerminationChecker? = null

    private var aStarExpansions = 0
    private var dHatExpansions = 0
    private var fHatExpansions = 0

    private val cleanupNodeComparator = Comparator<Node<StateType>> { lhs, rhs ->
        when {
            lhs.f < rhs.f -> -1
            lhs.f > rhs.f -> 1
            lhs.cost > rhs.cost -> -1
            lhs.cost < rhs.cost -> 1
            else -> 0
        }
    }

    private val focalNodeComparator = Comparator<Node<StateType>> { lhs, rhs ->
        when {
            lhs.dHat < rhs.dHat -> -1
            lhs.dHat > rhs.dHat -> 1
            lhs.fHat < rhs.fHat -> -1
            lhs.fHat > rhs.fHat -> 1
            lhs.cost > rhs.cost -> -1
            lhs.cost < rhs.cost -> 1
            else -> 0
        }
    }

    private val openNodeComparator = Comparator<Node<StateType>> { lhs, rhs ->
        when {
            lhs.fHat < rhs.fHat -> -1
            lhs.fHat > rhs.fHat -> 1
            lhs.d < rhs.d -> -1
            lhs.d > rhs.d -> 1
            lhs.cost > rhs.cost -> -1
            lhs.cost < rhs.cost -> 1
            else -> 0
        }
    }

    private val explicitNodeComparator = Comparator<Node<StateType>> { lhs, rhs ->
        when {
            lhs.fHat <= weight * rhs.f -> -1
            lhs.fHat > weight * rhs.f -> 1
            else -> 0
        }
    }

//    private val setFocalIndex: (node: Node<StateType>, i: Int) -> (Unit) = { node, i -> node.focalIndex = i }
//    private val getFocalIndex: (node: Node<StateType>) -> (Int) = { node -> node.focalIndex }

    private val rbTree = RBTree(openNodeComparator, explicitNodeComparator)

    inner class FocalList : AbstractAdvancedPriorityQueue<Node<StateType>>(arrayOfNulls(1000000), focalNodeComparator) {
        override fun getIndex(item: Node<StateType>): Int = item.getIndex(0)
        override fun setIndex(item: Node<StateType>, index: Int) = item.setIndex(0, index)
    }

    private val focal = FocalList()

    private val nodes: HashMap<StateType, Node<StateType>> = HashMap(1000000, 1.toFloat())
    private val openList = SynchronizedQueue(rbTree, focal, explicitNodeComparator, 0)

    inner class CleanupList : AbstractAdvancedPriorityQueue<Node<StateType>>(arrayOfNulls(1000000), cleanupNodeComparator) {
        override fun getIndex(item: Node<StateType>): Int = item.getIndex(1)
        override fun setIndex(item: Node<StateType>, index: Int) = item.setIndex(1, index)
    }

    private val cleanup = CleanupList()

    inner class Node<out StateType : State<StateType>>(val state: StateType, var heuristic: Double, var cost: Double,
                                                       var actionCost: Double, var action: Action, override var d: Double,
                                                       override var parent: Node<StateType>? = null) :
            Indexable, RBTreeElement<Node<StateType>, Node<StateType>>, Comparable<Node<StateType>>,
            SearchQueueElement<Node<StateType>>, ErrorTraceable {
        override val g: Double
            get() = cost
        override val h: Double
            get() = heuristic

        private val indexMap = Array(2) { -1 }
        override fun setIndex(key: Int, index: Int) {
            indexMap[key] = index
        }

        override fun getIndex(key: Int): Int {
            return indexMap[key]
        }

        override var node: RBTreeNode<Node<StateType>, Node<StateType>>?
            get() = redBlackNode
            set(value) { redBlackNode = value}

        override fun toString(): String {
            return "EECS.Node(state=$state, heuristic=$heuristic, cost=$cost, actionCost=$actionCost, " +
                    "action=$action, f=$f, d=$d, fHat=$fHat, dHat=$dHat)"
        }

        override val open: Boolean
            get() = indexMap[1] >= 0

        private var redBlackNode: RBTreeNode<Node<StateType>, Node<StateType>>? = null

        override val f: Double
            get() = g + h

        override val depth: Double = parent?.depth?.plus(1) ?: 0.0

        val fHat: Double
            get() = g + hHat

        override var hHat = h
            get() = h + (dHat * errorEstimator.meanErrorHeuristic)

        override var dHat = d
            get() = d / (1.0 - errorEstimator.meanErrorDistance)

        override var index: Int = -1

        override fun compareTo(other: Node<StateType>): Int {
            val diff = (this.f - other.f).toInt()
            if (diff == 0) return (other.cost - this.cost).toInt()
            return diff
        }
    }

    private fun insertNode(node: Node<StateType>) {
        val bestF = cleanup.peek() ?: node
        openList.add(node, bestF)
        cleanup.add(node)
        nodes[node.state] = node
    }

    private fun selectNode(): Node<StateType> {
        val bestDHat = openList.peekFocal()
        val bestFHat = openList.peekOpen()
        val bestF = cleanup.peek() ?: throw MetronomeException("Cleanup is Empty!")

        when {
            bestDHat != null && bestDHat.fHat <= weight * bestF.f -> {
                val chosenNode = openList.pollFocal() ?: throw MetronomeException("Focal is Empty!")
                cleanup.remove(chosenNode)
                ++dHatExpansions
                return chosenNode
            }
            bestFHat != null && bestFHat.fHat <= weight * bestF.f -> {
                val chosenNode = openList.pollOpen() ?: throw MetronomeException("Open is Empty!")
                cleanup.remove(chosenNode)
                ++fHatExpansions
                return chosenNode
            }
            else -> {
                val chosenNode = cleanup.pop() ?: throw MetronomeException("Clean up is Empty!")
                openList.remove(chosenNode)
                ++aStarExpansions
                return chosenNode
            }
        }
    }

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
                    cost = Double.MAX_VALUE,
                    d = domain.distance(successorState)
            )
            nodes[successorState] = undiscoveredNode
            undiscoveredNode
        } else {
            tempSuccessorNode
        }
    }

    private fun calculateStatistics(sourceNode: Node<StateType>, successorNode: Node<StateType>) {
        errorEstimator.addSample(sourceNode, successorNode)
    }

    private fun expandFromNode(sourceNode: Node<StateType>) {
        expandedNodeCount++
        val currentGValue = sourceNode.cost
        val successors = domain.successors(sourceNode.state)
        for (successor in successors) {
            val successorState = successor.state
            val successorNode = getNode(sourceNode, successor)
            // skip if we have our parent as a successor
            if (successorState == sourceNode.parent?.state) {
                continue
            }
            // update the statistics
            calculateStatistics(sourceNode, successorNode)

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
                    insertNode(successorNode)
                } else {
                    nodes[successorState] = successorNode
                }
            }
        }
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

    override fun plan(state: StateType, terminationChecker: TerminationChecker): List<Action> {
        this.terminationChecker = terminationChecker
        val node = Node(state, domain.heuristic(state), 0.0, 0.0, NoOperationAction, d = domain.distance(state))
        val startTime = System.nanoTime()
        nodes[state] = node
        cleanup.add(node)
        openList.add(node, node)
        generatedNodeCount++

        while (cleanup.isNotEmpty() && !terminationChecker.reachedTermination()) {
            val topNode = selectNode()
            if (domain.isGoal(topNode.state)) {
                executionNanoTime = System.nanoTime() - startTime
                return extractPlan(topNode, state)
            }
            expandFromNode(topNode)
        }
        if (terminationChecker.reachedTermination()) {
            throw MetronomeException("Reached termination condition, " +
                    "${terminationChecker.remaining() + 1} / ${terminationChecker.elapsed() - 1} remaining!")
        }
        throw GoalNotReachableException()
    }
}