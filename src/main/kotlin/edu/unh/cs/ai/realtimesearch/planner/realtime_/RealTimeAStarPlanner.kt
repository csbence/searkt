package edu.unh.cs.ai.realtimesearch.planner.realtime_

import edu.unh.cs.ai.realtimesearch.environment.Action
import edu.unh.cs.ai.realtimesearch.environment.Domain
import edu.unh.cs.ai.realtimesearch.environment.State
import edu.unh.cs.ai.realtimesearch.environment.SuccessorBundle
import edu.unh.cs.ai.realtimesearch.experiment.TerminationChecker
import edu.unh.cs.ai.realtimesearch.planner.RealTimePlanner
import org.slf4j.LoggerFactory
import java.util.*

/**
 * @author Bence Cserna (bence@cserna.net)
 */
class RealTimeAStarPlanner<StateType : State<StateType>>(domain: Domain<StateType>) : RealTimePlanner<StateType>(domain) {

    val logger = LoggerFactory.getLogger(RealTimeAStarPlanner::class.java)

    private val heuristicTable: MutableMap<StateType, Double> = hashMapOf()

    override fun selectAction(state: StateType, terminationChecker: TerminationChecker): Action {

        val successors = domain.successors(state)
        val sortedSuccessors = successors
                .map { successor -> successor to heuristicLookahead(successor) }
                .sortedBy { it.first.actionCost + it.second }

        val action = if (sortedSuccessors.size == 1) {
            val successorHeuristicPair = sortedSuccessors[0]
            heuristicTable[state] = successorHeuristicPair.second
            successorHeuristicPair.first.action
        } else if (sortedSuccessors.size >= 2) {
            // Save the second best actions heuristic value
            heuristicTable[state] = sortedSuccessors[1].second
            // Use the best action
            sortedSuccessors[0].first.action
        } else {
            throw RuntimeException("Cannot expand a state with no successors.")
        }

        logger.debug("Selected action: $action")
        return action
    }

    private fun heuristicLookahead(successor: SuccessorBundle<StateType>): Double {
        return heuristicTable[successor.state] ?: alphaPruningLookahead(successor.state, 3)
    }

    private fun alphaPruningLookahead(state: StateType, depth: Int): Double {
        data class MiniminNode(val state: StateType, val cost: Double, val depth: Int)

        var bestAvailable = Double.POSITIVE_INFINITY

        val openList: Queue<MiniminNode> = ArrayDeque()
        openList.add(MiniminNode(state, 0.0, depth))

        while (openList.isNotEmpty()) {
            val miniminNode = openList.remove()

            for (successor in domain.successors(miniminNode.state)) {
                val successorCost = successor.actionCost + domain.heuristic(successor.state) + miniminNode.cost

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
