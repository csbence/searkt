package edu.unh.cs.ai.realtimesearch.planner.suboptimal

import edu.unh.cs.ai.realtimesearch.MetronomeConfigurationException
import edu.unh.cs.ai.realtimesearch.MetronomeException
import edu.unh.cs.ai.realtimesearch.environment.*
import edu.unh.cs.ai.realtimesearch.experiment.configuration.ExperimentConfiguration
import edu.unh.cs.ai.realtimesearch.experiment.terminationCheckers.TerminationChecker
import edu.unh.cs.ai.realtimesearch.planner.classical.ClassicalPlanner
import edu.unh.cs.ai.realtimesearch.planner.exception.GoalNotReachableException
import edu.unh.cs.ai.realtimesearch.util.AdvancedPriorityQueue
import edu.unh.cs.ai.realtimesearch.util.Indexable
import org.slf4j.LoggerFactory
import java.util.HashMap
import kotlin.Comparator

class TentacleSearch<StateType : State<StateType>>(val domain: Domain<StateType>, val configuration: ExperimentConfiguration) : ClassicalPlanner<StateType>() {
    private val weight: Double = configuration.weight
            ?: throw MetronomeConfigurationException("Weight for Tentacle Search is not specified.")

    var terminationChecker: TerminationChecker? = null
    var tentacleDelay: Int = 100
    var tentacleExpansions: Int = 0
    var aStarExpansions: Int = 0

    class Node<StateType : State<StateType>>(val state: StateType, var heuristic: Double, var cost: Double,
                                             var actionCost: Double, var action: Action,
                                             var parent: TentacleSearch.Node<StateType>? = null,
                                             var d: Double, val depth: Int = 0) :
            Indexable {

//        private var singleStepErrorDistance: Double = 0.0
//        private var singleStepErrorHeuristic: Double = 0.0
//        var heuristicHat: Double = 0.0

//        init {
//            if (parent != null) {
//                singleStepErrorHeuristic = parent!!.singleStepErrorHeuristic + ((actionCost + heuristic) - parent!!.heuristic)
//                singleStepErrorDistance = parent!!.singleStepErrorDistance + ((1.0 + d) - parent!!.d)
//            }

//            heuristicHat = computeHeuristicHat()
//        }

//        private fun computeHeuristicHat(): Double {
//            var hHat = Double.MAX_VALUE
//            val singleStepErrorMean = if (cost == 0.0) singleStepErrorHeuristic else singleStepErrorHeuristic / depth
//            val distanceMean = if (cost == 0.0) singleStepErrorDistance else singleStepErrorDistance / depth
//            if (distanceMean < 1.0) hHat = heuristic + ((d / (1 - distanceMean)) * singleStepErrorMean)
//            return hHat
//        }

        override var index: Int = -1

        val f: Double
            get() = cost + heuristic
//        val fHat: Double
//            get() = cost + heuristicHat

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
    private val logger = LoggerFactory.getLogger(TentacleSearch::class.java)

    private val fValueComparator = Comparator<TentacleSearch.Node<StateType>> { lhs, rhs ->
        when {
            lhs.f < rhs.f -> -1
            lhs.f > rhs.f -> 1
            lhs.cost > rhs.cost -> -1 // Tie breaking on cost (g)
            lhs.cost < rhs.cost -> 1
            else -> 0
        }
    }

    private val dValueComparator = Comparator<TentacleSearch.Node<StateType>> { lhs, rhs ->
        when {
            lhs.d < rhs.d -> -1
            lhs.d > rhs.d -> 1
            lhs.cost > rhs.cost -> -1
            lhs.cost < rhs.cost -> 1
            else -> 0
        }

    }

    private val nodes: HashMap<StateType, TentacleSearch.Node<StateType>> = HashMap(1000000, 1.toFloat())
    private val tentacleNodes: HashMap<StateType, TentacleSearch.Node<StateType>> = HashMap(1000000, 1.toFloat())

    private val openList = AdvancedPriorityQueue(1000000, fValueComparator)
    private val focalList = AdvancedPriorityQueue(1000000, dValueComparator)

    private fun getNode(sourceNode: Node<StateType>, successorBundle: SuccessorBundle<StateType>,
                        nodeLookup: HashMap<StateType, TentacleSearch.Node<StateType>> = nodes): Node<StateType> {
        val successorState = successorBundle.state
        val tempSuccessorNode = nodeLookup[successorState]
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
            nodeLookup[successorState] = undiscoveredNode
            undiscoveredNode
        } else {
            tempSuccessorNode
        }
    }

    private fun expandNode(sourceNode: Node<StateType>) {
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

    private fun expandTentacleNode(sourceNode: Node<StateType>) {
        val currentGValue = sourceNode.cost
        for (successor in domain.successors(sourceNode.state)) {
            val successorState = successor.state
            val successorNode = getNode(sourceNode, successor, tentacleNodes)

            // skip if we have our parent as a successor
            if (successorState == sourceNode.parent?.state) {
                continue
            }

            // only generate states which have not been visited or with a cheaper cost
            val successorGValueFromCurrent = currentGValue + successor.actionCost
            if (successorNode.cost > successorGValueFromCurrent) {
                successorNode.apply {
                    cost = successorGValueFromCurrent
                    parent = sourceNode
                    action = successor.action
                    actionCost = successor.actionCost
                }
                if (!successorNode.open) {
                    focalList.add(successorNode)
                } else {
                    focalList.update(successorNode)
                }
            }
        }
    }

    // bound = w*fmin
    // expected = gmin + e*hmin
    // do astar until expected <= bound
    // then tentacle from fmin node until f(dmin) > w*fmin

    override fun plan(state: StateType, terminationChecker: TerminationChecker): List<Action> {
        this.terminationChecker = terminationChecker
        val rootNode = Node(state, domain.heuristic(state), 0.0, 0.0, NoOperationAction, d = domain.distance(state))
        var currentNode: Node<StateType>
        val startTime = System.nanoTime()
        nodes[state] = rootNode
        openList.add(rootNode)
        generatedNodeCount++

        logger.debug("Starting Tentacle Search with A* expansions")
        while (openList.isNotEmpty() && !terminationChecker.reachedTermination()) {
            val topNode = openList.peek() ?: throw GoalNotReachableException("Open list is empty")

            if (domain.isGoal(topNode.state)) {
                return extractPlan(topNode, state)
            }

            val suboptimalityBound = weight * topNode.f
            val perStepHeuristicError = if (topNode.depth == 0) 0.0 else (topNode.f - rootNode.f) / topNode.depth
            val expectedTotalCost = topNode.cost + topNode.heuristic + perStepHeuristicError * topNode.d

            tentacleDelay--
            // && tentacleDelay == 0
            if (expectedTotalCost * 1.05 <= suboptimalityBound && tentacleDelay == 0) {
                val tentacleGoalNode = performTentacle(topNode, suboptimalityBound)
                logger.debug("Returning to A* expansions")

                if (tentacleGoalNode != null) {
                    return extractPlan(tentacleGoalNode, state)
                }

                tentacleDelay = 100
            }

            currentNode = openList.pop() ?: throw GoalNotReachableException("Open list is empty")
            expandNode(currentNode)
            expandedNodeCount++
            aStarExpansions++
        }
        if (terminationChecker.reachedTermination()) {
            throw MetronomeException("Reached termination condition, " +
                    "${terminationChecker.remaining() + 1} / ${terminationChecker.elapsed() - 1} remaining!")
        }
        throw GoalNotReachableException()
    }

    private fun performTentacle(tentacleRoot: Node<StateType>, bound: Double): Node<StateType>? {
        focalList.clear()
        tentacleNodes.clear()
        focalList.add(tentacleRoot)
        logger.debug("Expanding tentacle...")
        while (focalList.isNotEmpty()) {
            val topNode = focalList.peek() ?: throw GoalNotReachableException("Tentacle list is empty")

            if (domain.isGoal(topNode.state)) {
                return topNode
            }

            if (topNode.f > bound) {
                return null
            }

            focalList.pop() ?: throw GoalNotReachableException("Tentacle list is empty")
            expandTentacleNode(topNode)
            tentacleExpansions++
            expandedNodeCount++
        }

        return null
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
