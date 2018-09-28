package edu.unh.cs.ai.realtimesearch.planner.suboptimal

import edu.unh.cs.ai.realtimesearch.MetronomeConfigurationException
import edu.unh.cs.ai.realtimesearch.MetronomeException
import edu.unh.cs.ai.realtimesearch.environment.*
import edu.unh.cs.ai.realtimesearch.experiment.configuration.ExperimentConfiguration
import edu.unh.cs.ai.realtimesearch.experiment.terminationCheckers.TerminationChecker
import edu.unh.cs.ai.realtimesearch.planner.classical.ClassicalPlanner
import edu.unh.cs.ai.realtimesearch.planner.exception.GoalNotReachableException
import edu.unh.cs.ai.realtimesearch.util.AdvancedPriorityQueue
import edu.unh.cs.ai.realtimesearch.util.ErrorEstimator
import edu.unh.cs.ai.realtimesearch.util.ErrorTraceable
import edu.unh.cs.ai.realtimesearch.util.Indexable
import java.util.*
import kotlin.Comparator
import kotlin.math.sqrt

class TildeSearch<StateType : State<StateType>>(val domain: Domain<StateType>, val configuration: ExperimentConfiguration) : ClassicalPlanner<StateType>() {
    private val weight: Double = configuration.weight
            ?: throw MetronomeConfigurationException("Weight for Tilde Search is not specified.")

    val errorEstimator = ErrorEstimator<Node<StateType>>()

    var terminationChecker: TerminationChecker? = null
    var stepsToProveBound = 0

    private val cleanupNodeComparator = Comparator<Node<StateType>> { lhs, rhs ->
        when {
            lhs.f < rhs.f -> -1
            lhs.f > rhs.f -> 1
            lhs.cost > rhs.cost -> -1
            lhs.cost < rhs.cost -> 1
            else -> 0
        }
    }

    private val openNodeComparator = Comparator<Node<StateType>> { lhs, rhs ->
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

    private val nodes: HashMap<StateType, Node<StateType>> = HashMap(100000000, 1.toFloat())

    private val openList = AdvancedPriorityQueue(1000000, openNodeComparator)

    inner class Node<StateType : State<StateType>>(val state: StateType, var heuristic: Double, var cost: Double,
                                             var actionCost: Double, var action: Action, override var d: Double,
                                             var parent: Node<StateType>? = null) :
            Indexable, ErrorTraceable{

        private val indexMap = Array(2) {-1}
        override val g: Double
            get() = cost
        override val h: Double
            get() = heuristic

        fun setIndex(key: Int, index: Int) {
            indexMap[key] = index
        }

        fun getIndex(key: Int): Int {
            return indexMap[key]
        }

        override val open: Boolean
            get() = indexMap[0] >= 0

        override val f: Double
            get() = cost + heuristic

        val depth: Double = parent?.depth?.plus(1) ?: 0.0

        private val fHat: Double
            get() = cost + hHat

        val fTilde: Double
            get() = fHat + (1.96 * sqrt(dHat * errorEstimator.varianceHeuristicError))

        var dHat = d / (1.0 - errorEstimator.meanDistanceError)
        var hHat = h + (dHat * errorEstimator.meanHeuristicError)


        override var index: Int = -1
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

    private fun calculateStatistics(sourceNode: Node<StateType>, successorNode: Node<StateType>?) {
        if (successorNode != null) {
            errorEstimator.addSample(sourceNode, successorNode)
        }
    }

    private fun expandFromNode(sourceNode: Node<StateType>) {
        val currentGValue = sourceNode.cost
        var bestChild: Node<StateType>? = null

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

            // keep track of the best child for statistics calculation
            if (bestChild != null) {
                val previousLowestError = (bestChild.h - sourceNode.h) + (bestChild.g - sourceNode.g)
                val currentError = (successorNode.h - sourceNode.h) + (successorNode.g - sourceNode.g)
                if (currentError < previousLowestError) {
                    bestChild = successorNode
                }
            } else {
                bestChild = successorNode
            }
        }
        //update the statistics using best child
        calculateStatistics(sourceNode, bestChild)
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

    private fun cleanUpOnF(goalNodeFValue: Double, terminationChecker: TerminationChecker): Node<StateType>? {
        openList.reorder(cleanupNodeComparator)
        while (openList.isNotEmpty() && !terminationChecker.reachedTermination()) {
            val topNode = openList.pop() ?: throw GoalNotReachableException("Open list is empty")
            if (domain.isGoal(topNode.state)) return topNode
            if ((weight * topNode.f) >= goalNodeFValue) {
                return null
            } else {
                expandFromNode(topNode)
            }
            stepsToProveBound++
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
        while (openList.isNotEmpty() && !terminationChecker.reachedTermination()) {
            val topNode = openList.pop() ?: throw GoalNotReachableException("Open list is empty")
            if (domain.isGoal(topNode.state)) {
                val proveSolution = cleanUpOnF(topNode.f, terminationChecker)
                executionNanoTime = System.nanoTime() - startTime
                return if (proveSolution == null) {
                    extractPlan(topNode, state)
                } else {
                    extractPlan(proveSolution, state)
                }
            }
            expandFromNode(topNode)
            expandedNodeCount++

        }
        checkTermination(terminationChecker)
        throw GoalNotReachableException()
    }
}