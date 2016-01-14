package edu.unh.cs.ai.realtimesearch.planner.classical.closedlist.heuristic

import edu.unh.cs.ai.realtimesearch.environment.Domain
import edu.unh.cs.ai.realtimesearch.environment.State
import edu.unh.cs.ai.realtimesearch.planner.classical.closedlist.ClassicalHeuristicPlanner
import java.util.*

/**
 * The greedy best first planner's heuristic is purely the nodes h value
 *
 * Instantiates as a classical heuristic planner whose comparator sorts on h value
 * @param domain is the domain to plan in
 */
class GreedyBestFirstPlanner<StateType : State<StateType>>(domain: Domain<StateType>) : ClassicalHeuristicPlanner<StateType>(domain,
        PriorityQueue(GreedyBestFirstPlanner.GreedyBestFirstNodeComparator(domain))) {

    /**
     * Compares to node purely by their cost. Returns the difference between
     * both the nodes, a negative value means the first node is less than the second.
     *
     * In a priority queue the least element will be at the head.
     */
    public class GreedyBestFirstNodeComparator<StateType : State<StateType>>(val domain: Domain<StateType>) : Comparator<Node<StateType>> {
        override fun compare(n1: Node<StateType>?, n2: Node<StateType>?): Int {
            if (n1 != null && n2 != null) {
                return domain.heuristic(n1.state).compareTo(domain.heuristic(n2.state))
            } else {
                throw RuntimeException("Cannot insert null into closed list")
            }
        }
    }

}