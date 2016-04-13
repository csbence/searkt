package edu.unh.cs.ai.realtimesearch.experiment.configuration

import edu.unh.cs.ai.realtimesearch.experiment.configuration.realtime.TimeBoundType
import edu.unh.cs.ai.realtimesearch.planner.Planners
import edu.unh.cs.ai.realtimesearch.planner.Planners.*

/**
 * Valid experiment configurations.
 *
 * @author Mike Bogochow
 * @since 4/3/16
 */
enum class Configurations {
    // General configurations
    ALGORITHM_NAME          ("algorithmName",       String::class.java),
    DOMAIN_NAME             ("domainName",          String::class.java),
    DOMAIN_INSTANCE_NAME    ("domainInstanceName",  String::class.java),
    RAW_DOMAIN              ("rawDomain",           String::class.java),
    TIME_LIMIT              ("timeLimit",           Long::class.java),
    ACTION_DURATION         ("actionDuration",      Long::class.java),
    TERMINATION_TYPE        ("terminationType",     String::class.java),
    // Realtime search configurations
    TIME_BOUND_TYPE         ("timeBoundType",       TimeBoundType::class.java,  LSS_LRTA_STAR, RTA_STAR, DYNAMIC_F_HAT),
    COMMITMENT_STRATEGY     ("commitmentStrategy",  String::class.java,         LSS_LRTA_STAR, RTA_STAR, DYNAMIC_F_HAT),
    LOOKAHEAD_DEPTH_LIMIT   ("lookaheadDepthLimit", Long::class.java,           RTA_STAR),
    // Anytime search configurations
    ANYTIME_MAX_COUNT       ("anytimeMaxCount",     Long::class.java,           ARA_STAR),
    // Weighted A* configurations
    WEIGHT                  ("weight",              Double::class.java,         WEIGHTED_A_STAR),
    NUM_ACTIONS             ("numActions",          Int::class.java),
    ACTION_FRACTION         ("actionFraction",      Double::class.java),
    STATE_FRACTION          ("stateFraction",       Double::class.java);



    val configurationName: String
    val valueType: Class<*>
    private val _planners: MutableList<Planners> = mutableListOf()
    val planners: List<Planners> get() = _planners.toList()

    constructor(configurationName: String, valueType: Class<*>, vararg planners: Planners) {
        this.configurationName = configurationName
        this.valueType = valueType
        for (planner in planners) {
            this._planners.add(planner)
        }
    }

    companion object {
        fun fromName(name: String): Configurations? {
            val values = values()
            for (value in values) {
                if (value.configurationName.equals(name))
                    return value
            }
            return null
        }
    }

    override fun toString(): String {
        return configurationName
    }
}