package edu.unh.cs.ai.realtimesearch.planner.suboptimal

import edu.unh.cs.ai.realtimesearch.MetronomeConfigurationException
import edu.unh.cs.ai.realtimesearch.MetronomeException
import edu.unh.cs.ai.realtimesearch.environment.*
import edu.unh.cs.ai.realtimesearch.experiment.configuration.ExperimentConfiguration
import edu.unh.cs.ai.realtimesearch.experiment.terminationCheckers.TerminationChecker
import edu.unh.cs.ai.realtimesearch.planner.classical.ClassicalPlanner
import edu.unh.cs.ai.realtimesearch.planner.exception.GoalNotReachableException
import edu.unh.cs.ai.realtimesearch.util.SearchQueueElement
import edu.unh.cs.ai.realtimesearch.util.collections.binheap.BinHeap
import edu.unh.cs.ai.realtimesearch.util.collections.gequeue.GEQueue
import edu.unh.cs.ai.realtimesearch.util.collections.rbtree.RBTreeElement
import edu.unh.cs.ai.realtimesearch.util.collections.rbtree.RBTreeNode
import java.util.*
import kotlin.Comparator

class ExplicitEstimationSearch<StateType : State<StateType>>(val domain: Domain<StateType>, val configuration: ExperimentConfiguration) : ClassicalPlanner<StateType>() {
    private val weight: Double = configuration.weight
            ?: throw MetronomeConfigurationException("Weight for Explicit Estimation Search is not specified.")

    val CLEANUP_ID = 0
    val FOCAL_ID = 1

    var terminationChecker: TerminationChecker? = null

    var aStarExpansions = 0
    var dHatExpansions = 0
    var fHatExpansions = 0

    private val cleanupNodeComparator = Comparator<Node<StateType>> { lhs, rhs ->
        when {
            lhs.f < rhs.f -> -1
            lhs.f > rhs.f -> 1
            rhs.g < lhs.g -> -1
            rhs.g > lhs.g -> 1
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
            lhs.g > rhs.g -> -1
            lhs.g < rhs.g -> 1
            else -> 0
        }
    }

    private val openNodeIgnoreTiesComparator = Comparator<Node<StateType>> { lhs, rhs ->
        when {
            lhs.fHat < rhs.fHat -> -1
            lhs.fHat > rhs.fHat -> 1
            else -> 0
        }
    }

    private val geNodeComparator = Comparator<Node<StateType>> { lhs, rhs ->
        when {
            lhs.fHat < weight * rhs.fHat -> -1
            lhs.fHat > weight * rhs.fHat -> 1
            else -> 0
        }
    }

    // cleanup is implemented as a binary heap
    private val cleanup = BinHeap(cleanupNodeComparator, CLEANUP_ID)

    // open is implemented as a red-black tree
    private val gequeue = GEQueue<Node<StateType>>(openNodeComparator, geNodeComparator, focalNodeComparator, FOCAL_ID)

    private val closed = HashMap<StateType, Node<StateType>>(1000000, 1.toFloat())

    inner class Node<out StateType : State<StateType>>(val state: StateType, var heuristic: Double, var cost: Double,
                                                       var actionCost: Double, var action: Action, override var d: Double,
                                                       override var parent: Node<StateType>? = null,
                                                       override val depth: Double = 0.0) :
            RBTreeElement<Node<StateType>, Node<StateType>>, SearchQueueElement<Node<StateType>>, Comparable<Node<StateType>> {
        override val f: Double
            get() = g + h
        val fHat: Double
            get() = g + hHat

        private var sseH: Double = 0.0
        private var sseD: Double = 0.0

        override var hHat: Double = h
        override var dHat: Double = d

        init {
            computePathHats()
        }

        private fun computePathHats() {
            if (parent != null) {
                sseH = parent!!.sseH + ((actionCost + h) - parent!!.h)
                sseD = parent!!.sseD + ((1 + d) - parent!!.d)
            }

            hHat = computeHHat()
            dHat = computeDHat()

            assert(fHat >= f)
            assert(dHat >= 0)
        }

        private fun computeDHat(): Double {
            var dHat = Double.MAX_VALUE
            val dMean = if (g == 0.0) sseD else sseD / depth
            if (dMean < 1) {
                dHat = d / (1 - dMean)
            }
            return dHat
        }

        private fun computeHHat(): Double {
            var hHat = Double.MAX_VALUE
            val sseMean = if (g == 0.0) sseH else sseH / depth
            val dMean = if (g == 0.0) sseD else sseD / depth
            if (dMean < 1) {
                hHat = h + ((d / (1 - dMean)) * sseMean)
            }
            return hHat
        }

        private var rbTreeNode: RBTreeNode<Node<StateType>, Node<StateType>>? = null

        override fun setNode(node: RBTreeNode<Node<StateType>, Node<StateType>>?) {
            rbTreeNode = node
        }

        override fun getNode(): RBTreeNode<Node<StateType>, Node<StateType>>? {
            return rbTreeNode
        }

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

        override fun toString(): String {
            return "ExplicitEstimationSearchH.Node(state=$state, heuristic=$heuristic, cost=$cost, actionCost=$actionCost, " +
                    "action=$action, f=$f, d=$d, fHat=$fHat, dHat=$dHat)"
        }

        override fun compareTo(other: Node<StateType>): Int {
            val diff = (this.f - other.f).toInt()
            if (diff == 0) return (other.cost - this.cost).toInt()
            return diff
        }

    }

    private fun insertNode(node: Node<StateType>, oldBest: Node<StateType>) {
        gequeue.add(node, oldBest)
        cleanup.add(node)
        closed[node.state] = node
    }

    private fun selectNode(): Node<StateType> {
        val value: Node<StateType>?
        val bestDHat = gequeue.peekFocal()
        val bestFHat = gequeue.peekOpen()
        val bestF = cleanup.peek()

        when {
            bestDHat.fHat <= weight * bestF.f -> {
                value = gequeue.pollFocal()
                cleanup.remove(value)
            }
            bestFHat.fHat <= weight * bestF.f -> {
                value = gequeue.pollOpen()
                cleanup.remove(value)
            }
            else -> {
                value = cleanup.poll()
                gequeue.remove(value)
            }
        }

        return value
    }

    private fun getNode(sourceNode: Node<StateType>, successorBundle: SuccessorBundle<StateType>): Node<StateType> {
        val successorState = successorBundle.state
        val tempSuccessorNode = closed[successorState]
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
                    d = domain.distance(successorState),
                    depth = sourceNode.depth + 1
            )
            closed[successorState] = undiscoveredNode
            undiscoveredNode
        } else {
            tempSuccessorNode
        }
    }

    private fun expandFromNode(sourceNode: Node<StateType>, oldBest: Node<StateType>) {
        val currentGValue = sourceNode.cost

        val successors = domain.successors(sourceNode.state)
        for (successor in successors) {
            val successorState = successor.state
            val isDuplicateNode = closed.containsKey(successorState)
            val successorNode = getNode(sourceNode, successor)
            // skip if we have our parent as a successor
            if (successorState == sourceNode.parent?.state) {
                continue
            }

            // only generate states which have not been visited or with a cheaper cost
            val successorGValueFromCurrent = currentGValue + successor.actionCost

            if (isDuplicateNode) {
                if (successorNode.cost > successorGValueFromCurrent) {
                    successorNode.apply {
                        cost = successorGValueFromCurrent
                        parent = sourceNode
                        action = successor.action
                        actionCost = successor.actionCost
                    }
                    if (successorNode.getIndex(CLEANUP_ID) != -1) {
                        gequeue.remove(successorNode)
                        cleanup.remove(successorNode)
                        closed.remove(successorNode.state)
                    }
                    insertNode(successorNode, oldBest)
                }
            } else {
                if (successorNode.cost > successorGValueFromCurrent) {
                    successorNode.apply {
                        cost = successorGValueFromCurrent
                        parent = sourceNode
                        action = successor.action
                        actionCost = successor.actionCost
                    }
                }
                insertNode(successorNode, oldBest)
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

        closed[state] = node
        cleanup.add(node)
        gequeue.add(node, node)
        generatedNodeCount++
        gequeue.updateFocal(null, node, 0)

        while (cleanup.isNotEmpty && !terminationChecker.reachedTermination()) {
            val oldBest = gequeue.peekOpen()
            val topNode = selectNode()
            if (domain.isGoal(topNode.state)) {
                return extractPlan(topNode, state)
            }
            expandFromNode(topNode, oldBest)
            expandedNodeCount++

            val newBest = gequeue.peekOpen()
            val fHatChange = openNodeIgnoreTiesComparator.compare(newBest, oldBest)
            gequeue.updateFocal(oldBest, newBest, fHatChange)
        }
        if (terminationChecker.reachedTermination()) {
            System.err.println("Used all ${terminationChecker.elapsed()} expansions")
            throw MetronomeException("Reached termination condition, " +
                    "${terminationChecker.remaining() + 1} / ${terminationChecker.elapsed() - 1} remaining!")
        }
        throw GoalNotReachableException()
    }
}