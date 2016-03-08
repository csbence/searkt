package edu.unh.cs.ai.realtimesearch.experiment.configuration

interface ExperimentConfiguration {
    val domainName: String
    val rawDomain: String
    val algorithmName: String
    val terminationCheckerType: String
    val terminationCheckerParameter: Int

    operator fun get(key: String) : Any?
    operator fun set(key: String, value: Any)

    fun <T> getTypedValue(key: String): T? = this[key] as? T
    fun contains(key: String): Boolean
}