package edu.unh.cs.ai.realtimesearch.planner.realtime

import edu.unh.cs.ai.realtimesearch.environment.*
import edu.unh.cs.ai.realtimesearch.experiment.terminationCheckers.TerminationChecker
import edu.unh.cs.ai.realtimesearch.planner.RealTimePlanner
import edu.unh.cs.ai.realtimesearch.util.AdvancedPriorityQueue
import edu.unh.cs.ai.realtimesearch.util.Indexable
import org.slf4j.LoggerFactory
import java.lang.Double.min
import java.util.*

class RealTimeComprehensiveSearch<StateType: State<StateType>>(val domain: Domain<StateType>) : RealTimePlanner<StateType>(){
    //Configuration parameters
    private val expansionRatio : Double = 2.0 //HARD CODED FOR TESTING
    private val actionPlanLimit = 100 //HARD CODED FOR TESTING
    //Logger
    private val logger = LoggerFactory.getLogger(RealTimeComprehensiveSearch::class.java)

    class Node<StateType: State<StateType>> (val state: StateType, var h: Double, val action: Action,
                                            val actionCost: Long, ancestor: Node<StateType>) : Indexable {
        override var index: Int = -1

        //add to ancestors
        val ancestors = ArrayList<Node<StateType>>()
        //will add to successors when node is expanded
        val successors = ArrayList<Node<StateType>>()

        init { ancestors.add(ancestor) }

        override fun hashCode(): Int = state.hashCode()
        override fun equals(other: Any?): Boolean {
            return when {
                other == null -> false
                other !is Node<*> -> false
                other.state == state -> true
                else -> false
            }
        }
    }

    private var lastExpansionCount : Long = 0

    //Closed List
    private val closed = HashMap<StateType, Node<StateType>>(100000000, 1.toFloat())

    //Frontier (open list) and its comparator
    private val frontierComparator = Comparator<Node<StateType>> { lhs, rhs ->
        when {
            lhs.h < rhs.h -> -1
            lhs.h > rhs.h -> 1
            else -> 0
        }
    }
    private val frontier = AdvancedPriorityQueue(1000000, frontierComparator)

    //Backlog Queue
    //Sorted on fuzzy-f: h value plus estimate of G value from current state
    private val backlogComparator = Comparator<Node<StateType>> { lhs, rhs ->
        //using heuristic for "fuzzy g value"
        //Our nodes aren't tracking distance from agent state as actions are committed, so must use estimate
        val lhsFuzzyF = lhs.h + domain.heuristic(lhs.state)
        val rhsFuzzyF = rhs.h + domain.heuristic(rhs.state)

        //break ties on lower h
        when {
            lhsFuzzyF < rhsFuzzyF -> -1
            lhsFuzzyF > rhsFuzzyF -> 1
            lhs.h < rhs.h -> -1
            lhs.h > rhs.h -> 1
            else -> 0
        }
    }
    //Will need to be reordered before every learning phase
    private val backlogQueue = AdvancedPriorityQueue(1000000, backlogComparator)

    /**
     * Core Function of planner. Splits into 3 phases:
     * <ul>
     *     <li>Exploration (Expansion)</li>
     *     <li>Learning (Backward propagation)</li>
     *     <li>Movement (Action Commitment)</li>
     * </ul>
     * Expansion is bounded by termination checker, learning is bounded by expansions * config ratio
     */
    override fun selectAction(sourceState: StateType, terminationChecker: TerminationChecker): List<ActionBundle> {

        while (!terminationChecker.reachedTermination()) {
            println("Test select action")
            terminationChecker.notifyExpansion()
        }
        println("action selection finished!")

        val succ : List<SuccessorBundle<StateType>> = domain.successors(sourceState)

        var actionList = ArrayList<ActionBundle>()

        for (successor in succ) {
            actionList.add(ActionBundle(NoOperationAction, 0))
        }

        return actionList
    }

    //Learning Phase
    private fun propagateHeuristic(terminationChecker: TerminationChecker) {
        val limit = (expansionRatio * lastExpansionCount).toLong()

        //resort backlog min queue
        backlogQueue.reorder(backlogComparator)

        for (i in 1..limit) {
            //break checks
            if (terminationChecker.reachedTermination()) {
                logger.debug("Learning phase could not complete before termination")
                break
            }

            if (backlogQueue.isEmpty()) {
                logger.debug("Reached the end of the backlog queue")
                break
            }

            val nextNode: Node<StateType> = backlogQueue.peek()!! //assert not null: we already checked for empty queue

            var bestH = Double.POSITIVE_INFINITY
            for (successor in nextNode.successors) {
                //checking against successor h + the cost to get to that successor
                bestH = min(bestH, successor.h + successor.actionCost)
            }
        }

    }
}