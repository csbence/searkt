package edu.unh.cs.ai.realtimesearch.environment

interface DomainInstance<StateType : State<StateType>> {
    fun getDomain(): Domain<StateType>
    fun getInitialState(): StateType
}