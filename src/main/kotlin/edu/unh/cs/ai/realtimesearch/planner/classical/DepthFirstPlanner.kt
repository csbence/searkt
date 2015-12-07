package edu.unh.cs.ai.realtimesearch.planner.classical

import edu.unh.cs.ai.realtimesearch.domain.Action
import edu.unh.cs.ai.realtimesearch.domain.Domain
import edu.unh.cs.ai.realtimesearch.domain.State
import edu.unh.cs.ai.realtimesearch.domain.SuccessorSet
import edu.unh.cs.ai.realtimesearch.planner.Planner
import java.util.*

class DepthFirstPlanner(val domain: Domain) : Planner {
    data class Node(val parent: Node?, val successorSet: SuccessorSet)

    private var generatedNodes = 0
    private val openList: Deque<Node> = linkedListOf()

    /** Classic planner interface */
    fun plan(state: State): List<Action> {

        // init class members
        // (in case we planned with this planner before)
        openList.clear()
        var cur_node = Node(null, SuccessorSet(state, null, 0.0))

        // main loop
        while (! domain.isGoal(cur_node.successorSet.successorState)) {

            // expand (only those not visited yet)
            for (successor in domain.succesors(cur_node.successorSet.successorState)) {
                if (! visitedBefore(successor.successorState, cur_node)) {
                    generatedNodes.inc()
                    openList.add(Node(cur_node, successor))
                }
            }

            // check next node
            cur_node = openList.pop() // TODO Probably going to make the world explode..
        }

        return getActions(cur_node)
    }

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
    private fun visitedBefore(state: State, leave: Node): Boolean {
        var node = leave

        // root will have null action. So as long as the parent action
        // is not null, we can take it's parent and assume all is good
        while (node.parent!!.successorSet.action != null) {

            if (state != node.successorSet.successorState)
                return true

            node = node.parent!! // TODO: is the world going up in flames again?
        }

        return false;
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
        // root will have null action. So as long as the parent action
        // is not null, we can take it's parent and assume all is good
        while (node.parent!!.successorSet.action != null) {
            actions.add(node.successorSet.action!!)
            node = node.parent!! // TODO: is the world going up in flames again?
        }

        return actions.reversed() // we are adding actions in wrong order, to return the reverser
    }

}

