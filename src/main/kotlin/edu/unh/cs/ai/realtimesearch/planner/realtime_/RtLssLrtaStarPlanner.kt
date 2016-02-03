package edu.unh.cs.ai.realtimesearch.planner.realtime_

import edu.unh.cs.ai.realtimesearch.environment.Action
import edu.unh.cs.ai.realtimesearch.environment.Domain
import edu.unh.cs.ai.realtimesearch.environment.State
import edu.unh.cs.ai.realtimesearch.experiment.TerminationChecker
import edu.unh.cs.ai.realtimesearch.experiment.measureInt
import edu.unh.cs.ai.realtimesearch.logging.*
import edu.unh.cs.ai.realtimesearch.planner.RealTimePlanner
import org.slf4j.LoggerFactory
import java.util.*
import kotlin.Double.Companion.POSITIVE_INFINITY

/**
 * Local Search Space Learning Real Time Search A*, a type of RTS planner.
 *
 * Runs A* until out of resources, then selects action up till the most promising state.
 * While executing that plan, it will:
 * - update all the heuristic values along the path (dijkstra)
 * - Run A* from the expected destination state
 *
 * This loop continue until the goal has been found
 */
class RtLssLrtaStarPlanner<StateType : State<StateType>>(domain: Domain<StateType>) : RealTimePlanner<StateType>(domain) {
    data class StateCostPair<out StateType : State<out StateType>>(val state: StateType, val actionCost: Double)

    private val logger = LoggerFactory.getLogger(LssLrtaStarPlanner::class.java)

    private val fValueComparator = compareBy<StateType> { heuristicTable.getOrPut(it, { domain.heuristic(it) }) + costTable.getOrPut(it, { POSITIVE_INFINITY }) }
    private val heuristicComparator = compareBy<StateType> { heuristicTable.getOrPut(it, { domain.heuristic(it) }) }

    // cached h and g values
    private val heuristicTable: MutableMap<StateType, Double> = hashMapOf()
    private val costTable: MutableMap<StateType, Double> = hashMapOf()
    private val treePointers: MutableMap<StateType, Pair<StateType, Action>> = hashMapOf()
    private val closedList: MutableSet<StateType> = hashSetOf()

    private val predecessors: MutableMap<StateType, MutableList<StateCostPair<StateType>>> = hashMapOf()

    // LSS stores heuristic values. Use those, but initialize them according to the domain heuristic
    // The cost values are initialized to infinity
    private var openList = PriorityQueue<StateType>(fValueComparator)

    // for fast lookup we maintain a set in parallel
    private val openSet = hashSetOf<StateType>()

    private var rootState: StateType? = null

    /**
     * Prepares LSS for a completely unrelated new search. Sets mode to init
     * When a new action is selected, all members that persist during selection action phase are cleared
     */
    override public fun reset() {
        super.reset()

        heuristicTable.clear()
        treePointers.clear()
        resetSearch()

        // Ready to start new search!
        rootState = null
        resetSearch()
    }

    /**
     * Clears closed list and initiates open list with current root state
     */
    private fun resetSearch() {
        logger.info { "New Search..." }

        costTable.clear()
        clearOpenList()
        closedList.clear()

        // change openList ordering back to f value
        reorderOpenListBy(fValueComparator)

        //        addToOpenList(rootState!!)
        //        costTable.put(rootState!!, 0.0)
    }

    /**
     * Selects a action given current state.
     *
     * LSS_LRTA* will generate a full plan to some frontier, and stick to that plan. So the action returned will
     * always be the first on in the current plan.
     *
     * LSS-LRTAStar will plan to a specific frontier, and continue
     * to plan from there. This planning abides a termination criteria, meaning that it plans under constraints
     *
     * @param state is the current state
     * @param terminationChecker is the constraint
     * @return a current action
     */
    override fun selectAction(state: StateType, terminationChecker: TerminationChecker): List<Action> {
        // Initiate for the first search

        if (rootState == null) {
            rootState = state
        } else if (state != rootState) {
            // The given state should be the last target
            logger.error { "Inconsistent world state. Expected $rootState got $state" }
        }

        // TODO check whether the given state is goal or not

        logger.info { "Selecting action in state $state, but considering rootState $rootState" }
        // Every turn learn then A* until time expires

        if (closedList.isNotEmpty()) {
            dijkstra(terminationChecker)
        }

        val targetState = aStar(state, terminationChecker)
        val plan = extractPlan(targetState, state)
        rootState = targetState

        return plan
    }

    /**
     * Runs AStar until termination and returns the path to the head of openList
     * Will just repeatedly expand according to A*.
     */
    private fun aStar(state: StateType, terminationChecker: TerminationChecker): StateType {
        // actual core steps of A*, building the tree
        initializeAStar()

        costTable.put(state, 0.0)
        var currentState = state
        addToOpenList(state)
        logger.debug { "Starting A* from state: $state" }

        val expandedNodes = measureInt({ expandedNodes }) {
            while (!terminationChecker.reachedTermination() && !domain.isGoal(currentState)) {
                currentState = popOpenList()
                expandFromNode(currentState)
            }
        }

        if (expandedNodes == 0 && !domain.isGoal(currentState)) {
//            throw InsufficientTerminationCriterionException("Not enough time to expand even one node")
        } else {
            logger.info { "A* : expanded $expandedNodes nodes" }
        }

        logger.info { "Done with AStar at $currentState" }

        return currentState
    }

    private fun initializeAStar() {
        treePointers.clear()
        predecessors.clear()
        clearOpenList()
        closedList.clear()

        costTable.forEach { costTable.put(it.key, POSITIVE_INFINITY) }
        reorderOpenListBy(fValueComparator)
    }

    /**
     * Expands a node and add it to closed list. For each successor
     * it will add it to the open list and store it's g value, as long as the
     * state has not been seen before, or is found with a lower g value
     */
    private fun expandFromNode(state: StateType) {
        expandedNodes += 1

        //        logger.debug { "Expanding state $state, h(${heuristicTable[state]}) & g(${costTable[state]})" }

        closedList.add(state)

        val currentGValue = costTable[state]!!
        val successors = domain.successors(state)



        for (successor in successors) {
            logger.trace { "Considering successor ${successor.state}" }

            // Add the current state as the predecessor of the child state
            addPredecessor(successor.state, state, successor.actionCost)

            // only generate those state that are not visited yet or whose cost value are lower than this path
            val successorGValue = costTable.getOrPut(successor.state, { POSITIVE_INFINITY })
            val successorGValueFromCurrent = currentGValue + successor.actionCost
            if (successorGValue > successorGValueFromCurrent) {
                generatedNodes += 1

                // here we generate a state. We store it's g value and remember how to get here via the treePointers
                costTable[successor.state] = successorGValueFromCurrent
                treePointers.put(successor.state, Pair(state, successor.action))

                logger.debug { "Expanding from $state --> ${successor.state} :: open list size: ${openList.size}" }
                logger.trace { "Adding it to to cost table with value " + costTable[successor.state] }

                if (!inOpenList(successor.state)) {
                    addToOpenList(successor.state)
                }
            } else {
                logger.trace {
                    "Did not add, because it's cost is ${costTable[successor.state]} compared to cost of predecessor ( ${costTable[state]}), and action cost ${successor.actionCost}"
                }
            }
        }
    }

    fun addPredecessor(childState: StateType, parentState: StateType, actionCost: Double) {
        val parentCostPair = StateCostPair(parentState, actionCost)

        val predecessorStates = predecessors[childState]
        if (predecessorStates == null) {
            predecessors[childState] = arrayListOf(parentCostPair)
        } else {
            predecessorStates.add(parentCostPair)
        }
    }

    /**
     * Performs Dijkstra updates until runs out of resources or done
     *
     * Updates the mode to SEARCH if done with DIJKSTRA
     *
     * Dijkstra updates repeatedly pop the state s according to their heuristic value, and then update
     * the cost values for all it's visited successors, based on the heuristic s.
     *
     * This increases the stored heuristic values, ensuring that A* won't go in circles, and in general generating
     * a better table of heuristics.
     *
     * When first called (mode == NEW_DIJKSTRA), this will set the cost of all states in the closed list to infinity.
     * We then update
     *
     */
    private fun dijkstra(terminationChecker: TerminationChecker) {
        logger.info { "Doing Dijkstra" }

        // set all g(s) for s in closedList to infinite
        closedList.forEach { heuristicTable.put(it, POSITIVE_INFINITY) }
        // change openList ordering to heuristic only
        reorderOpenListBy(heuristicComparator)

        // update all g(s) in closedList, starting from frontiers in openList
        var counter = 0 // TODO
        var removedCounter = 0;
        val visited: HashSet<StateType> = hashSetOf()

//        logger.info { "\nOpen list: ${openSet.size} - ${openList.size}" }
//        openSet.forEach { logger.debug("$it") }
//
//        logger.info { "\nClosed list: ${closedList.size}" }
//        closedList.forEach { logger.debug("$it") }
        //

        while (!terminationChecker.reachedTermination() && !openList.isEmpty()) {
            //            logger.debug { "Closed list $closedList" }
            //            logger.debug { "Open list $openSet\n" }

            val state = popOpenList()

            val removed = closedList.remove(state)
            logger.debug { "Dijkstra step: ${counter++} :: open list size: ${openList.size} :: closed list size: ${closedList.size} :: #succ: ${predecessors[state]?.size} :: $state      Removed: $removed - $removedCounter" }

            //TODO remove block
            if (removed) removedCounter++

            if (visited.contains(state)) {
                logger.warn { "State was visited multiple times: $state" }
            }
            visited.add(state)
            // remove end

            val currentHeuristicValue = heuristicTable[state]!!
            //            logger.debug { "Checking for predecessors of $state (h value: $currentHeuristicValue)" }

            // update heuristic value for each predecessor
            predecessors[state]?.forEach {

                val predecessorHeuristicValue = heuristicTable[it.state]
                logger.debug { "Considering predecessor ${it.state} with heuristic value $predecessorHeuristicValue" }

                // only update those that we found in the closed list and whose are lower than new found heuristic
//                if (predecessorHeuristicValue != null && it.state in closedList && predecessorHeuristicValue > (currentHeuristicValue + it.actionCost)) {
                if (predecessorHeuristicValue != null && it.state in closedList && predecessorHeuristicValue > (currentHeuristicValue + it.actionCost)) {


                        heuristicTable[it.state] = currentHeuristicValue + it.actionCost
                    logger.debug { "Updated to " + heuristicTable[it.state] }

                    if (!inOpenList(it.state))
                        addToOpenList(it.state)
                }
            } ?: assert(state == rootState)
        }

        // update mode if done
        if (closedList.isEmpty()) {
            logger.info { "Done with Dijkstra" }
            resetSearch()
        } else {
            logger.warn { "Incomplete learning step. " }
        }

    }

    /**
     * Given a state, this function returns the path according to the tree pointers
     */
    private fun extractPlan(targetState: StateType, sourceState: StateType): List<Action> {
        val actions = arrayListOf<Action>()

        // first step
        var stateActionPair: Pair<StateType, Action> = treePointers[targetState] ?: return emptyList()

        logger.debug() { "Extracting plan" }

        // keep on pushing actions to our queue until source state (our root) is reached
        while (stateActionPair.first != sourceState) {
            actions.add(stateActionPair.second) // push to head, queue will pop head
            stateActionPair = treePointers[stateActionPair.first]!!
        }

        logger.debug() { "Plan extracted" }


        // add last action
        actions.add(stateActionPair.second)

        return actions.reversed()
    }

    private fun clearOpenList() {
        logger.debug { "Clear open list" }
        openList.clear()
        openSet.clear()
    }

    private fun inOpenList(state: StateType) = openSet.contains(state)

    private fun popOpenList(): StateType {
        val state = openList.remove()
        openSet.remove(state)

        assert(openList.size == openSet.size)
        return state
    }

    private fun addToOpenList(state: StateType) {
        openList.add(state)
        openSet.add(state)

        assert(openList.size == openSet.size)

    }

    private fun reorderOpenListBy(comparator: Comparator<StateType>) {
        val tempOpenList = openList.toArrayList() // O(1)
        if (tempOpenList.size >= 1) {
            openList = PriorityQueue<StateType>(tempOpenList.size, comparator) // O(1)
        } else {
            openList = PriorityQueue<StateType>(comparator) // O(1)
        }
        openList.addAll(tempOpenList) // O(n * log(n))
    }
}