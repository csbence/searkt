package edu.unh.cs.ai.realtimesearch.planner.classical

import edu.unh.cs.ai.realtimesearch.environment.Action
import edu.unh.cs.ai.realtimesearch.environment.State
import edu.unh.cs.ai.realtimesearch.planner.Planner

abstract class ClassicalPlanner<StateType : State<StateType>> : Planner<StateType>() {
    /**
     * Plan execution time not including the garbage collection time.
     */
    var executionNanoTime: Long = 0L

    /**
     * Returns a plan for a given initial state. A plan consists of a list of actions
     *
     * @param state is the initial state
     * @return a list of action compromising the plan
     */
    abstract fun plan(state: StateType): List<Action>
}