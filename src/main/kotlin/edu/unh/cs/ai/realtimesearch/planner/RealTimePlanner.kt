package edu.unh.cs.ai.realtimesearch.planner

import edu.unh.cs.ai.realtimesearch.environment.Action
import edu.unh.cs.ai.realtimesearch.environment.Domain
import edu.unh.cs.ai.realtimesearch.environment.State
import edu.unh.cs.ai.realtimesearch.experiment.terminationCheckers.TerminationChecker

/**
 * A planner for real time search environments, where a constraint is placed
 * on the the amount of time allowed to plan. A RTS planner requires to return
 * a action within a certain time limit
 *
 * @param domain: The domain to plan in
 */
abstract class RealTimePlanner<StateType : State<StateType>>(protected val domain: Domain<StateType>) : Planner<StateType>() {
    /**
     * Data class to store [Action]s along with their execution time.
     *
     * The [duration] is measured in nanoseconds.
     */
    data class ActionBundle(val action: Action, val duration: Long)

    /**
     * Returns an action while abiding the termination checker's criteria.
     *
     * @param state is the state to pick an action for
     * @param terminationChecker provides the termination criteria
     * @return an action for current state
     */
    abstract fun selectAction(state: StateType, terminationChecker: TerminationChecker): List<ActionBundle>

    /**
     * Called before the first [selectAction] call.
     *
     * This call does not count towards the planning time.
     */
    open fun init() {

    }
}

