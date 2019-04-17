package edu.unh.cs.searkt.environment

interface DomainInstance<StateType : State<StateType>> {
    fun getDomain(): Domain<StateType>
    fun getInitialState(): StateType
}