package edu.unh.cs.ai.realtimesearch.planner.realTime

import edu.unh.cs.ai.realtimesearch.environment.Action
import edu.unh.cs.ai.realtimesearch.environment.Domain
import edu.unh.cs.ai.realtimesearch.environment.State
import edu.unh.cs.ai.realtimesearch.experiment.TerminationChecker
import edu.unh.cs.ai.realtimesearch.planner.RealTimePlanner
import org.slf4j.LoggerFactory
import java.util.*

class LSS_LRT_AStarPlanner(domain: Domain) : RealTimePlanner(domain) {

    private val logger = LoggerFactory.getLogger("LLS_LRT")

    // results/logging
    private var generatedNodes = 0
    private var expandedNodes = 0

    // cached h and g values
    private val heuristicTable: MutableMap<State, Double> = hashMapOf()
    private val costTable: MutableMap<State, Double> = hashMapOf()
    private val treePointers: MutableMap<State, Pair<State, Action>> = hashMapOf()

    // default search lists
    private val closedList: HashSet<State> = hashSetOf()
    private var openList = PriorityQueue(LLS_LRT_AStarStateComparator(domain, heuristicTable, costTable))

    // current plan in execution
    private var executingPlan: Queue<Action> = linkedListOf()
    private var rootState: State? = null

    // current mode, either doing dijkstra updates or astar search
    enum class Mode {NEW_SEARCH, ASTAR, NEW_DIJKSTRA, DIJKSTRA, FOUND_GOAL}
    private var mode = Mode.NEW_SEARCH

    /**
     * TODO: document
     */
    override fun selectAction(state: State, terminationChecker: TerminationChecker): Action {
        // only first ever call in an experiment will require this
        if (rootState == null)
            rootState = state

        logger.info("Selecting action in state $state, but considering rootState $rootState")

        // 2 Possible scenarios:
        // 1) We currently have no plan and need to think of one
        // 2) We are currently executing a plan and have time to search more
        // in 2) we could either be doing dijkstra or more searching

        if (executingPlan.isEmpty()) { // 1): no current plan
            logger.info("Currently no plan, executing AStar")

            // emergency: we need to have at least a clean search if not done with dijkstra
            if (mode == Mode.DIJKSTRA) setMode(Mode.NEW_SEARCH)

            // get plan and store new root
            val endState = AStar(terminationChecker)
            executingPlan = extractPlan(endState)

            logger.info("Got a new plan, up to state $endState of plan $executingPlan")

            if (mode != Mode.FOUND_GOAL) {
                setMode(Mode.NEW_DIJKSTRA)
                rootState = endState
            }

        } else { // 2) We are executing a plan
            when (mode) {
                Mode.NEW_DIJKSTRA -> Dijkstra(terminationChecker)
                Mode.DIJKSTRA -> Dijkstra(terminationChecker)
                Mode.ASTAR -> AStar(terminationChecker)
                Mode.NEW_SEARCH -> AStar(terminationChecker)
                Mode.FOUND_GOAL -> logger.info("In mode found goal")
            }
        }

        val action = executingPlan.remove()
        logger.info("Returning action $action and ${executingPlan.size} actions left")

        return action
    }


    /**
     * Runs AStar until termination and returns the path to the head of openlist
     *
     * @param terminationChecker defines the termination criterium
     */
    private fun AStar(terminationChecker: TerminationChecker): State {
        // reset stuff for new search
        if (mode == Mode.NEW_SEARCH) {
            logger.info("New Search...")

            generatedNodes = 0
            expandedNodes = 0

            costTable.clear()
            openList.clear()
            closedList.clear()

            openList.add(rootState)
            costTable.put(rootState!!, 0.0)
        }

        setMode(Mode.ASTAR)

        var state = openList.remove()

        while(!terminationChecker.reachedTermination() && !domain.isGoal(state))
            state = expandNode(state)

        logger.info("Done with AStar")

        if (domain.isGoal(state)) setMode(Mode.FOUND_GOAL)

        return state
    }

    private fun Dijkstra(terminationChecker: TerminationChecker): Boolean {
        logger.info("Doing Dijkstra")

        if (mode == Mode.NEW_DIJKSTRA)
            closedList.forEach { heuristicTable.put(it, Double.MAX_VALUE)}
        setMode(Mode.DIJKSTRA)

        // change openList ordering to heuristic only
        var tempOpenList = openList.toArrayList()
        openList = PriorityQueue(GreedyLLS_LRT_AStarStateComparator(heuristicTable))
        openList.addAll(tempOpenList)

        while (!terminationChecker.reachedTermination() && !closedList.isEmpty()) {
            val state = openList.remove()
            closedList.remove(state)

            logger.debug("Checking for predecessors of " + state + " (h value: " + heuristicTable[state] + ")") // TODO: trace
            for (predecessor in domain.predecessors(state)) {
                logger.trace("Considering predecessor " + predecessor.state + " with heuristic value "
                        + heuristicTable[predecessor.state])

                if (predecessor.state in closedList &&
                        heuristicTable[predecessor.state]!! > (heuristicTable[state]!! + predecessor.cost))  {
                    heuristicTable[predecessor.state] = heuristicTable[state]!! + predecessor.cost
                    logger.trace("Updated to " + heuristicTable[predecessor.state])

                    if (!openList.contains(predecessor.state))
                        openList.add(predecessor.state)
                }


            }
        }

        // change openList ordering back to f value
        tempOpenList = openList.toArrayList()
        openList = PriorityQueue(LLS_LRT_AStarStateComparator(domain, heuristicTable, costTable))
        openList.addAll(tempOpenList)

        val done = closedList.isEmpty()
        if (done) setMode(Mode.NEW_SEARCH)

        return done
    }

    private fun expandNode(state: State): State {
        logger.debug("Expanding state " + state)
        closedList.add(state)

        expandedNodes.inc()
        for (successor in domain.successors(state)) {
            logger.trace("Considering successor " + successor)

            if (costTable.getOrPut(successor.state, { Double.POSITIVE_INFINITY}) >
                    (costTable[state]!! + successor.cost)) {

                costTable[successor.state] = costTable[state]!! + successor.cost
                logger.trace("Adding it to to cost table with value " + costTable[successor.state])
                treePointers.put(successor.state, Pair(state, successor.action!!))

                generatedNodes.inc()
                if (!openList.contains(successor.state))
                    openList.add(successor.state)
            } else
                logger.trace("Did not add, because it's cost is " + costTable[successor.state] +
                        " compared to cost of predecessor ( " + costTable[state] + "), and action cost " + successor.cost)
        }

        return openList.remove()
    }

    private fun extractPlan(state: State): Queue<Action> {
        val actions: Deque<Action> = linkedListOf()

        var stateActionPair: Pair<State, Action> = treePointers[state]!!

        while (stateActionPair.first != rootState) {
            actions.push(stateActionPair.second) // push to head, queue will pop head
            stateActionPair = treePointers[stateActionPair.first]!!
        }

        // add last action
        actions.push(stateActionPair.second)

        return actions // we are adding actions in wrong order, to return the reverser
    }

    /**
     * Sets the mode (and logs some)
     */
    private fun setMode(newMode: Mode) {
        mode = newMode
        logger.info("Setting mode to " + mode)
    }

    /**
     * Uses the heuristic and cost tables in LLS_LRTA_AStar planner to calculate
     * the heuristic and g values. If those are not found, infinite is used
     * as according to the algorithm
     *
     * TODO: will this round -0.4 to 0? That would be bad
     */
    public class LLS_LRT_AStarStateComparator(val domain: Domain,
                                              val heuristicTable: MutableMap<State, Double>,
                                              val costTable: MutableMap<State, Double>) : Comparator<State> {
        override fun compare(s1: State?, s2: State?): Int {
            if (s1 != null && s2 != null) {

                return ((heuristicTable.getOrPut(s1, { domain.heuristic(s1)}) + costTable.getOrPut(s1, { Double.POSITIVE_INFINITY})) -
                        (heuristicTable.getOrPut(s2, { domain.heuristic(s1)}) + costTable.getOrPut(s1, { Double.POSITIVE_INFINITY}))).toInt()
            }

            else throw RuntimeException("Cannot insert null into closed list")
        }
    }

    /**
     * Uses the heuristic and cost tables in LLS_LRTA_AStar planner to calculate
     * the heuristic and g values. If those are not found, infinite is used
     * as according to the algorithm
     *
     * TODO: will this round -0.4 to 0? That would be bad
     */
    public class GreedyLLS_LRT_AStarStateComparator(val heuristicTable: MutableMap<State, Double>) : Comparator<State> {
        override fun compare(s1: State?, s2: State?): Int {
            if (s1 != null && s2 != null) {

                return (heuristicTable.getOrPut(s1, { Double.POSITIVE_INFINITY}) -
                        heuristicTable.getOrPut(s2, { Double.POSITIVE_INFINITY})).toInt()
            }

            else throw RuntimeException("Cannot insert null into closed list")
        }
    }

}