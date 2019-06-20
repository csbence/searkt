package edu.unh.cs.searkt.planner.suboptimal

import edu.unh.cs.searkt.MetronomeConfigurationException
import edu.unh.cs.searkt.MetronomeException
import edu.unh.cs.searkt.environment.*
import edu.unh.cs.searkt.experiment.configuration.ExperimentConfiguration
import edu.unh.cs.searkt.experiment.terminationCheckers.TerminationChecker
import edu.unh.cs.searkt.planner.Planners
import edu.unh.cs.searkt.planner.classical.OfflinePlanner
import edu.unh.cs.searkt.planner.exception.GoalNotReachableException
import edu.unh.cs.searkt.util.AdvancedPriorityQueue
import edu.unh.cs.searkt.util.Indexable
import edu.unh.cs.searkt.util.SearchQueueElement
import java.util.HashMap
import kotlin.Comparator
import kotlin.math.sqrt

class WeightedAStar<StateType : State<StateType>>(val domain: Domain<StateType>, val configuration: ExperimentConfiguration) : OfflinePlanner<StateType>() {
    private val weight: Double = configuration.weight
            ?: throw MetronomeConfigurationException("Weight for weighted A* is not specified.")

    private val algorithmName = configuration.algorithmName

    private val optimisticWeight: Double = weight // (2.0 * weight) - 1.0

    var terminationChecker: TerminationChecker? = null

    inner class Node<StateType : State<StateType>>(val state: StateType, var heuristic: Double, var cost: Double,
                                             var actionCost: Double, var action: Action,
                                             override var parent: Node<StateType>? = null) :
            Indexable, SearchQueueElement<Node<StateType>> {
        var isClosed = false
        private val indexMap = Array(1) { -1 }
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

        val fPrime: Double
            get() = g + (weight * h)

        override fun setIndex(key: Int, index: Int) {
            indexMap[key] = index
        }

        override fun getIndex(key: Int): Int {
            return indexMap[key]
        }

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

        val xdp: Double
            get() = (1 / (2 * optimisticWeight)) * ((((2 * optimisticWeight) - 1) *
                    h) + sqrt(Math.pow(g - h, 2.0) + (4 * optimisticWeight * h * g)))

        val xup: Double
            get() = (1 / (2 * optimisticWeight)) * (g + h + sqrt(Math.pow(g + h, 2.0) +
                    ((4 * optimisticWeight) * (optimisticWeight - 1) * Math.pow(h, 2.0))))

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

    private val xdpComparator = Comparator<Node<StateType>> { lhs, rhs ->
        when {
            lhs.xdp < rhs.xdp -> -1
            lhs.xdp > rhs.xdp -> 1
            lhs.g > rhs.g -> -1 // tie breaking on cost
            lhs.g < rhs.g -> 1
            else -> 0
        }
    }


    private val xupComparator = Comparator<Node<StateType>> { lhs, rhs ->
        when {
            lhs.xup < rhs.xup -> -1
            lhs.xup > rhs.xup -> 1
            lhs.g > rhs.g -> -1 // tie breaking on cost
            lhs.g < rhs.g -> 1
            else -> 0
        }
    }

    private val comparator = when (algorithmName) {
        Planners.WEIGHTED_A_STAR_XDP -> xdpComparator
        Planners.WEIGHTED_A_STAR_XUP -> xupComparator
        Planners.WEIGHTED_A_STAR -> fValueComparator
        Planners.WEIGHTED_A_STAR_DD -> fValueComparator
        else -> throw MetronomeException("Unrecognized algorithm name for Weighted A*")
    }

    private val nodes: HashMap<StateType, Node<StateType>> = HashMap(1000000, 1.toFloat())

    private var openList = AdvancedPriorityQueue(1000000, comparator)

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
                    }
                } else if (isDuplicate && successorNode.open) {
                    openList.update(successorNode)
                } else {
                    openList.add(successorNode)
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
        generatedNodeCount++

        while (openList.isNotEmpty() && !terminationChecker.reachedTermination()) {
            val topNode = openList.peek() ?: throw GoalNotReachableException("Open list is empty")
            if (domain.isGoal(topNode.state)) {
                return extractPlan(topNode, state)
            }
            currentNode = openList.pop() ?: throw GoalNotReachableException("Open list is empty")
            expandFromNode(currentNode)
            currentNode.isClosed = true
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