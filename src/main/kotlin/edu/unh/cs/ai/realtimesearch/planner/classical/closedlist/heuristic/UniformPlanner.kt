package edu.unh.cs.ai.realtimesearch.planner.classical.closedlist.heuristic

import edu.unh.cs.ai.realtimesearch.environment.Domain
import edu.unh.cs.ai.realtimesearch.environment.State
import edu.unh.cs.ai.realtimesearch.planner.classical.closedlist.ClassicalHeuristicPlanner
import java.util.*

/**
 * The uniform planner's heuristic is purely the cost of current node
 *
 * Instantiates as a classical heuristic planner whose comparator sorts on g value
 * @param domain is the domain to plan in
 */
class UniformPlanner<StateType : State<StateType>>(domain: Domain<StateType>) : ClassicalHeuristicPlanner<StateType>(domain,
        PriorityQueue(UniformPlanner.UniformNodeComparator())) {

    /**
     * Compares to node purely by their cost. Returns the difference between
     * both the nodes, a negative value means the first node is less than the second.
     *
     * In a priority queue the least element will be at the head.
     */
    class UniformNodeComparator<StateType : State<StateType>> : Comparator<Node<StateType>> {
        override fun compare(n1: Node<StateType>?, n2: Node<StateType>?): Int {
            if (n1 != null && n2 != null) {
                return n1.cost.compareTo(n2.cost)
            } else {
                throw RuntimeException("Cannot insert null into closed list")
            }
        }
    }
}