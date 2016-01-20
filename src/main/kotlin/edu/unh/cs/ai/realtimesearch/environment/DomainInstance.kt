package edu.unh.cs.ai.realtimesearch.environment

/**
 * @author Bence Cserna (bence@cserna.net)
 */
interface DomainInstance<StateType : State<StateType>> {
    fun getDomain(): Domain<StateType>
    fun getInitialState(): StateType
}