package edu.unh.cs.ai.realtimesearch.agent

import edu.unh.cs.ai.realtimesearch.domain.Domain
import edu.unh.cs.ai.realtimesearch.domain.State
import edu.unh.cs.ai.realtimesearch.planner.ClassicalPlanner

class ClassicalAgent(val planner: ClassicalPlanner) : Agent {

    /**
     * Returns a list of actions, given an initial state.
     *
     * @param state: initial state
     * @return a plan, consisting of a list of actions
     */
    fun plan(state: State) = planner.plan(state)

}