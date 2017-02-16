package edu.unh.cs.ai.realtimesearch.environment

/**
 * Represents a state which can be discretized.  The original state is preserved so that it can be used by the
 * domain while a planner can use the discretized state.  This is achieved by determining equality and hashcode in
 * terms of the discretized state while making the original state publicly accessible.
 *
 * @author Mike Bogochow (mgp36@unh.edu)
 *
 * @param state the state to be discretized
 */
open class DiscretizedState<ActualState : DiscretizableState<ActualState>>(val state: ActualState) : State<DiscretizedState<ActualState>> {
    fun copy(state: ActualState): DiscretizedState<ActualState> = DiscretizedState(state.copy())
    override fun copy(): DiscretizedState<ActualState> = copy(state)

    val discretizedState: ActualState = state.discretize()

    override fun equals(other: Any?): Boolean {
        return when {
            other == null -> false
            other === this -> true
            other !is DiscretizedState<*> -> false
            else -> discretizedState.equals(other.discretizedState)
        }
    }

    override fun hashCode(): Int {
        return discretizedState.hashCode()
    }

    override fun toString(): String {
        val stringBuilder = StringBuilder()
        stringBuilder.append("state:")
        stringBuilder.append(state.toString())
        stringBuilder.append(",discretizedState:")
        stringBuilder.append(discretizedState.toString())
        return stringBuilder.toString()
    }
}