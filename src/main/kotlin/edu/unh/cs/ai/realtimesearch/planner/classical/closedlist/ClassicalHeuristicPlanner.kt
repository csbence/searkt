package edu.unh.cs.ai.realtimesearch.planner.classical.closedlist

import edu.unh.cs.ai.realtimesearch.domain.Domain
import edu.unh.cs.ai.realtimesearch.planner.ClassicalPlanner
import edu.unh.cs.ai.realtimesearch.planner.classical.ClosedListPlanner
import java.util.*

/**
 * Abstract class for classical heuristic planners
 *
 * Contains a priority open queue, whose comparator is set by the constructor
 */
abstract class ClassicalHeuristicPlanner(domain: Domain, val openList: PriorityQueue<ClassicalPlanner.Node>) :
        ClosedListPlanner(domain) {


    /** ClassicalPlanner interface **/

    /**
     * Clears open list
     */
    override protected fun initiatePlan() {
        openList.clear();
        super.initiatePlan()
    }

    /**
     * Adds the node to back of openlist
     */
    protected override fun generateNode(node: Node) {
        openList.add(node)
        super.generateNode(node)
    }

    /**
     * Gets node from front of openlist
     */
    override fun popFromOpenList(): Node = openList.remove()
}