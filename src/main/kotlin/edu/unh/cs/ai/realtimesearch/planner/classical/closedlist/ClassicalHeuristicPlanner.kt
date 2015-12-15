package edu.unh.cs.ai.realtimesearch.planner.classical.closedlist

import edu.unh.cs.ai.realtimesearch.environment.Domain
import edu.unh.cs.ai.realtimesearch.planner.ClassicalPlanner
import edu.unh.cs.ai.realtimesearch.planner.classical.ClosedListPlanner
import java.util.*

/**
 * Abstract class for classical heuristic planners
 *
 * Contains a priority open queue, whose comparator is set by the constructor
 *
 * @param domain: is the domain to be planned in
 * @param openList is the list used for deciding which nodes to expand upon
 */
open class ClassicalHeuristicPlanner(domain: Domain, val openList: PriorityQueue<ClassicalPlanner.Node>) :
        ClosedListPlanner(domain) {



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