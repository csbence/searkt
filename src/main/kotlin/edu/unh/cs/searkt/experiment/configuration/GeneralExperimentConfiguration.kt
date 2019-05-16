package edu.unh.cs.searkt.experiment.configuration

import edu.unh.cs.searkt.experiment.configuration.realtime.LookaheadType
import edu.unh.cs.searkt.experiment.configuration.realtime.TerminationType
import edu.unh.cs.searkt.planner.CommitmentStrategy
import edu.unh.cs.searkt.planner.SafeRealTimeSearchTargetSelection
import edu.unh.cs.searkt.planner.SafetyBackup
import edu.unh.cs.searkt.planner.SafetyProof
import edu.unh.cs.searkt.planner.realtime.BackupComparator
import edu.unh.cs.searkt.planner.realtime.TBAOptimization
import kotlinx.serialization.Optional
import kotlinx.serialization.Serializable
import java.util.concurrent.TimeUnit

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
        @Optional
        val terminationType: TerminationType = TerminationType.UNLIMITED,
        @Optional val domainPath: String? = null,
        val actionDuration: Long,
        @Optional
        val timeLimit: Long? = null,
        @Optional
        val expansionLimit: Long? = null,

        // Domain
        @Optional
        var domainSeed: Long? = null,
        @Optional
        val domainSizeMultiplier: Int = 1,

        // AStar
        @Optional
        var weight: Double? = null,

        // Real time experiment
        @Optional
        val lookaheadType: LookaheadType? = null,
        @Optional
        val commitmentStrategy: CommitmentStrategy? = null,
        @Optional
        val terminationTimeEpsilon: Long = TimeUnit.NANOSECONDS.convert(2000, TimeUnit.MICROSECONDS),
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
        @Optional
        val filterUnsafe: Boolean = false,

        @Optional
        val variant: String? = null,

        // SZero
        @Optional
        val safetyBackup: SafetyBackup? = null,

        // Anytime Experiment
        @Optional
        val anytimeMaxCount: Long? = null,

        // Error Models
        @Optional val errorModel: String? = null,

        //Envelope-based searching (i.e. not LSS)
        @Optional
        val backlogRatio: Double? = null,

        // TBA*
        @Optional
        val tbaOptimization: TBAOptimization? = null,

        // Envelope Search
        @Optional
        val backupComparator: BackupComparator? = null
)
