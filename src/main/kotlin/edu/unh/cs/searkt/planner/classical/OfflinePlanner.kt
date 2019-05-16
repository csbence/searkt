package edu.unh.cs.searkt.planner.classical

import edu.unh.cs.searkt.environment.Action
import edu.unh.cs.searkt.environment.State
import edu.unh.cs.searkt.experiment.terminationCheckers.TerminationChecker
import edu.unh.cs.searkt.planner.Planner

abstract class OfflinePlanner<StateType : State<StateType>> : Planner<StateType>() {

    /**
     * Returns a plan for a given initial state. A plan consists of a list of actions
     *
     * @param state is the initial state
     * @return a list of action compromising the plan
     */
    abstract fun plan(state: StateType, terminationChecker: TerminationChecker): List<Action>
}