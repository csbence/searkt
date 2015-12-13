package edu.unh.cs.ai.realtimesearch.planner.classical

import edu.unh.cs.ai.realtimesearch.domain.Domain
import edu.unh.cs.ai.realtimesearch.domain.State
import edu.unh.cs.ai.realtimesearch.planner.ClassicalPlanner
import java.util.*

/**
 * The famous depth first planner.
 *
 * @param domain is the domain to plan in
 */
class DepthFirstPlanner(domain: Domain) : ClassicalPlanner(domain) {

    // TODO: proper logging here
    // private val logger = LoggerFactory.getLogger("DepthFirstPlanner")
    private val openList: Deque<Node> = linkedListOf()

    /** ClassicalPlanner interface **/

    /**
     * Clears open list
     */
    override fun initiatePlan() { openList.clear() }

    /**
     * Adds node to front of openlist
     */
    override fun generateNode(node: Node) { openList.push(node) }

    /**
     * Return node in front of openlist
     */
    override fun popFromOpenList() =  openList.pop()

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
    override fun visitedBefore(state: State, leave: Node): Boolean {
        var node: Node? = leave

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

