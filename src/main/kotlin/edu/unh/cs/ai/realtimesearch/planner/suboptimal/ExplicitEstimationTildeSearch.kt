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
import kotlin.math.sqrt

class ExplicitEstimationTildeSearch<StateType : State<StateType>>(val domain: Domain<StateType>, val configuration: ExperimentConfiguration) : ClassicalPlanner<StateType>() {
    private val weight: Double = configuration.weight
            ?: throw MetronomeConfigurationException("Weight for Explicit Estimation Search is not specified.")

    var terminationChecker: TerminationChecker? = null

    private val fNodeComparator = Comparator<ExplicitEstimationTildeSearch.Node<StateType>> { lhs, rhs ->
        when {
            lhs.f < rhs.f -> -1
            lhs.f > rhs.f -> 1
            lhs.cost > rhs.cost -> -1
            lhs.cost < rhs.cost -> 1
            else -> 0
        }
    }

    private val dHatComparator = Comparator<ExplicitEstimationTildeSearch.Node<StateType>> { lhs, rhs ->
        when {
            lhs.dHat < rhs.dHat -> -1
            lhs.dHat > rhs.dHat -> 1
            lhs.cost > rhs.cost -> -1
            lhs.cost < rhs.cost -> 1
            lhs.fHat < rhs.fHat -> -1
            lhs.fHat > rhs.fHat -> 1
            else -> 0
        }
    }

    private val fHatComparator = Comparator<ExplicitEstimationTildeSearch.Node<StateType>> { lhs, rhs ->
        when {
            lhs.fHat < rhs.fHat -> -1
            lhs.fHat > rhs.fHat -> 1
            lhs.cost > rhs.cost -> -1
            lhs.cost < rhs.cost -> 1
            lhs.dHat < rhs.dHat -> -1
            lhs.dHat > rhs.dHat -> 1
            else -> 0
        }
    }


    private val fTildeComparator = Comparator<ExplicitEstimationTildeSearch.Node<StateType>> { lhs, rhs ->
        when {
            lhs.fTilde < rhs.fTilde -> -1
            lhs.fTilde > rhs.fTilde -> 1
            lhs.d < rhs.d -> -1
            lhs.d > rhs.d -> 1
            lhs.cost > rhs.cost -> -1
            lhs.cost < rhs.cost -> 1
            else -> 0
        }
    }

    private val promisingComparator = Comparator<ExplicitEstimationTildeSearch.Node<StateType>> { lhs, rhs ->
        when {
            lhs.fTilde < weight * rhs.f -> -1
            lhs.fTilde > weight * lhs.f -> 1
            else -> 0
        }
    }

    private val qualifiedComparator = Comparator<ExplicitEstimationTildeSearch.Node<StateType>> { lhs, rhs ->
        when {
            lhs.f < weight * rhs.fHat -> -1
            lhs.f > weight * rhs.fHat -> 1
            else -> 0
        }
    }

    // accessors and setters for the individual indices within each container

    private val qualifiedSetIndex: (node: Node<StateType>, index: Int) -> (Unit) = { node, index -> node.qualifiedIndex = index }
    private val qualifiedGetIndex: (node: Node<StateType>) -> (Int) = { node -> node.qualifiedIndex }

    private val promisingSetIndex: (node: Node<StateType>, index: Int) -> (Unit) = { node, index -> node.promisingIndex = index }
    private val promisingGetIndex: (node: Node<StateType>) -> (Int) = { node -> node.promisingIndex }

    private val fHatSetIndex: (node: Node<StateType>, index: Int) -> (Unit) = { node, index -> node.fHatIndex = index }
    private val fHatGetIndex: (node: Node<StateType>) -> (Int) = { node -> node.fHatIndex }

    // set up for the containers to store the nodes

    private val qualifiedRBTree = TreeMap<Node<StateType>, Node<StateType>>(fNodeComparator)
    private val qualifiedFocal = AdvancedPriorityQueue(arrayOfNulls(100000000), fTildeComparator, qualifiedSetIndex, qualifiedGetIndex)

    private val promisingRBTree = TreeMap<Node<StateType>, Node<StateType>>(fTildeComparator)
    private val promisingFocal = AdvancedPriorityQueue(arrayOfNulls(100000000), dHatComparator, promisingSetIndex, promisingGetIndex)

    private val nodes: HashMap<StateType, ExplicitEstimationTildeSearch.Node<StateType>> = HashMap<StateType, ExplicitEstimationTildeSearch.Node<StateType>>(100000000, 1.toFloat()).resize()

    // uses four separate containers to store the nodes during search

    private val qualifiedNodes = ExplicitQueue(qualifiedRBTree, qualifiedFocal, qualifiedComparator, qualifiedGetIndex)
    private val promisingNodes = ExplicitQueue(promisingRBTree, promisingFocal, promisingComparator, promisingGetIndex)

    private val fHatHeap = AdvancedPriorityQueue(arrayOfNulls(100000000), fHatComparator, fHatSetIndex, fHatGetIndex)

    class ExplicitQueue<E>(private val open: TreeMap<E, E>, private val focal: AdvancedPriorityQueue<E>, private val explicitComparator: Comparator<E>,
                           private val getFocalIndex: (E) -> (Int)) where E : RedBlackTreeElement<E, E> {

        fun isEmpty(): Boolean = open.firstEntry() == null || open.firstEntry().value == null

        fun isNotEmpty(): Boolean = !isEmpty()

        // returns true when the element qualifies for left prefix
        fun add(e: E, oldBest: E): Boolean {
            open[e] = e
            if (explicitComparator.compare(e, oldBest) <= 0) {
                focal.add(e)
                return true
            }
            return false
        }

        fun updateFocal(oldBest: E?, newBest: E?, fHatChange: Int) {
            if (oldBest == null || fHatChange != 0) {
                if (oldBest != null && fHatChange < 0) {
                    open.replace(newBest!!, oldBest)
                } else if (oldBest?.getNode() == null) {
                    open.replace(oldBest!!, newBest!!)
                }
            }
        }

        fun remove(e: E) {
            open.remove(e)
            if (getFocalIndex(e) != -1) {
                focal.remove(e)
            }
        }

        fun pollOpen(): E? {
            val e = open.pollFirstEntry().value
            if (e != null && getFocalIndex(e) != -1) {
                focal.remove(e)
            }
            return e
        }

        fun pollFocal(): E? {
            val e = focal.pop()
            if (e != null) {
                open.remove(e)
            }
            return e
        }

        fun peekOpen(): E? = open.firstEntry().value
        fun peekFocal(): E? = focal.peek()
    }

    class Node<StateType : State<StateType>>(val state: StateType, var heuristic: Double, var cost: Long,
                                             var actionCost: Long, var action: Action, var d: Double,
                                             var parent: ExplicitEstimationTildeSearch.Node<StateType>? = null) : RedBlackTreeElement<Node<StateType>, Node<StateType>>, Comparable<Node<StateType>> {
        val open: Boolean
            get() = index >= 0

        var qualifiedIndex: Int = -1

        var promisingIndex: Int = -1

        var fHatIndex: Int = -1

        private var redBlackNode: RedBlackTreeNode<Node<StateType>, Node<StateType>>? = null

        val f: Double
            get() = cost + heuristic

        private val depth: Int = parent?.depth?.plus(1) ?: 17

        private var sseH = 0.0

        private var sseD = 0.0

        val fHat: Double
            get() = cost + hHat

        val fTilde: Double
            get() = fHat + (1.96 * sqrt(dHat * dHatVariance))

        private var hHat = 0.0

        var dHat = 0.0

        private var dHatMean: Double = 0.0
        private var dHatVariance: Double = 0.0

        var index: Int = -1

        init {
            computePathHats(parent, actionCost.toDouble())
        }

        private fun calculateDHatMean(): Double {
            return parent!!.dHatMean + ((dHat - parent!!.dHatMean) / depth)
        }

        private fun calculateDHatVariance(): Double {
            val sampleVariance = parent!!.dHatVariance + ((dHat - parent!!.dHatMean) * (dHat - dHatMean))
            return sampleVariance / (depth - 1)
        }

        private fun computePathHats(parent: Node<StateType>?, edgeCost: Double) {
            if (parent != null) {
                this.sseH = parent.sseH + ((edgeCost + heuristic) - parent.heuristic)
                this.sseD = parent.sseD + ((1 + d) - parent.d)
            }

            this.hHat = computeHHat()
            this.dHat = computeDHat()

            dHatMean = if (parent == null) {
                dHat
            } else {
                calculateDHatMean()
            }

            dHatVariance = if (parent == null) {
                0.0
            } else {
                calculateDHatVariance()
            }

            assert(fHat >= f)
            assert(dHat >= 0)
        }

        private fun computeHHat(): Double {
            var hHat = Double.MAX_VALUE
            val sseMean = if (cost == 0L) sseH else sseH / depth
            val dMean = if (cost == 0L) sseD else sseD / depth
            if (dMean < 1) {
                hHat = heuristic + ((d / (1 - dMean)) * sseMean)
            }
            return hHat
        }

        private fun computeDHat(): Double {
            var dHat = Double.MAX_VALUE
            val dMean = if (cost == 0L) sseD else sseD / depth
            if (dMean < 1) {
                dHat = d / (1 - dMean)
            }
            return dHat
        }

        override fun compareTo(other: Node<StateType>): Int {
            val diff = (this.f - other.f).toInt()
            if (diff == 0) return (other.cost - this.cost).toInt()
            return diff
        }

        override fun getNode(): RedBlackTreeNode<Node<StateType>, Node<StateType>>? {
            return redBlackNode
        }

        override fun setNode(node: RedBlackTreeNode<Node<StateType>, Node<StateType>>?) {
            this.redBlackNode = node
        }

    }

    private fun selectNode(): Node<StateType> {
        val dHatMin = promisingNodes.peekFocal()
        val fHatMin = fHatHeap.peek() ?: throw MetronomeException("F-Hat heap empty!")
        val fMin = qualifiedNodes.peekOpen() ?: throw MetronomeException("F rb-tree empty!")

        when {
            dHatMin != null && dHatMin.f <= weight * fMin.f && dHatMin.fHat <= weight * fHatMin.fHat -> {
                val chosenNode = promisingNodes.pollFocal()!!
                qualifiedNodes.remove(chosenNode)
                fHatHeap.remove(chosenNode)
                return chosenNode
               // return dHatMin
            }
            dHatMin != null && dHatMin.f <= weight * fMin.f && dHatMin.fHat > weight * fHatMin.fHat-> {
                val chosenNode = fHatHeap.pop()!!
                promisingNodes.remove(chosenNode)
                qualifiedNodes.remove(chosenNode)
                // return fHatMin
            }
            else -> {
                val chosenNode = qualifiedNodes.pollOpen()!!
                promisingNodes.remove(chosenNode)
                fHatHeap.remove(chosenNode)
                return chosenNode
               // return fMin
            }
        }
        throw MetronomeException("Select node is broken.")
   }

    private fun initializeAStar(): Long = System.currentTimeMillis()

    private fun getNode(sourceNode: Node<StateType>, successorBundle: SuccessorBundle<StateType>): Node<StateType> {
        val successorState = successorBundle.state
        val tempSuccessorNode = nodes[successorState]
        return if (tempSuccessorNode == null) {
            generatedNodeCount++
            terminationChecker!!.notifyExpansion()
            val undiscoveredNode = Node(
                    state = successorState,
                    heuristic = weight * domain.heuristic(successorState),
                    actionCost = successorBundle.actionCost,
                    action = successorBundle.action,
                    parent = sourceNode,
                    cost = Long.MAX_VALUE,
                    d = domain.distance(successorState)
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
        for (successor in domain.successors(sourceNode.state)) {
            val successorState = successor.state
            val successorNode = getNode(sourceNode, successor)
            // skip if we have our parent as a successor
            if (successorState == sourceNode.parent?.state) {
                continue
            }

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
                    fHatHeap.update(successorNode)
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

    private fun insertNode(node: Node<StateType>) {
        nodes[node.state] = node
        fHatHeap.add(node)
        val fMinNode = if (qualifiedNodes.isNotEmpty()) { qualifiedNodes.peekOpen() } else node
        val fHatMinNode = fHatHeap.peek() ?: node
        // only add nodes which are qualified and promising
        qualifiedNodes.add(node, fMinNode!!)
        promisingNodes.add(node, fHatMinNode)
    }

    override fun plan(state: StateType, terminationChecker: TerminationChecker): List<Action> {
        this.terminationChecker = terminationChecker
        val startTime = initializeAStar()
        val node = Node(state, weight * domain.heuristic(state), 0, 0, NoOperationAction, d = domain.distance(state))
        nodes[state] = node
        fHatHeap.add(node)
        qualifiedNodes.add(node, node)
        promisingNodes.add(node, node)
        generatedNodeCount++

        while (fHatHeap.isNotEmpty() && !terminationChecker.reachedTermination()) {
            println("Expansion #$expandedNodeCount")
            val oldBestQualified = qualifiedNodes.peekOpen()
            val oldBestPromising = promisingNodes.peekOpen()

            val topNode = selectNode() // openList.peek() ?: throw GoalNotReachableException("Open list is empty")
            if (domain.isGoal(topNode.state)) {
                executionNanoTime = System.currentTimeMillis() - startTime
                return extractPlan(topNode, state)
            }
            expandFromNode(topNode)

            val newBestQualified = qualifiedNodes.peekOpen()
            val newBestPromising = promisingNodes.peekOpen()

            if (newBestQualified != null) {
                val fChange = fNodeComparator.compare(newBestQualified, oldBestQualified)
                qualifiedNodes.updateFocal(oldBestQualified, newBestQualified, fChange)
            }
            if (newBestPromising != null) {
                val fTildeChange = fTildeComparator.compare(newBestPromising, oldBestPromising)
                promisingNodes.updateFocal(oldBestPromising, newBestPromising, fTildeChange)
            }
        }
        if (terminationChecker.reachedTermination()) {
            throw MetronomeException("Reached termination condition, " +
                    "${terminationChecker.remaining() + 1} / ${terminationChecker.elapsed() - 1} remaining!")
        }
        throw GoalNotReachableException()
    }
}