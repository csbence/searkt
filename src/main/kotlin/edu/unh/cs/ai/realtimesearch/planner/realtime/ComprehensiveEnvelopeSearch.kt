package edu.unh.cs.ai.realtimesearch.planner.realtime

import edu.unh.cs.ai.realtimesearch.environment.Action
import edu.unh.cs.ai.realtimesearch.environment.Domain
import edu.unh.cs.ai.realtimesearch.environment.State
import edu.unh.cs.ai.realtimesearch.experiment.configuration.ExperimentConfiguration
import edu.unh.cs.ai.realtimesearch.experiment.result.ExperimentResult
import edu.unh.cs.ai.realtimesearch.experiment.terminationCheckers.TerminationChecker
import edu.unh.cs.ai.realtimesearch.logging.debug
import edu.unh.cs.ai.realtimesearch.planner.RealTimePlanner
import edu.unh.cs.ai.realtimesearch.planner.SearchEdge
import edu.unh.cs.ai.realtimesearch.planner.SearchNode
import edu.unh.cs.ai.realtimesearch.planner.exception.GoalNotReachableException
import edu.unh.cs.ai.realtimesearch.util.AdvancedPriorityQueue
import edu.unh.cs.ai.realtimesearch.util.Indexable
import edu.unh.cs.ai.realtimesearch.visualizer
import org.slf4j.LoggerFactory
import java.lang.Double.min
import java.util.*
import kotlin.system.measureTimeMillis

/**
 * Examines the full state space as time allows, propagating back learned heuristic throughout all generated state
 * values through the closed list. Maintains the frontier of to-be-examined nodes between iterations
 * Action Plan is composed of simply the nodes with the best heuristic value adjacent to the current agent state
 * @author Kevin C. Gall
 * @date 3/17/18
 */
class ComprehensiveEnvelopeSearch<StateType : State<StateType>>(
        val domain: Domain<StateType>,
        val configuration: ExperimentConfiguration) : RealTimePlanner<StateType>() {
    //Configuration parameters
    private val expansionRatio: Double = configuration.backlogRatio ?: 1.0
    //Logger and timekeeping
    private val logger = LoggerFactory.getLogger(ComprehensiveEnvelopeSearch::class.java)
    var dijkstraTimer = 0L
    var expansionTimer = 0L


    class Node<StateType : State<StateType>>(override val state: StateType, override var heuristic: Double) : SearchNode<StateType, Node<StateType>>, Indexable {

        // We need this for the visualizer - this should be resolved
        override var cost: Long
            get() = TODO("not implemented") //To change initializer of created properties use File | Settings | File Templates.
            set(value) {}
        override var actionCost: Long
            get() = TODO("not implemented") //To change initializer of created properties use File | Settings | File Templates.
            set(value) {}
        override var action: Action
            get() = TODO("not implemented") //To change initializer of created properties use File | Settings | File Templates.
            set(value) {}
        override var parent: Node<StateType>
            get() = TODO("not implemented") //To change initializer of created properties use File | Settings | File Templates.
            set(value) {}
        override val predecessors: MutableList<SearchEdge<Node<StateType>>>
            get() = TODO("not implemented") //To change initializer of created properties use File | Settings | File Templates.

        override var index: Int = -1

        val ancestors = HashMap<StateType, DiEdge<StateType>>()
        val successors = HashMap<StateType, DiEdge<StateType>>()

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
            fun <StateType : State<StateType>> createEdge(source: Node<StateType>, destination: Node<StateType>, actionCost: Long, action: Action) {
                val edge = DiEdge(source, destination, actionCost, action)

                source.successors[destination.state] = edge
                destination.ancestors[source.state] = edge
            }
        }
    }

    //Directed edge class - describes relationship from node to node
    data class DiEdge<StateType : State<StateType>>(val source: Node<StateType>, val destination: Node<StateType>,
                                                    val actionCost: Long, val action: Action) {
        fun getCost(): Double {
            return destination.heuristic + actionCost
        }
    }

    //initialization and persistent state
    private var lastExpansionCount: Long = 0
    private var iterationCount = 0
    private var backupCount = 0
    private var foundGoal = false

    private var currentAgentState: StateType? = null

    //Closed List (also includes nodes on the frontier)
    private val closed = HashMap<StateType, Node<StateType>>(100000000, 1.toFloat())

    //Frontier (open list) and its comparator
    private val greedyHComparator = Comparator<Node<StateType>> { lhs, rhs ->
        when {
            lhs.heuristic < rhs.heuristic -> -1
            lhs.heuristic > rhs.heuristic -> 1
            else -> 0
        }
    }
    private val frontier = AdvancedPriorityQueue(1000000, greedyHComparator)

    //Backlog Queue and Backward Frontier
    //Sorted on fuzzy-f: h value plus estimate of G value from current state
    private val fuzzyFComparator = Comparator<Node<StateType>> { lhs, rhs ->
        //using heuristic for "fuzzy g value"
        //Our nodes aren't tracking distance from agent state as actions are committed, so must use estimate
        val lhsFuzzyG = domain.heuristic(currentAgentState!!, lhs.state)
        val rhsFuzzyG = domain.heuristic(currentAgentState!!, rhs.state)
        val lhsFuzzyF = lhs.heuristic + lhsFuzzyG
        val rhsFuzzyF = rhs.heuristic + rhsFuzzyG

        //break ties on lower H -> this is better info!
        when {
            lhsFuzzyF < rhsFuzzyF -> -1
            lhsFuzzyF > rhsFuzzyF -> 1
            lhs.heuristic < rhs.heuristic -> -1
            rhs.heuristic > lhs.heuristic -> 1
            else -> 0
        }
    }
    //Will need to be reordered before every learning phase
    private val backlogQueue = AdvancedPriorityQueue(1000000, fuzzyFComparator)
    private val backwardFrontier = AdvancedPriorityQueue(1000000, fuzzyFComparator)

    // Visualizer
    private val expandedNodes = mutableListOf<Node<StateType>>()

    override fun init() {
        dijkstraTimer = 0
        expansionTimer = 0
        foundGoal = false
        iterationCount = 0
        lastExpansionCount = 0
        backupCount = 0
        frontier.clear()
        closed.clear()
        backlogQueue.clear()
    }

    override fun appendPlannerSpecificResults(results: ExperimentResult) {
        results.backupCount = this.backupCount
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
     *         <ol>
     *             <li>Pre Goal: Greedy Best First from envelope</li>
     *             <li>Post Goal: Backward A* from goal to current agent</li>
     *         </ol>
     *     </li>
     *     <li>
     *         Movement (Action Commitment)<br/>
     *         Always set to commit 1 action
     *     </li>
     * </ul>
     */
    override fun selectAction(sourceState: StateType, terminationChecker: TerminationChecker): List<ActionBundle> {
        logger.debug { "Selecting action with source state:\n $sourceState" }

        if (iterationCount == 0) {
            frontier.add(Node(sourceState, domain.heuristic(sourceState)))
            closed[sourceState] = frontier.peek()!!
        }

        currentAgentState = sourceState
        val thisNode = closed[sourceState]
        if (thisNode !== null) {
            thisNode.visitCount++
        } else {
            throw GoalNotReachableException("Current state unexamined. The planner is confused!")
        }

        dijkstraTimer += measureTimeMillis { dijkstra(terminationChecker) }

        expansionTimer += measureTimeMillis {
            if (!thisNode.expanded && !thisNode.onGoalPath) {
                expandFrontierNode(terminationChecker, thisNode)
            }

            if (!foundGoal) {
                foundGoal = expandFrontier(terminationChecker)
            }

            if (foundGoal) {
                backwardAStar(terminationChecker)
            }
        }

        iterationCount++

        logger.debug {
            """
            |*****Status after Iteration $iterationCount*****
            |Timers:
            |   Dijkstra - $dijkstraTimer
            |   Expansion - $expansionTimer
            |
            |Frontier Size: ${frontier.size}
            |Backlog Queue Size: ${backlogQueue.size}
            """.trimMargin()
        }



        visualizer?.updateSearchEnvelope(expandedNodes)
        visualizer?.updateAgentLocation(thisNode)
        visualizer?.delay()

        return moveAgent(sourceState)
    }

    //Learning Phase
    private fun dijkstra(terminationChecker: TerminationChecker) {
        val limit = (expansionRatio * lastExpansionCount).toLong()

        logger.debug { "Learning Phase: Limit $limit" }

        //resort min queues
        //with more analysis and work, this may be informed by techniques from D* Lite
        backlogQueue.reorder(fuzzyFComparator)

        for (i in 1..limit) {
            //break checks
            if (terminationChecker.reachedTermination()) {
                logger.debug { "Learning phase could not complete before termination" }
                break
            }

            if (backlogQueue.isEmpty()) {
                logger.debug { "Reached the end of the backlog queue" }
                break
            }

            val nextNode = backlogQueue.pop()!! //assert not null: we already checked for empty queue

            updateNodeHeuristic(nextNode)
        }
    }

    private fun updateNodeHeuristic(node: Node<StateType>) {
        //sanity error check
        if (node.onGoalPath) {
            throw IllegalArgumentException("Error in program: goal path node should not have heuristic updated")
        }

        var bestH = Double.POSITIVE_INFINITY
        var bestNode: Node<StateType>? = null
        for (successorMapEntry in node.successors) {
            val successorEdge = successorMapEntry.value
            val successorNode = successorEdge.destination

            //checking successor node h + the cost to get to that successor
            val tempBestH = bestH
            bestH = min(bestH, successorNode.heuristic + successorEdge.actionCost)

            if (tempBestH != bestH) bestNode = successorNode
        }

        //if  bestH is greater than h, this means it's more accurate
        if (node.heuristic < bestH && bestNode != null) {
            node.heuristic = bestH

            //add ancestors to backlog for examination
            addAncestorsToBacklog(node)
        }

        //counting backup whether or not the heuristic was updated
        backupCount++
    }

    /**
     *  Add previously expanded ancestors to backlog queue. Note that if an ancestor is on the
     *  goal path, we ignore it: it is part of the backwards A*, so should not be touched in backups
     */
    private fun addAncestorsToBacklog(node: Node<StateType>) {
        node.ancestors.values.forEach {
            if (!it.source.onGoalPath && !backlogQueue.contains(it.source)) {
                backlogQueue.add(it.source)
            }
        }
    }

    /**
     * Expansion Phase<br/>
     * Similar to A* except taking only h into account, not g (or f)
     * If the goal is found, this phase ends immediately and algorithm proceeds to new stage: backward search
     * @return whether or not the goal was found
     */
    private fun expandFrontier(terminationChecker: TerminationChecker): Boolean {
        logger.debug { "Expansion Phase" }

        //reset in prep for next learning phase
        lastExpansionCount = 0

        while (!terminationChecker.reachedTermination()) {
            val nextNode = frontier.pop() ?: throw GoalNotReachableException("No reachable path to goal")
            expandedNodes.add(nextNode)
            val expandedGoal = expandFrontierNode(terminationChecker, nextNode)

            if (expandedGoal) return true

            lastExpansionCount++
        }
        return false
    }

    /**
     * @return Whether or not the goal node was just expanded
     */
    private fun expandFrontierNode(terminationChecker: TerminationChecker, frontierNode: Node<StateType>): Boolean {
        if (domain.isGoal(frontierNode.state)) {
            frontierNode.onGoalPath = true

            //We'll abandon frontier now in favor of backward frontier
            //clear it to reset indices
            frontier.clear()
            backwardFrontier.add(frontierNode)

            return true
        } else {
            val successors = domain.successors(frontierNode.state)

            var bestNode: Node<StateType> = frontierNode
            var bestH: Double = Double.POSITIVE_INFINITY
            successors.forEach {
                val successorNode: Node<StateType>?
                if (!closed.containsKey(it.state)) {
                    successorNode = Node(it.state, domain.heuristic(it.state))
                    closed[it.state] = successorNode

                    if (!foundGoal) frontier.add(successorNode)
                    generatedNodeCount++
                } else {
                    successorNode = closed[it.state]
                    successorNode ?: throw NullPointerException("Closed list has State Key which points to Null")
                }

                Node.createEdge(frontierNode, successorNode, it.actionCost, it.action)

                val tempH = successorNode.heuristic + it.actionCost
                if (tempH < bestH) {
                    bestH = tempH
                    bestNode = successorNode
                }
            }
            if (bestH > frontierNode.heuristic) {
                frontierNode.heuristic = bestH

                addAncestorsToBacklog(frontierNode)
            }

            frontierNode.expanded = true
            expandedNodeCount++
            terminationChecker.notifyExpansion()
            return false
        }
    }

    /**
     * When goal is found, initiate backward search from goal to agent's current state
     */
    private fun backwardAStar(terminationChecker: TerminationChecker) {
        //sanity error check
        if (!foundGoal) {
            throw IllegalStateException("Cannot engage backward A* if goal has not been found")
        }

        //resort frontier
        backwardFrontier.reorder(fuzzyFComparator)

        var goalPathNode = backwardFrontier.pop()

        while (!terminationChecker.reachedTermination()) {
            if (goalPathNode == null) {
                logger.debug { "Goal path propagation complete! No more nodes to update" }
                return
            }

            expandGoalPathNode(terminationChecker, goalPathNode)

            goalPathNode = if (!terminationChecker.reachedTermination()) backwardFrontier.pop() else null
        }
    }

    private fun expandGoalPathNode(terminationChecker: TerminationChecker, goalPathNode: Node<StateType>) {
        val ancestors = domain.predecessors(goalPathNode.state)

        ancestors.forEach {
            var ancestorNode: Node<StateType>?

            if (closed.containsKey(it.state)) {
                ancestorNode = closed[it.state]
                ancestorNode ?: throw NullPointerException("Closed list has State Key which points to Null")

                if (!ancestorNode.onGoalPath && backlogQueue.contains(ancestorNode)) {
                    backlogQueue.remove(ancestorNode)
                    ancestorNode.index = -1
                }
            } else {
                ancestorNode = Node(it.state, 0.0)
                closed[it.state] = ancestorNode

                generatedNodeCount++
            }

            //Add edge now
            Node.createEdge(ancestorNode, goalPathNode, it.actionCost, it.action)

            //we only want to update nodes that are not already on the goal path. If already on goal path, that means
            //its heuristic is already perfect
            if (ancestorNode.onGoalPath) return@forEach

            //We are able to set the exact heuristic value because this is backward search
            ancestorNode.heuristic = goalPathNode.heuristic + it.actionCost
            ancestorNode.onGoalPath = true

            backwardFrontier.add(ancestorNode)
        }

        expandedNodeCount++
        terminationChecker.notifyExpansion()
    }

    //Agent moves to the next state with the lowest cost (action cost + h), breaking ties on h.
    //If on the goal path, follows diminishing h values
    private fun moveAgent(sourceState: StateType): List<ActionBundle> {
        logger.debug { "Movement Phase" }

        //only commit 1 at a time
        val actionList = ArrayList<ActionBundle>()

        var currentNode = closed[sourceState]
                ?: throw NullPointerException("Closed list has State Key which points to Null")

        //hard coding limit of 1 for now. May change later, so keeping it in loop
        val actionPlanLimit = 1
        for (i in 1..actionPlanLimit) {
            if (domain.isGoal(currentNode.state)) break

            //break when currentNode is on the frontier - we haven't examined past it yet!
            if (currentNode.successors.size == 0) break

            val pathEdge = currentNode.successors.values.reduce { minSoFar, next ->
                val lhsCost = minSoFar.getCost()
                val rhsCost = next.getCost()

                when {
                    minSoFar.destination.onGoalPath && !next.destination.onGoalPath -> minSoFar
                    next.destination.onGoalPath && !minSoFar.destination.onGoalPath -> next
                    lhsCost < rhsCost -> minSoFar
                    lhsCost > rhsCost -> next
                    minSoFar.destination.heuristic < next.destination.heuristic -> minSoFar
                    minSoFar.destination.heuristic > next.destination.heuristic -> next
                    else -> minSoFar
                }
            }

            actionList.add(ActionBundle(pathEdge.action, pathEdge.actionCost))

            //update the heuristic of the node we just left right now. This is so we don't get caught in local minima
            //Only need to update if not on goal path. Goal path info should be perfect
            if (!currentNode.onGoalPath) updateNodeHeuristic(currentNode)

            currentNode = pathEdge.destination
        }

        if (actionList.size == 0) {
            throw GoalNotReachableException("Agent has reached a dead end")
        }

        return actionList
    }

    enum class ComprehensiveConfigurations(private val configurationName: String) {
        BACKLOG_RATIO("backlogRatio");

        override fun toString() = configurationName
    }
}