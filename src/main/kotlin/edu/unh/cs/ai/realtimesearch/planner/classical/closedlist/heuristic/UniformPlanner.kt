package edu.unh.cs.ai.realtimesearch.planner.classical.closedlist.heuristic

import edu.unh.cs.ai.realtimesearch.domain.Domain
import edu.unh.cs.ai.realtimesearch.planner.classical.closedlist.ClassicalHeuristicPlanner
import java.util.*

/**
 * The uniform planner's heuristic is purely the cost of current node
 */
class UniformPlanner(domain: Domain) : ClassicalHeuristicPlanner(domain,
        PriorityQueue(UniformPlanner.UniformNodeComparator())) {

    /**
     * Compares to node purely by their cost. Returns the difference between
     * both the nodes, a negative value means the first node is less than the second.
     *
     * In a priority queue the least element will be at the head.
     *
     * TODO: will this round -0.4 to 0? That would be bad
     */
    public class UniformNodeComparator : Comparator<Node> {
        override fun compare(n1: Node?, n2: Node?): Int {
            if (n1 != null && n2 != null)
                return (n1.cost - n2.cost).toInt()
             else throw RuntimeException("Cannot insert null into closed list")
        }
    }

}