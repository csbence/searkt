package edu.unh.cs.ai.realtimesearch.environment

/**
 * @author Bence Cserna (bence@cserna.net)
 */
interface State {
    open fun copy(): State
}