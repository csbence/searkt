package edu.unh.cs.ai.realtimesearch.planner.realtime

import edu.unh.cs.ai.realtimesearch.MetronomeConfigurationException
import edu.unh.cs.ai.realtimesearch.MetronomeException
import edu.unh.cs.ai.realtimesearch.environment.Domain
import edu.unh.cs.ai.realtimesearch.environment.NoOperationAction
import edu.unh.cs.ai.realtimesearch.environment.State
import edu.unh.cs.ai.realtimesearch.environment.SuccessorBundle
import edu.unh.cs.ai.realtimesearch.experiment.configuration.ExperimentConfiguration
import edu.unh.cs.ai.realtimesearch.experiment.terminationCheckers.TerminationChecker
import edu.unh.cs.ai.realtimesearch.planner.*
import edu.unh.cs.ai.realtimesearch.planner.exception.GoalNotReachableException
import edu.unh.cs.ai.realtimesearch.planner.realtime.TBAOptimization.NONE
import edu.unh.cs.ai.realtimesearch.planner.realtime.TBAOptimization.SHORTCUT
import edu.unh.cs.ai.realtimesearch.util.AdvancedPriorityQueue
import edu.unh.cs.ai.realtimesearch.util.generateWhile
import edu.unh.cs.ai.realtimesearch.util.resize
import java.util.*
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
    private val traceCost = 10 //cost of backtrace relative to expansion

    private val resourceRatio = 0.9 //ratio of time for Expansions

    override var iterationCounter = 0L

    private val nodes: HashMap<StateType, PureRealTimeSearchNode<StateType>> = HashMap<StateType, PureRealTimeSearchNode<StateType>>(100000000).resize()

    // LSS stores heuristic values. Use those, but initialize them according to the domain heuristic
    // The cost values are initialized to infinity
    override var openList = AdvancedPriorityQueue<PureRealTimeSearchNode<StateType>>(10000000, fValueComparator)

    private var rootState: StateType? = null
    private var lastAgentState: StateType? = null
    private var foundGoal : Boolean = false

    private var aStarPopCounter = 0
    private var expansionLimit = 0L

    //Relevant Path lists
    private var traceInProgress : MutableList<PureRealTimeSearchNode<StateType>>? = null
    private var targetPath : MutableList<PureRealTimeSearchNode<StateType>>? = null

    var aStarTimer = 0L

    private val aStarSequence
        get() = generateSequence {
            aStarPopCounter++

            val currentNode = openList.pop() ?: throw GoalNotReachableException("Open list is empty.")

            expandFromNode(this, currentNode, {})

            currentNode
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

        if (rootState == null) {
            rootState = sourceState
            val rootNode = getNodeAsRoot(sourceState)
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

//            println()
//            println(currentAgentNode)

            plan = if (currentTargetPath[0].state == sourceState) {
                if (currentTargetPath.size == 1) {
                    /* First handle the edge case where the agent has reached the end of its
                     * path while another path is being traced. Begin backtracing to root
                     */
                    targetPath = mutableListOf(currentAgentNode.parent)
                    if (currentAgentNode.state == rootState) null
                    else constructPath(listOf(currentAgentNode.state, currentAgentNode.parent.state), domain)
                } else {
                    /* The agent's current state is an ancestor of the current best
                     * Move agent to current best target
                     * Remove current state from the target path for the next iteration
                     */
                    targetPath = currentTargetPath.subList(1, currentTargetPath.size)
                    extractPath(currentTargetPath[currentTargetPath.lastIndex], sourceState)
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
                        if (currentTargetPath.last().cost >= priorTargetPath.last().cost
                            || (priorTargetPath.size == 1 && priorTargetPath[0] == currentAgentNode)) {
                            findNewPath(currentTargetPath, currentAgentNode)
                        } else {
                            findNewPath(priorTargetPath, currentAgentNode)
                        }
                    }
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
            constructPath(listOf(sourceState, targetNode.state), domain)
        } else {
            plan!!
        }
        lastAgentState = sourceState

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
                }

        if (expansionLimit == 0L) expansionLimit = currentExpansionDuration
        return openList.peek() ?: throw GoalNotReachableException("Open list is empty.")
    }

    /**
     * Get the next path to be followed. If best node has not been fully back traced,
     * return current target path
     */
    private fun getCurrentPath(sourceState : StateType, terminationChecker: TerminationChecker)
            : MutableList<PureRealTimeSearchNode<StateType>> {

        val topOfOpen = openList.peek() ?: throw GoalNotReachableException()
        //Goal found and traced
        if (foundGoal && traceInProgress == null && targetPath?.last() == topOfOpen) {
            return targetPath!!
        }

        val bestNode = if (domain.isGoal(topOfOpen.state)) {
            foundGoal = true
            topOfOpen
        } else {
            aStar(terminationChecker)
        }

        //if no traceback in progress, trace from best node. Otherwise pick up where we left off
        val targetNode = traceInProgress?.first() ?: bestNode

        //calculate backtrace here. Simply using expansion limit until we determine best way to compare with other algorithms
        //plus 2 because we pop from the frontier (not expanded) and the root is "free"
        val traceLimit = expansionLimit + 2
        var currentTraceCount = 0

        val bestNodeTraceback = extractNodeChain(targetNode, {
            currentTraceCount++
            currentTraceCount >= traceLimit || it == rootState!! || it == sourceState
        })

        val currentBacktrace = bestNodeTraceback.toMutableList()
        //adding previous trace to current trace. Cutting out first element of previous trace, as it will be duplicate
        currentBacktrace.addAll(traceInProgress?.subList(1, traceInProgress!!.size) ?: listOf())

        //Note, setting currentTargetPath here as either the previous target or the new backtrace
        return if (currentBacktrace[0].state == rootState || currentBacktrace[0].state == sourceState) {
            traceInProgress = null
            currentBacktrace
        } else {
            traceInProgress = currentBacktrace

            assert(targetPath != null)
            targetPath!!
        }
    }

    private fun findNewPath(bestPath : MutableList<PureRealTimeSearchNode<StateType>>, currentAgentNode : PureRealTimeSearchNode<StateType>) : List<RealTimePlanner.ActionBundle>? {
        // Find common ancestor
        val sourceState = currentAgentNode.state

        val sourceIndex = bestPath.indexOfFirst { it.state == sourceState }

        targetPath = bestPath
        if (sourceIndex > -1) {
            //Note: setting target path here
            targetPath = bestPath.subList(sourceIndex + 1, bestPath.size)
            return extractPath(bestPath[bestPath.lastIndex], sourceState)
        } else if (currentAgentNode.state == rootState) { //if we're at the root, there's no path!
            return null
        } else {
            //single step plan follows immediate back pointer
            return constructPath(listOf(currentAgentNode.state, currentAgentNode.parent.state), domain)
        }
    }

    private fun getUninitializedNode(state: StateType): PureRealTimeSearchNode<StateType> {
        val tempNode = nodes[state]

        return if (tempNode != null) {
            tempNode
        } else {
            generatedNodeCount++
            val node = PureRealTimeSearchNode(
                    state = state,
                    heuristic = domain.heuristic(state),
                    actionCost = 0,
                    action = NoOperationAction,
                    cost = Long.MAX_VALUE,
                    iteration = 0)

            nodes[state] = node
            node
        }
    }

    private fun getNodeAsRoot(state: StateType): PureRealTimeSearchNode<StateType> {

        val node = getUninitializedNode(state)
        node.apply {
            cost = 0
            actionCost = 0
            iteration = iterationCounter
            parent = node
            action = NoOperationAction
            predecessors.clear()
        }

        nodes[state] = node
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

            nodes[successorState] = undiscoveredNode
            undiscoveredNode
        } else {
            tempSuccessorNode
        }
    }
}

enum class TBAOptimization {
    NONE, SHORTCUT, THRESHOLD
}

enum class TBAStarConfiguration(val key: String) {
    TBA_OPTIMIZATION("tbaOptimization");

    override fun toString(): String = key
}

