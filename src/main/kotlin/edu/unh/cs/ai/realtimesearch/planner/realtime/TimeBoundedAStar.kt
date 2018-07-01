package edu.unh.cs.ai.realtimesearch.planner.realtime

import edu.unh.cs.ai.realtimesearch.MetronomeConfigurationException
import edu.unh.cs.ai.realtimesearch.MetronomeException
import edu.unh.cs.ai.realtimesearch.environment.*
import edu.unh.cs.ai.realtimesearch.experiment.configuration.ExperimentConfiguration
import edu.unh.cs.ai.realtimesearch.experiment.configuration.realtime.TerminationType
import edu.unh.cs.ai.realtimesearch.experiment.terminationCheckers.TerminationChecker
import edu.unh.cs.ai.realtimesearch.experiment.terminationCheckers.getTerminationChecker
import edu.unh.cs.ai.realtimesearch.planner.*
import edu.unh.cs.ai.realtimesearch.planner.exception.GoalNotReachableException
import edu.unh.cs.ai.realtimesearch.planner.realtime.TBAOptimization.NONE
import edu.unh.cs.ai.realtimesearch.planner.realtime.TBAOptimization.SHORTCUT
import edu.unh.cs.ai.realtimesearch.util.AdvancedPriorityQueue
import edu.unh.cs.ai.realtimesearch.util.generateWhile
import edu.unh.cs.ai.realtimesearch.util.resize
import edu.unh.cs.ai.realtimesearch.visualizer
import java.util.*
import kotlin.system.measureNanoTime
import kotlin.system.measureTimeMillis

/**
 * @author Bence Cserna (bence@cserna.net)
 * @author Kevin C. Gall
 */
class TimeBoundedAStar<StateType : State<StateType>>(override val domain: Domain<StateType>, val configuration: ExperimentConfiguration) : RealTimePlanner<StateType>(), RealTimePlannerContext<StateType, PureRealTimeSearchNode<StateType>> {

    // Configuration
    private val tbaOptimization = configuration.tbaOptimization
            ?: throw MetronomeConfigurationException("TBA* optimization is not specified")

    //HARD CODED for testing. Should be configurable
    /** cost of backtrace relative to expansion. Lower number means backtrace is more costly */
    private val traceCost = 1
    /** ratio of tracebacks to expansions */
    private val backlogRatio = configuration.backlogRatio ?: 1.0

    override var iterationCounter = 0L

    private val nodes: HashMap<StateType, PureRealTimeSearchNode<StateType>> = HashMap<StateType, PureRealTimeSearchNode<StateType>>(100_000_000, 1.0f).resize()

    // LSS stores heuristic values. Use those, but initialize them according to the domain heuristic
    // The cost values are initialized to infinity
    override var openList = AdvancedPriorityQueue<PureRealTimeSearchNode<StateType>>(10000000, fValueComparator)

    private var rootState: StateType? = null
    private var lastAgentState: StateType? = null
    private var foundGoal : Boolean = false

    private var aStarPopCounter = 0
    private var expansionLimit = 0L

    // Basically a Linked List Deque implementation with a hash map of states registered in the chain
    data class PathTrace<StateType : State<StateType>>(
            var pathEnd: PureRealTimeSearchNode<StateType>,
            val states : MutableSet<PureRealTimeSearchNode<StateType>> = mutableSetOf()) {
        var pathHead = pathEnd
        init {states.add(pathEnd)}
    }

    private var traceInProgress : PathTrace<StateType>? = null
    private var targetPath : PathTrace<StateType>? = null

    var aStarTimer = 0L

    // SearchEnvelope for visualizer
    private val expandedNodes = mutableListOf<PureRealTimeSearchNode<StateType>>()

    private val aStarSequence
        get() = generateSequence {
            var currentNode : PureRealTimeSearchNode<StateType>? = null

            val sequenceGeneratorTime = measureNanoTime{
                aStarPopCounter++

                val popOpenExecutionTime = measureNanoTime {
                    currentNode = openList.pop() ?: throw GoalNotReachableException("Open list is empty.")
                }
                printMessage("""Pop execution time: $popOpenExecutionTime""")

                expandedNodes.add(currentNode!!)
                val expandFromNodeExecutionTime = measureNanoTime {
                    expandFromNode(this, currentNode!!){}
                }
                printMessage("""Expand Node Execution Time: $expandFromNodeExecutionTime""")
            }
            printMessage("""A* Node Expansion total: $sequenceGeneratorTime""")

            currentNode!!
        }

    //"Priming" the hash table with the initial state so that the first iteration does not spend time initializing
    //the HashMap. We remove the initial state so that we don't cheat on first iteration planning time
    override fun init(initialState : StateType) {
        val node = PureRealTimeSearchNode(
                state = initialState,
                heuristic = domain.heuristic(initialState),
                actionCost = 0,
                action = NoOperationAction,
                cost = 0,
                iteration = 0)
        node.parent = node

        val hashPutExecutionTime = measureNanoTime {
            nodes[initialState] = node
        }
        println("""Init put execution time: $hashPutExecutionTime""")
        nodes.remove(initialState)
    }

    /**
     * Selects a action given current sourceState.
     *
     * TBA* Searches the state space as a single A* search which is interrupted at
     * the time bound of every iteration. The agent selects a path along the route
     * to the best node on the open list frontier that has been discovered so far.
     * If a better frontier node is discovered, the agent switches paths to that node.
     *
     * @param sourceState is the current sourceState
     * @param terminationChecker is the constraint
     * @return a current action
     */
    override fun selectAction(sourceState: StateType, terminationChecker: TerminationChecker): List<RealTimePlanner.ActionBundle> {
        // Initiate for the first search
        printMessage("""Initial: ${terminationChecker.remaining()}""")
        if (rootState == null) {
            rootState = sourceState
            printMessage("""Getting root: ${terminationChecker.remaining()}""")
            val rootNode = getRootNode(sourceState)
            openList.add(rootNode)
        }

        val currentAgentNode = nodes[sourceState]
                ?: throw MetronomeException("Agent is at an unknown state. It is unlikely that the planner sent the agent to an undiscovered node.")

        if (domain.isGoal(sourceState)) {
            // The start sourceState is the goal sourceState
            return emptyList()
        }

        var plan: List<RealTimePlanner.ActionBundle>? = null
        aStarTimer += measureTimeMillis {
            val currentTargetPath = getCurrentPath(sourceState, terminationChecker)

            //checking if agent is on what is identified as the current target path
            val findPathExecutionTime = measureNanoTime {
                plan = if (currentTargetPath.pathHead.state == sourceState) {
                    if (currentTargetPath.pathHead == currentTargetPath.pathEnd) {
                        /* First handle the edge case where the agent has reached the end of its
                         * path while another path is being traced. Begin backtracing to root
                         */
                        targetPath = PathTrace(currentAgentNode.parent)
                        if (currentAgentNode.state == rootState) null
                        else constructPath(listOf(currentAgentNode.state, currentAgentNode.parent.state), domain)
                    } else {
                        /* The agent's current state is an ancestor of the current best
                         * Move agent to current best target
                         * Remove current state from the target path for the next iteration
                         */
                        currentTargetPath.pathHead = currentTargetPath.pathHead.next!!
                        targetPath = currentTargetPath
                        //TODO: Add support for multiple commit
                        extractPath(currentTargetPath.pathHead, sourceState)
                    }
                } else {
                    when (tbaOptimization) {
                    //Will always follow the currentTargetPath
                        NONE -> findNewPath(currentTargetPath, currentAgentNode)
                        SHORTCUT -> TODO()
                    //Will check to see if the currentTargetPath has g-value >= targetPath
                    //If so, switch targets. (Could be refs to the same list. That's fine)
                        TBAOptimization.THRESHOLD -> {
                            val priorTargetPath = targetPath ?:
                            throw MetronomeException("Target path is empty at movement phase!")
                            //check if the new path has at least as high a g value as the current path.
                            //Exception is if the agent is currently backtracing: make the new current
                            //best the target path
                            if (currentTargetPath.pathEnd.cost >= priorTargetPath.pathEnd.cost
                                    || (priorTargetPath.pathEnd == priorTargetPath.pathHead
                                            && priorTargetPath.pathHead == currentAgentNode)) {
                                findNewPath(currentTargetPath, currentAgentNode)
                            } else {
                                findNewPath(priorTargetPath, currentAgentNode)
                            }
                        }
                    }
                }
            }
            printMessage("""Find Path Execution Time: ${findPathExecutionTime}; Remaining Time: ${terminationChecker.remaining()}""")

        }

        //check if the plan is null or empty. If so, attempt to move to the last state the agent was in
        val safePlan = if(plan == null || plan?.size == 0) {
            val targetNode = nodes[lastAgentState] ?:
                throw MetronomeException("Cannot construct plan: no plan or previous state")

            if (configuration.commitmentStrategy != CommitmentStrategy.SINGLE)
                throw MetronomeException("TBA* does not support commitment strategies other than SINGLE")
            //TODO: Add support for multiple commitment.
            // All it requires is tracking the last state committed rather than the agent's previous state
            constructPath(listOf(sourceState, targetNode.state), domain)
        } else {
            plan!!
        }
        lastAgentState = sourceState
        visualizer?.updateSearchEnvelope(expandedNodes)
        visualizer?.updateAgentLocation(currentAgentNode)
        visualizer?.delay()

        return safePlan
    }

    /**
     * Runs AStar until termination and returns the path to the head of openList
     * Will just repeatedly expand according to A*.
     */
    private fun aStar(terminationChecker: TerminationChecker): PureRealTimeSearchNode<StateType> {
        var currentExpansionDuration = 0L

        aStarSequence
                .generateWhile {
                    !terminationChecker.reachedTermination() && !domain.isGoal(openList.peek()?.state
                            ?: throw GoalNotReachableException("Open list is empty."))
                }
                .forEach {
                    terminationChecker.notifyExpansion()
                    currentExpansionDuration++
                    expandedNodes.add(it)
                }

        if (expansionLimit == 0L) expansionLimit = currentExpansionDuration
        return openList.peek() ?: throw GoalNotReachableException("Open list is empty.")
    }

    /**
     * Get the next path to be followed. If best node has not been fully back traced,
     * return current target path
     * Uses full allotted time quantum minus configured "epsilon" buffer
     */
    private fun getCurrentPath(sourceState : StateType, terminationChecker: TerminationChecker) : PathTrace<StateType> {

        val topOfOpen = openList.peek() ?: throw GoalNotReachableException()
        //Goal found and traced
        if (foundGoal && traceInProgress == null && targetPath?.pathEnd == topOfOpen) {
            return targetPath!!
        }

        val tracebackBuffer = if (configuration.terminationType == TerminationType.TIME) {
            /*  For real time bounds, divvying up time by dividing it up into chunks
             *  according to the configurable trace cost estimation and allocating time according
             *  to the intended "backlog ratio", which is the amount of traces that should occur per
             *  expansion
             */
            (backlogRatio * (terminationChecker.remaining().toDouble() / (traceCost + 1.0))).toLong()
        } else {
            0L
        }

        val aStarTermChecker = getTerminationChecker(configuration, terminationChecker.remaining() - tracebackBuffer)

        var bestNode : PureRealTimeSearchNode<StateType>? = null
        val aStarExecutionTime = measureNanoTime {
            bestNode = if (domain.isGoal(topOfOpen.state)) {
                foundGoal = true
                topOfOpen
            } else {
                aStar(aStarTermChecker)
            }
        }
        printMessage("""A* Execution Time: ${aStarExecutionTime}; Time Remaining: ${terminationChecker.remaining()}""")

        //if no traceback in progress, trace from best node. Otherwise pick up where we left off
        val targetNode = traceInProgress?.pathHead ?: bestNode!!

        /*  if using real time bound, we will use the termination checker instead of
         *  a numeric trace limit
         */
        val traceLimit = (expansionLimit * backlogRatio) + 2
        var currentTraceCount = 0
        val traceBound : () -> Boolean = if (configuration.terminationType == TerminationType.TIME) {
            {
                terminationChecker.reachedTermination() && targetPath != null
            }
        } else {
            {
                currentTraceCount++ >= traceLimit
            }
        }

        if (traceInProgress == null) {
            traceInProgress = PathTrace(targetNode)
        }

        val currentBacktrace = traceInProgress!!
        var currentNode = targetNode

        printMessage("""Before Trace: ${terminationChecker.remaining()}""")
        while (!traceBound() && currentNode.state != rootState && currentNode.state != sourceState) {
            currentBacktrace.pathHead = currentNode.parent
            currentBacktrace.states.add(currentNode.parent)
            currentNode.parent.next = currentNode
            currentNode = currentNode.parent
        }
        printMessage("""After Trace: ${terminationChecker.remaining()}""")


        //Note, setting currentTargetPath here as either the previous target or the new backtrace
        return if (currentBacktrace.pathHead.state == rootState || currentBacktrace.pathHead.state == sourceState) {
            traceInProgress = null
            currentBacktrace
        } else {
            assert(targetPath != null)
            targetPath!!
        }
    }

    /**
     * Checks if the agent's current state is in the target path. If so, the path from current state to the next state
     * toward end of the path. Otherwise backtrace to root
     * Average case constant time. (Worst case linear in path size, but only because of hash-set properties.
     */
    private fun findNewPath(bestPath : PathTrace<StateType>, currentAgentNode : PureRealTimeSearchNode<StateType>)
            : List<RealTimePlanner.ActionBundle>? {
        // Find common ancestor
        val sourceState = currentAgentNode.state
        val sourceOnPath = bestPath.states.contains(currentAgentNode)

        targetPath = bestPath
        if (sourceOnPath) {
            //Note: setting target path here
            targetPath = bestPath
            //TODO: refactor for multiple commit
            return extractPath(currentAgentNode.next, sourceState)
        } else if (currentAgentNode.state == rootState) { //if we're at the root, there's no path!
            return null
        } else {
            //single step plan follows immediate back pointer
            return constructPath(listOf(currentAgentNode.state, currentAgentNode.parent.state), domain)
        }
    }

    private fun getRootNode(state: StateType): PureRealTimeSearchNode<StateType> {
        generatedNodeCount++
        val node = PureRealTimeSearchNode(
                state = state,
                heuristic = domain.heuristic(state),
                actionCost = 0,
                action = NoOperationAction,
                cost = 0,
                iteration = 0)
        node.parent = node

        val hashPutExecutionTime = measureNanoTime {
            nodes[state] = node
        }
        printMessage("""Hash Table Execution Time: ${hashPutExecutionTime}""")
        return node
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
                    actionCost = successor.actionCost,
                    action = successor.action,
                    parent = parent,
                    cost = Long.MAX_VALUE,
                    iteration = iterationCounter
            )

            val addNodeExecutionTime = measureNanoTime {
                nodes.put(successorState, undiscoveredNode)
            }
            printMessage("""Put Node execution time: $addNodeExecutionTime""")
            undiscoveredNode
        } else {
            tempSuccessorNode!!
        }
    }
}

inline fun printMessage(msg : String) = 0//println(msg)

enum class TBAOptimization {
    NONE, SHORTCUT, THRESHOLD
}

enum class TBAStarConfiguration(val key: String) {
    TBA_OPTIMIZATION("tbaOptimization"),
    BACKLOG_RATIO("backlogRatio");

    override fun toString(): String = key
}

