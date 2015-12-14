package edu.unh.cs.ai.realtimesearch.planner

import edu.unh.cs.ai.realtimesearch.environment.Action
import edu.unh.cs.ai.realtimesearch.environment.State
import edu.unh.cs.ai.realtimesearch.experiment.TerminationChecker

/**
 * @author Bence Cserna (bence@cserna.net)
 *
 * A planner for real time search environments, where a constraint is placed
 * on the the amount of time allowed to plan. A RTS planner requires to return
 * a action within a certain time limit
 */
interface RealTimePlanner : Planner {

    fun selectAction(state: State, terminationChecker: TerminationChecker): Action
}

