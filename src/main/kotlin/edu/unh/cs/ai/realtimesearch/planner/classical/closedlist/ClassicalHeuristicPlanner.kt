package edu.unh.cs.ai.realtimesearch.planner.classical.closedlist

import edu.unh.cs.ai.realtimesearch.environment.Domain
import edu.unh.cs.ai.realtimesearch.environment.State
import edu.unh.cs.ai.realtimesearch.planner.classical.ClassicalPlannerBase
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
open class ClassicalHeuristicPlanner<StateType : State<StateType>>(domain: Domain<StateType>, val openList: PriorityQueue<ClassicalPlannerBase.Node<StateType>>) :
        ClosedListPlanner<StateType>(domain) {

    /**
     * Clears open list
     */
    override fun initiatePlan() {
        openList.clear();
        super.initiatePlan()
    }

    /**
     * Adds the node to back of openList
     */
    override fun generateNode(node: Node<StateType>) {
        openList.add(node)
        super.generateNode(node)
    }

    /**
     * Gets node from front of openList
     */
    override fun popFromOpenList(): Node<StateType> {
        val node = openList.remove()
        //        val costs = openList.toArray().maps { it as Node<StateType> }.map { it.cost + domain.heuristic(it.state) }
        //        println("Cost: ${ node.cost + domain.heuristic(node.state)} Id: ${node.state.hashCode()}")
        return node
    }
}