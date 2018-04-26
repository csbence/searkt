package edu.unh.cs.ai.realtimesearch.planner.realtime

import edu.unh.cs.ai.realtimesearch.MetronomeException
import edu.unh.cs.ai.realtimesearch.environment.Domain
import edu.unh.cs.ai.realtimesearch.environment.NoOperationAction
import edu.unh.cs.ai.realtimesearch.environment.State
import edu.unh.cs.ai.realtimesearch.environment.SuccessorBundle
import edu.unh.cs.ai.realtimesearch.experiment.configuration.ExperimentConfiguration
import edu.unh.cs.ai.realtimesearch.experiment.terminationCheckers.FakeTerminationChecker
import edu.unh.cs.ai.realtimesearch.experiment.terminationCheckers.TerminationChecker
import edu.unh.cs.ai.realtimesearch.planner.*
import edu.unh.cs.ai.realtimesearch.planner.exception.GoalNotReachableException
import edu.unh.cs.ai.realtimesearch.util.AdvancedPriorityQueue
import edu.unh.cs.ai.realtimesearch.util.generateWhile
import edu.unh.cs.ai.realtimesearch.util.resize
import java.util.*
import kotlin.system.measureTimeMillis

/**
 * @author Bence Cserna (bence@cserna.net)
 */
class TimeBoundedAStar<StateType : State<StateType>>(override val domain: Domain<StateType>, val configuration: ExperimentConfiguration) : RealTimePlanner<StateType>(), RealTimePlannerContext<StateType, PureRealTimeSearchNode<StateType>> {

    // Configuration
    override var iterationCounter = 0L

    private val nodes: HashMap<StateType, PureRealTimeSearchNode<StateType>> = HashMap<StateType, PureRealTimeSearchNode<StateType>>(100000000).resize()

    // LSS stores heuristic values. Use those, but initialize them according to the domain heuristic
    // The cost values are initialized to infinity
    override var openList = AdvancedPriorityQueue<PureRealTimeSearchNode<StateType>>(10000000, fValueComparator)

    private var rootState: StateType? = null

    private var aStarPopCounter = 0

    var aStarTimer = 0L

    var lastSafeNode: PureRealTimeSearchNode<StateType>? = null

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
    override fun selectAction(sourceState: StateType, terminationChecker: TerminationChecker): List<RealTimePlanner.ActionBundle> {
        // Initiate for the first search

        if (rootState == null) {
            rootState = sourceState
            val rootNode = getNodeAsRoot(sourceState)
            openList.add(rootNode)
        }

        val currentAgentState = nodes[sourceState]
                ?: throw MetronomeException("Agent is at an unknown state. It is unlikely that the planner sent the agent to an undiscovered node.")

        if (domain.isGoal(sourceState)) {
            // The start sourceState is the goal sourceState
            return emptyList()
        }

        // Exploration phase
        var plan: List<RealTimePlanner.ActionBundle>? = null
        aStarTimer += measureTimeMillis {
            val topOfOpen = openList.peek() ?: throw GoalNotReachableException()

            val targetNode = if (domain.isGoal(topOfOpen.state)) {
                topOfOpen // Stick to the goal if we already found it
            } else {
                aStar(FakeTerminationChecker)
            }

            val rootToBestChain = extractNodeChain(targetNode, { it == rootState!! }).map { it.state }.toSet()

            plan = if (rootToBestChain.contains(currentAgentState.state)) {
                // Move agent to target
                extractPath(targetNode, sourceState)
            } else {
                // Find common ancestor
                val commonAncesterToAgent = extractNodeChain(targetNode, { rootToBestChain.contains(it) })

                extractPath(targetNode, sourceState)
            }

        }

        return plan!!
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

        return openList.peek() ?: throw GoalNotReachableException("Open list is empty.")
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

