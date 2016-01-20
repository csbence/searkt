package edu.unh.cs.ai.realtimesearch.experiment

/**
 * @author Bence Cserna (bence@cserna.net)
 */
class ExperimentConfiguration {
    fun getDomainName(): String {
        throw UnsupportedOperationException("not implemented")
    }

    fun getRawDomain(): String {
        throw UnsupportedOperationException("not implemented")
    }

    fun getAlgorithmName(): String {
        throw UnsupportedOperationException("not implemented")
    }

    fun getNumberOfRuns(): Int {
        throw UnsupportedOperationException("not implemented")
    }

    fun getTerminationCheckerType() : String {
        throw UnsupportedOperationException("not implemented")
    }

    fun getTerminationCheckerParameter(): Int {
        throw UnsupportedOperationException("not implemented")
    }

}