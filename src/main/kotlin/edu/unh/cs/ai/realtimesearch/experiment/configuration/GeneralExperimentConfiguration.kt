package edu.unh.cs.ai.realtimesearch.experiment.configuration

import com.fasterxml.jackson.databind.annotation.JsonSerialize

@JsonSerialize(`as` = ExperimentData::class)
open class GeneralExperimentConfiguration(values: MutableMap<String, Any?> = hashMapOf<String, Any?>()) : ExperimentData(values) {
    constructor(domainName: String,
                rawDomain: String?,
                algorithmName: String,
                terminationCheckerType: String,
                domainPath: String? = null) : this() {
        this.domainName = domainName
        this.algorithmName = algorithmName
        this.terminationType = terminationCheckerType

        if (rawDomain != null) {
            this.rawDomain = rawDomain
        } else if (domainPath != null) {
            this.domainPath = domainPath
        } else {
            throw RuntimeException("Invalid configuration. Either rawDomain or domainPath has to be specified.")
        }
    }

    var domainName: String by valueStore
    var rawDomain: String? by valueStore
    var domainPath: String by valueStore
    var algorithmName: String by valueStore
    var terminationType: String by valueStore
    var actionDuration: Long by valueStore
    var timeLimit: Long by valueStore
    var expansionLimit: Long by valueStore

    override fun contains(key: String) = valueStore.containsKey(key)
}