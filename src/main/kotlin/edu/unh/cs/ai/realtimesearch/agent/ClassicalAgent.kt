package edu.unh.cs.ai.realtimesearch.agent

import edu.unh.cs.ai.realtimesearch.environment.State
import edu.unh.cs.ai.realtimesearch.planner.ClassicalPlanner

/**
 * The agent for classic search problems, deterministic fully observable and without constraints.
 */
class ClassicalAgent(val planner: ClassicalPlanner) : Agent {

    /**
     * Returns a list of actions, given an initial state.
     *
     * @param state: initial state
     * @return a plan, consisting of a list of actions
     */
    public fun plan(state: State) = planner.plan(state)

}