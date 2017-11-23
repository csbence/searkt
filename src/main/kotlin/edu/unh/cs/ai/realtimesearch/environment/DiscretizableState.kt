package edu.unh.cs.ai.realtimesearch.environment

/**
 * A continuous state which may be discretized into a general state.
 *
 * @author Mike Bogochow (mgp36@unh.edu)
 */
interface DiscretizableState<out StateType : DiscretizableState<StateType>> : State<StateType> {
    fun discretize(): StateType
}