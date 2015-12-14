package edu.unh.cs.ai.realtimesearch.planner.classical

import edu.unh.cs.ai.realtimesearch.environment.Domain
import edu.unh.cs.ai.realtimesearch.environment.State
import edu.unh.cs.ai.realtimesearch.planner.ClassicalPlanner
import java.util.*

/**
 * Abstract class for any class that maintains a closed list.
 * Closed list is implemented as a set of states
 *
 * Implements checking if a state has been visited before and has a domain
 *
 r @param domain is the domain to plan in
 */
abstract class ClosedListPlanner(domain: Domain) : ClassicalPlanner(domain) {

    // TODO: proper logging here
    // private val logger = LoggerFactory.getLogger("ClosedListPlanner")
    private val closedList: HashSet<State> = hashSetOf()

    /** Interface of ClassicalPlanner **/

    /**
     * Clears closed list
     */
    override protected fun initiatePlan() {
        closedList.clear();
        super.initiatePlan()
    }

    /**
     * Adds the state of the node to the closedlist
     */
    protected override fun generateNode(node: Node) { closedList.add(node.state) }

    /**
     * Returns whether a state has been visited before
     *
     * Checks whether state is in closed list
     *
     * @param state is the state to check for
     * @return whether state has been seen before
     */
    protected override fun visitedBefore(state: State, leave: Node) = state in closedList


}