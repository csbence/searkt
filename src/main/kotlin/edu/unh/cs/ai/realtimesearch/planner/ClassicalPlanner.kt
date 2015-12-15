package edu.unh.cs.ai.realtimesearch.planner

import edu.unh.cs.ai.realtimesearch.environment.Action
import edu.unh.cs.ai.realtimesearch.environment.Domain
import edu.unh.cs.ai.realtimesearch.environment.State

/**
 * The abstract class for classical planners. Assume fully observable, deterministic nature.
 *
 * Possible derivatives of this class are depthfirst search, A* etc.
 *
 * @param domain is the domain to plan in
 * @author Bence Cserna (bence@cserna.net)
 */
abstract class ClassicalPlanner(protected val domain: Domain) : Planner {

    public var generatedNodes = 0
    public var expandedNodes = 0

    data class Node(val parent: Node?, val state: State,
                    val action: Action?, val cost: Double)

    /** Interface3 functions **/

    /**
     * Resets all variables in the planner. Called before a new planning task
     */
    open protected fun initiatePlan() {
        generatedNodes = 0
    }

    /**
     * Checks whether a state has been visited before from current node.
     *
     * @param state is the current state that is being visited
     * @param leave is node at the end of the current path
     * @return whether state has been visited
     */
    protected abstract fun visitedBefore(state: State, leave: Node): Boolean

    /**
     * Add node to open list
     *
     * @param node to add
     */
    protected abstract fun generateNode(node: Node)

    /**
     * Pops a node from the open list
     *
     * @return the next node to expand
     */
    protected abstract fun popFromOpenList(): Node

    /**
     * Returns a plan for a given initial state. A plan consists of a list of actions
     *
     * @param state is the initial state
     * @return a list of action compromising the plan
     */
    fun plan(state: State): List<Action> {
        // get ready / reset for plan
        reset()

        // main loop
        var currentNode = Node(null, state, null, 0.0)
        while (!domain.isGoal(currentNode.state)) {
            currentNode = expandNode(currentNode)
        }

        return extractPlan(currentNode)
    }

    /**
     * Expands single node and generates the sucessor nodes
     *
     * @param node is the node to expand
     * @return the next node of interest
     */
    public fun expandNode(node: Node): Node {
        expandedNodes += 1

        // expand (only those not visited yet)
        for (successor in domain.successors(node.state)) {
            if (!visitedBefore(successor.state, node)) {
                generatedNodes += 1

                // generate the node with correct cost
                val nodeCost = successor.cost + node.cost
                generateNode(Node(node, successor.state,
                        successor.action, nodeCost))
            }
        }

        return popFromOpenList()
    }

    /**
     * @brief Returns the actions necessary to get to node
     *
     * @param leave the current end of the path
     * @return list of actions to get to leave
     */
    protected fun extractPlan(leave: Node): List<Action> {
        val actions: MutableList<Action> = arrayListOf()

        var node = leave
        // root will have null action. So as long as the parent
        // is not null, we can take it's action and assume all is good
        while (node.parent != null) {
            actions.add(node.action!!)
            node = node.parent!!
        }

        return actions.reversed() // we are adding actions in wrong order, to return the reverser
    }

    /**
     * Forcefully resets the planner to initial state, with no prior search
     * history or generated node.
     */
    public fun reset() {
        generatedNodes = 0
        expandedNodes = 0
        initiatePlan()
    }
}