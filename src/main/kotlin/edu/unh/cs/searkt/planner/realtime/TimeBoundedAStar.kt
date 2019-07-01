package edu.unh.cs.searkt.planner.realtime

import edu.unh.cs.searkt.MetronomeConfigurationException
import edu.unh.cs.searkt.MetronomeException
import edu.unh.cs.searkt.environment.*
import edu.unh.cs.searkt.experiment.configuration.ExperimentConfiguration
import edu.unh.cs.searkt.experiment.configuration.realtime.TerminationType
import edu.unh.cs.searkt.experiment.result.ExperimentResult
import edu.unh.cs.searkt.experiment.terminationCheckers.TerminationChecker
import edu.unh.cs.searkt.experiment.terminationCheckers.getTerminationChecker
import edu.unh.cs.searkt.planner.*
import edu.unh.cs.searkt.planner.exception.GoalNotReachableException
import edu.unh.cs.searkt.planner.realtime.TBAOptimization.NONE
import edu.unh.cs.searkt.planner.realtime.TBAOptimization.SHORTCUT
import edu.unh.cs.searkt.util.AbstractAdvancedPriorityQueue
import edu.unh.cs.searkt.util.AdvancedPriorityQueue
import edu.unh.cs.searkt.util.resize
import kotlinx.serialization.ImplicitReflectionSerializer
import java.util.*
import kotlin.system.measureTimeMillis

/**
 * @author Bence Cserna (bence@cserna.net)
 * @author Kevin C. Gall
 *
 * Maintains a single A* search throughout the life of the algorithm. Commits the agent along the path to the
 * best node discovered so far and switches paths once a better one is discovered. Depends on undirected state space
 * (i.e. the ability to return to a parent node).
 * Splits time between the A* search and tracing a path from the best frontier node (at the start of the trace) to
 * either the agent or the root node.
 *
 * @history 7/4/2018 Kevin C. Gall Tuned to work under a 10ms real time bound. Note: stripped out Kotlin constructs
 * and convenience functions on purpose. Do not add them back if you want to hit real time bounds!
 */
class TimeBoundedAStar<StateType : State<StateType>>(override val domain: Domain<StateType>, val configuration: ExperimentConfiguration) :
        RealTimePlanner<StateType>(),
        RealTimePlannerContext<StateType, TimeBoundedAStar.TBANode<StateType>> {

    // Attribute Keys
    private val RESTARTS = "restarts"

    // Configuration
    private val tbaOptimization = configuration.tbaOptimization
            ?: throw MetronomeConfigurationException("TBA* optimization is not specified")
    private val strategy = configuration.lookaheadStrategy ?: LookaheadStrategy.A_STAR
    private val weight = configuration.weight ?: 1.0

    //HARD CODED for testing. Should be configurable
    /** cost of backtrace relative to expansion as expansion cost / backtrace cost. Lower number means backtrace is more costly */
    private val traceCost = 5 //number tuned on Lubuntu 18.04 w/ low-latency kernel, OpenJ9 JVM, Intel i7-4770 3.4GHz (4 core)
    /** ratio of tracebacks to expansions */
    private val backupRatio = configuration.backupRatio ?: 1.0

    override var iterationCounter = 0L

    class TBANode<StateType : State<StateType>>(
            override val state: StateType,
            override var heuristic: Double,
            override var cost: Long,
            override var actionCost: Long,
            override var action: Action,
            override var iteration: Long,
            parent: TBANode<StateType>? = null
    ) : RealTimeSearchNode<StateType, TBANode<StateType>> {
        var secondaryIndex: Int = -1

        override var index: Int = -1
        override var closed = false

        /** Nodes that generated this SafeRealTimeSearchNode as a successor in the current exploration phase. */
        override var predecessors: MutableList<SearchEdge<TBANode<StateType>>> = arrayListOf()

        /** Parent pointer that points to the min cost predecessor. */
        override var parent: TBANode<StateType> = parent ?: this

        /** Optional-use descendant pointer which can store the node's next best successor */
        var next: TBANode<StateType>? = null

        override var lastLearnedHeuristic = heuristic
        override var minCostPathLength: Long = 0L

        override fun hashCode(): Int = state.hashCode()

        override fun equals(other: Any?): Boolean = when (other) {
            null -> false
            is SearchNode<*, *> -> state == other.state
            is State<*> -> state == other
            else -> false
        }

        override fun toString() =
                "RTSNode: [State: $state h: $heuristic, g: $cost, iteration: $iteration, actionCost: $actionCost, parent: ${parent.state}, open: $open]"
    }

    private val nodes: HashMap<StateType, TBANode<StateType>> = HashMap<StateType, TBANode<StateType>>(100_000_000, 1.0f).resize()

    inner class SecondaryOpenList(comparator: Comparator<in TBANode<StateType>>)
            : AbstractAdvancedPriorityQueue<TBANode<StateType>>(arrayOfNulls(1000000), comparator) {
        override fun getIndex(item: TBANode<StateType>): Int = item.secondaryIndex
        override fun setIndex(item: TBANode<StateType>, index: Int) {
            item.secondaryIndex = index
        }
    }

    var primaryOpenIsA = false
    override var openList = getNewOpenList()
    var updateOpenList: AbstractAdvancedPriorityQueue<TBANode<StateType>>? = null


    private var rootState: StateType? = null
    private var lastAgentState: StateType? = null
    private var foundGoal : Boolean = false

    private var aStarPopCounter = 0
    private var expansionLimit = 0L

    data class PathEdge<StateType : State<StateType>>(
            val node: TBANode<StateType>, val next: PathEdge<StateType>?, val action: Action = node.action, val cost: Long = node.actionCost
    )
    data class PathTrace<StateType : State<StateType>>(
            val goal: TBANode<StateType>,
            val edges : MutableMap<TBANode<StateType>, PathEdge<StateType>> = mutableMapOf()) {
        val pathEnd = PathEdge(goal, null)
        var pathHead = pathEnd
        init {edges[pathEnd.node] = pathEnd}
    }

    private var traceInProgress : PathTrace<StateType>? = null
    private var targetPath : PathTrace<StateType>? = null

    var aStarTimer = 0L

    // SearchEnvelope for visualizer
    private val expandedNodes = mutableListOf<TBANode<StateType>>()

    //"Priming" the hash table with the initial state so that the first iteration does not spend time initializing
    //the HashMap. We remove the initial state so that we don't cheat on first iteration planning time
    override fun init(initialState : StateType) {
        val node = TBANode(
                state = initialState,
                heuristic = domain.heuristic(initialState) * weight,
                actionCost = 0,
                action = NoOperationAction,
                cost = 0,
                iteration = 0)
        node.parent = node

        nodes[initialState] = node
        nodes.remove(initialState)
    }

    @ImplicitReflectionSerializer
    override fun appendPlannerSpecificResults(results: ExperimentResult) {
        results.attributes[RESTARTS] = this.counters[RESTARTS] ?: 0
    }

    private fun getNewOpenList() : AbstractAdvancedPriorityQueue<TBANode<StateType>> {
        val newOpen = if (primaryOpenIsA) {
            SecondaryOpenList(when (strategy) {
                LookaheadStrategy.GBFS -> heuristicComparator
                else -> fValueComparator
            })
        } else {
            AdvancedPriorityQueue<TBANode<StateType>>(10000000, when (strategy){
                LookaheadStrategy.GBFS -> heuristicComparator
                else -> fValueComparator
            })
        }

        primaryOpenIsA = !primaryOpenIsA
        return newOpen
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
        printTime("""Initial: ${terminationChecker.remaining()}"""){}
        if (rootState == null) {
            rootState = sourceState
            printTime("""Getting root: ${terminationChecker.remaining()}"""){}
            val rootNode = getRootNode(sourceState)
            openList.add(rootNode)
        }

        val currentAgentNode = nodes[sourceState]
                ?: throw MetronomeException("Agent is at an unknown state. It is unlikely that the planner sent the agent to an undiscovered node.")

        if (domain.isGoal(sourceState)) {
            // The start sourceState is the goal sourceState
            return emptyList()
        }

        var plan: List<ActionBundle>? = null
        aStarTimer += measureTimeMillis {
            var currentTargetPath = getCurrentPath(sourceState, terminationChecker)
            currentTargetPath = when(tbaOptimization) {
                NONE -> currentTargetPath
                SHORTCUT -> TODO()
                TBAOptimization.THRESHOLD -> {
                    when {
                        targetPath == null -> currentTargetPath
                        //check if the new path has at least as high a g value as the current path.
                        //Exception is if the agent is currently backtracing: make the new current
                        //best the target path
                        currentTargetPath.pathEnd.cost >= targetPath!!.pathEnd.cost -> currentTargetPath
                        targetPath!!.pathEnd == targetPath!!.pathHead
                                && targetPath!!.pathHead.node == currentAgentNode -> currentTargetPath
                        else -> targetPath!!
                    }
                }
            }

            //checking if agent is on what is identified as the current target path
            printTime("Find Path Execution Time") {
                plan = if (currentTargetPath.pathHead.node.state == sourceState) {
                    if (currentTargetPath.pathHead == currentTargetPath.pathEnd) {
                        /* First handle the edge case where the agent has reached the end of its
                         * path while another path is being traced. Begin backtracing to root
                         */
                        targetPath = PathTrace(currentAgentNode.parent)

                        if (currentAgentNode.state == rootState) null
                        else backtrack(currentAgentNode)
                    } else {
                        /* The agent's current state is an ancestor of the current best
                         * Move agent to current best target
                         * Remove current state from the target path for the next iteration
                         */
                        targetPath = currentTargetPath
                        currentTargetPath.pathHead = currentTargetPath.pathHead.next ?:
                                throw MetronomeException("Bug - attempted to get the next path node when one does not exist")
                        //TODO: Add support for multiple commit
                        listOf(ActionBundle(currentTargetPath.pathHead.action, currentTargetPath.pathHead.cost))
                    }
                } else {
                    findNewPath(currentTargetPath, currentAgentNode)
                }
            }

        }

        //check if the plan is null or empty. If so, attempt to move to the last state the agent was in
        val safePlan = if(plan == null || plan?.size == 0) {
            val targetNode = nodes[lastAgentState] ?:
                throw MetronomeException("Cannot construct plan: no plan or previous state")

            if (configuration.commitmentStrategy != CommitmentStrategy.SINGLE)
                throw MetronomeException("TBA* does not support commitment strategies other than SINGLE")
            //TODO: Add support for multiple commitment.
            // All it requires is tracking the last state committed rather than the agent's previous state
            backtrack(currentAgentNode, targetNode)
        } else {
            plan!!
        }
        lastAgentState = sourceState
//        visualizer?.updateSearchEnvelope(expandedNodes)
//        visualizer?.updateAgentLocation(currentAgentNode)
//        visualizer?.delay()

        return safePlan
    }

    /**
     * Runs AStar until termination and returns the path to the head of openList
     * Will just repeatedly expand according to A*.
     */
    private fun aStar(terminationChecker: TerminationChecker): TBANode<StateType> {
        // for expansion termination type bookkeeping on first iteration.
        // TODO: Implement this using the TerminationChecker.remaining() function instead
        var currentExpansionDuration = 0L

        val expansions = aStarPopCounter
        while (!terminationChecker.reachedTermination() && !domain.isGoal(openList.peek()?.state
                        ?: throw GoalNotReachableException("Open list is empty."))) {
            aStarPopCounter++

            val currentNode = openList.pop() ?: throw GoalNotReachableException("Open list is empty.")

            expandFromNode(this, currentNode){}

            terminationChecker.notifyExpansion()
            currentExpansionDuration++
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
            (backupRatio * (terminationChecker.remaining().toDouble() / (traceCost + 1.0))).toLong()
        } else {
            0L
        }

        val aStarTermChecker = getTerminationChecker(configuration, terminationChecker.remaining() - tracebackBuffer)

        var bestNode : TBANode<StateType>? = null
        printTime("A* Execution Time") {
            bestNode = if (domain.isGoal(topOfOpen.state)) {
                foundGoal = true
                topOfOpen
            } else {
                aStar(aStarTermChecker)
            }
        }

        //if no traceback in progress, trace from best node. Otherwise pick up where we left off
        val targetNode = traceInProgress?.pathHead?.node ?: bestNode!!

        /*  if using real time bound, we will use the termination checker instead of
         *  a numeric trace limit
         */
        val traceLimit = (expansionLimit * backupRatio) + 2
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

        printTime("Trace execution time") {
            while (!traceBound() && currentNode.state != rootState && currentNode.state != sourceState) {
                currentBacktrace.pathHead =
                        PathEdge(currentNode.parent, currentBacktrace.pathHead)
                currentBacktrace.edges[currentNode.parent] = currentBacktrace.pathHead
                currentNode = currentNode.parent
            }
        }

        //Note, setting currentTargetPath here as either the previous target or the new backtrace
        return if (currentBacktrace.pathHead.node.state == rootState || currentBacktrace.pathHead.node.state == sourceState) {
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
    private fun findNewPath(bestPath : PathTrace<StateType>, currentAgentNode : TBANode<StateType>)
            : List<RealTimePlanner.ActionBundle>? {
        // Find common ancestor
        var sourceOnPath = bestPath.edges.containsKey(currentAgentNode)

        targetPath = bestPath

        return when {
            sourceOnPath -> {
                val edge = bestPath.edges[currentAgentNode]!!

                if(edge.next == null) {
                    //reset trace and start backtracking
                    traceInProgress = null
                    targetPath = PathTrace(currentAgentNode.parent)
                    backtrack(currentAgentNode)
                } else {
                    bestPath.pathHead = edge.next
                    listOf(RealTimePlanner.ActionBundle(bestPath.pathHead.action, bestPath.pathHead.cost))
                }
            }
            currentAgentNode.state == rootState -> null
            else -> backtrack(currentAgentNode)
        }
    }

    private fun updateHeuristics(): Unit = TODO()

    private fun getRootNode(state: StateType): TBANode<StateType> {
        generatedNodeCount++
        val node = TBANode(
                state = state,
                heuristic = domain.heuristic(state) * weight,
                actionCost = 0,
                action = NoOperationAction,
                cost = 0,
                iteration = 0)
        node.parent = node

        printTime("Hash table execution") {
            nodes[state] = node
        }
        return node
    }

    /**
     * Get a node for the state if exists, else create a new node.
     *
     * @return node corresponding to the given state.
     */
    override fun getNode(parent: TBANode<StateType>, successor: SuccessorBundle<StateType>): TBANode<StateType> {
        val successorState = successor.state
        val tempSuccessorNode = nodes[successorState]

        return if (tempSuccessorNode == null) {
            generatedNodeCount++

            val undiscoveredNode = TBANode(
                    state = successorState,
                    heuristic = domain.heuristic(successorState) * weight,
                    actionCost = successor.actionCost.toLong(),
                    action = successor.action,
                    parent = parent,
                    cost = Long.MAX_VALUE,
                    iteration = iterationCounter
            )

            nodes[successorState] = undiscoveredNode

            undiscoveredNode
        } else {
            tempSuccessorNode!!
        }
    }

    private fun backtrack(currentNode: TBANode<StateType>,
                          nextNode: TBANode<StateType> = currentNode.parent): List<ActionBundle> {
        val transition = domain.transition(currentNode.state, nextNode.state)

        if (transition == null) {
            return restart(currentNode)


        } else {
            return listOf(ActionBundle(transition.first, transition.second.toLong()))
        }
    }

    private fun restart(currentNode: TBANode<StateType>) : List<ActionBundle> {
        incrementCounter(RESTARTS)

        // do 1-step lookahead
        val bestSuccessor = domain.successors(currentNode.state).minBy {
            val successor = getNode(currentNode, it)
            successor.heuristic
        } ?: throw GoalNotReachableException("Reached dead end")

        // move open list to update reference, create new primary open list
        // open lists probably have to alternate b/c  we only have so many simultaneous indices
        // only 1 open list can be searching, one updating at a time (b/c of indices)
        // TODO: write function that will update heuristic values if updated open is populated

        throw UnsupportedOperationException("Not finished Yet")

        return listOf(ActionBundle(bestSuccessor.action, bestSuccessor.actionCost.toLong()))
    }

    @Suppress("UNUSED_PARAMETER")
    private inline fun printTime(msg: String, noinline fn: () -> Unit){
        fn()
//        printNanoTime(msg, fn)
    }
}

enum class TBAOptimization {
    NONE, SHORTCUT, THRESHOLD
}

enum class TBAStarConfiguration(val key: String) {
    TBA_OPTIMIZATION("tbaOptimization"),
    BACKUP_RATIO("backupRatio");

    override fun toString(): String = key
}

