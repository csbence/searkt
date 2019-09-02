package edu.unh.cs.searkt.planner.realtime

import edu.unh.cs.searkt.MetronomeConfigurationException
import edu.unh.cs.searkt.MetronomeException
import edu.unh.cs.searkt.environment.*
import edu.unh.cs.searkt.experiment.configuration.ExperimentConfiguration
import edu.unh.cs.searkt.experiment.configuration.realtime.TerminationType
import edu.unh.cs.searkt.experiment.measure
import edu.unh.cs.searkt.experiment.result.ExperimentResult
import edu.unh.cs.searkt.experiment.terminationCheckers.TerminationChecker
import edu.unh.cs.searkt.experiment.terminationCheckers.getTerminationChecker
import edu.unh.cs.searkt.planner.*
import edu.unh.cs.searkt.planner.exception.GoalNotReachableException
import edu.unh.cs.searkt.planner.realtime.TBAOptimization.*
import edu.unh.cs.searkt.util.AbstractAdvancedPriorityQueue
import edu.unh.cs.searkt.util.resize
import kotlinx.serialization.ImplicitReflectionSerializer
import java.util.*
import kotlin.math.roundToLong
import kotlin.system.measureNanoTime
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
 * @history 7/2/2019 Kevin C. Gall Refactoring as Restarting TBA* for directed domains. Refactor not engineered
 * for real time bounds
 * @history 8/20/19 Kevin C. Gall Implementing different "Shortcut" optimization than described in the paper. This
 * one is time bounded and based on I-ES
 */
class TimeBoundedAStar<StateType : State<StateType>>(override val domain: Domain<StateType>, val configuration: ExperimentConfiguration) :
        RealTimePlanner<StateType>(),
        RealTimePlannerContext<StateType, TimeBoundedAStar.TBANode<StateType>> {

    // Attribute Keys
    private val RESTARTS = "restarts"
    private val GOAL_FOUND_ITERATION = "goalPathExtracted"
    private val BACKTRACKS = "backtracks"
    private val SHORTCUTS_FOUND = "shortcutsFound"
    private val TRACEBACK_COUNTER = "tracebacks"
    private var tracebacks = 0L
    private val TRACEBACK_TIMER = "tracebackTime"
    private var tracebackTime = 0L
    private val EXPANDED_NODE_TIME = "expandedNodeTime"
    private var expandedNodeTime = 0L

    // Configuration
    private val tbaOptimization = configuration.tbaOptimization
            ?: throw MetronomeConfigurationException("TBA* optimization is not specified")
    private val strategy = configuration.lookaheadStrategy ?: LookaheadStrategy.A_STAR
    private val weight = configuration.weight ?: 1.0
    private val shortcutRatio = 0.1 // ratio of time to spend on shortcutting when applicable

    //HARD CODED for testing. Should be configurable
    /** cost of backtrace relative to expansion as expansion cost / backtrace cost. Lower number means backtrace is more costly */
    private val traceCost = 5 //number tuned on Lubuntu 18.04 w/ low-latency kernel, OpenJ9 JVM, Intel i7-4770 3.4GHz (4 core)
    /** ratio of tracebacks to expansions */
    private val backupRatio = configuration.backupRatio ?: 1.0

    // This variable is over-used and not actually reflective of the iteration count :(
    // roll another custom one
    override var iterationCounter = 0L
    private var realIterationCounter = 0L

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
        var secondaryOpenListVersion = 0L
        var secondaryClosed: Boolean = false

        override var index: Int = -1
        var primaryOpenListVersion = 0L
        override var closed = false

        // Need all different attributes for shortcuts b/c restarting TBA* requires us to maintain other information
        var shortcutIndex: Int = -1
        var shortcutOpenListVersion = 0L
        var shortcutClosed: Boolean = false
        var shortcutCost: Long = 0L
        var shortcutHeuristic: Double = 0.0
        var shortcutParent: TBANode<StateType>? = null
        var shortcutAction: Action? = null
        var shortcutActionCost: Long = -1L
        /** This one is so that we can backtrack along our shortcut branch if we have to switch paths yet again */
        var shortcutBacktrackParent: TBANode<StateType>? = null

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

    private val weightedFComparator = Comparator<TBANode<StateType>> { lhs, rhs ->
        val lhsF = (lhs.heuristic * weight) + lhs.cost
        val rhsF = (rhs.heuristic * weight) + rhs.cost

        // break ties on low H
        when {
            lhsF < rhsF -> -1
            lhsF > rhsF -> 1
            lhs.heuristic < rhs.heuristic -> -1
            lhs.heuristic > rhs.heuristic -> 1
            else -> 0
        }
    }
    private val heuristicComparator = Comparator<TBANode<StateType>> { lhs, rhs ->
        when {
            lhs.heuristic < rhs.heuristic -> -1
            lhs.heuristic > rhs.heuristic -> 1
            else -> 0
        }
    }
    private val weightedFShortcutComparator = Comparator<TBANode<StateType>> { lhs, rhs ->
        val lhsF = (lhs.shortcutHeuristic * weight) + lhs.shortcutCost
        val rhsF = (rhs.shortcutHeuristic * weight) + rhs.shortcutCost

        // break ties on low H
        when {
            lhsF < rhsF -> -1
            lhsF > rhsF -> 1
            lhs.shortcutHeuristic < rhs.shortcutHeuristic -> -1
            lhs.shortcutHeuristic > rhs.shortcutHeuristic -> 1
            else -> 0
        }
    }
    private val heuristicShortcutComparator = Comparator<TBANode<StateType>> { lhs, rhs ->
        when {
            lhs.shortcutHeuristic < rhs.shortcutHeuristic -> -1
            lhs.shortcutHeuristic > rhs.shortcutHeuristic -> 1
            else -> 0
        }
    }

    inner class PrimaryOpenList(comparator: Comparator<in TBANode<StateType>>)
        : AbstractAdvancedPriorityQueue<TBANode<StateType>>(arrayOfNulls(1000000), comparator) {
        var version = 0L

        override fun add(item: TBANode<StateType>) {
            super.add(item)
            setClosed(item, false)
            item.primaryOpenListVersion = version
        }

        override fun getIndex(item: TBANode<StateType>): Int = item.index
        override fun setIndex(item: TBANode<StateType>, index: Int) {
            item.primaryOpenListVersion = version
            item.index = index
        }

        override fun setClosed(item: TBANode<StateType>, newValue: Boolean) {
            item.closed = newValue
        }

        override fun isClosed(item: TBANode<StateType>): Boolean = item.closed && item.primaryOpenListVersion == version

        override fun isOpen(item: TBANode<StateType>): Boolean {
            return super.isOpen(item) && item.primaryOpenListVersion == version
        }
    }

    inner class SecondaryOpenList(comparator: Comparator<in TBANode<StateType>>)
            : AbstractAdvancedPriorityQueue<TBANode<StateType>>(arrayOfNulls(1000000), comparator) {
        var version = 0L

        override fun add(item: TBANode<StateType>) {
            super.add(item)
            setClosed(item, false)
            item.secondaryOpenListVersion = version
        }

        override fun getIndex(item: TBANode<StateType>): Int = item.secondaryIndex
        override fun setIndex(item: TBANode<StateType>, index: Int) {
            item.secondaryIndex = index
        }

        override fun setClosed(item: TBANode<StateType>, newValue: Boolean) {
            item.secondaryClosed = newValue
        }

        override fun isClosed(item: TBANode<StateType>): Boolean = item.secondaryClosed && item.secondaryOpenListVersion == version
        override fun isOpen(item: TBANode<StateType>): Boolean {
            return super.isOpen(item) && item.secondaryOpenListVersion == version
        }
    }

    inner class ShortcutOpenList(comparator: Comparator<in TBANode<StateType>>)
        : AbstractAdvancedPriorityQueue<TBANode<StateType>>(arrayOfNulls(1000000), comparator) {
        var version = 0L

        override fun add(item: TBANode<StateType>) {
            super.add(item)
            setClosed(item, false)
            item.shortcutOpenListVersion = version
        }

        override fun getIndex(item: TBANode<StateType>): Int = item.shortcutIndex
        override fun setIndex(item: TBANode<StateType>, index: Int) {
            item.shortcutIndex = index
        }

        override fun setClosed(item: TBANode<StateType>, newValue: Boolean) {
            item.shortcutClosed = newValue
        }

        override fun isClosed(item: TBANode<StateType>): Boolean = item.shortcutClosed && item.shortcutOpenListVersion == version
        override fun isOpen(item: TBANode<StateType>): Boolean {
            return super.isOpen(item) && item.shortcutOpenListVersion == version
        }
    }

    private var openIsPrimary = true
    private val primaryOpen = PrimaryOpenList(weightedFComparator)
    private val secondaryOpen = SecondaryOpenList(weightedFComparator)
    /**
     * separate open list for shortcuts because of restarting TBA* - it uses the prior open list for
     * its heuristic update, so we cannot reuse it after the goal has been found
     */
    private val shortcutOpen = ShortcutOpenList(when (strategy) {
        LookaheadStrategy.GBFS -> heuristicShortcutComparator
        LookaheadStrategy.A_STAR -> weightedFShortcutComparator
        LookaheadStrategy.PSEUDO_F -> throw MetronomeException("Unsupported lookahead type in TBA*")
    })

    override var openList = getNextOpenList(false)
    private var updateOpenList: AbstractAdvancedPriorityQueue<TBANode<StateType>>? = null
    private var updateIsFresh = false


    private var rootState: StateType? = null
    private var lastAgentState: StateType? = null
    private var foundGoal : Boolean = false
    private var goalFoundIteration: Long = -1L

    private var aStarPopCounter = 0
    private var expansionLimit = 0L

    data class PathEdge<StateType : State<StateType>>(
            val node: TBANode<StateType>, var next: PathEdge<StateType>?, val action: Action = node.action, val cost: Long = node.actionCost
    )
    data class PathTrace<StateType : State<StateType>>(
            val pathEnd: PathEdge<StateType>,
            var edges : MutableMap<TBANode<StateType>, PathEdge<StateType>> = mutableMapOf()) {
        val goal = pathEnd.node
        var pathHead = pathEnd

        constructor(goal: TBANode<StateType>): this(PathEdge(goal, null))

        init {edges[pathEnd.node] = pathEnd}
    }

    private var traceInProgress : PathTrace<StateType>? = null
    private var targetPath : PathTrace<StateType>? = null
    private var shortcutGoal: TBANode<StateType>? = null
    private var goalShortcutFound = false

    var aStarTimer = 0L

    private inline fun goalIsTraced() = foundGoal && traceInProgress == null && domain.isGoal(targetPath?.pathEnd?.node?.state ?: rootState!!)

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
        results.attributes[RESTARTS] = this.counters[RESTARTS]?.toLong() ?: 0L
        results.attributes[BACKTRACKS] = this.counters[BACKTRACKS]?.toLong() ?: 0L
        results.attributes[GOAL_FOUND_ITERATION] = goalFoundIteration
        results.attributes[SHORTCUTS_FOUND] = this.counters[SHORTCUTS_FOUND]?.toLong() ?: 0L

        results.attributes[EXPANDED_NODE_TIME] = expandedNodeTime
        results.attributes[TRACEBACK_COUNTER] = tracebacks
        results.attributes[TRACEBACK_TIMER] = tracebackTime
    }

    private fun getNextOpenList(switchOpenList: Boolean) : AbstractAdvancedPriorityQueue<TBANode<StateType>> {
        val comparator = (if (strategy == LookaheadStrategy.GBFS) heuristicComparator else weightedFComparator)

        if (switchOpenList) openIsPrimary = !openIsPrimary

        return if (openIsPrimary) {
            primaryOpen.clear()
            primaryOpen.reorder(comparator)
            primaryOpen.version++
            primaryOpen
        } else {
            secondaryOpen.clear()
            secondaryOpen.reorder(comparator)
            secondaryOpen.version++
            secondaryOpen
        }
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
    override fun selectAction(sourceState: StateType, terminationChecker: TerminationChecker): List<ActionBundle> {
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
        // NOTE: this accounting IS NOT real time - only Expansion-based time bounds
        val updateTerminationChecker = getTerminationChecker(configuration, terminationChecker.remaining())

        val ratio = if (goalIsTraced()) 1.0 else shortcutRatio
        val shortcutTime = (terminationChecker.remaining() * ratio).roundToLong()

        val (explorationTerminationChecker, shortcutTerminationChecker) =
                if ((tbaOptimization != SHORTCUT && tbaOptimization != BOTH) || shortcutOpen.isEmpty()) {
                    Pair(terminationChecker, getTerminationChecker(configuration, 0L))
                } else {
                    Pair(
                        getTerminationChecker(configuration, terminationChecker.remaining() - shortcutTime),
                        getTerminationChecker(configuration, shortcutTime)
                    )
                }

        if (domain.isGoal(sourceState)) {
            // The start sourceState is the goal sourceState
            return emptyList()
        }

        var plan: List<ActionBundle>? = null
        aStarTimer += measureTimeMillis {
            var currentTargetPath = getCurrentPath(sourceState, explorationTerminationChecker)
            currentTargetPath = when(tbaOptimization) {
                NONE, SHORTCUT -> currentTargetPath
                THRESHOLD, BOTH -> {
                    when {
                        targetPath == null -> currentTargetPath
                        //check if the new path has at least as high a g value as the current path.
                        //Exception is if the agent is currently backtracing: make the new current
                        //best the target path
                        currentTargetPath.pathEnd.node.cost >= targetPath!!.pathEnd.node.cost -> currentTargetPath
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
                    findNewPath(currentTargetPath, currentAgentNode, shortcutTerminationChecker)
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

        if (updateOpenList != null) updateHeuristics(updateTerminationChecker)

        if (goalFoundIteration == -1L && goalIsTraced()) {
            goalFoundIteration = realIterationCounter
        }

        realIterationCounter++
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

        expandedNodeTime += measureNanoTime {
            while (!terminationChecker.reachedTermination() && !domain.isGoal(openList.peek()?.state
                            ?: throw GoalNotReachableException("Open list is empty."))) {
                aStarPopCounter++

                val currentNode = openList.pop() ?: throw GoalNotReachableException("Open list is empty.")

                expandFromNode(this, currentNode, checkOutdated = {!openList.isOpen(it) && !openList.isClosed(it)})

                terminationChecker.notifyExpansion()
                currentExpansionDuration++
            }
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

        val topOfOpen = openList.peek() ?: throw GoalNotReachableException("Open list is empty - no target to compare with")

        if (goalIsTraced()) {
            return targetPath ?: throw MetronomeException("Goal reached but no target path?")
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

        tracebackTime += measureNanoTime {
            while (!traceBound() && currentNode.state != rootState && currentNode.state != sourceState) {
                currentBacktrace.pathHead =
                        PathEdge(currentNode.parent, currentBacktrace.pathHead)
                currentBacktrace.edges[currentNode.parent] = currentBacktrace.pathHead
                currentNode = currentNode.parent

                tracebacks++
            }
        }

        //Note, setting currentTargetPath here as either the previous target or the new backtrace
        return if (currentBacktrace.pathHead.node.state == rootState || currentBacktrace.pathHead.node.state == sourceState) {
            traceInProgress = null

            // only kill the shortcut if the shortcut goal is not on the traced path
            if (!currentBacktrace.edges.containsKey(shortcutGoal)) {
                shortcutOpen.clear()
                shortcutGoal = null
            }
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
    private fun findNewPath(bestPath : PathTrace<StateType>, currentAgentNode : TBANode<StateType>,
                            terminationChecker: TerminationChecker)
            : List<ActionBundle>? {
        // Find common ancestor
        var sourceOnPath = bestPath.edges.containsKey(currentAgentNode)

        // The "Shortcut" optimization
        val shortcutPath = if (!sourceOnPath && (tbaOptimization == SHORTCUT || tbaOptimization == BOTH)) {
            findShortcut(bestPath, currentAgentNode, terminationChecker)
        } else null
        targetPath = if (shortcutPath != null) {
            sourceOnPath = true
            shortcutPath
        } else bestPath

        return when {
            sourceOnPath -> {
                // kill shortcuts: we're on the path!
                shortcutOpen.clear()
                shortcutGoal = null

                val currentPath = targetPath!!

                val edge = currentPath.edges[currentAgentNode]!!

                if(edge.next == null) {
                    //reset trace and start backtracking
                    traceInProgress = null
                    targetPath = PathTrace(currentAgentNode.parent)
                    backtrack(currentAgentNode)
                } else {
                    currentPath.pathHead = edge.next!!
                    listOf(ActionBundle(currentPath.pathHead.action, currentPath.pathHead.cost))
                }
            }
            currentAgentNode.state == rootState -> null
            else -> backtrack(currentAgentNode)
        }
    }

    private fun updateHeuristics(terminationChecker: TerminationChecker) {
        dynamicDijkstra(this,
                updateOpenList ?: throw MetronomeException("Cannot update from no open list"),
                updateIsFresh,
                reachedTermination = {
                    val finished = it.isEmpty() || terminationChecker.reachedTermination()
                    terminationChecker.notifyExpansion()

                    finished
                })

        updateIsFresh = false
    }

    /**
     * Seeds shortcut open with all target state. Attempts to find a shortcut using the agent's current state
     * as the heuristic target
     */
    private fun findShortcut(bestPath : PathTrace<StateType>, currentAgentNode : TBANode<StateType>, terminationChecker: TerminationChecker)
            : PathTrace<StateType>? {
        if (shortcutOpen.isEmpty()) {
            shortcutOpen.version++
            val seedNode = bestPath.pathEnd.node
            seedNode.shortcutCost = 0
            seedNode.shortcutAction = null
            seedNode.shortcutActionCost = -1
            seedNode.shortcutParent = null
            seedNode.shortcutHeuristic = domain.heuristic(seedNode.state, currentAgentNode.state)
            shortcutOpen.add(seedNode)

            shortcutGoal = seedNode
        }

        var shortcutFound = false
        while (!terminationChecker.reachedTermination() && shortcutOpen.isNotEmpty()) {
            val currentNode = shortcutOpen.pop() ?: throw MetronomeException("Shortcut Open should not be empty")

            // goal check
            if (currentNode == currentAgentNode) {
                shortcutFound = true
                break
            }

            // Only expand generated predecessors
            for (predecessorEdge in currentNode.predecessors) {
            //for (predecessorBundle in domain.predecessors(currentNode.state)) {
                val predecessorNode = predecessorEdge.node

                // check if it's outdated
                if (!shortcutOpen.isClosed(predecessorNode) && !shortcutOpen.isOpen(predecessorNode)) {
                    predecessorNode.shortcutCost = Long.MAX_VALUE
                    predecessorNode.shortcutHeuristic = domain.heuristic(predecessorNode.state, currentAgentNode.state)
                }

                val successorG = currentNode.shortcutCost + predecessorNode.actionCost
                if (predecessorNode.shortcutCost > successorG) {
                    // Must use separate instance vars for shortcut search so that we don't re-wire the tree
                    predecessorNode.apply {
                        shortcutCost = successorG.toLong()
                        shortcutParent = currentNode
                        shortcutAction = predecessorNode.action
                        shortcutActionCost = predecessorNode.actionCost.toLong()
                    }

                    if (!shortcutOpen.isOpen(predecessorNode)) {
                        shortcutOpen.add(predecessorNode)
                    } else {
                        shortcutOpen.update(predecessorNode)
                    }

                }
            }

            terminationChecker.notifyExpansion()
            shortcutOpen.setClosed(currentNode, true)
            expandedNodeCount++
        }

        // Redo path extraction - extract to target
        return if (shortcutFound) {
            this.incrementCounter(SHORTCUTS_FOUND)
            if (domain.isGoal(shortcutGoal!!.state)) {
                goalShortcutFound = true
                goalFoundIteration = realIterationCounter
                shortcutOpen.clear()
            }

            val pathHead = PathEdge(currentAgentNode, null)
            val edgeMap = mutableMapOf(currentAgentNode to pathHead)

            // The PathEdge cost and action variables hold the values that it takes to transition
            // to that node from its predecessor in the trace. Things are inverted in the shortcut
            // search, so we need to track the predecessor too
            var currentTracePredecessor = currentAgentNode
            var currentTraceNode = currentAgentNode.shortcutParent ?:
                throw MetronomeException("Invalid shortcut. Not terminated at agent node")
            var currentTraceEdge = pathHead

            var foundTarget = false
            while (!bestPath.edges.containsKey(currentTracePredecessor)) {
                val newEdge = PathEdge(
                        currentTraceNode,
                        null,
                        currentTracePredecessor.shortcutAction ?: throw MetronomeException("No shortcut action available"),
                        currentTracePredecessor.shortcutActionCost)
                edgeMap[currentTraceNode] = newEdge
                currentTraceEdge.next = newEdge

                currentTraceNode.shortcutBacktrackParent = currentTracePredecessor

                currentTracePredecessor = currentTraceNode
                currentTraceEdge = newEdge
                if (currentTraceNode.shortcutParent == null) {
                    foundTarget = true
                    break
                } else {
                    currentTraceNode = currentTraceNode.shortcutParent ?: throw MetronomeException("Bug - can't be null...")
                }
            }

            // We check that we found our target and that it's the end of the current best path.
            // recall that we allow the shortcut to not switch targets when the prior target is
            // still on the path to the best node
            val shortcutTrace = if (foundTarget && bestPath.pathEnd.node == currentTraceNode) {
                PathTrace(currentTraceEdge, edgeMap)
            } else {
                var targetPathEdge = bestPath.edges[currentTracePredecessor]
                assert(currentTraceEdge.node == targetPathEdge?.node)
                currentTraceEdge.next = targetPathEdge?.next ?: throw MetronomeException("Bug - shortcut found but not to goal?")

                targetPathEdge = currentTraceEdge.next

                // still trace through the remaining path to put them in the new edge map to maintain the
                // invariant that the edge hashmap has all nodes to edges
                while (targetPathEdge != null) {
                    edgeMap[targetPathEdge.node] = targetPathEdge
                    targetPathEdge = targetPathEdge.next
                }

                PathTrace(bestPath.pathEnd, edgeMap)
            }
            shortcutTrace.pathHead = pathHead

            shortcutTrace
        } else null
    }

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
    override fun getNode(parent: TBANode<StateType>, successor: SuccessorBundle<StateType>): TBANode<StateType> = getNodeInner(parent, successor)
    private fun getNode(successor: SuccessorBundle<StateType>): TBANode<StateType> = getNodeInner(null, successor)
    private fun getNodeInner(parent: TBANode<StateType>?, successor: SuccessorBundle<StateType>): TBANode<StateType> {
        val successorState = successor.state
        val tempSuccessorNode = nodes[successorState]

        return if (tempSuccessorNode == null) {
            generatedNodeCount++

            val undiscoveredNode = TBANode(
                    state = successorState,
                    heuristic = domain.heuristic(successorState),
                    actionCost = successor.actionCost.toLong(),
                    action = successor.action,
                    parent = parent,
                    cost = Long.MAX_VALUE,
                    iteration = iterationCounter
            )

            nodes[successorState] = undiscoveredNode

            undiscoveredNode
        } else {
            tempSuccessorNode
        }
    }

    private fun backtrack(currentNode: TBANode<StateType>,
                          nextNode: TBANode<StateType> = currentNode.parent): List<ActionBundle> {
        // We prefer following parent pointers of the tree.
        // But we handle the edge case that we have to backtrack while on a shortcut path
        // whose nodes had not otherwise been touched by the search
        val backtrackTarget = if (currentNode == nextNode) { // i.e. no parent - shortcut!
            currentNode.shortcutBacktrackParent ?: throw MetronomeException("No parent?")
        } else {
            nextNode
        }

        val transition = domain.transition(currentNode.state, backtrackTarget.state)

        return if (transition == null) {
            restart(currentNode)
        } else {
            this.incrementCounter(BACKTRACKS)
            listOf(ActionBundle(transition.first, transition.second.toLong()))
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
        var switchOpenList = false
        if (updateOpenList == null) {
            switchOpenList = true
            updateIsFresh = true
            updateOpenList = openList
        }
        openList = getNextOpenList(switchOpenList)

        // If we were in the midst of finding shortcuts, clear
        if (shortcutOpen.isNotEmpty()) {
            shortcutOpen.clear()
            shortcutGoal = null
        }

        // Reset global vars to reflect new search
        val newSeed = getNode(currentNode, bestSuccessor)
        newSeed.apply {
            parent = this
            cost = 0
            actionCost = 0
            action = NoOperationAction
        }
        rootState = newSeed.state
        foundGoal = false
        targetPath = null
        traceInProgress = null

        openList.add(newSeed) // seed open list with the next state we will reach

        return listOf(ActionBundle(bestSuccessor.action, bestSuccessor.actionCost.toLong()))
    }

    @Suppress("UNUSED_PARAMETER")
    private inline fun printTime(msg: String, noinline fn: () -> Unit){
        fn()
//        printNanoTime(msg, fn)
    }
}

enum class TBAOptimization {
    NONE, SHORTCUT, THRESHOLD, BOTH
}

enum class TBAStarConfiguration(val key: String) {
    TBA_OPTIMIZATION("tbaOptimization"),
    BACKUP_RATIO("backupRatio");

    override fun toString(): String = key
}

