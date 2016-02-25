package edu.unh.cs.ai.realtimesearch.planner.realtime_

import edu.unh.cs.ai.realtimesearch.environment.Action
import edu.unh.cs.ai.realtimesearch.environment.Domain
import edu.unh.cs.ai.realtimesearch.environment.State
import edu.unh.cs.ai.realtimesearch.environment.SuccessorBundle
import edu.unh.cs.ai.realtimesearch.experiment.TerminationChecker
import edu.unh.cs.ai.realtimesearch.experiment.terminationCheckers.InsufficientTerminationCriterionException
import edu.unh.cs.ai.realtimesearch.logging.debug
import edu.unh.cs.ai.realtimesearch.planner.realtime_.RealTimePlanner
import org.slf4j.LoggerFactory
import java.util.*

class RealTimeAStarPlanner<StateType : State<StateType>>(domain: Domain<StateType>) : RealTimePlanner<StateType>(domain) {

    data class SuccessorHeuristicPair<out StateType : State<out StateType>>(val successorBundle: SuccessorBundle<StateType>, val heuristicLookahead: Double)

    val logger = LoggerFactory.getLogger(RealTimeAStarPlanner::class.java)
    val depthLimit = 5

    private val heuristicTable: MutableMap<StateType, Double> = hashMapOf()

    override fun selectAction(state: StateType, terminationChecker: TerminationChecker): List<Action> {

        val successors = domain.successors(state)
        val sortedSuccessors = evaluateSuccessors(successors, terminationChecker).sortedBy { it.successorBundle.actionCost + it.heuristicLookahead }

        val action = if (sortedSuccessors.size == 1) {
            val successorHeuristicPair = sortedSuccessors[0]
            heuristicTable[state] = successorHeuristicPair.heuristicLookahead
            successorHeuristicPair.successorBundle.action
        } else if (sortedSuccessors.size >= 2) {
            // Save the second best actions heuristic value
            heuristicTable[state] = sortedSuccessors[1].heuristicLookahead
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

        for (depth in 0..depthLimit) {
            successorHeuristicPairs = heuristicLookahead(successors, depth, terminationChecker) ?: return successorHeuristicPairs
        }

        return successorHeuristicPairs
    }

    private fun heuristicLookahead(successors: List<SuccessorBundle<StateType>>, depth: Int, terminationChecker: TerminationChecker): List<SuccessorHeuristicPair<StateType>>? {
        val successorHeuristicPairs = arrayListOf<SuccessorHeuristicPair<StateType>>()

        for (successor in successors) {
            val heuristicLookahead = heuristicLookahead(successor, depth, terminationChecker) ?: return null
            successorHeuristicPairs.add(SuccessorHeuristicPair(successor, heuristicLookahead))
        }

        return successorHeuristicPairs
    }

    private fun heuristicLookahead(successor: SuccessorBundle<StateType>, depthLimit: Int, terminationChecker: TerminationChecker): Double? {
        if (terminationChecker.reachedTermination()) {
            return null
        }

        return heuristicTable[successor.state] ?: alphaPruningLookahead(successor.state, depthLimit, terminationChecker)
    }

    private fun alphaPruningLookahead(state: StateType, depth: Int, terminationChecker: TerminationChecker): Double? {
        data class MiniminNode(val state: StateType, val cost: Double, val depth: Int)

        var bestAvailable = Double.POSITIVE_INFINITY

        val openList: Queue<MiniminNode> = ArrayDeque()
        openList.add(MiniminNode(state, 0.0, depth))

        while (openList.isNotEmpty()) {
            val miniminNode = openList.remove()

            if (terminationChecker.reachedTermination()) {
                return null
            }

            for (successor in domain.successors(miniminNode.state)) {
                val successorCost = successor.actionCost + domain.heuristic(successor.state) + miniminNode.cost

                if (domain.isGoal(successor.state)) {
                    // Korf: If the goal state is encountered before the search horizon,
                    // then tha path is terminated and a heuristic value of zero is returned [...].
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
                }
            }
        }

        return bestAvailable
    }

}
