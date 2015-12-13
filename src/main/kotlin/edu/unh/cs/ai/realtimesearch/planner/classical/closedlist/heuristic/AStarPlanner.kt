package edu.unh.cs.ai.realtimesearch.planner.classical.closedlist.heuristic

import edu.unh.cs.ai.realtimesearch.domain.Domain
import edu.unh.cs.ai.realtimesearch.planner.classical.closedlist.ClassicalHeuristicPlanner
import java.util.*

class AStarPlanner(domain: Domain) : ClassicalHeuristicPlanner(domain,
        PriorityQueue(AStarPlanner.AStarNodeComparator(domain))) {

    /**
     * Compares to node purely by their cost. Returns the difference between
     * both the nodes, a negative value means the first node is less than the second.
     *
     * In a priority queue the least element will be at the head.
     *
     * TODO: will this round -0.4 to 0? That would be bad
     * TODO: test
     */
    public class AStarNodeComparator(val domain: Domain) : Comparator<Node> {
        override fun compare(n1: Node?, n2: Node?): Int {
            if (n1 != null && n2 != null)
                return (
                        (domain.heuristic(n1.state) + n1.cost) -
                        (domain.heuristic(n2.state) + n2.cost)
                                ).toInt()
            else throw RuntimeException("Cannot insert null into closed list")
        }
    }

}
