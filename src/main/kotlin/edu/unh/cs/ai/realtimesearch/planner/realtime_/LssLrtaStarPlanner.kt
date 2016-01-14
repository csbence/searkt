package edu.unh.cs.ai.realtimesearch.planner.realtime_

import edu.unh.cs.ai.realtimesearch.environment.Action
import edu.unh.cs.ai.realtimesearch.environment.Domain
import edu.unh.cs.ai.realtimesearch.environment.State
import edu.unh.cs.ai.realtimesearch.experiment.TerminationChecker
import edu.unh.cs.ai.realtimesearch.planner.RealTimePlanner
import org.slf4j.LoggerFactory
import java.util.*

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
class LssLrtaStarPlanner<StateType : State<StateType>>(domain: Domain<StateType>) : RealTimePlanner<StateType>(domain) {

    private val logger = LoggerFactory.getLogger(LssLrtaStarPlanner::class.java)

    // cached h and g values
    private val heuristicTable: MutableMap<StateType, Double> = hashMapOf()
    private val costTable: MutableMap<StateType, Double> = hashMapOf()
    private val treePointers: MutableMap<StateType, Pair<StateType, Action>> = hashMapOf()

    // default search lists
    private val closedList: HashSet<StateType> = hashSetOf()

    // LSS stores heuristic values. Use those, but initialize them according to the domain heuristic
    // The cost values are initialized to infinity
    private var openList = PriorityQueue<StateType>(compareBy {
        heuristicTable.getOrPut(it, { domain.heuristic(it) }) + costTable.getOrPut(it, { Double.POSITIVE_INFINITY })
    })
    // for fast lookup we maintain a set in parallel
    private val openSet = hashSetOf<StateType>()

    // current plan in execution
    private var executingPlan: Queue<Action> = ArrayDeque()
    private var rootState: StateType? = null

    /**
     * Current mode, either doing dijkstra updates or AStar search
     *
     * Since both A* and Dijkstra performs can be interrupted by the termination checker,
     * and both initiate values at the start, there is a distinction between the first dijkstra
     * and A* run, and the others.
     *
     * This ensures that those initiation steps are not taken if we are continuing from a previous run
     */
    enum class Mode {INIT, NEW_SEARCH, A_STAR, NEW_DIJKSTRA, DIJKSTRA, FOUND_GOAL }

    private var mode = Mode.INIT

    /**
     * Prepares LSS for a completely unrelated new search. Sets mode to init
     * When a new action is selected, all members that persist during selection action phase are cleared
     */
    override public fun reset() {
        mode = Mode.INIT
        super.reset()
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
    override fun selectAction(state: StateType, terminationChecker: TerminationChecker): Action {
        // Initiate for the first search
        if (mode == Mode.INIT) {

            // clear members that persist during action selection
            heuristicTable.clear()
            treePointers.clear()
            executingPlan = ArrayDeque()

            // Ready to start new search!
            rootState = state
            mode = Mode.NEW_SEARCH
        }

        logger.info("Selecting action in state $state, but considering rootState $rootState")

        // 2 Possible scenarios:
        // 1) We currently have no plan and need to think of one
        // 2) We are currently executing a plan and have time to search more
        //      2a) Doing dijkstra
        //      2b) Doing A*

        if (executingPlan.isEmpty()) {
            // 1): no current plan

            val endState = generateExecutionPlan(terminationChecker)
            executingPlan = extractPlan(endState)

            logger.info("Got a new plan, up to state $endState " +
                    ", h(${heuristicTable[endState]}) & g(${costTable[endState]}), of plan size  ${executingPlan.size}")

            if (domain.isGoal(endState)) {
                setMode(Mode.FOUND_GOAL)
            } else {
                // setup for next steps: Dijkstra and new root state
                setMode(Mode.NEW_DIJKSTRA)
                rootState = endState
            }
        } else {
            // 2) We are executing a plan. We either need to do a) Dijkstra or b) more searching
            when (mode) {
                Mode.NEW_DIJKSTRA, Mode.DIJKSTRA -> Dijkstra(terminationChecker) // a

                Mode.A_STAR, Mode.NEW_SEARCH -> {
                    // b
                    val endState = AStar(terminationChecker)

                    // if we find a goal while executing, simply extend the plan
                    if (domain.isGoal(endState)) {
                        setMode(Mode.FOUND_GOAL)
                        executingPlan.addAll(extractPlan(endState))
                    }

                }
                Mode.FOUND_GOAL -> logger.info("In mode found goal")

                else -> {
                    throw RuntimeException("LSS-LRTA does not expect to be in mode $mode here")
                }
            }
        }

        // actual return an action from the current plan
        val action = executingPlan.remove()
        logger.info("Returning action $action with plan $executingPlan left")

        return action
    }

    /**
     * Sets a execution plan to current most promising state (f value)
     *
     * Returns end state of the plan
     */
    private fun generateExecutionPlan(terminationChecker: TerminationChecker): StateType {
        logger.info("Currently no plan, executing AStar")

        // emergency: we need to have at least a clean search if not done with dijkstra
        if (mode == Mode.DIJKSTRA) {
            logger.error("Not finished with Dijkstra backups, but starting new search")
            setMode(Mode.NEW_SEARCH)
        }

        // generate new plan
        val endState = AStar(terminationChecker)

        return endState
    }


    /**
     * Runs AStar until termination and returns the path to the head of openList
     *
     * The first call, when in mode NEW_SEARCH, it will clear the open list, closed list & cost table
     * Other than that will just repeatedly expand according to A*.
     */
    private fun AStar(terminationChecker: TerminationChecker): StateType {
        // During first call, get ready for a search (erase previous open/closed list and cost able
        if (mode == Mode.NEW_SEARCH) {
            logger.info("New Search...")

            costTable.clear()
            clearOpenList()
            closedList.clear()

            addToOpenList(rootState!!)
            costTable.put(rootState!!, 0.0)

            // We do not need to setup for a new search after this
            setMode(Mode.A_STAR)
        }

        // actual core steps of A*, building the tree
        var state = popOpenList()
        while (!terminationChecker.reachedTermination() && !domain.isGoal(state))
            state = expandNode(state)

        logger.info("Done with AStar")
        return state
    }

    /**
     * Performs Dijkstra updates until runs out of resources or done
     *
     * Updates the mode to NEW_SEARCH if done with DIJKSTRA
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
    private fun Dijkstra(terminationChecker: TerminationChecker) {
        logger.info("Doing Dijkstra")

        // set all g(s) for s in closedList to infinite
        if (mode == Mode.NEW_DIJKSTRA) {
            closedList.forEach { heuristicTable.put(it, Double.MAX_VALUE) }

            // no need to setup dijkstra again next round
            setMode(Mode.DIJKSTRA)
        }

        // change openList ordering to heuristic only
        var tempOpenList = openList.toArrayList()
        openList = PriorityQueue<StateType>(compareBy { heuristicTable.getOrPut(it, { domain.heuristic(it) }) })
        openList.addAll(tempOpenList)

        // update all g(s) in closedList, starting from frontiers in openList
        while (!terminationChecker.reachedTermination() && !closedList.isEmpty()) {
            val state = popOpenList()
            closedList.remove(state)

            val currentHeuristicValue = heuristicTable[state]!!
            logger.debug("Checking for predecessors of $state (h value: $currentHeuristicValue)")

            // update heuristic value for each predecessor
            domain.predecessors(state).forEach {

                val predecessorHeuristicValue = heuristicTable[it.state]
                logger.trace("Considering predecessor ${it.state} with heuristic value $predecessorHeuristicValue")

                // only update those that we found in the closed list and whose are lower than new found heuristic
                if (predecessorHeuristicValue != null && it.state in closedList &&
                        predecessorHeuristicValue > (currentHeuristicValue + it.actionCost)) {

                    heuristicTable[it.state] = currentHeuristicValue + it.actionCost
                    logger.trace("Updated to " + heuristicTable[it.state])

                    if (!inOpenList(it.state))
                        addToOpenList(it.state)
                }
            }
        }

        // change openList ordering back to f value
        tempOpenList = openList.toArrayList()
        openList = PriorityQueue<StateType>(compareBy {
            heuristicTable.getOrPut(it, { domain.heuristic(it) }) + costTable.getOrPut(it, { Double.POSITIVE_INFINITY })
        })
        openList.addAll(tempOpenList)

        // update mode if done
        if (closedList.isEmpty()) setMode(Mode.NEW_SEARCH)
    }

    /**
     * Expands a node and add it to closed list. For each successor
     * it will add it to the open list and store it's g value, as long as the
     * state has not been seen before, or is found with a lower g value
     */
    private fun expandNode(state: StateType): StateType {
        expandedNodes += 1

        logger.debug("Expanding state " + state + ", " +
                "h(${heuristicTable[state]}) & g(${costTable[state]})")

        closedList.add(state)

        val currentGValue = costTable[state]!!
        for (successor in domain.successors(state)) {
            logger.trace("Considering successor ${successor.state}")

            // only generate those state that are not visited yet or whose cost value are lower than this path
            val successorGValue = costTable.getOrPut(successor.state, { Double.POSITIVE_INFINITY })
            val successorGValueFromCurrent = currentGValue + successor.actionCost
            if (successorGValue > successorGValueFromCurrent) {
                generatedNodes += 1

                // here we generate a state. We store it's g value and remember how to get here via the treePointers
                costTable[successor.state] = successorGValueFromCurrent
                treePointers.put(successor.state, Pair(state, successor.action!!))

                logger.trace("Adding it to to cost table with value " + costTable[successor.state])

                if (!inOpenList(successor.state))
                    addToOpenList(successor.state)
            } else {
                logger.trace("Did not add, because it's cost is " + costTable[successor.state] +
                        " compared to cost of predecessor ( " + costTable[state] + "), and action cost " + successor.actionCost)
            }
        }

        return popOpenList()
    }

    /**
     * Given a state, this function returns the path according to the tree pointers
     */
    private fun extractPlan(state: StateType): Queue<Action> {
        val actions: Deque<Action> = ArrayDeque()

        // first step
        var stateActionPair: Pair<StateType, Action> = treePointers[state]!!

        // keep on pushing actions to our queue until source state (our root) is reached
        while (stateActionPair.first != rootState) {
            actions.push(stateActionPair.second) // push to head, queue will pop head
            stateActionPair = treePointers[stateActionPair.first]!!
        }

        // add last action
        actions.push(stateActionPair.second)

        return actions
    }

    /**
     * Sets the mode of LSS-LRTA(and logs some)
     */
    private fun setMode(newMode: Mode) {
        mode = newMode
        logger.info("Setting mode to " + mode)
    }

    private fun clearOpenList() {
        openList.clear()
        openSet.clear()
    }

    private fun inOpenList(state: StateType) = openSet.contains(state)

    private fun popOpenList(): StateType {
        val state = openList.remove()
        openSet.remove(state)
        return state
    }

    private fun addToOpenList(state: StateType) {
        openList.add(state)
        openSet.add(state)
    }
}