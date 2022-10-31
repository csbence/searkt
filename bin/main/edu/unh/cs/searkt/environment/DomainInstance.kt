package edu.unh.cs.searkt.environment

interface DomainInstance<StateType : State<StateType>> {
    val domain: Domain<StateType>
    val initialState: StateType
}