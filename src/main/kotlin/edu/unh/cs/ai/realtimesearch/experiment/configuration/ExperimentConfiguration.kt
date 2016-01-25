package edu.unh.cs.ai.realtimesearch.experiment.configuration

interface ExperimentConfiguration {
    fun getDomainName(): String
    fun getRawDomain(): String
    fun getAlgorithmName(): String
    fun getNumberOfRuns(): Int
    fun getTerminationCheckerType(): String
    fun getTerminationCheckerParameter(): Int
}