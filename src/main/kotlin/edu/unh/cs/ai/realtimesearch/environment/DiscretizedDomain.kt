package edu.unh.cs.ai.realtimesearch.environment

/**
 * Wrapper around a domain which takes {@link DiscretizableState}s.  All method implementations are based on actual
 * states and not their discretized versions.
 *
 * @author Mike Bogochow (mgp36@unh.edu)
 */
class DiscretizedDomain<StateType : DiscretizableState<StateType>, DomainType : Domain<StateType>>(val domain: DomainType) : Domain<DiscretizedState<StateType>> {
    /**
     * Get successor states from the given state for all valid actions.
     */
    override fun successors(state: DiscretizedState<StateType>): List<SuccessorBundle<DiscretizedState<StateType>>> {
        // to return
        val successors: MutableList<SuccessorBundle<DiscretizedState<StateType>>> = arrayListOf()
        val indiscreteSuccessors = domain.successors(state.state)

        for (successor in indiscreteSuccessors) {
            successors.add(SuccessorBundle(DiscretizedState(successor.state),
                    successor.action, successor.actionCost))
        }
        return successors
    }

    override fun predecessors(state: DiscretizedState<StateType>): List<SuccessorBundle<DiscretizedState<StateType>>> {
        // to return
        val predecessors: MutableList<SuccessorBundle<DiscretizedState<StateType>>> = arrayListOf()
        val indiscretePredecessors = domain.predecessors(state.state)

        for (successor in indiscretePredecessors) {
            predecessors.add(SuccessorBundle(DiscretizedState(successor.state),
                    successor.action, successor.actionCost))
        }
        return predecessors
    }

    /**
     * Returns a heuristic for a state.
     *
     * @param state the state to provide a heuristic for
     */
    override fun heuristic(state: DiscretizedState<StateType>): Double = domain.heuristic(state.state)

    override fun heuristic(startState: DiscretizedState<StateType>, endState: DiscretizedState<StateType>): Double
            = domain.heuristic(startState.state, endState.state)

    /**
     * Goal distance estimate.  Equal to the difference between the goal positions and actual positions.
     */
    override fun distance(state: DiscretizedState<StateType>): Double = domain.distance(state.state)

    /**
     * Returns whether the given state is a goal state.
     * @return true if the links within a threshold of positions and velocities.
     */
    override fun isGoal(state: DiscretizedState<StateType>): Boolean = domain.isGoal(state.state)

    override fun getGoals(): List<DiscretizedState<StateType>> {
        val list: MutableList<DiscretizedState<StateType>> = arrayListOf()

        for (goal in domain.getGoals()) {
            list.add(DiscretizedState(goal))
        }

        return list
    }
}