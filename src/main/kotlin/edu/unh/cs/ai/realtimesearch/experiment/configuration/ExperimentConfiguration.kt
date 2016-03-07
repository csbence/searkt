package edu.unh.cs.ai.realtimesearch.experiment.configuration

interface ExperimentConfiguration {
    fun getDomainName(): String
    fun getRawDomain(): String
    fun getAlgorithmName(): String
    fun getTerminationCheckerType(): String
    fun getTerminationCheckerParameter(): Int

    operator fun get(key: String) : Any?
    fun <T> getTypedValue(key: String): T? = this[key] as? T

    fun contains(key: String): Boolean
}