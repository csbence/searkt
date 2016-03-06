package edu.unh.cs.ai.realtimesearch.environment.acrobot

import edu.unh.cs.ai.realtimesearch.environment.DiscretizedState
import edu.unh.cs.ai.realtimesearch.environment.Domain
import edu.unh.cs.ai.realtimesearch.environment.SuccessorBundle

/**
 * The Acrobot domain with discretized states.
 */
class DiscretizedAcrobot(val configuration: AcrobotConfiguration = defaultAcrobotConfiguration) : Domain<DiscretizedState<AcrobotState>> {
    private val acrobot = Acrobot()

    /**
     * Get successor states from the given state for all valid actions.
     */
    override fun successors(state: DiscretizedState<AcrobotState>): List<SuccessorBundle<DiscretizedState<AcrobotState>>> {
        // to return
        val successors : MutableList<SuccessorBundle<DiscretizedState<AcrobotState>>> = arrayListOf()
        val actualState = state.state

        for (action in AcrobotAction.values()) {
            // add the legal movement actions
            successors.add(SuccessorBundle(DiscretizedState(acrobot.calculateNextState(actualState, action)),
                    action, actionCost = configuration.stateConfiguration.timeStep))
        }

        return successors
    }

    /**
     * Returns a heuristic for a Acrobot state.  If the state does not have enough energy to reach the goal, must
     * inject energy before trying to reach the goal.  If the state does have enough energy, attempt to move towards
     * the goal.
     *
     * @param state the state to provide a heuristic for
     */
    override fun heuristic(state: DiscretizedState<AcrobotState>): Double = acrobot.heuristic(state.state)

    /**
     * Goal distance estimate.  Equal to the difference between the goal positions and actual positions.
     */
    override fun distance(state: DiscretizedState<AcrobotState>): Double = acrobot.distance(state.state)

    /**
     * Returns whether the given state is a goal state.
     * @return true if the links within a threshold of positions and velocities.
     */
    override fun isGoal(state: DiscretizedState<AcrobotState>): Boolean = acrobot.isGoal(state.state)

    /**
     * Prints the state values of the actual state and of the discretized state.
     *
     * @param state the state whose values should be printed
     */
    override fun print(state: DiscretizedState<AcrobotState>): String {
        val description = StringBuilder()
        description.append(acrobot.print(state.state))
        description.append(acrobot.print(state.discretizedState))
        return description.toString()
    }

    /**
     * Retrieve a random state
     */
    override fun randomState(): DiscretizedState<AcrobotState> = DiscretizedState(acrobot.randomState())
}

