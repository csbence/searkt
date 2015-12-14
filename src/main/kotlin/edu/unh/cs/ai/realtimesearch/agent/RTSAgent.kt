package edu.unh.cs.ai.realtimesearch.agent

import edu.unh.cs.ai.realtimesearch.environment.Action
import edu.unh.cs.ai.realtimesearch.environment.State
import edu.unh.cs.ai.realtimesearch.experiment.TerminationChecker
import edu.unh.cs.ai.realtimesearch.planner.RealTimePlanner

/**
 * An agent for RTS experiments. In RTS the agent is repeatedly asked to provide
 * an action, under some constraint.
 */
class RTSAgent(val planner: RealTimePlanner) {

    /**
     * Returns an action within the termination constraint. Simply calls the
     * RTS planner interface.
     *
     * @param state is the state for which to provide an action
     * @param terminationChecker is the object that returns whether the agent should terminate
     * @return an action, given current state and termination check
     */
    public fun selectAction(state: State, terminationChecker: TerminationChecker): Action {
        return planner.selectAction(state, terminationChecker)
    }
}