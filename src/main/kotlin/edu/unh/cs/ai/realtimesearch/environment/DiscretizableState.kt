package edu.unh.cs.ai.realtimesearch.environment

/**
 * A continuous state which may be discretized into a general state.
 */
interface DiscretizableState<out StateType: DiscretizableState<StateType>>: State<StateType> {
    open fun discretize(): StateType
}