package edu.unh.cs.ai.realtimesearch.planner

import edu.unh.cs.ai.realtimesearch.domain.Action

/**
 * @author Bence Cserna (bence@cserna.net)
 */
interface ClassicalPlanner : Planner {
    fun plan(): List<Action>

}