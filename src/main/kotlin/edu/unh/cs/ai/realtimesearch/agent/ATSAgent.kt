package edu.unh.cs.ai.realtimesearch.agent

import edu.unh.cs.ai.realtimesearch.environment.State
import edu.unh.cs.ai.realtimesearch.planner.AnytimePlanner

/**
 * An agent for RTS experiments. In RTS the agent is repeatedly asked to provide
 * an action, under some constraint.
 */
class ATSAgent<StateType : State<StateType>>(val planner: AnytimePlanner<StateType>) {

    /**
     * Returns an action within the termination constraint. Simply calls the
     * RTS planner interface.
     *
     * @param state is the state for which to provide an action
     * @param terminationChecker is the object that returns whether the agent should terminate
     * @return an action, given current state and termination check
     */
    //fun selectAction(state: StateType, terminationChecker: TimeTerminationChecker) = planner.selectAction(state, terminationChecker)

    /**
     * Resets the agent for a new run. This function is called whenever a new run starts. This should prepare
     * the agent for a new experiment, clearing out any experience or data from previous searches. This function
     * is necessary for the agent to understand that the next selectAction is unrelated to previous action selection.
     */
    fun reset() = planner.reset()
}