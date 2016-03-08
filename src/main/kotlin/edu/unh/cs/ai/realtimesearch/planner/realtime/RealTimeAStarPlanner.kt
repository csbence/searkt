package edu.unh.cs.ai.realtimesearch.planner.realtime

import edu.unh.cs.ai.realtimesearch.environment.Action
import edu.unh.cs.ai.realtimesearch.environment.Domain
import edu.unh.cs.ai.realtimesearch.environment.State
import edu.unh.cs.ai.realtimesearch.environment.SuccessorBundle
import edu.unh.cs.ai.realtimesearch.experiment.TerminationChecker
import edu.unh.cs.ai.realtimesearch.experiment.terminationCheckers.InsufficientTerminationCriterionException
import edu.unh.cs.ai.realtimesearch.logging.debug
import edu.unh.cs.ai.realtimesearch.planner.RealTimePlanner
import org.slf4j.LoggerFactory
import java.util.*

class RealTimeAStarPlanner<StateType : State<StateType>>(domain: Domain<StateType>, val depthLimit: Int) : RealTimePlanner<StateType>(domain) {

    data class SuccessorHeuristicPair<out StateType : State<out StateType>>(val successorBundle: SuccessorBundle<StateType>, val heuristicLookahead: Double)

    val logger = LoggerFactory.getLogger(RealTimeAStarPlanner::class.java)

    private val heuristicTable: MutableMap<StateType, Double> = hashMapOf()

    override fun selectAction(state: StateType, terminationChecker: TerminationChecker): List<Action> {
        val successors = domain.successors(state)
        val sortedSuccessors = evaluateSuccessors(successors, terminationChecker).sortedBy { it.successorBundle.actionCost + it.heuristicLookahead }

        val action = if (sortedSuccessors.size == 1) {
            // Only one action is available
            val successorHeuristicPair = sortedSuccessors[0]
            heuristicTable[state] = successorHeuristicPair.heuristicLookahead + successorHeuristicPair.successorBundle.actionCost
            successorHeuristicPair.successorBundle.action
        } else if (sortedSuccessors.size >= 2) {
            // Save the second best action's f value
            val successorHeuristicPair = sortedSuccessors[1]
            heuristicTable[state] = successorHeuristicPair.heuristicLookahead + successorHeuristicPair.successorBundle.actionCost
            // Use the best action
            sortedSuccessors[0].successorBundle.action
        } else {
            throw RuntimeException("Cannot expand a state with no successors.")
        }

        logger.debug { "Selected action: $action" }
        return Collections.singletonList(action)
    }

    private fun evaluateSuccessors(successors: List<SuccessorBundle<StateType>>, terminationChecker: TerminationChecker): List<SuccessorHeuristicPair<StateType>> {
        var successorHeuristicPairs = heuristicLookahead(successors, 0, terminationChecker) ?: throw InsufficientTerminationCriterionException("Not enough time to calculate the successor f values.")

        for (depth in 1..depthLimit) {
            successorHeuristicPairs = heuristicLookahead(successors, depth, terminationChecker) ?: return successorHeuristicPairs
        }

        return successorHeuristicPairs
    }

    /**
     * Time bounded heuristic lookahead.
     *
     * Uses IDA* to augment the heuristic value of the given node.
     */
    private fun heuristicLookahead(successors: List<SuccessorBundle<StateType>>, depth: Int, terminationChecker: TerminationChecker): List<SuccessorHeuristicPair<StateType>>? {
        val successorHeuristicPairs = arrayListOf<SuccessorHeuristicPair<StateType>>()

        for (successor in successors) {
            val heuristicLookahead = heuristicLookahead(successor, depth, terminationChecker) ?: return null
            successorHeuristicPairs.add(SuccessorHeuristicPair(successor, heuristicLookahead))
        }

        return successorHeuristicPairs
    }

    /**
     * Depth limited heuristic lookahead.
     */
    private fun heuristicLookahead(successor: SuccessorBundle<StateType>, depthLimit: Int, terminationChecker: TerminationChecker): Double? {
        if (terminationChecker.reachedTermination()) {
            return null
        }

        return heuristicTable[successor.state] ?: alphaPruningLookahead(successor.state, depthLimit, terminationChecker)
    }

    /**
     * Minimin lookahead search from a given node. The lookahead search uses depth limited A* to find the most promising
     * state on the search frontier. Alpha pruning is used to reduce the number of extended states.
     *
     * @return Best backed up heuristic value on the horizon, or the goal's backed up value if found.
     */
    private fun alphaPruningLookahead(sourceState: StateType, depthLimit: Int, terminationChecker: TerminationChecker): Double? {
        if (domain.isGoal(sourceState)) {
            return 0.0
        }

        data class MiniminNode(val state: StateType, val cost: Double, val depth: Int)

        var bestAvailable = Double.POSITIVE_INFINITY

        val openList: Queue<MiniminNode> = ArrayDeque()
        openList.add(MiniminNode(sourceState, 0.0, depthLimit))

        while (openList.isNotEmpty()) {
            val miniminNode = openList.remove()
            expandedNodeCount++

            if (terminationChecker.reachedTermination()) {
                return null
            }

            for (successor in domain.successors(miniminNode.state)) {
                val successorCost = successor.actionCost + domain.heuristic(successor.state) + miniminNode.cost

                if (domain.isGoal(successor.state)) {
                    // Korf: If the goal state is encountered before the search horizon,
                    // then the path is terminated and a heuristic value of zero is returned [...].
                    return miniminNode.cost + successor.actionCost
                }

                if (successorCost > bestAvailable) {
                    // Prune if the current cost is higher then the best available
                    continue
                } else if (miniminNode.depth == 0) {
                    // Leaf level reached
                    bestAvailable = Math.min(bestAvailable, successorCost)
                } else {
                    openList.add(MiniminNode(successor.state, miniminNode.cost + successor.actionCost, miniminNode.depth - 1))
                    generatedNodeCount++
                }
            }
        }

        return bestAvailable
    }

}