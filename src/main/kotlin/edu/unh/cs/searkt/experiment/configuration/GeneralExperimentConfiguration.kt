package edu.unh.cs.searkt.experiment.configuration

import edu.unh.cs.searkt.experiment.configuration.realtime.LookaheadType
import edu.unh.cs.searkt.experiment.configuration.realtime.TerminationType
import edu.unh.cs.searkt.planner.*
import edu.unh.cs.searkt.planner.realtime.BackupComparator
import edu.unh.cs.searkt.planner.realtime.TBAOptimization
import edu.unh.cs.searkt.planner.suboptimal.SuboptimalBoundImprovement
import kotlinx.serialization.Transient
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
    @Transient val rawDomain: String? = null,
    var algorithmName: Planners,
    @Transient
        val terminationType: TerminationType = TerminationType.UNLIMITED,
    @Transient val domainPath: String? = null,
    val actionDuration: Long,
    @Transient
        val timeLimit: Long? = null,
    @Transient
        val expansionLimit: Long? = null,

        // Domain
    @Transient
        var domainSeed: Long? = null,
    @Transient
        val domainSizeMultiplier: Int = 1,

        // AStar
    @Transient
        var weight: Double? = null,

        // Real time experiment
    @Transient
        val lookaheadType: LookaheadType? = null,
    @Transient
        val commitmentStrategy: CommitmentStrategy? = null,
    @Transient
        val terminationTimeEpsilon: Long = TimeUnit.NANOSECONDS.convert(2000, TimeUnit.MICROSECONDS),
    @Transient
        val stepLimit: Long? = null,

        // RTA*
    @Transient
        val lookaheadDepthLimit: Long? = null,

        // Safe search general
    @Transient
        val targetSelection: SafeRealTimeSearchTargetSelection? = null,

        // SRTS
    @Transient
        val safetyExplorationRatio: Double? = null,
    @Transient
        val safetyProof: SafetyProof? = null,
    @Transient
        val filterUnsafe: Boolean = false,

    @Transient
        val variant: String? = null,

        // SZero
    @Transient
        val safetyBackup: SafetyBackup? = null,

        // Anytime Experiment
    @Transient
        val anytimeMaxCount: Long? = null,

        // Error Models
    @Transient val errorModel: String? = null,

        //Envelope-based searching (i.e. not LSS)
    @Transient
        val backlogRatio: Double? = null,

        // TBA*
    @Transient
        val tbaOptimization: TBAOptimization? = null,

        // Envelope Search
    @Transient
        val backupComparator: BackupComparator? = null,

        // Bounded Suboptimal Exploration
    @Transient
        val embeddedAlgorithm: Planners? = null,

    @Transient
        val suboptimalBoundImprovement: SuboptimalBoundImprovement? = null

)
