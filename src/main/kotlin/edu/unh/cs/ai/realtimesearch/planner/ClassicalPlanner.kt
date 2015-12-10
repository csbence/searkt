package edu.unh.cs.ai.realtimesearch.planner

import edu.unh.cs.ai.realtimesearch.domain.Action
import edu.unh.cs.ai.realtimesearch.domain.State

/**
 * @author Bence Cserna (bence@cserna.net)
 */
interface ClassicalPlanner : Planner {

    fun plan(state: State): List<Action>
}