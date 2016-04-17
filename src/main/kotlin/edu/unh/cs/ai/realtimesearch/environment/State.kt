package edu.unh.cs.ai.realtimesearch.environment

interface State<out StateType : State<StateType>> {
    open fun copy(): StateType
}