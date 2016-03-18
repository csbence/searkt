package edu.unh.cs.ai.realtimesearch.agent

import edu.unh.cs.ai.realtimesearch.environment.State
import edu.unh.cs.ai.realtimesearch.planner.classical.ClassicalPlanner

/**
 * The agent for classic search problems, deterministic fully observable and without constraints.
 */
class ClassicalAgent<StateType : State<StateType>>(val planner: ClassicalPlanner<StateType>) : Agent {

    /**
     * Returns a list of actions, given an initial state.
     *
     * @param state: initial state
     * @return a plan, consisting of a list of actions
     */
    fun plan(state: StateType) = planner.plan(state)

}