package edu.unh.cs.ai.realtimesearch.planner.classical

import edu.unh.cs.ai.realtimesearch.environment.Domain
import edu.unh.cs.ai.realtimesearch.environment.State
import edu.unh.cs.ai.realtimesearch.planner.classical.ClassicalPlannerBase
import java.util.*

/**
 * The famous depth first planner.
 *
 * @param domain is the domain to plan in
 */
class DepthFirstPlanner<StateType : State<StateType>>(domain: Domain<StateType>) : ClassicalPlannerBase<StateType>(domain) {

    private val openList: Deque<Node<StateType>> = LinkedList()

    /** ClassicalPlanner interface **/

    /**
     * Clears open list
     */
    override fun initiatePlan() = openList.clear()

    /**
     * Adds node to front of openlist
     */
    override fun generateNode(node: Node<StateType>) = openList.push(node)

    /**
     * Return node in front of openlist
     */
    override fun popFromOpenList() = openList.pop()

    /**
     * @brief Checks whether a state has been visited before in current path
     *
     * Will go through node up to root to see if state is in 1 of the nodes
     *
     * @param state is the state to check whether it has been visited before
     * @param leave is current end of the path
     *
     * @return true if state has been visited before
     */
    override fun visitedBefore(state: StateType, leave: Node<StateType>): Boolean {
        var node: Node<StateType>? = leave

        // root will have null action. So as long as the parent
        // is not null, we can take it's action and assume all is good
        while (node != null) {

            if (state == node.state)
                return true

            node = node.parent
        }

        return false
    }

}