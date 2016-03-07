package edu.unh.cs.ai.realtimesearch.experiment.configuration

import com.fasterxml.jackson.annotation.JsonUnwrapped

class ExperimentConfigurationDto() : ExperimentConfiguration {

    constructor(domainName: String,
                rawDomain: String,
                algorithmName: String,
                terminationCheckerType: String,
                terminationCheckerParameter: Int) : this() {
        this.domainName = domainName
        this.rawDomain = rawDomain
        this.algorithmName = algorithmName
        this.terminationCheckerType = terminationCheckerType
        this.terminationCheckerParameter = terminationCheckerParameter
    }

    private @JsonUnwrapped val valueStore = hashMapOf<String, Any>()

    override var domainName: String by valueStore
    override var rawDomain: String by valueStore
    override var algorithmName: String by valueStore
    override var terminationCheckerType: String by valueStore
    override var terminationCheckerParameter: Int by valueStore

    override fun contains(key: String) = valueStore.containsKey(key)
    override fun get(key: String) = valueStore[key]
    override fun set(key: String, value: Any) {
        valueStore[key] = value
    }
}