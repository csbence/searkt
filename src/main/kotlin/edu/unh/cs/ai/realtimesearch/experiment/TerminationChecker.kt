package edu.unh.cs.ai.realtimesearch.experiment

/**
 * Checks whether a RTS searcher should terminate. This interface allows
 * for various termination types in a RTS experiment
 */
interface TerminationChecker {

    /**
     * Called just before an experiment starts,
     * allows for any initiation of the checker
     */
    public fun init()

    /**
     * Checks whether the termination criteria has been reached
     *
     * @param: Still unsure what the parameter should be. A whole agent, or should it be templated?
     * @return True if the the criteria has been reached
     */
    public fun reachedTermination(): Boolean
}