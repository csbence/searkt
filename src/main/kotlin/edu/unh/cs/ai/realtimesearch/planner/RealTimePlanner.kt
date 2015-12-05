package edu.unh.cs.ai.realtimesearch.planner

import edu.unh.cs.ai.realtimesearch.domain.Action
import edu.unh.cs.ai.realtimesearch.domain.State

/**
 * @author Bence Cserna (bence@cserna.net)
 */
interface RealTimePlanner : Planner {
    fun selectAction(state: State): Action
}