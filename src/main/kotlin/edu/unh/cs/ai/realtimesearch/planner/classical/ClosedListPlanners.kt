package edu.unh.cs.ai.realtimesearch.planner.classical

import edu.unh.cs.ai.realtimesearch.domain.Action
import edu.unh.cs.ai.realtimesearch.domain.Domain
import edu.unh.cs.ai.realtimesearch.domain.State
import edu.unh.cs.ai.realtimesearch.planner.ClassicalPlanner
import java.util.*

/**
 * Abstract class for any class that maintains a closed list.
 * Closed list is implemented as a set of states
 *
 * Implements checking if a state has been visited before and has a domain
 *
 * @param domain is the domain to plan in
 */
abstract class ClosedListPlanners(domain: Domain) : ClassicalPlanner(domain) {
    private val closedList: HashSet<State> = hashSetOf()

    override fun plan(state: State): List<Action> {
        return emptyList()
    }

    /**
     * Returns whether a state has been visited before
     *
     * @param state is the state to check for
     * @return whether state has been seen before
     */
    protected fun visitedBefore(state: State) = state in closedList


}