package edu.unh.cs.ai.realtimesearch.planner.classical.closedlist.heuristic

import edu.unh.cs.ai.realtimesearch.environment.Domain
import edu.unh.cs.ai.realtimesearch.environment.State
import edu.unh.cs.ai.realtimesearch.planner.classical.closedlist.ClassicalHeuristicPlanner
import java.util.*

/**
 * A*, expands nodes according to their f value.
 *
 * Will create a classical heuristic planner with f value comparator.
 *
 * @param domain is the domain to plan in
 */
class ClassicalAStarPlanner<StateType : State<StateType>>(domain: Domain<StateType>) : ClassicalHeuristicPlanner<StateType>(domain,
        PriorityQueue(ClassicalAStarPlanner.AStarNodeComparator(domain))) {

    /**
     * Compares to node purely by their cost. Returns the difference between
     * both the nodes, a negative value means the first node is less than the second.
     *
     * In a priority queue the least element will be at the head.
     */
    public class AStarNodeComparator<State>(val domain: Domain<State>) : Comparator<Node<State>> {
        override fun compare(node1: Node<State>?, n2: Node<State>?): Int {
            if (node1 != null && n2 != null) {
                val node1Value = domain.heuristic(node1.state) + node1.cost
                val node2Value = domain.heuristic(n2.state) + n2.cost
                val value = node1Value.compareTo(node2Value)

                if (value == 0) {
                    return domain.heuristic(node1.state).compareTo(domain.heuristic(n2.state))
                }

                return value
            } else {
                throw RuntimeException("Cannot insert null into closed list")
            }
        }
    }
}
