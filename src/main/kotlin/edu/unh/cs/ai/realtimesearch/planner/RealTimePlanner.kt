package edu.unh.cs.ai.realtimesearch.planner

import edu.unh.cs.ai.realtimesearch.environment.Action
import edu.unh.cs.ai.realtimesearch.environment.Domain
import edu.unh.cs.ai.realtimesearch.environment.State
import edu.unh.cs.ai.realtimesearch.experiment.TerminationChecker

/**
 * A planner for real time search environments, where a constraint is placed
 * on the the amount of time allowed to plan. A RTS planner requires to return
 * a action within a certain time limit
 *
 * @param domain: The domain to plan in
 */
abstract class RealTimePlanner<StateType : State<StateType>>(protected val domain: Domain<StateType>) : Planner {

    override var generatedNodeCount = 0
    override var expandedNodeCount = 0

    /**
     * Returns an action while abiding the termination checker's criteria.
     *
     * @param state is the state to pick an action for
     * @param terminationChecker provides the termination criteria
     * @return an action for current state
     */
    abstract fun selectAction(state: StateType, terminationChecker: TerminationChecker): List<Action<StateType>>

    /**
     * Resets the planner for a new run. This function is called whenever a new run starts. This should prepare
     * the learner for a completely new unrelated selectAction call experiment. Some learners maintain plans,
     * for example, rather than plan for every single selectAction call, and this should clear out such data.
     *
     * NOTE: do not forget to call super.reset() when implementing this. Will reset the node count
     */
    open fun reset() {
        generatedNodeCount = 0
        expandedNodeCount = 0
    }

}

