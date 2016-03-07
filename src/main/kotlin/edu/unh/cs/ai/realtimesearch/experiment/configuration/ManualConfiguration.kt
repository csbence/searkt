package edu.unh.cs.ai.realtimesearch.experiment.configuration

import groovy.json.JsonSlurper

data class ManualConfiguration(private val domainName: String,
                               private val rawDomain: String,
                               private val algorithmName: String,
                               private val numberOfRuns: Int,
                               private val terminationCheckerType: String,
                               private val terminationCheckerParameter: Int) : ExperimentConfiguration {
    private val valueStore = hashMapOf<String, Any>()

    override fun contains(key: String) = valueStore.containsKey(key)
    override fun getValue(key: String) = valueStore[key]

    fun setValue(key: String, value: Any) {
        valueStore[key] = value
    }

    override fun getDomainName(): String = domainName
    override fun getRawDomain(): String = rawDomain
    override fun getAlgorithmName(): String = algorithmName
    override fun getNumberOfRuns(): Int = numberOfRuns
    override fun getTerminationCheckerType(): String = terminationCheckerType
    override fun getTerminationCheckerParameter(): Int = terminationCheckerParameter

    companion object {
        fun fromString(string: String): ExperimentConfiguration = fromMap(JsonSlurper().parseText(string) as Map<*,*>)

        fun fromMap(map: Map<*,*>): ExperimentConfiguration {
            return ManualConfiguration(
                    map["domainName"] as String,
                    map["rawDomain"] as String,
                    map["algorithmName"] as String,
                    map["numberOfRuns"] as Int,
                    map["terminationCheckerType"] as String,
                    map["terminationCheckerParameter"] as Int
            )
        }
    }
}