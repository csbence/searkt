package edu.unh.cs.ai.realtimesearch.environment

interface State<out StateType : State<StateType>> {
    fun copy(): StateType

    fun projectX(): Int = throw UnsupportedOperationException("State can't be projected to X")
    fun projectY(): Int = throw UnsupportedOperationException("State can't be projected to Y")
    fun projectZ(): Int = throw UnsupportedOperationException("State can't be projected to Z")
}