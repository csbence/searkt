package edu.unh.cs.ai.realtimesearch.environment

interface State<State> {
    open fun copy(): State
}