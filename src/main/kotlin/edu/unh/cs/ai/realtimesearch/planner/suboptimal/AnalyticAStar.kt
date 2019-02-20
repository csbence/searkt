package edu.unh.cs.ai.realtimesearch.planner.suboptimal

import edu.unh.cs.ai.realtimesearch.MetronomeConfigurationException
import edu.unh.cs.ai.realtimesearch.MetronomeException
import edu.unh.cs.ai.realtimesearch.environment.*
import edu.unh.cs.ai.realtimesearch.experiment.configuration.ExperimentConfiguration
import edu.unh.cs.ai.realtimesearch.experiment.terminationCheckers.TerminationChecker
import edu.unh.cs.ai.realtimesearch.planner.classical.ClassicalPlanner
import edu.unh.cs.ai.realtimesearch.planner.exception.GoalNotReachableException
import edu.unh.cs.ai.realtimesearch.planner.suboptimal.AnalyticAStar.ExpansionPhase.OPTIMAL
import edu.unh.cs.ai.realtimesearch.planner.suboptimal.AnalyticAStar.ExpansionPhase.SUBOPTIMAL_NEW
import edu.unh.cs.ai.realtimesearch.util.AbstractAdvancedPriorityQueue
import edu.unh.cs.ai.realtimesearch.util.AdvancedPriorityQueue
import edu.unh.cs.ai.realtimesearch.util.Indexable
import edu.unh.cs.ai.realtimesearch.util.SearchQueueElement
import org.slf4j.LoggerFactory
import java.util.HashMap
import kotlin.Comparator
import kotlin.math.abs
import kotlin.math.max

class AnalyticAStar<StateType : State<StateType>>(val domain: Domain<StateType>, val configuration: ExperimentConfiguration) : ClassicalPlanner<StateType>() {
    private val weight: Double = configuration.weight
            ?: throw MetronomeConfigurationException("Weight for weighted A* is not specified.")

    var terminationChecker: TerminationChecker? = null

    class Node<StateType : State<StateType>>(val state: StateType, var heuristic: Double, var cost: Double,
                                             var actionCost: Double, var action: Action,
                                             override var parent: AnalyticAStar.Node<StateType>? = null) :
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

    @Suppress("unused")
    private val logger = LoggerFactory.getLogger(AnalyticAStar::class.java)

    private val fValueComparator = Comparator<AnalyticAStar.Node<StateType>> { lhs, rhs ->
        when {
            lhs.f < rhs.f -> -1
            lhs.f > rhs.f -> 1
            lhs.cost > rhs.cost -> -1 // Tie breaking on cost (g)
            lhs.cost < rhs.cost -> 1
            else -> 0
        }
    }

    private val hValueComparator = Comparator<AnalyticAStar.Node<StateType>> { lhs, rhs ->
        when {
            lhs.h < rhs.h -> -1
            lhs.h > rhs.h -> 1
            lhs.cost > rhs.cost -> -1 // Tie breaking on cost (g)
            lhs.cost < rhs.cost -> 1
            else -> 0
        }
    }

    inner class GreedyOpen : AbstractAdvancedPriorityQueue<Node<StateType>>(arrayOfNulls(1000000), hValueComparator) {
        override fun getIndex(item: Node<StateType>): Int = item.getIndex(0)
        override fun setIndex(item: Node<StateType>, index: Int) = item.setIndex(0, index)
    }

    private var greedyOpen = GreedyOpen()

    private val nodes: HashMap<StateType, AnalyticAStar.Node<StateType>> = HashMap(1000000, 1.toFloat())

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

    private fun expandNode(sourceNode: Node<StateType>, open: AbstractAdvancedPriorityQueue<Node<StateType>>): Node<StateType>? {
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

    override fun plan(startState: StateType, terminationChecker: TerminationChecker): List<Action> {
        this.terminationChecker = terminationChecker
        val node = Node(startState, domain.heuristic(startState), 0.0, 0.0, NoOperationAction)
        nodes[startState] = node
        openList.add(node)
        generatedNodeCount++

        var rampupDelay = 10000
        var currentBound = 0.0
        var stepErrorAvg = 0.0
        var greedyStepErrorAvg = 0.0
        iteration++

        val optimalExpansionCounts = arrayListOf<Int>()
        val suboptimalExpansionCounts = arrayListOf<Int>()
        val suboptimalMinDistance = arrayListOf<Double>()

        fun expandOptimal(): Node<StateType>? {
            var expanded = 0

            while (openList.isNotEmpty()) {
                val currentNode = openList.pop()!!

                currentNode.isClosed = true

                expandNode(currentNode, openList)?.let { return it }
                expanded++

                val hDiff = node.h - currentNode.h
                val totalError = currentNode.g - hDiff

                val stepError = if (currentNode.g == 0.0) 0.0
                else abs(totalError / currentNode.g)

                val learningRate = 0.001 + (rampupDelay + 1) / 998
                stepErrorAvg = stepErrorAvg * (1 - learningRate) + stepError * learningRate

                if (rampupDelay > 0) {
                    --rampupDelay
                    continue
                }


                val pessimisticStepError = max(stepErrorAvg, greedyStepErrorAvg)
                val costEstimate = currentNode.h * pessimisticStepError + currentNode.f
                val upperBound = weight * currentNode.f

                val reach = true // costEstimate < upperBound
                // println("reach: $reach stepError: $stepError costEstimate: $costEstimate upperBound: $upperBound g: ${currentNode.g}")

                if (upperBound == currentBound) continue


                if (reach) {
                    if (upperBound >= currentBound) greedyOpen.clear()
                    currentBound = upperBound
//                    println(expandedNodeCount)
//                    println("A*       error: $stepErrorAvg bound: $upperBound expanded: $expanded")

                    rampupDelay = 10000
                    return null
                }
            }

            throw GoalNotReachableException("Open list is empty")
        }

        fun expandSuboptimal(): Node<StateType>? {
            var minVisitedH = Double.MAX_VALUE
            var minHError = 0.0
            var expanded = 0

            while (greedyOpen.isNotEmpty()) {
                if (greedyOpen.peek()!!.f >= currentBound) {
//                    println(expandedNodeCount)
//                    println("Bound reached.  error: $greedyStepErrorAvg bound: $currentBound minVisitedH: $minVisitedH expanded: $expanded")

                    suboptimalMinDistance.add(minVisitedH)
                    return null
                }

                expanded++
                val currentNode = greedyOpen.pop()!!


                if (currentNode.isClosed || currentNode.expanded == iteration) {
                    continue
                }

                val goalNode = expandNode(currentNode, greedyOpen)
                if (goalNode != null) return goalNode

                if (currentNode.h < minVisitedH) {
                    minVisitedH = currentNode.h

                    val hDiff = node.h - currentNode.h
                    val totalError = currentNode.g - hDiff

                    val stepError = if (currentNode.g == 0.0) 0.0
                    else abs(totalError / currentNode.g)

                    greedyStepErrorAvg = stepError
//
//                    val learningRate = 0.001
//                    greedyStepErrorAvg = greedyStepErrorAvg * (1 - learningRate) + stepError * learningRate
                }

            }

            return null
        }

        fun populateGreedyQueue() {
            val topK = openList.backingArray
                    .take(openList.size)

            greedyOpen.clear()

            ++iteration
            topK.forEach { greedyOpen.add(it!!) }

            expansionPhase = SUBOPTIMAL_NEW
//            println("Populate greedy queue")
        }

        while (openList.isNotEmpty() && !terminationChecker.reachedTermination()) {
//            println("+++\n")

            var startExpansionCount = expandedNodeCount

            expandOptimal()?.let { return extractPlan(it, startState) }

            optimalExpansionCounts.add(expandedNodeCount - startExpansionCount)

            // Continue previous iteration
//            if (greedyOpen.isNotEmpty()) {
//                expandSuboptimal()?.let { return extractPlan(it, startState) }
//            }

            startExpansionCount = expandedNodeCount

            populateGreedyQueue()
            expandSuboptimal()?.let { return extractPlan(it, startState) }

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

//            println("  estimatedExpansions: $expansionBound")
            rampupDelay = max(expansionBound.toInt(), 10000)
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