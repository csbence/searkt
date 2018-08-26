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

class TildeSearch<StateType : State<StateType>>(val domain: Domain<StateType>, val configuration: ExperimentConfiguration) : ClassicalPlanner<StateType>() {
    private val weight: Double = configuration.weight
            ?: throw MetronomeConfigurationException("Weight for Tilde Search is not specified.")

    var terminationChecker: TerminationChecker? = null

    private val cleanupNodeComparator = Comparator<TildeSearch.Node<StateType>> { lhs, rhs ->
        when {
            lhs.f < rhs.f -> -1
            lhs.f > rhs.f -> 1
            lhs.cost > rhs.cost -> -1
            lhs.cost < rhs.cost -> 1
            else -> 0
        }
    }

    private val openNodeComparator = Comparator<TildeSearch.Node<StateType>> { lhs, rhs ->
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

    // set up for the containers to store the nodes

    private val nodes: HashMap<StateType, TildeSearch.Node<StateType>> = HashMap(100000000, 1.toFloat())

    private val openList = BinHeap(1000000, openNodeComparator, 0)


    private var fMinExpansion = 0
    private var fHatMinExpansion = 0
    private var dHatMinExpansion = 0

    class Node<StateType : State<StateType>>(val state: StateType, var heuristic: Double, var cost: Double,
                                             var actionCost: Double, var action: Action, override var d: Double,
                                             override var parent: TildeSearch.Node<StateType>? = null) :
            Indexable, Comparable<Node<StateType>>, SearchQueueElement<Node<StateType>>{

        private val indexMap = Array(1) {-1}
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
            get() = indexMap[0] >= 0

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
                    openList.add(successorNode)
                } else {
                    openList.update(successorNode)
                }
            }
        }
    }

    private fun checkTermination(terminationChecker: TerminationChecker) {
        if (terminationChecker.reachedTermination()) {
            throw MetronomeException("Reached termination condition, " +
                    "${terminationChecker.remaining() + 1} / ${terminationChecker.elapsed() - 1} remaining!\n")
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

    private fun cleanUpOnF(goalNodeFValue: Double, terminationChecker: TerminationChecker): Boolean {
        openList.reorder(cleanupNodeComparator)
        val lowestFNode = openList.peek()
        if (lowestFNode == null || lowestFNode.f > goalNodeFValue){
            openList.reorder(openNodeComparator)
            return true
        }
        while(!openList.isEmpty && !terminationChecker.reachedTermination()) {
            val topNode = openList.poll()
            if (topNode == null || topNode.f > goalNodeFValue) {
                openList.reorder(openNodeComparator)
                return true
            } else {
                expandFromNode(topNode)
            }
        }
        checkTermination(terminationChecker)
        throw GoalNotReachableException()
    }

    override fun plan(state: StateType, terminationChecker: TerminationChecker): List<Action> {
        this.terminationChecker = terminationChecker
        val node = Node(state, domain.heuristic(state), 0.0, 0.0, NoOperationAction, d = domain.distance(state))
        val startTime = System.nanoTime()
        nodes[state] = node
        openList.add(node)
        generatedNodeCount++

        while (!openList.isEmpty && !terminationChecker.reachedTermination()) {
            val topNode = openList.poll() ?: throw GoalNotReachableException("Open list is empty")
            if (domain.isGoal(topNode.state)) {
                if (cleanUpOnF(topNode.f, terminationChecker)) {
                    executionNanoTime = System.nanoTime() - startTime
                    return extractPlan(topNode, state)
                }
            }
            expandFromNode(topNode)

        }
        checkTermination(terminationChecker)
        throw GoalNotReachableException()
    }
}