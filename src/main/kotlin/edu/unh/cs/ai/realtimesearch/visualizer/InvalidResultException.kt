package edu.unh.cs.ai.realtimesearch.visualizer

/**
 * Exception due to receiving an {@link ExperimentResult} with invalid formatting.
 *
 * @author Mike Bogochow
 * @since 4/6/16
 */
class InvalidResultException : Exception {
    constructor() : super()

    constructor(msg: String) : super(msg)

    constructor(msg: String, cause: Throwable) : super(msg, cause)
}