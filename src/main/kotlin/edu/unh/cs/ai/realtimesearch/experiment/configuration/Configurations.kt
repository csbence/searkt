package edu.unh.cs.ai.realtimesearch.experiment.configuration

/**
 * Valid experiment configuration keys.
 *
 * @author Mike Bogochow
 * @author Bence Cserna (bence@cserna.net)
 * @since 4/3/16
 */
enum class Configurations {
    // General configurations
    ALGORITHM_NAME          ("algorithmName"),
    DOMAIN_NAME             ("domainName"),
    DOMAIN_INSTANCE_NAME    ("domainInstanceName"),
    RAW_DOMAIN              ("rawDomain"),
    DOMAIN_PATH             ("domainPath"),
    TIME_LIMIT              ("timeLimit"),
    ACTION_DURATION         ("actionDuration"),
    TERMINATION_TYPE        ("terminationType"),
    // Real-time search configurations
    LOOKAHEAD_TYPE          ("lookaheadType"),
    COMMITMENT_STRATEGY     ("commitmentStrategy"),
    LOOKAHEAD_DEPTH_LIMIT   ("lookaheadDepthLimit"),
    // Anytime search configurations
    ANYTIME_MAX_COUNT       ("anytimeMaxCount"),
    // Weighted A* configurations
    WEIGHT                  ("weight"),
    // Point Robot with Inertia configurations
    NUM_ACTIONS             ("numActions"),
    ACTION_FRACTION         ("actionFraction"),
    STATE_FRACTION          ("stateFraction");


    val configurationName: String
    constructor(configurationName: String) {
        this.configurationName = configurationName
    }

    companion object {
        val valueMap by lazy {
            val map = mutableMapOf<String, Configurations>()
            val values = values()
            for (value in values) {
                map.put(value.configurationName, value)
            }
            map
        }

        fun fromName(name: String): Configurations? = valueMap[name]
    }

    override fun toString(): String {
        return configurationName
    }
}