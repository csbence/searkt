package edu.unh.cs.searkt.planner.suboptimal

import edu.unh.cs.searkt.MetronomeConfigurationException
import edu.unh.cs.searkt.MetronomeException
import edu.unh.cs.searkt.environment.*
import edu.unh.cs.searkt.experiment.configuration.ExperimentConfiguration
import edu.unh.cs.searkt.experiment.terminationCheckers.TerminationChecker
import edu.unh.cs.searkt.planner.Planners
import edu.unh.cs.searkt.planner.classical.OfflinePlanner
import edu.unh.cs.searkt.planner.exception.GoalNotReachableException
import edu.unh.cs.searkt.util.AbstractAdvancedPriorityQueue
import edu.unh.cs.searkt.util.Indexable
import edu.unh.cs.searkt.util.SearchQueueElement
import java.util.HashMap
import kotlin.Comparator

class SubPotential<StateType : State<StateType>>(val domain: Domain<StateType>, val configuration: ExperimentConfiguration) : OfflinePlanner<StateType>() {

    enum class SearchPhase { ASTAR, POTENTIAL, BEES }

    private var phase: SearchPhase = SearchPhase.ASTAR

    private val weight: Double = configuration.weight
            ?: throw MetronomeConfigurationException("Weight for suboptimal potential is not defined.")

    private var fMin: Double = 0.0
    private var heuristicErrorSum: Double = 0.0
    private var samplesTaken: Double = 11.0
    private val heuristicError: Double
        get() = heuristicErrorSum / samplesTaken

    private val algorithmName = configuration.algorithmName

    var terminationChecker: TerminationChecker? = null

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
            get() = heuristic + heuristicError
        override val dHat: Double
            get() = heuristic

        val fHat: Double
            get() = g + hHat

        val fPrime: Double
            get() = g + (weight * h)

        override fun setIndex(key: Int, index: Int) {
            indexMap[key] = index
        }

        override fun getIndex(key: Int): Int {
            return indexMap[key]
        }

        val potential: Double
            get() = ((weight * fMin) - g ) / h

        override var index: Int = -1

        override val f: Double
            get() = cost + heuristic

        @Suppress("UNCHECKED_CAST")
        override fun equals(other: Any?): Boolean {
            return try {
                val otherCast = other as Node<*>
                otherCast.state == this.state
            } catch (exp: ClassCastException) {
                false
            }
        }

        override fun hashCode(): Int = state.hashCode()

        override fun toString(): String =
                "Node: [State: $state h: $heuristic, g: $cost, actionCost: $actionCost, parent: ${parent?.state}, open: $open ]"
    }

    private val fValueComparator = Comparator<Node<StateType>> { lhs, rhs ->
        when {
            lhs.fPrime < rhs.fPrime -> -1
            lhs.fPrime > rhs.fPrime -> 1
            lhs.cost > rhs.cost -> -1 // Tie breaking on cost (g)
            lhs.cost < rhs.cost -> 1
            else -> 0
        }
    }

    private val fHatComparator = Comparator<Node<StateType>> { lhs, rhs ->
        when {
            lhs.fHat < rhs.fHat -> -1
            lhs.fHat > rhs.fHat -> 1
            lhs.cost > rhs.cost -> -1
            lhs.cost < rhs.cost -> 1
            else -> 0
        }
    }

    private val potentialComparator = Comparator<Node<StateType>> { lhs, rhs ->
        when {
            lhs.potential > rhs.potential -> -1
            lhs.potential < rhs.potential -> 1

            lhs.f < rhs.f -> -1
            lhs.f > rhs.f -> 1

            lhs.h < rhs.h -> -1
            lhs.h > rhs.h -> 1

            else -> 0
        }
    }

    private val comparator = fValueComparator

    private val nodes: HashMap<StateType, Node<StateType>> = HashMap(1000000, 1.toFloat())

    inner class OpenList : AbstractAdvancedPriorityQueue<Node<StateType>>(arrayOfNulls(1000000), comparator) {
        override fun getIndex(item: Node<StateType>): Int = item.getIndex(0)
        override fun setIndex(item: Node<StateType>, index: Int) = item.setIndex(0, index)
    }

    private val openList = OpenList()

    inner class FTrackHeap : AbstractAdvancedPriorityQueue<Node<StateType>>(arrayOfNulls(1000000), fHatComparator) {
        override fun getIndex(item: Node<StateType>): Int = item.getIndex(1)
        override fun setIndex(item: Node<StateType>, index: Int) = item.setIndex(1, index)
    }

    private val fTrackHeap = FTrackHeap()

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
        val successors = domain.successors(sourceNode.state)
        val bestSuccessor = successors.minBy { domain.heuristic(it.state) }
        if (bestSuccessor != null) {
            heuristicErrorSum += sourceNode.h - domain.heuristic(bestSuccessor.state)
            samplesTaken++
        }
        for (successor in successors) {
//            openList.forEach { println("${it.state} | f: ${it.f} | h: ${it.h} | g: ${it.g}") }
//            println("---")
            val successorState = successor.state
            val successorNode = getNode(sourceNode, successor)

            // skip if we have our parent as a successor
            if (successorState == sourceNode.parent?.state) {
                continue
            }

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
                if (isDuplicate && !successorNode.open) {
                    if (algorithmName == Planners.WEIGHTED_A_STAR) {
                        openList.add(successorNode)
                        fTrackHeap.add(successorNode)
                    }
                } else if (isDuplicate && successorNode.open) {
                    openList.update(successorNode)
                    fTrackHeap.update(successorNode)
                } else {
                    openList.add(successorNode)
                    fTrackHeap.add(successorNode)
                }
            }
        }
    }

    override fun plan(state: StateType, terminationChecker: TerminationChecker): List<Action> {
        this.terminationChecker = terminationChecker
        val node = Node(state, domain.heuristic(state), 0.0, 0.0, NoOperationAction)
        var currentNode: Node<StateType>
        nodes[state] = node
        openList.add(node)
        fTrackHeap.add(node)
        generatedNodeCount++

        while (openList.isNotEmpty() && !terminationChecker.reachedTermination()) {
            // check if limit reached reheapify on potential
            // then use potential search until we find a goal
            // then swap the other heap to track f instead of fHat
            if (expandedNodeCount > 11 && phase == SearchPhase.ASTAR) {
                if (fTrackHeap.peek()!!.fHat <= (weight * openList.peek()!!.f)) {
                    phase = SearchPhase.POTENTIAL
                    openList.reorder(potentialComparator)
                    fTrackHeap.reorder(fValueComparator)
                }
            }

            val topNode = openList.peek() ?: throw GoalNotReachableException("Open list is empty")
            if (domain.isGoal(topNode.state)) {
                return extractPlan(topNode, state)
            }
            currentNode = openList.pop() ?: throw GoalNotReachableException("Open list is empty")
            fTrackHeap.remove(currentNode)
            expandFromNode(currentNode)
            currentNode.isClosed = true
            expandedNodeCount++

            fMin = when (phase) {
                SearchPhase.ASTAR -> openList.peek()!!.f
                SearchPhase.POTENTIAL -> fTrackHeap.peek()!!.f
                SearchPhase.BEES -> fTrackHeap.peek()!!.f
            }

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