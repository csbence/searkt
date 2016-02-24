package edu.unh.cs.ai.realtimesearch.environment

interface DiscretizableState<out StateType: DiscretizableState<StateType>>: State<StateType> {
    open fun discretize(): StateType
}