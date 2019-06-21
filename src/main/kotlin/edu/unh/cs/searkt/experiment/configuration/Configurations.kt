package edu.unh.cs.searkt.experiment.configuration

/**
 * Valid experiment configuration keys.
 *
 * @author Mike Bogochow
 * @author Bence Cserna (bence@cserna.net)
 * @since 4/3/16
 */
enum class Configurations(val configurationName: String) {
    // General configurations
    ALGORITHM_NAME("algorithmName"),
    DOMAIN_NAME("domainName"),
    DOMAIN_INSTANCE_NAME("domainInstanceName"),
    RAW_DOMAIN("rawDomain"),
    DOMAIN_PATH("domainPath"),
    TIME_LIMIT("timeLimit"),
    EXPANSION_LIMIT("expansionLimit"),
    STEP_LIMIT("stepLimit"),
    ACTION_DURATION("actionDuration"),
    TERMINATION_TYPE("terminationType"),
    LOOKAHEAD_STRATEGY("lookaheadStrategy"),
    // Domain-specific configurations
    DOMAIN_SEED("domainSeed"),
    IS_SAFE("isSafe"),
    // Real-time search configurations
    LOOKAHEAD_TYPE("lookaheadType"),
    COMMITMENT_STRATEGY("commitmentStrategy"),
    LOOKAHEAD_DEPTH_LIMIT("lookaheadDepthLimit"),
    TERMINATION_EPSILON("terminationTimeEpsilon"),
    // Anytime search configurations
    ANYTIME_MAX_COUNT("anytimeMaxCount"),
    // Weighted A* configurations
    WEIGHT("weight"),
    // Point Robot with Inertia configurations
    NUM_ACTIONS("numActions"),
    ACTION_FRACTION("actionFraction"),
    STATE_FRACTION("stateFraction");

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