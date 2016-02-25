package edu.unh.cs.ai.realtimesearch.planner.classical.closedlist

import edu.unh.cs.ai.realtimesearch.environment.Domain
import edu.unh.cs.ai.realtimesearch.environment.State
import edu.unh.cs.ai.realtimesearch.planner.classical.ClosedListPlanner
import java.util.*

/**
 * Breadth first planner. Will expand nodes according to first in first out
 *
 * @param domain: The domain to plan in
 */
class BreadthFirstPlanner<StateType : State<StateType>>(domain: Domain<StateType>) : ClosedListPlanner<StateType>(domain) {

    private val openList: Deque<Node<StateType>> = ArrayDeque()

    /**
     * Clears open list
     */
    override protected fun initiatePlan() {
        openList.clear();
        super.initiatePlan()
    }

    /**
     * Adds the node to back of openlist
     *
     * @param node is the node that is generated
     */
    protected override fun generateNode(node: Node<StateType>) {
        openList.add(node)
        super.generateNode(node)
    }

    /**
     * Gets node from front of openlist
     * @return first on the openlist (first in first out)
     */
    override fun popFromOpenList(): Node<StateType> = openList.pop()
}