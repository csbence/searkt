package edu.unh.cs.ai.realtimesearch.experiment.configuration

data class ManualConfiguration(private val domainName: String,
                               private val rawDomain: String,
                               private val algorithmName: String,
                               private val numberOfRuns: Int,
                               private val terminationCheckerType: String,
                               private val terminationCheckerParameter: Int) : ExperimentConfiguration {
    override fun getDomainName(): String = domainName
    override fun getRawDomain(): String = rawDomain
    override fun getAlgorithmName(): String = algorithmName
    override fun getNumberOfRuns(): Int = numberOfRuns
    override fun getTerminationCheckerType(): String = terminationCheckerType
    override fun getTerminationCheckerParameter(): Int = terminationCheckerParameter
}