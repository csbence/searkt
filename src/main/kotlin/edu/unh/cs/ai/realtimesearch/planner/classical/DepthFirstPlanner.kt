package edu.unh.cs.ai.realtimesearch.planner.classical

import edu.unh.cs.ai.realtimesearch.domain.Action
import edu.unh.cs.ai.realtimesearch.domain.Domain
import edu.unh.cs.ai.realtimesearch.domain.State
import edu.unh.cs.ai.realtimesearch.domain.SuccessorBundle
import edu.unh.cs.ai.realtimesearch.planner.ClassicalPlanner
import org.slf4j.LoggerFactory
import java.util.*

class DepthFirstPlanner(val domain: Domain) : ClassicalPlanner {
    data class Node(val parent: Node?, val successorBundle: SuccessorBundle)

    private val logger = LoggerFactory.getLogger("DepthFirstPlanner")
    private var generatedNodes = 0
    private val openList: Deque<Node> = linkedListOf()

    fun removeMe() {
        logger.error("Error error error. This is how you add a param: {}", 4)
        logger.warn("Warn!")
        logger.info("Info info")
        logger.debug("This is a debug message !")
        logger.trace("trace")
    }


    /** Classic planner interface */
    override fun plan(state: State): List<Action> {
        removeMe()
        return emptyList()

        // init class members
        // (in case we planned with this planner before)
        openList.clear()
        generatedNodes = 0

        // main loop
        var currentNode = Node(null, SuccessorBundle(state, null, 0.0))
        while (!domain.isGoal(currentNode.successorBundle.successorState)) {

            print("Popped " + currentNode.toString() + "\n")

            // expand (only those not visited yet)
            for (successor in domain.successors(currentNode.successorBundle.successorState)) {
                print("Expanding to " + successor.successorState.toString() + "...")
                if (!visitedBefore(successor.successorState, currentNode)) {
                    print("Added to open list\n")
                    generatedNodes.inc()
                    openList.add(Node(currentNode, successor))
                } else {
                    print("Already visited\n")
                }
            }

            // check next node
            currentNode = openList.pop()
        }

        return getActions(currentNode)
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
        var node: Node? = leave

        // root will have null action. So as long as the parent
        // is not null, we can take it's action and assume all is good
        while (node != null) {

            if (state == node.successorBundle.successorState)
                return true

            node = node.parent
        }

        return false
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

