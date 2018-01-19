package edu.unh.cs.ai.realtimesearch.experiment.configuration

import edu.unh.cs.ai.realtimesearch.experiment.configuration.realtime.LookaheadType
import edu.unh.cs.ai.realtimesearch.experiment.configuration.realtime.TerminationType
import edu.unh.cs.ai.realtimesearch.planner.CommitmentStrategy
import edu.unh.cs.ai.realtimesearch.planner.SafeRealTimeSearchTargetSelection
import edu.unh.cs.ai.realtimesearch.planner.SafetyBackup
import edu.unh.cs.ai.realtimesearch.planner.SafetyProof
import kotlinx.serialization.Optional
import kotlinx.serialization.Serializable

open class GeneralExperimentConfiguration(values: MutableMap<String, Any?> = hashMapOf()) : ExperimentData(values) {
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

@Serializable
data class ExperimentConfiguration(
        val domainName: String,
        @Optional val rawDomain: String? = null,
        val algorithmName: String,
        val terminationType: TerminationType,
        @Optional val domainPath: String? = null,
        val actionDuration: Long,
        val timeLimit: Long,
        val expansionLimit: Long,

        // Domain
        @Optional
        var domainSeed: Long = 0,

        // AStar
        @Optional
        var weight: Double = 0.0,

        // Real time experiment
        @Optional
        val lookaheadType: LookaheadType? = null,
        @Optional
        val commitmentStrategy: CommitmentStrategy? = null,
        @Optional
        val stepLimit: Long? = null,

        // RTA*
        @Optional
        val lookaheadDepthLimit: Long? = null,

        // Safe search general
        @Optional
        val targetSelection: SafeRealTimeSearchTargetSelection? = null,

        // SRTS
        @Optional
        val safetyExplorationRatio: Double? = null,
        @Optional
        val safetyProof: SafetyProof? = null,

        // SZero
        @Optional
        val safetyBackup: SafetyBackup? = null,

        // Anytime Experiment
        @Optional
        val anytimeMaxCount: Long = 0
)
