package edu.unh.cs.ai.realtimesearch.planner.realtime

import edu.unh.cs.ai.realtimesearch.environment.Domain
import edu.unh.cs.ai.realtimesearch.environment.State
import edu.unh.cs.ai.realtimesearch.environment.SuccessorBundle
import edu.unh.cs.ai.realtimesearch.experiment.configuration.Configurations
import edu.unh.cs.ai.realtimesearch.experiment.configuration.GeneralExperimentConfiguration
import edu.unh.cs.ai.realtimesearch.experiment.configuration.InvalidFieldException
import edu.unh.cs.ai.realtimesearch.experiment.terminationCheckers.InsufficientTerminationCriterionException
import edu.unh.cs.ai.realtimesearch.experiment.terminationCheckers.TerminationChecker
import edu.unh.cs.ai.realtimesearch.logging.debug
import edu.unh.cs.ai.realtimesearch.planner.RealTimePlanner
import edu.unh.cs.ai.realtimesearch.util.resize
import org.slf4j.LoggerFactory
import java.util.*

class RealTimeAStarPlanner<StateType : State<StateType>>(domain: Domain<StateType>, var configuration: GeneralExperimentConfiguration) : RealTimePlanner<StateType>(domain) {
    private val depthLimit: Long = configuration.getTypedValue(Configurations.LOOKAHEAD_DEPTH_LIMIT.toString()) ?: throw InvalidFieldException("\"${Configurations.LOOKAHEAD_DEPTH_LIMIT}\" is not found. Please add it to the experiment configuration.")

    data class SuccessorHeuristicPair<out StateType : State<StateType>>(val successorBundle: SuccessorBundle<StateType>, val heuristicLookahead: Double)
    data class MiniminNode<StateType : State<StateType>>(val state: StateType, val cost: Double, val depth: Int)
    data class HeuristicNode<StateType : State<StateType>>(var heuristic: Double, var interation: Int)

    val logger = LoggerFactory.getLogger(RealTimeAStarPlanner::class.java)

    private val heuristicTable: HashMap<StateType, Double> = HashMap<StateType, Double>(100000000, 1F).resize()

    val openList: Queue<MiniminNode<StateType>> = ArrayDeque(1000000)
    //    val closedList: MutableSet<StateType> = HashSet<StateType>(1000000, 1.5F).resize()

    override fun selectAction(sourceState: StateType, terminationChecker: TerminationChecker): List<ActionBundle> {
        val successors = domain.successors(sourceState)
        val unsortedSuccessors = evaluateSuccessors(successors, terminationChecker)

        var bestSuccessor: SuccessorHeuristicPair<StateType>? = null
        var secondBestSuccessor: SuccessorHeuristicPair<StateType>? = null

        for (successor in unsortedSuccessors) {
            if (bestSuccessor == null) {
                bestSuccessor = successor
            } else if (bestSuccessor.successorBundle.actionCost + bestSuccessor.heuristicLookahead >= successor.successorBundle.actionCost + successor.heuristicLookahead) {
                secondBestSuccessor = bestSuccessor
                bestSuccessor = successor
            }
        }

        val action = when {
            bestSuccessor == null -> throw RuntimeException("Cannot expand a sourceState with no successors.")
            secondBestSuccessor == null -> {
                // Only one action is available
                heuristicTable[sourceState] = bestSuccessor.heuristicLookahead + bestSuccessor.successorBundle.actionCost
                ActionBundle(bestSuccessor.successorBundle.action, bestSuccessor.successorBundle.actionCost)
            }
            else -> {
                // Save the second best action's f value
                heuristicTable[sourceState] = secondBestSuccessor.heuristicLookahead + secondBestSuccessor.successorBundle.actionCost
                // Use the best action
                ActionBundle(bestSuccessor.successorBundle.action, bestSuccessor.successorBundle.actionCost)
            }
        }

        logger.debug { "Selected action: $action" }
        return Collections.singletonList(action)
    }

    private fun evaluateSuccessors(successors: List<SuccessorBundle<StateType>>, terminationChecker: TerminationChecker): List<SuccessorHeuristicPair<StateType>> {
        var successorHeuristicPairs = heuristicLookahead(successors, 0, terminationChecker) ?: throw InsufficientTerminationCriterionException("Not enough time to calculate the successor f values.")

        for (depth in 1..depthLimit.toInt()) {
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

        // Initialize
        var bestAvailable = Double.POSITIVE_INFINITY
        openList.clear()
        //        closedList.clear()

        openList.add(MiniminNode(sourceState, 0.0, depthLimit))

        while (openList.isNotEmpty()) {
            val miniminNode = openList.remove()
            //            closedList.add(miniminNode.state)
            expandedNodeCount++

            if (terminationChecker.reachedTermination()) {
                return null
            }

            for (successor in domain.successors(miniminNode.state)) {
                //                if (successor.state in closedList) {
                //                    continue // Skip the already visited items
                //                }

                val successorCost = successor.actionCost + domain.heuristic(successor.state) + miniminNode.cost

                if (domain.isGoal(successor.state)) {
                    // Korf: If the goal state is encountered before the search horizon,
                    // then the path is terminated and a heuristic value of zero is returned [...].
                    return miniminNode.cost + successor.actionCost
                }

                if (successorCost > bestAvailable) {
                    continue // Prune if the current cost is higher then the best available
                } else if (miniminNode.depth == 0) {
                    bestAvailable = Math.min(bestAvailable, successorCost) // Leaf level reached
                } else {
                    openList.add(MiniminNode(successor.state, miniminNode.cost + successor.actionCost, miniminNode.depth - 1))
                    generatedNodeCount++
                }
            }
        }

        return bestAvailable
    }

}