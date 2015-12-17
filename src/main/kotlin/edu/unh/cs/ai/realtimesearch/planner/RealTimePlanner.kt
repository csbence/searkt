package edu.unh.cs.ai.realtimesearch.planner

import edu.unh.cs.ai.realtimesearch.environment.Action
import edu.unh.cs.ai.realtimesearch.environment.Domain
import edu.unh.cs.ai.realtimesearch.environment.State
import edu.unh.cs.ai.realtimesearch.experiment.TerminationChecker

/**
 * @author Bence Cserna (bence@cserna.net)
 *
 * A planner for real time search environments, where a constraint is placed
 * on the the amount of time allowed to plan. A RTS planner requires to return
 * a action within a certain time limit
 *
 * @param domain: The domain to plan in
 */
abstract class RealTimePlanner(protected val domain: Domain) : Planner {

    public var generatedNodes = 0
    public var expandedNodes = 0

    /**
     * Returns an action while abiding the termination checker's criteria.
     *
     * @param state is the state to pick an action for
     * @param terminationChecker provides the termination criteria
     * @return an action for current state
     */
    abstract public fun selectAction(state: State, terminationChecker: TerminationChecker): Action

    /**
     * Resets the planner for a new run. This function is called whenever a new run starts. This should prepare
     * the learner for a completely new unrelated selectAction call experiment. Some learners maintain plans,
     * for example, rather than plan for every single selectAction call, and this should clear out such data.
     *
     * NOTE: do not forget to call super.reset() when implementing this. Will reset the node count
     */
    open public fun reset() {
        generatedNodes = 0
        expandedNodes = 0
    }

}

