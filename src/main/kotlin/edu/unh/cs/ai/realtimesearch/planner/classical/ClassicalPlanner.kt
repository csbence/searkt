package edu.unh.cs.ai.realtimesearch.planner.classical

import edu.unh.cs.ai.realtimesearch.environment.Action
import edu.unh.cs.ai.realtimesearch.environment.State
import edu.unh.cs.ai.realtimesearch.planner.Planner

interface ClassicalPlanner<StateType : State<StateType>> : Planner {

    /**
     * Returns a plan for a given initial state. A plan consists of a list of actions
     *
     * @param state is the initial state
     * @return a list of action compromising the plan
     */
    fun plan(state: StateType): List<Action>
}