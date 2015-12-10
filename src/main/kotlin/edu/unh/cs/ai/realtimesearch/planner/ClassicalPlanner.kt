package edu.unh.cs.ai.realtimesearch.planner

import edu.unh.cs.ai.realtimesearch.domain.Action
import edu.unh.cs.ai.realtimesearch.domain.Domain
import edu.unh.cs.ai.realtimesearch.domain.State
import edu.unh.cs.ai.realtimesearch.domain.SuccessorBundle

/**
 * The abstract class for classical planners. Assume fully observable, deterministic nature.
 *
 * Possible derivatives of this class are depthfirst search, A* etc.
 *
 * @param domain is the domain to plan in
 * @author Bence Cserna (bence@cserna.net)
 */
abstract class ClassicalPlanner(protected val domain: Domain) : Planner {

    // TODO: proper logging here
    // private val logger = LoggerFactory.getLogger("ClassicalPlanner")
    private var generatedNodes = 0

    data class Node(val parent: Node?, val successorBundle: SuccessorBundle)

    /** Interface3 functions **/

    /**
     * Resets all variables in the planner. Called before a new planning task
     */
    open protected fun initiatePlan() { generatedNodes = 0 }

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
        generatedNodes = 0
        initiatePlan()

        // main loop
        var currentNode = Node(null, SuccessorBundle(state, null, 0.0))
        while (!domain.isGoal(currentNode.successorBundle.successorState)) {

            // expand (only those not visited yet)
            for (successor in domain.successors(currentNode.successorBundle.successorState)) {
                if (!visitedBefore(successor.successorState, currentNode)) {
                    generatedNodes.inc()

                    // generate the node with correct cost
                    val nodeCost = successor.cost + currentNode.successorBundle.cost
                    generateNode(Node(currentNode, successor.copy(cost = nodeCost)))
                }
            }

            // check next node
            currentNode = popFromOpenList()
        }

        return getActions(currentNode)
    }

    /**
     * @brief Returns the actions necessary to get to node
     *
     * @param leave the current end of the path
     *
     * @return list of actions to get to leave
     */
    private fun getActions(leave: Node): List<Action> {
        val actions: MutableList<Action> = arrayListOf()

        var node = leave
        // root will have null action. So as long as the parent
        // is not null, we can take it's action and assume all is good
        while (node.parent != null) {
            actions.add(node.successorBundle.action!!)
            node = node.parent!!
        }

        return actions.reversed() // we are adding actions in wrong order, to return the reverser
    }
}