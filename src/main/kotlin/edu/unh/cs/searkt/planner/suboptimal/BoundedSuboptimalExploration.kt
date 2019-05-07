package edu.unh.cs.searkt.planner.suboptimal

import edu.unh.cs.searkt.MetronomeConfigurationException
import edu.unh.cs.searkt.MetronomeException
import edu.unh.cs.searkt.environment.*
import edu.unh.cs.searkt.experiment.configuration.ExperimentConfiguration
import edu.unh.cs.searkt.experiment.terminationCheckers.TerminationChecker
import edu.unh.cs.searkt.planner.classical.OfflinePlanner
import edu.unh.cs.searkt.planner.exception.GoalNotReachableException
import edu.unh.cs.searkt.planner.suboptimal.BoundedSuboptimalExploration.ExpansionPhase.OPTIMAL
import edu.unh.cs.searkt.planner.suboptimal.BoundedSuboptimalExploration.ExpansionPhase.SUBOPTIMAL_NEW
import edu.unh.cs.searkt.util.AbstractAdvancedPriorityQueue
import edu.unh.cs.searkt.util.AdvancedPriorityQueue
import edu.unh.cs.searkt.util.Indexable
import edu.unh.cs.searkt.util.SearchQueueElement
import java.util.HashMap
import kotlin.Comparator
import kotlin.math.abs
import kotlin.math.max

class BoundedSuboptimalExploration<StateType : State<StateType>>(val domain: Domain<StateType>, val configuration: ExperimentConfiguration) : OfflinePlanner<StateType>() {
    private val weight: Double = configuration.weight
            ?: throw MetronomeConfigurationException("Weight for weighted A* is not specified.")

    var terminationChecker: TerminationChecker? = null

    var aStarExpansions = 0
    var greedyExpansions = 0

    class Node<StateType : State<StateType>>(val state: StateType, var heuristic: Double, var cost: Double,
                                             var actionCost: Double, var action: Action,
                                             override var parent: Node<StateType>? = null) :
            Indexable, SearchQueueElement<Node<StateType>> {
        var isClosed = false
        var expanded = 0

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

        override fun setIndex(key: Int, index: Int) {
            indexMap[key] = index
        }

        override fun getIndex(key: Int): Int {
            return indexMap[key]
        }

        override var index: Int = -1

        override val f: Double
            get() = cost + heuristic

        var suboptimalCost: Double = 0.0

        override fun equals(other: Any?): Boolean {
            if (other != null && other is Node<*>) {
                return state == other.state
            }
            return false
        }

        override fun hashCode(): Int = state.hashCode()

        override fun toString(): String =
                "Node: [State: $state h: $heuristic, g: $cost, actionCost: $actionCost, parent: ${parent?.state}, open: $open ]"
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

    private val weightedValueComparator = Comparator<Node<StateType>> { lhs, rhs ->
        when {
            lhs.g + lhs.h * weight < rhs.g + rhs.h * weight -> -1
            lhs.g + lhs.h * weight > rhs.g + rhs.h * weight -> 1
            lhs.cost > rhs.cost -> -1 // Tie breaking on cost (g)
            lhs.cost < rhs.cost -> 1
            else -> 0
        }
    }

    private val hValueComparator = Comparator<Node<StateType>> { lhs, rhs ->
        when {
            lhs.h < rhs.h -> -1
            lhs.h > rhs.h -> 1
            lhs.cost > rhs.cost -> -1 // Tie breaking on cost (g)
            lhs.cost < rhs.cost -> 1
            else -> 0
        }
    }

    inner class GreedyOpen : AbstractAdvancedPriorityQueue<Node<StateType>>(arrayOfNulls(1000000), weightedValueComparator) {
        override fun getIndex(item: Node<StateType>): Int = item.getIndex(0)
        override fun setIndex(item: Node<StateType>, index: Int) = item.setIndex(0, index)
    }

    private var greedyOpen = GreedyOpen()

    private val nodes: HashMap<StateType, BoundedSuboptimalExploration.Node<StateType>> = HashMap(1000000, 1.toFloat())

    private var openList = AdvancedPriorityQueue(1000000, fValueComparator)

    private var iteration = 0

    private enum class ExpansionPhase {
        OPTIMAL, SUBOPTIMAL_OLD, SUBOPTIMAL_NEW
    }

    private var expansionPhase = OPTIMAL

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

    private fun expandNode(sourceNode: Node<StateType>, open: AbstractAdvancedPriorityQueue<Node<StateType>>, suboptimalExpansion: Boolean): Node<StateType>? {
        expandedNodeCount++
        if (sourceNode.expanded != 0) reexpansions++
        sourceNode.expanded = iteration

        if (domain.isGoal(sourceNode.state)) {
            return sourceNode
        }

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
            if (successorNode.cost >= successorGValueFromCurrent) {
                assert(successorNode.state == successor.state)

                if (suboptimalExpansion) {
                    successorNode.suboptimalCost = successorGValueFromCurrent
                }

                successorNode.apply {
                    cost = successorGValueFromCurrent
                    parent = sourceNode
                    action = successor.action
                    actionCost = successor.actionCost
                }

                if (successorNode.isClosed) {
                    continue
                }

                if (!open.contains(successorNode)) {
                    open.add(successorNode)
                } else {
                    open.update(successorNode)
                }
            }
        }

        return null
    }

    override fun plan(state: StateType, terminationChecker: TerminationChecker): List<Action> {
        this.terminationChecker = terminationChecker
        val node = Node(state, domain.heuristic(state), 0.0, 0.0, NoOperationAction)
        nodes[state] = node
        openList.add(node)
        generatedNodeCount++

        val maxRam = 1000
        var rampupDelay = maxRam
        var currentBound = 0.0
        var stepErrorAvg = 0.0
        var greedyStepErrorAvg = 0.0
        iteration++

        // Min value difference of the optimal search tree open list and the suboptimal search tree
        var allowanceImprovement = 0.0

        val optimalExpansionCounts = arrayListOf<Int>()
        val suboptimalExpansionCounts = arrayListOf<Int>()
        val suboptimalMinDistance = arrayListOf<Double>()


        /**
         * This is a naive implementation of the cut improvement, please consider to exploit more advanced data structures.
         */
        fun minCutImprovement(): Double? {
            return openList.backingArray
                    .take(openList.size)
                    .requireNoNulls()
                    .filter { it.suboptimalCost != 0.0 }
                    .minBy { it.suboptimalCost - it.cost }
                    ?.let { it.suboptimalCost - it.cost }
        }

        fun expandOptimal(): Node<StateType>? {
            var expanded = 0

            while (openList.isNotEmpty()) {
                val currentNode = openList.pop()!!

                currentNode.isClosed = true

                expandNode(currentNode, openList, false)?.let { return it }
                expanded++
                aStarExpansions++

                val hDiff = node.h - currentNode.h
                val totalError = currentNode.g - hDiff

                val stepError = if (currentNode.g == 0.0) 0.0
                else abs(totalError / currentNode.g)

                val learningRate = 0.001 + (rampupDelay - 1) / maxRam
                stepErrorAvg = stepErrorAvg * (1 - learningRate) + stepError * learningRate

                if (rampupDelay > 0) {
                    --rampupDelay
                    continue
                }


                val pessimisticStepError = max(stepErrorAvg, greedyStepErrorAvg)
                val costEstimate = currentNode.h * pessimisticStepError + currentNode.f // estimate
                val upperBound = weight * currentNode.f

                val reach = costEstimate < upperBound
//                 println("reach: $reach stepError: $stepError costEstimate: $costEstimate upperBound: $upperBound g: ${currentNode.g}")

                // We did not improve, there is no point to do suboptimal expansions (in the simple case)
                if (upperBound == currentBound) continue

                if (upperBound > currentBound) {
                    // We have more room to play with
                    currentBound = upperBound
                    return null
                    print("optimal expansions: $expanded ")
                }

                // todo utilize the error models to figure out if we should do some meta improvements

            }

            throw GoalNotReachableException("Open list is empty")
        }

        var minVisitedH = Double.MAX_VALUE
        var minHError = 0.0

        fun expandSuboptimal(): Node<StateType>? {
            var expanded = 0

            while (greedyOpen.isNotEmpty()) {
                if (greedyOpen.peek()!!.f >= (currentBound + allowanceImprovement)) {
//                    println(expandedNodeCount)
//                    println("Bound reached.  error: $greedyStepErrorAvg bound: $currentBound minVisitedH: $minVisitedH expanded: $expanded")

                    suboptimalMinDistance.add(minVisitedH)
                    print("suboptimal expansions: $expanded")
                    return null
                }

                expanded++
                greedyExpansions++
                val currentNode = greedyOpen.pop()!!


                if (currentNode.isClosed || currentNode.expanded == iteration) {
                    continue
                }

                val goalNode = expandNode(currentNode, greedyOpen, true)
                if (goalNode != null) {
                    suboptimalMinDistance.add(minVisitedH)
                    return goalNode
                }

                if (currentNode.h <= minVisitedH) {
                    minVisitedH = currentNode.h

                    val hDiff = node.h - currentNode.h
                    val totalError = currentNode.g - hDiff

                    val stepError = if (currentNode.g == 0.0) 0.0
                    else abs(totalError / currentNode.g)

                    greedyStepErrorAvg = stepError

//                    val learningRate = 0.001
//                    greedyStepErrorAvg = greedyStepErrorAvg * (1 - learningRate) + stepError * learningRate
                }

            }

            print("suboptimal expansions: $expanded")
            suboptimalMinDistance.add(minVisitedH)
            return null
        }

        fun populateGreedyQueue() {
            val topK = openList.backingArray
                    .take(openList.size)

            greedyOpen.clear()

            // Reset metrics
            minVisitedH = Double.MAX_VALUE
            minHError = 0.0

            ++iteration
            topK.forEach { greedyOpen.add(it!!) }

            expansionPhase = SUBOPTIMAL_NEW
//            println("Populate greedy queue")
        }

        while (openList.isNotEmpty() && !terminationChecker.reachedTermination()) {
            println("+++\n")

            var startExpansionCount = expandedNodeCount

            expansionPhase = OPTIMAL
            expandOptimal()?.let { return extractPlan(it, state) }

            allowanceImprovement = minCutImprovement() ?: 0.0

            optimalExpansionCounts.add(expandedNodeCount - startExpansionCount)

            startExpansionCount = expandedNodeCount

            if (greedyOpen.isEmpty()) {
                println("Polulate greedy open! $iteration")
                populateGreedyQueue()
            }

            expandSuboptimal()?.let { return extractPlan(it, state) }

            suboptimalExpansionCounts.add(expandedNodeCount - startExpansionCount)

            val expansionBound = (1 until suboptimalExpansionCounts.size)
                    .map { i ->
                        val distanceImprovement = suboptimalMinDistance[i - 1] - suboptimalMinDistance[i]
                        val distanceImprovementPerExpansion = distanceImprovement / optimalExpansionCounts[i]

                        val estimatedExpansions = suboptimalMinDistance[i] / distanceImprovementPerExpansion

                        estimatedExpansions
                    }
                    .filter { it != 0.0 && it != Double.POSITIVE_INFINITY }
                    .fold(0.0) { avg, current -> avg * 0.4 + current * 0.6 }

            println("  estimatedExpansions: $expansionBound bound: $currentBound allowanceImprovement: $allowanceImprovement")
            rampupDelay = max(expansionBound.toInt(), maxRam)
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