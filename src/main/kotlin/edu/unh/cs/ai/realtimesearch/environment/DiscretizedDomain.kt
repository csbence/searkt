package edu.unh.cs.ai.realtimesearch.environment

class DiscretizedDomain<StateType : DiscretizableState<StateType>, DomainType : Domain<StateType>>(val domain: DomainType) : Domain<DiscretizedState<StateType>> {
    /**
     * Get successor states from the given state for all valid actions.
     */
    override fun successors(state: DiscretizedState<StateType>): List<SuccessorBundle<DiscretizedState<StateType>>> {
        // to return
        val successors : MutableList<SuccessorBundle<DiscretizedState<StateType>>> = arrayListOf()
        val indiscreteSuccessors = domain.successors(state.state)

        for (successor in indiscreteSuccessors) {
            successors.add(SuccessorBundle(DiscretizedState(successor.state),
                    successor.action, successor.actionCost))
        }
        return successors
    }

    /**
     * Returns a heuristic for a state.
     *
     * @param state the state to provide a heuristic for
     */
    override fun heuristic(state: DiscretizedState<StateType>): Double = domain.heuristic(state.state)

    /**
     * Goal distance estimate.  Equal to the difference between the goal positions and actual positions.
     */
    override fun distance(state: DiscretizedState<StateType>): Double = domain.distance(state.state)

    /**
     * Returns whether the given state is a goal state.
     * @return true if the links within a threshold of positions and velocities.
     */
    override fun isGoal(state: DiscretizedState<StateType>): Boolean = domain.isGoal(state.state)

    /**
     * Prints the state values of the actual state and of the discretized state.
     *
     * @param state the state whose values should be printed
     */
    override fun print(state: DiscretizedState<StateType>): String {
        val description = StringBuilder()
        description.append(domain.print(state.state))
        description.append(domain.print(state.discretizedState))
        return description.toString()
    }

    /**
     * Retrieve a random state
     */
    override fun randomState(): DiscretizedState<StateType> = DiscretizedState(domain.randomState())
}