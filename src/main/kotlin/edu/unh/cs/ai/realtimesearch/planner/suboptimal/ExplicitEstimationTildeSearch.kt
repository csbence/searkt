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
            ?: throw MetronomeConfigurationException("Weight for Explicit Estimation Tilde Search is not specified.")
    private val errorModel: String = configuration.errorModel
            ?: throw MetronomeConfigurationException("Error model for Explicit Estimation Tilde Search is not specified")
    private val actionDuration = configuration.actionDuration

    var terminationChecker: TerminationChecker? = null

    private val cleanupNodeComparator = Comparator<ExplicitEstimationTildeSearch.Node<StateType>> { lhs, rhs ->
        when {
            lhs.f < rhs.f -> -1
            lhs.f > rhs.f -> 1
            lhs.cost > rhs.cost -> -1
            lhs.cost < rhs.cost -> 1
            else -> 0
        }
    }

    private val focalNodeComparator = Comparator<ExplicitEstimationTildeSearch.Node<StateType>> { lhs, rhs ->
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

    private val openNodeComparator = Comparator<ExplicitEstimationTildeSearch.Node<StateType>> { lhs, rhs ->
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

    private val newOpenNodeComparator = Comparator<ExplicitEstimationTildeSearch.Node<StateType>> { lhs, rhs ->
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
            lhs.fTilde <= weight * rhs.fHat -> -1
            lhs.fTilde > weight * rhs.fHat -> 1
            else -> 0
        }
    }

    private val qualifiedComparator = Comparator<ExplicitEstimationTildeSearch.Node<StateType>> { lhs, rhs ->
        when {
            lhs.f <= weight * rhs.f -> -1
            lhs.f > weight * rhs.f -> 1
            else -> 0
        }
    }

    // set up for the containers to store the nodes

    private val qualifiedRBTree = RBTree(cleanupNodeComparator, qualifiedComparator)

    inner class QualifiedFocalList : AbstractAdvancedPriorityQueue<ExplicitEstimationTildeSearch.Node<StateType>>(arrayOfNulls(1000000), newOpenNodeComparator) {
        override fun getIndex(item: ExplicitEstimationTildeSearch.Node<StateType>): Int = item.getIndex(0)
        override fun setIndex(item: ExplicitEstimationTildeSearch.Node<StateType>, index: Int) = item.setIndex(0, index)
    }

    private val qualifiedFocal = QualifiedFocalList()


    private val promisingRBTree = RBTree(newOpenNodeComparator, promisingComparator)

    inner class PromisingFocalList : AbstractAdvancedPriorityQueue<ExplicitEstimationTildeSearch.Node<StateType>>(arrayOfNulls(1000000), focalNodeComparator) {
        override fun getIndex(item: Node<StateType>): Int = item.getIndex(1)
        override fun setIndex(item: Node<StateType>, index: Int) = item.setIndex(1, index)
    }

    private val promisingFocal = PromisingFocalList()

    private val nodes: HashMap<StateType, ExplicitEstimationTildeSearch.Node<StateType>> = HashMap(100000000, 1.toFloat())

    // uses four separate containers to store the nodes during search

    private val qualifiedNodes = SynchronizedQueue(qualifiedRBTree, qualifiedFocal, qualifiedComparator, 0)
    private val promisingNodes = SynchronizedQueue(promisingRBTree, promisingFocal, promisingComparator, 1)

    inner class FHatList : AbstractAdvancedPriorityQueue<ExplicitEstimationTildeSearch.Node<StateType>>(arrayOfNulls(1000000), openNodeComparator) {
        override fun getIndex(item: Node<StateType>): Int = item.getIndex(2)
        override fun setIndex(item: Node<StateType>, index: Int) = item.setIndex(2, index)
    }

    private val fHatHeap = FHatList()

    private var fMinExpansion = 0
    private var fHatMinExpansion = 0
    private var dHatMinExpansion = 0

    class Node<StateType : State<StateType>>(val state: StateType, var heuristic: Double, var cost: Double,
                                             var actionCost: Double, var action: Action, override var d: Double,
                                             override var parent: ExplicitEstimationTildeSearch.Node<StateType>? = null) :
            Indexable, RBTreeElement<Node<StateType>, Node<StateType>>,
            Comparable<Node<StateType>>, SearchQueueElement<Node<StateType>>{
        override var node: RBTreeNode<Node<StateType>, Node<StateType>>?
            get() = internalNode
            set(value) { internalNode = value }
        private val indexMap = Array(3) { -1 }
        override val g: Double
            get() = cost
        override val h: Double
            get() = heuristic

        override fun setIndex(key: Int, index: Int) {
            indexMap[key] = index
        }

        override fun getIndex(key: Int): Int {
            return indexMap[key]
        }

        override val open: Boolean
            get() = indexMap[2] >= 0

        private var internalNode: RBTreeNode<Node<StateType>, Node<StateType>>? = null

        override val f: Double
            get() = cost + heuristic

        override val depth: Double = parent?.depth?.plus(1) ?: 17.0

        private var sseH = 0.0

        private var sseD = 0.0

        val fHat: Double
            get() = cost + hHat

        val fTilde: Double
            get() = fHat + (1.96 * sqrt(dHat * dHatVariance))

        override var hHat = 0.0

        override var dHat = 0.0

        private var dHatMean: Double = 0.0
        private var dHatVariance: Double = 0.0

        override var index: Int = -1

        init {
            computePathHats(parent, actionCost)
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
            val sseMean = if (cost == 0.0) sseH else sseH / depth
            val dMean = if (cost == 0.0) sseD else sseD / depth
            if (dMean < 1) {
                hHat = heuristic + ((d / (1 - dMean)) * sseMean)
            }
            return hHat
        }

        private fun computeDHat(): Double {
            var dHat = Double.MAX_VALUE
            val dMean = if (cost == 0.0) sseD else sseD / depth
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

    }

    private fun selectNode(): Node<StateType> {
        when {
            qualifiedNodes.focal.isEmpty() -> {
                val chosenNode = qualifiedNodes.pollOpen()!!
                promisingNodes.remove(chosenNode)
                fHatHeap.remove(chosenNode)
                ++fMinExpansion
                return chosenNode
                // nothing qualifies [f <= w*fMin]
                // return fMin
            }
            promisingNodes.focal.isEmpty() -> {
                val chosenNode = fHatHeap.pop()!!
                promisingNodes.remove(chosenNode)
                qualifiedNodes.remove(chosenNode)
                ++fHatMinExpansion
                return chosenNode
                // nothing promising [f~ <= w*fHatMin]
                // return fHatMin
            }
            else -> {
                val chosenNode = promisingNodes.pollFocal()!!
                qualifiedNodes.remove(chosenNode)
                fHatHeap.remove(chosenNode)
                ++dHatMinExpansion
                return chosenNode
                // something is in qualified AND promising
                // return dHatMin which is from promising
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
//        println("fMinNodesExpanded: $fMinExpansion | fHatNodesExpanded: $fHatMinExpansion | dHatNodesExpanded $dHatMinExpansion")
        return actions
    }

    private fun insertNode(node: Node<StateType>) {
        val fMinNode = (if (qualifiedNodes.isNotEmpty()) {
            qualifiedNodes.peekOpen()
        } else node)!!
        val fHatMinNode = fHatHeap.peek() ?: node
        // only add nodes which are qualified and promising
        qualifiedNodes.add(node, fMinNode)
        nodes[node.state] = node
        fHatHeap.add(node)
        promisingNodes.add(node, fHatMinNode)
    }

    override fun plan(state: StateType, terminationChecker: TerminationChecker): List<Action> {
        this.terminationChecker = terminationChecker
        val node = Node(state, domain.heuristic(state), 0.0, 0.0, NoOperationAction, d = domain.distance(state))
        val startTime = System.nanoTime()
        nodes[state] = node
        fHatHeap.add(node)
        qualifiedNodes.add(node, node)
        promisingNodes.add(node, node)
        generatedNodeCount++

        while (fHatHeap.isNotEmpty() && !terminationChecker.reachedTermination()) {
            val topNode = selectNode()
            if (domain.isGoal(topNode.state)) {
                executionNanoTime = System.nanoTime() - startTime
                return extractPlan(topNode, state)
            }
            expandFromNode(topNode)

        }
        if (terminationChecker.reachedTermination()) {
            throw MetronomeException("Reached termination condition, " +
                    "${terminationChecker.remaining() + 1} / ${terminationChecker.elapsed() - 1} remaining!\n\t" +
                    "fMinNodesExpanded: $fMinExpansion | fHatNodesExpanded: $fHatMinExpansion | dHatNodesExpanded $dHatMinExpansion")
        }
        throw GoalNotReachableException()
    }
}