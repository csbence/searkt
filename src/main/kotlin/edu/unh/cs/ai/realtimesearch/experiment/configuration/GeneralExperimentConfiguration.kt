package edu.unh.cs.ai.realtimesearch.experiment.configuration

import com.fasterxml.jackson.databind.annotation.JsonSerialize
import groovy.json.JsonSlurper

@JsonSerialize(`as` = ExperimentData::class)
open class GeneralExperimentConfiguration(map: MutableMap<String, Any> = hashMapOf()) : ExperimentData(map) {
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

    var domainName: String by valueStore
    var rawDomain: String by valueStore
    var algorithmName: String by valueStore
    var terminationCheckerType: String by valueStore
    var terminationCheckerParameter: Int by valueStore

    override fun contains(key: String) = valueStore.containsKey(key)

    companion object {
        fun fromString(string: String): GeneralExperimentConfiguration = fromMap(JsonSlurper().parseText(string) as Map<*,*>)

        fun fromMap(map: Map<*,*>?): GeneralExperimentConfiguration {
            if (map == null) return GeneralExperimentConfiguration()
            return GeneralExperimentConfiguration(
                    map["domainName"] as String,
                    map["rawDomain"] as String,
                    map["algorithmName"] as String,
                    map["terminationCheckerType"] as String,
                    map["terminationCheckerParameter"] as Int
            )
        }
    }
}