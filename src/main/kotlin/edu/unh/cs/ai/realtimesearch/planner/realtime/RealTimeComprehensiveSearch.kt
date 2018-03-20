package edu.unh.cs.ai.realtimesearch.planner.realtime

import edu.unh.cs.ai.realtimesearch.environment.*
import edu.unh.cs.ai.realtimesearch.experiment.terminationCheckers.TerminationChecker
import edu.unh.cs.ai.realtimesearch.planner.RealTimePlanner
import edu.unh.cs.ai.realtimesearch.planner.exception.GoalNotReachableException
import edu.unh.cs.ai.realtimesearch.util.AdvancedPriorityQueue
import edu.unh.cs.ai.realtimesearch.util.Indexable
import org.slf4j.LoggerFactory
import java.lang.Double.min
import java.lang.Long.max
import java.util.*
import kotlin.system.measureTimeMillis

/**
 * Real time heuristic planner. Examines the full state space as time allows, propagating back learned heuristic
 * values through the closed list. Maintains the frontier of to-be-examined nodes between iterations
 * Action Plan is composed of simply the nodes with the best heuristic value adjacent to the current agent state
 * @author Kevin C. Gall
 * @date 3/17/18
 */
class RealTimeComprehensiveSearch<StateType: State<StateType>>(val domain: Domain<StateType>) : RealTimePlanner<StateType>(){
    //Configuration parameters
    private val expansionRatio : Double = 25.0 //HARD CODED FOR TESTING
    private val goalPathToBacklogRatio : Double = 0.2 //HARD CODED FOR TESTING
    //Logger and timekeeping
    private val logger = LoggerFactory.getLogger(RealTimeComprehensiveSearch::class.java)
    var dijkstraTimer = 0L
    var expansionTimer = 0L


    class Node<StateType: State<StateType>> (val state: StateType, var heuristic: Double) : Indexable {
        override var index: Int = -1

        //add to ancestors
        val ancestors = ArrayList<DiEdge<StateType>>()
        //will add to successors when node is expanded
        val successors = ArrayList<DiEdge<StateType>>()

        var onGoalPath = false
        var expanded = false
        var visitCount = 0

        override fun hashCode(): Int = state.hashCode()
        override fun equals(other: Any?): Boolean {
            return when {
                other == null -> false
                other !is Node<*> -> false
                other.state == state -> true
                else -> false
            }
        }

        companion object EdgeFactory {
            fun <StateType : State<StateType>> createEdge(source : Node<StateType>, destination : Node<StateType>, actionCost : Long, action : Action) {
                val edge = DiEdge(source, destination, actionCost, action)

                source.successors.add(edge)
                destination.ancestors.add(edge)
            }
        }
    }

    //Directed edge class - describes relationship from node to node
    data class DiEdge<StateType : State<StateType>> (val source : Node<StateType>, val destination : Node<StateType>,
                                                val actionCost : Long, val action: Action) {
        fun getCost() : Double {
            return destination.heuristic + actionCost
        }
    }

    //initialization and persistent state
    private var lastExpansionCount : Long = 0
    private var iterationCount = 0
    private var foundGoal = false

    private var currentAgentState : StateType? = null

    //Closed List (also includes nodes on the frontier)
    private val closed = HashMap<StateType, Node<StateType>>(100000000, 1.toFloat())

    //Frontier (open list) and its comparator
    private val frontierComparator = Comparator<Node<StateType>> { lhs, rhs ->
        when {
            lhs.heuristic < rhs.heuristic -> -1
            lhs.heuristic > rhs.heuristic -> 1
            else -> 0
        }
    }
    private val frontier = AdvancedPriorityQueue(1000000, frontierComparator)

    //Backlog Queue
    //Sorted on fuzzy-f: h value plus estimate of G value from current state
    private val backlogComparator = Comparator<Node<StateType>> { lhs, rhs ->
        //using heuristic for "fuzzy g value"
        //Our nodes aren't tracking distance from agent state as actions are committed, so must use estimate
        val lhsFuzzyF = lhs.heuristic + domain.heuristic(currentAgentState!!, lhs.state)
        val rhsFuzzyF = rhs.heuristic + domain.heuristic(currentAgentState!!, rhs.state)

        //break ties on lower h
        when {
            lhsFuzzyF < rhsFuzzyF -> -1
            lhsFuzzyF > rhsFuzzyF -> 1
            lhs.heuristic < rhs.heuristic -> -1
            lhs.heuristic > rhs.heuristic -> 1
            lhs.onGoalPath -> 1
            rhs.onGoalPath -> -1
            else -> 0
        }
    }
    //Will need to be reordered before every learning phase
    private val backlogQueue = AdvancedPriorityQueue(1000000, backlogComparator)
    private val goalPathQueue = AdvancedPriorityQueue(1000000, backlogComparator)

    override fun init() {
        dijkstraTimer = 0
        expansionTimer = 0
        foundGoal = false
        iterationCount = 0
        lastExpansionCount = 0
        frontier.clear()
        closed.clear()
        backlogQueue.clear()
    }

    /**
     * Core Function of planner. Splits into 3 phases:
     * <ul>
     *     <li>
     *         Learning (Backward propagation)<br/>
     *         Bounded by previous iteration's expansions * config ratio
     *     </li>
     *     <li>
     *         Exploration (Expansion)<br/>
     *         Bounded by termination checker
     *     </li>
     *     <li>
     *         Movement (Action Commitment)<br/>
     *         Bounded by configuration setting
     *     </li>
     * </ul>
     */
    override fun selectAction(sourceState: StateType, terminationChecker: TerminationChecker): List<ActionBundle> {
        logger.debug("""
            |Selecting action with source state:
            |$sourceState
            """.trimMargin())
        if (iterationCount == 0) {
            frontier.add(Node(sourceState, domain.heuristic(sourceState)))
            closed.put(sourceState, frontier.peek()!!)
        }

        currentAgentState = sourceState
        val thisNode = closed[sourceState]
        if (thisNode !== null) {
            thisNode.visitCount++
        } else {
            throw GoalNotReachableException("Current state unexamined. The planner is confused!")
        }

        dijkstraTimer += measureTimeMillis { dijkstra(terminationChecker) }

        expansionTimer += measureTimeMillis { foundGoal = expandFrontier(terminationChecker, thisNode) }

        iterationCount++

        logger.debug("""
            |*****Status after Iteration $iterationCount*****
            |Timers:
            |   Dijkstra - $dijkstraTimer
            |   Expansion - $expansionTimer
            |
            |Frontier Size: ${frontier.size}
            |Backlog Queue Size: ${backlogQueue.size}
            """.trimMargin())
        return moveAgent(sourceState)
    }

    //Learning Phase
    private fun dijkstra(terminationChecker: TerminationChecker) {
        val limit = (expansionRatio * lastExpansionCount).toLong()
//        var goalPathLimit = 0L
//        var backlogLimit = limit
//
//        if (foundGoal) {
//            goalPathLimit = (limit * goalPathToBacklogRatio).toLong()
//            backlogLimit = limit - goalPathLimit
//
//            if (backlogLimit < backlogQueue.size) {
//                backlogLimit = backlogQueue.size.toLong()
//                goalPathLimit = limit - backlogLimit
//            }
//        }

        logger.debug("Learning Phase: Limit $limit")
        //resort min queues
        backlogQueue.reorder(backlogComparator)
        //goalPathQueue.reorder(backlogComparator)

//        for (i in 1..goalPathLimit) {
//            if (terminationChecker.reachedTermination()) {
//                logger.debug("Learning phase could not complete before termination")
//                break
//            }
//
//            if (goalPathQueue.isEmpty()) {
//                logger.debug("Reached end of goal path queue")
//                backlogLimit += (goalPathLimit - i) //give backlog the stolen time
//                break
//            }
//
//            val goalNode = goalPathQueue.pop()!!
//            updateGoalPathAncestors(goalNode)
//        }

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

            val nextNode = backlogQueue.pop()!! //assert not null: we already checked for empty queue

            if (domain.isGoal(nextNode.state)) {
                addAncestorsToBacklog(nextNode)
            } else {
                updateNodeHeuristic(nextNode)
            }
        }
    }

//    private fun updateGoalPathAncestors(goalPathNode : Node<StateType>) {
//        if (goalPathNode.ancestors.size == 0) return
//
//        goalPathNode.ancestors.forEach {
//            if (!it.source.onGoalPath) {
//                val source = it.source
//                source.heuristic = goalPathNode.heuristic + it.actionCost
//                source.onGoalPath = true
//                if (backlogQueue.contains(source)) backlogQueue.remove(source)
//
//                goalPathQueue.add(source) //creating a backward frontier
//            }
//        }
//    }

    private fun updateNodeHeuristic(node : Node<StateType>) {
        var bestH = Double.POSITIVE_INFINITY
        var bestNode : Node<StateType>? = null
        for (successorEdge in node.successors) {
            val successorNode = successorEdge.destination

            //checking successor node h + the cost to get to that successor
            val tempBestH = bestH
            bestH = min(bestH, successorNode.heuristic + successorEdge.actionCost)

            if (tempBestH != bestH) bestNode = successorNode
        }

        //if  bestH is greater than h, this means it's more accurate
        if (node.heuristic < bestH && bestNode != null) {
            node.heuristic = bestH

            //if node is not on goal, add ancestors to backlog for examination
            addAncestorsToBacklog(node)
        }
    }

    private fun addAncestorsToBacklog(node : Node<StateType>) {
        node.ancestors.forEach {
            if (node.onGoalPath) it.source.onGoalPath = true

            if (!backlogQueue.contains(it.source)) {
                backlogQueue.add(it.source)
            }
        }
    }

    /**
     * Expansion Phase<br/>
     * Similar to A* except taking only h into account, not g (or f)
     * If the goal is found, it is added to the backlog queue, and expansion ends immediately
     * @return whether or not the goal was found
     */
    private fun expandFrontier(terminationChecker: TerminationChecker, sourceNode : Node<StateType>) : Boolean {
        logger.debug("Expansion Phase")

        //reset in prep for next learning phase
        val tempLastExpansionCount = lastExpansionCount
        lastExpansionCount = 0

        var nextNode : Node<StateType>? = if (!sourceNode.expanded) {
            frontier.remove(sourceNode)
            sourceNode
        } else frontier.pop()

        while (!terminationChecker.reachedTermination()) {
            if (nextNode === null) {
                if (foundGoal) {
                    lastExpansionCount = max(lastExpansionCount, tempLastExpansionCount)
                    return true
                } else {
                    throw GoalNotReachableException("No reachable path to goal")
                }
            } else {
                val expandedGoal = expandFrontierNode(nextNode)
                foundGoal = foundGoal || expandedGoal

                terminationChecker.notifyExpansion()
                nextNode.expanded = true
            }

            lastExpansionCount++
            nextNode = if (!terminationChecker.reachedTermination()) frontier.pop() else null
        }

        return foundGoal
    }

    private fun expandFrontierNode(frontierNode : Node<StateType>) : Boolean {
        val isGoal = domain.isGoal(frontierNode.state)
        if (isGoal) {
            frontierNode.onGoalPath = true

            //reorder frontier on fuzzy f. Now that we've found the goal, we want to expand nodes around the agent first
            frontier.reorder(backlogComparator)
        } else {
            val successors = domain.successors(frontierNode.state)

            var bestNode : Node<StateType> = frontierNode
            var bestH : Double = Double.POSITIVE_INFINITY
            successors.forEach {
                val successorNode: Node<StateType>?
                if (!closed.containsKey(it.state)) {
                    successorNode = Node(it.state, domain.heuristic(it.state))
                    closed[it.state] = successorNode

                    frontier.add(successorNode)
                    generatedNodeCount++
                } else {
                    successorNode = closed[it.state]
                }

                //null check
                successorNode ?: throw NullPointerException("Closed list has State Key which points to Null")

                Node.createEdge(frontierNode, successorNode, it.actionCost, it.action)

                val tempH = successorNode.heuristic + it.actionCost
                if (tempH < bestH) {
                    bestH = tempH
                    bestNode = successorNode
                }
            }
            if (bestH > frontierNode.heuristic) {
                frontierNode.heuristic = bestH

                if (bestNode.onGoalPath) {
                    frontierNode.onGoalPath = true
                    addAncestorsToBacklog(frontierNode)
                }
            }
        }

        expandedNodeCount++
        return isGoal
    }

    //Agent moves to the next state with the lowest cost (action cost + h), breaking ties on h
    private fun moveAgent(sourceState : StateType) : List<ActionBundle> {
        logger.debug("Movement Phase")

        //only commit 1 at a time
        val actionList = ArrayList<ActionBundle>()

        var currentNode = closed[sourceState] ?:
            throw NullPointerException("Closed list has State Key which points to Null")

        //hard coding limit of 1 for now. May change later, so keeping it in loop
        val actionPlanLimit = 1
        for (i in 1..actionPlanLimit) {
            if (domain.isGoal(currentNode.state)) break

            //break when currentNode is on the frontier - we haven't examined past it yet!
            if (currentNode.successors.size == 0) break

            val pathEdge= currentNode.successors.reduce { minSoFar, next ->
                val lhsCost = minSoFar.getCost()
                val rhsCost = next.getCost()

                when {
                    minSoFar.destination.onGoalPath && !next.destination.onGoalPath -> minSoFar
                    next.destination.onGoalPath && !minSoFar.destination.onGoalPath -> next
                    lhsCost < rhsCost -> minSoFar
                    lhsCost > rhsCost -> next
                    minSoFar.destination.heuristic < next.destination.heuristic -> minSoFar
                    minSoFar.destination.heuristic > next.destination.heuristic -> next
                    minSoFar.destination.visitCount < next.destination.visitCount -> minSoFar
                    minSoFar.destination.visitCount > next.destination.visitCount -> next
                    else -> minSoFar
                }
            }

            actionList.add(ActionBundle(pathEdge.action, pathEdge.actionCost))

            //update the heuristic of the node we just left right now. This is so we don't get caught in local minima
            updateNodeHeuristic(currentNode)

            currentNode = pathEdge.destination
        }

        if (actionList.size == 0) {
            throw GoalNotReachableException("Agent has reached a dead end")
        }

        return actionList
    }
}