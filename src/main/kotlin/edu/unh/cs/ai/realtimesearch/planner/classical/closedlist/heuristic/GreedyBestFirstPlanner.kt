package edu.unh.cs.ai.realtimesearch.planner.classical.closedlist.heuristic

import edu.unh.cs.ai.realtimesearch.environment.Domain
import edu.unh.cs.ai.realtimesearch.planner.classical.closedlist.ClassicalHeuristicPlanner
import java.util.*

/**
 * The greedy best first planner's heuristic is purely the nodes h value
 *
 * Instantiates as a classical heuristic planner whose comparator sorts on h value
 * @param domain is the domain to plan in
 */
class GreedyBestFirstPlanner (domain: Domain) : ClassicalHeuristicPlanner(domain,
        PriorityQueue(GreedyBestFirstPlanner.GreedyBestFirstNodeComparator(domain))) {

    /**
     * Compares to node purely by their cost. Returns the difference between
     * both the nodes, a negative value means the first node is less than the second.
     *
     * In a priority queue the least element will be at the head.
     *
     * TODO: will this round -0.4 to 0? That would be bad
     * TODO: test
     */
    public class GreedyBestFirstNodeComparator(val domain: Domain) : Comparator<Node> {
        override fun compare(n1: Node?, n2: Node?): Int {
            if (n1 != null && n2 != null)
                return (domain.heuristic(n1.state) -
                        domain.heuristic(n2.state)).toInt()
            else throw RuntimeException("Cannot insert null into closed list")
        }
    }

}