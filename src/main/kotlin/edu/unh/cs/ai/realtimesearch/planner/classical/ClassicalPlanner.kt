package edu.unh.cs.ai.realtimesearch.planner.classical

import edu.unh.cs.ai.realtimesearch.environment.Action
import edu.unh.cs.ai.realtimesearch.environment.State

interface ClassicalPlanner<StateType : State<StateType>> {

    /**
     * Returns a plan for a given initial state. A plan consists of a list of actions
     *
     * @param state is the initial state
     * @return a list of action compromising the plan
     */
    fun plan(state: StateType): List<Action>

    /**
     * Return the number of node expanded during the search.
     *
     * A node is counted when it is removed from the open list.
     */
    fun getExpandedNodeCount(): Int


    /**
     * Return the number of node generate during the search.
     *
     * A node is counted when it is added to the open list.
     */
    fun getGeneratedNodeCount(): Int
}