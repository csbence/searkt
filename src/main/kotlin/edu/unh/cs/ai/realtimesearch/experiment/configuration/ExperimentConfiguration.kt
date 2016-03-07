package edu.unh.cs.ai.realtimesearch.experiment.configuration

interface ExperimentConfiguration {
    fun getDomainName(): String
    fun getRawDomain(): String
    fun getAlgorithmName(): String
    fun getTerminationCheckerType(): String
    fun getTerminationCheckerParameter(): Int

    fun getValue(key: String): Any?
    fun getBoolean(key: String): Boolean? = getValue(key) as? Boolean
    fun getInt(key: String): Int? = getValue(key) as? Int
    fun getDouble(key: String): Double? = getValue(key) as? Double
    fun <T> getTypedValue(key: String) : T? = getValue(key) as? T
    fun contains(key: String): Boolean
}