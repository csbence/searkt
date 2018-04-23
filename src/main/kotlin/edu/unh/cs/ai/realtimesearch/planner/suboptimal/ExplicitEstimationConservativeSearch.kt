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
import kotlin.math.abs

class ExplicitEstimationConservativeSearch<StateType : State<StateType>>(val domain: Domain<StateType>, val configuration: ExperimentConfiguration) : ClassicalPlanner<StateType>() {
    private val weight: Double = configuration.weight
            ?: throw MetronomeConfigurationException("Weight for Explicit Estimation Search is not specified.")
    private val errorModel: String = configuration.errorModel
            ?: throw MetronomeException("Error model for Explicit Estimation Search is not specified.")
    private val actionDuration: Long = configuration.actionDuration

    var terminationChecker: TerminationChecker? = null

    private var heuristicErrorGlobalSum: Double = 0.0
    private var globalSamples: Double = 17.0 // start as if we have already seen some (perfect) samples
    private var distanceErrorGlobalSum: Double = 0.0

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
    private val focal = BinHeap(1000000, focalNodeComparator, 0) //AdvancedPriorityQueue(arrayOfNulls(100000000), focalNodeComparator, setFocalIndex, getFocalIndex)

    private val nodes: HashMap<StateType, Node<StateType>> = HashMap(1000000, 1.toFloat())
    private val openList = ExplicitQueue(rbTree, focal, explicitNodeComparator)
    private val cleanup = BinHeap(1000000, cleanupNodeComparator, 1)

    class ExplicitQueue<E>(private val open: RBTree<E, E>, private val focal: BinHeap<E>, private val explicitComparator: Comparator<E>)
            where E : RBTreeElement<E, E>, E : Indexable, E: SearchQueueElement<E> {

        fun isEmpty(): Boolean = focal.isEmpty

        fun isNotEmpty(): Boolean = !isEmpty()

        fun add(e: E, oldBest: E) {
//            open[e] = e
            open.insert(e, e)
            if (explicitComparator.compare(e, oldBest) <= 0) {
                focal.add(e)
            }
        }

        fun remove(e: E) {
//            open.remove(e)
            open.delete(e)
            if (e.getIndex(0) != -1) {
                focal.remove(e)
            }
        }

        fun pollOpen(): E? {
//            val e = open.pollFirstEntry().value
            val e = open.poll()
            if (e != null && e.getIndex(0) != -1) {
                focal.remove(e)
            }
            return e
        }

        fun pollFocal(): E? {
//            val e = focal.pop()
            val e = focal.poll()
            if (e != null) {
//                open.remove(e)
                open.delete(e)
            }
            return e
        }

        fun peekOpen(): E? =  open.peek() //if (open.firstEntry() != null) open.firstEntry().value else null
        fun peekFocal(): E? = focal.peek()
    }

    inner class Node<out StateType : State<StateType>>(val state: StateType, var heuristic: Double, var cost: Double,
                                                       var actionCost: Double, var action: Action, override var d: Double,
                                                       override var parent: Node<StateType>? = null) :
            Indexable, RBTreeElement<Node<StateType>, Node<StateType>>, Comparable<Node<StateType>>, SearchQueueElement<Node<StateType>> {
        override val g: Double
            get() = cost
        override val h: Double
            get() = heuristic

        private val indexMap = Array(2, {-1})
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
            return "EES.Node(state=$state, heuristic=$heuristic, cost=$cost, actionCost=$actionCost, " +
                    "action=$action, f=$f, d=$d, fHat=$fHat, dHat=$dHat)"
        }

        override val open: Boolean
            get() = indexMap[1] >= 0

//        var focalIndex: Int = -1

        private var redBlackNode: RBTreeNode<Node<StateType>, Node<StateType>>? = null

        override val f: Double
            get() = cost + heuristic

        private val artificialDepth: Int = parent?.artificialDepth?.plus(1) ?: 17
        override val depth: Double = parent?.depth?.plus(1) ?: 0.0

        private var sseH = 0.0

        private var sseD = 0.0

        val fHat: Double
            get() = cost + hHat

        override var hHat = 0.0

        override var dHat = 0.0

        private var dHatMean: Double = 0.0
        private var dHatVariance: Double = 0.0

        override var index: Int = -1

        fun computeHats() {
            when (errorModel) {
                "path" -> computePathHats(parent, actionCost)
                "global" -> computeGlobalHats()
                else -> throw MetronomeException("Unknown and unsupported error model $errorModel, supported models" +
                        ": \"path\" or \"global\"")
            }
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
        }

        private fun calculateDHatMean(): Double {
            return parent!!.dHatMean + ((dHat - parent!!.dHatMean) / artificialDepth)
        }

        private fun calculateDHatVariance(): Double {
            val sampleVariance = parent!!.dHatVariance + ((dHat - parent!!.dHatMean) * (dHat - dHatMean))
            return sampleVariance / (artificialDepth - 1)
        }

        private fun computeGlobalHats() {
            val currentGlobalHeuristicError = heuristicErrorGlobalSum / globalSamples
            val currentGlobalDistanceError = distanceErrorGlobalSum / globalSamples
            this.hHat = heuristic + ((this.d / (actionDuration - currentGlobalDistanceError)) * currentGlobalHeuristicError)
            this.dHat = this.d / (1 - currentGlobalDistanceError)
            assert(fHat >= f)
            assert(dHat >= 0)
        }

        private fun computePathHats(parent: Node<StateType>?, edgeCost: Double) {
            if (parent != null) {
                this.sseH = parent.sseH + ((edgeCost + heuristic) - parent.heuristic)
                this.sseD = parent.sseD + ((1 + d) - parent.d)
            }
            this.hHat = computeHHat()
            this.dHat = computeDHat()
            assert(fHat >= f)
            assert(dHat >= 0)
        }

        private fun computeHHat(): Double {
            var hHat = Double.MAX_VALUE
            val sseMean = if (cost == 0.0) sseH else sseH / artificialDepth
            val dMean = if (cost == 0.0) sseD else sseD / artificialDepth
            if (dMean < 1) {
                hHat = heuristic + ((d / (1 - dMean)) * sseMean)
            }
            return hHat
        }

        private fun computeDHat(): Double {
            var dHat = Double.MAX_VALUE
            val dMean = if (cost == 0.0) sseD else sseD / artificialDepth
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

//        override fun getNode(): RedBlackTreeNode<Node<StateType>, Node<StateType>>? {
//            return redBlackNode
//        }
//
//        override fun setNode(node: RedBlackTreeNode<Node<StateType>, Node<StateType>>?) {
//            this.redBlackNode = node
//        }

    }

    private fun insertNode(node: Node<StateType>) {
        val bestF = cleanup.peek() ?: node
        openList.add(node, bestF)
        cleanup.add(node)
        nodes[node.state] = node
    }

    private fun selectNode(): Node<StateType> {
        val bestDHat = openList.peekFocal() ?: throw MetronomeException("Focal is Empty!")
        val bestFHat = openList.peekOpen() ?: throw MetronomeException("Open is Empty!")
        val bestF = cleanup.peek() ?: throw MetronomeException("Cleanup is Empty!")

        when {
            bestDHat.fHat <= weight * bestF.f -> {
                val chosenNode = openList.pollFocal() ?: throw MetronomeException("Focal is Empty!")
                cleanup.remove(chosenNode)
                ++dHatExpansions
                return chosenNode
            }
            bestFHat.fHat <= weight * bestF.f -> {
                val chosenNode = openList.pollOpen() ?: throw MetronomeException("Open is Empty!")
                cleanup.remove(chosenNode)
                ++fHatExpansions
                return chosenNode
            }
            else -> {
                val chosenNode = cleanup.poll() ?: throw MetronomeException("Clean up is Empty!")
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

    private fun calculateGlobalErrors(sourceNode: Node<StateType>) {
        val currentGValue = sourceNode.cost
        val successors = domain.successors(sourceNode.state)
        val currentDepthValue = sourceNode.depth
        // calculate global heuristic error based off of the best child
        var bestChild = successors.first()
        successors.forEach { successor ->
            val successorF = currentGValue + successor.actionCost + domain.heuristic(successor.state)
            val bestChildF = currentGValue + bestChild.actionCost + domain.heuristic(bestChild.state)
            // if the successor has a better f value make it the new best child
            if (successorF < bestChildF) {
                bestChild = successor
            }
            // if the successor has the same f and lower d make it the new best child
            if (successorF == bestChildF && domain.distance(successor.state) < domain.distance(bestChild.state)) {
                bestChild = successor
            }
        }
        // bestChild will be the successor of the source with the lowest f (ties broken on d)
        // calculate the global errors
        val bestChildF = currentGValue + bestChild.actionCost + domain.heuristic(bestChild.state)
        val parentF = sourceNode.f // currentGValue + domain.heuristic(sourceNode.state)
        heuristicErrorGlobalSum += bestChildF - parentF // should be equal if not record the error
        val bestChildL = currentDepthValue + 1 + domain.distance(bestChild.state)
        val parentL = currentDepthValue + domain.distance(sourceNode.state)
        distanceErrorGlobalSum += bestChildL - parentL // should be equal if not record the error
        globalSamples += 1 // tally how many samples we have seen
    }

    private fun expandFromNode(sourceNode: Node<StateType>) {
        expandedNodeCount++
        val currentGValue = sourceNode.cost
        val successors = domain.successors(sourceNode.state)
        calculateGlobalErrors(sourceNode)
        for (successor in successors) {
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
                successorNode.computeHats() // set the inadmissible estimates after setting the cost
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

        while (openList.isNotEmpty() && !terminationChecker.reachedTermination()) {
            val topNode = selectNode() // openList.peek() ?: throw GoalNotReachableException("Open list is empty")
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