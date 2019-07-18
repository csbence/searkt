package edu.unh.cs.searkt.planner.realtime

import edu.unh.cs.searkt.environment.Domain
import edu.unh.cs.searkt.environment.NoOperationAction
import edu.unh.cs.searkt.environment.State
import edu.unh.cs.searkt.environment.SuccessorBundle
import edu.unh.cs.searkt.experiment.configuration.ExperimentConfiguration
import edu.unh.cs.searkt.experiment.configuration.realtime.TerminationType
import edu.unh.cs.searkt.experiment.terminationCheckers.TerminationChecker
import edu.unh.cs.searkt.experiment.terminationCheckers.getTerminationChecker
import edu.unh.cs.searkt.planner.*
import edu.unh.cs.searkt.planner.exception.GoalNotReachableException
import edu.unh.cs.searkt.util.AdvancedPriorityQueue
import edu.unh.cs.searkt.util.resize
import kotlin.Long.Companion.MAX_VALUE
import kotlin.system.measureNanoTime
import kotlin.system.measureTimeMillis

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
class LssLrtaStarPlanner<StateType : State<StateType>>(override val domain: Domain<StateType>, val configuration: ExperimentConfiguration) :
        RealTimePlanner<StateType>(),
        RealTimePlannerContext<StateType, PureRealTimeSearchNode<StateType>> {
    override var iterationCounter = 0L

    private val nodes: HashMap<StateType, PureRealTimeSearchNode<StateType>> = HashMap<StateType, PureRealTimeSearchNode<StateType>>(100000000, 1.toFloat()).resize()

    // LSS stores heuristic values. Use those, but initialize them according to the domain heuristic
    // The cost values are initialized to infinity
    override var openList = AdvancedPriorityQueue<PureRealTimeSearchNode<StateType>>(10000000, fValueComparator)
    private var rootState: StateType? = null

    // Performance measurement
    private var aStarPopCounter = 0
    private var dijkstraPopCounter = 0
    var aStarTimer = 0L
    var dijkstraTimer = 0L

    // configuration
    /** The maximum proportion of each iteration that can be spent on learning. */
    private val learningMaxFactor = 0.75
    /** Cost in nanoseconds of following a tree pointer. Used in buffer calculation on when to stop exploring */
    private val treeFollowingFactor = 100.0

    override fun init(initialState: StateType) {
        val node = PureRealTimeSearchNode(
                state = initialState,
                heuristic = domain.heuristic(initialState),
                actionCost = 0,
                action = NoOperationAction,
                cost = 0,
                iteration = 0)
        node.parent = node

        nodes[initialState] = node
        nodes.remove(initialState)
    }

    /**
     * Selects a action given current sourceState.
     *
     * LSS_LRTA* will generate a full plan to some frontier, and stick to that plan. So the action returned will
     * always be the first on in the current plan.
     *
     * LSS-LRTAStar will plan to a specific frontier, and continue
     * to plan from there. This planning abides a termination criteria, meaning that it plans under constraints
     *
     * @param sourceState is the current sourceState
     * @param terminationChecker is the constraint
     * @return a current action
     */
    override fun selectAction(sourceState: StateType, terminationChecker: TerminationChecker): List<ActionBundle> {
        // Initiate for the first search
        //println(sourceState)

        if (rootState == null) {
            rootState = sourceState
        } else if (sourceState != rootState) {
            // The given sourceState should be the last target
        }

        if (domain.isGoal(sourceState)) {
            // The start sourceState is the goal sourceState
            return emptyList()
        }

        // Learning phase
        if (openList.isNotEmpty()) {
            //calculate learning time and get a new termination checker. To satisfy all possible checkers, we need
            //to reset the bound after we construct the checker
            val learningTime = (terminationChecker.remaining() * learningMaxFactor).toLong()
            val learningTerminationChecker = getTerminationChecker(configuration, learningTime)
            learningTerminationChecker.resetTo(learningTime)

            dijkstraTimer += measureTimeMillis {
                //Note that dynamic dijkstra does not notify expansions: expansion-based termination checkers will never "reach termination"
                val dijkstraNanoTimer = measureNanoTime {
                    dynamicDijkstra(this, openList, reachedTermination = { open ->
                        open.isEmpty() || learningTerminationChecker.reachedTermination()
                    })
                }
                printMessage("""Learning: $dijkstraNanoTimer""")
            }
        }

        // Exploration phase
        var plan: List<ActionBundle>? = null
        aStarTimer += measureTimeMillis {
            val aStarNanoTimer = measureNanoTime {
                val targetNode = aStar(sourceState, terminationChecker)

                /* Execution time accounted for with pathLengthBuffer in aStar operation */
                plan = extractPath(targetNode, sourceState)
                rootState = targetNode.state
            }

            printMessage("""A* Time: $aStarNanoTimer""")
        }


        printMessage("""Remaining Time: ${terminationChecker.remaining()}""")

        return plan!!
    }

    /**
     * Runs AStar until termination and returns the path to the head of openList
     * Will just repeatedly expand according to A*.
     */
    private fun aStar(state: StateType, terminationChecker: TerminationChecker): PureRealTimeSearchNode<StateType> {
        // actual core steps of A*, building the tree
        initializeAStar()

        val node = PureRealTimeSearchNode(state, domain.heuristic(state), 0, 0, NoOperationAction, iterationCounter)
        nodes[state] = node
        var currentNode = node
        openList.add(node)

        var pathLengthBuffer = 0L
        while (!terminationChecker.reachedTermination(pathLengthBuffer)) {
            aStarPopCounter++

            val topNode = openList.peek() ?: throw GoalNotReachableException("Open list is empty.")
            if (domain.isGoal(topNode.state)) return topNode

            currentNode = openList.pop()
                    ?: throw GoalNotReachableException("Goal not reachable. Open list is empty.")
            expandFromNode(this, currentNode)

            //we only care about path length for time termination limits. Expansion limits get free tree following
            if (configuration.terminationType == TerminationType.TIME) {
                pathLengthBuffer = ((openList.peek()?.minCostPathLength
                        ?: 0).toDouble() * treeFollowingFactor).toLong()
            }
            terminationChecker.notifyExpansion()
        }

        if (node == currentNode && !domain.isGoal(currentNode.state)) {
            //            throw InsufficientTerminationCriterionException("Not enough time to expand even one node")
        }

        return openList.peek() ?: throw GoalNotReachableException("Open list is empty.")
    }

    private fun initializeAStar() {
        iterationCounter++
        openList.clear()
        openList.reorder(fValueComparator)
    }

    /**
     * Get a node for the state if exists, else create a new node.
     *
     * @return node corresponding to the given state.
     */
    override fun getNode(parent: PureRealTimeSearchNode<StateType>, successor: SuccessorBundle<StateType>): PureRealTimeSearchNode<StateType> {
        val successorState = successor.state
        val tempSuccessorNode = nodes[successorState]

        return if (tempSuccessorNode == null) {
            generatedNodeCount++

            val undiscoveredNode = PureRealTimeSearchNode(
                    state = successorState,
                    heuristic = domain.heuristic(successorState),
                    actionCost = successor.actionCost.toLong(),
                    action = successor.action,
                    parent = parent,
                    cost = MAX_VALUE,
                    iteration = iterationCounter
            )

            nodes[successorState] = undiscoveredNode
            undiscoveredNode
        } else {
            tempSuccessorNode
        }
    }

    //conveniently turn on / off console printing
    @Suppress("UNUSED_PARAMETER")
    private fun printMessage(message: String) = 0//println(message)
}
