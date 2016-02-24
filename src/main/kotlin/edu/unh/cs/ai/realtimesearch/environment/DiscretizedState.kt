package edu.unh.cs.ai.realtimesearch.environment

open class DiscretizedState<ActualState: DiscretizableState<ActualState>>(val state: ActualState): State<DiscretizedState<ActualState>> {
    fun copy(state: ActualState): DiscretizedState<ActualState> = DiscretizedState(state.copy())
    override fun copy(): DiscretizedState<ActualState> = copy(state)

    val discretizedState: ActualState
    init {
        discretizedState = state.discretize()
    }

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