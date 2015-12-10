package edu.unh.cs.ai.realtimesearch.planner

import edu.unh.cs.ai.realtimesearch.domain.Action
import edu.unh.cs.ai.realtimesearch.domain.Domain
import edu.unh.cs.ai.realtimesearch.domain.State

/**
 * The abstract class for classical planners. Assume fully observable, deterministic nature.
 *
 * Possible derivatives of this class are depthfirst search, A* etc.
 *
 * @param domain is the domain to plan in
 * @author Bence Cserna (bence@cserna.net)
 */
abstract class ClassicalPlanner(protected val domain: Domain) : Planner {

    abstract fun plan(state: State): List<Action>
}