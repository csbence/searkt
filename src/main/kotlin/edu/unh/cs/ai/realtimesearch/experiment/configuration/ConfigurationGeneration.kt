package edu.unh.cs.ai.realtimesearch.experiment.configuration

import edu.unh.cs.ai.realtimesearch.environment.Domains
import edu.unh.cs.ai.realtimesearch.experiment.configuration.Configurations.*
import edu.unh.cs.ai.realtimesearch.experiment.configuration.realtime.LookaheadType
import edu.unh.cs.ai.realtimesearch.experiment.configuration.realtime.TerminationType
import edu.unh.cs.ai.realtimesearch.planner.Planners

/**
 * @author Bence Cserna (bence@cserna.net)
 */
fun <K, V> Collection<Map<K, V>>.cartesianProduct(key: K, values: Iterable<V>): Collection<Map<K, V>> {
    return map { original ->
        values.map { value ->
            val newMap = HashMap(original)
            newMap[key] = value
            newMap
        }
    }.flatten()
}

typealias DomainPath = String

fun generateConfigurations(
        domains: Iterable<Pair<Domains, DomainPath>>,
        planners: Iterable<Planners>,
        actionDurations: Iterable<Long>,
        terminationType: TerminationType,
        lookaheadType: LookaheadType,
        timeLimit: Long,
        expansionLimit: Long,
        stepLimit: Long,
        domainExtras: List<Triple<Domains, String, Iterable<Long>>>? = null,
        plannerExtras: Iterable<Triple<Planners, Any, Iterable<Any>>>? = null): Collection<Map<String, Any>> {

    var configurations: Collection<Map<String, Any>> = domains.map {
        mapOf(DOMAIN_NAME.toString() to it.first.toString(), DOMAIN_PATH.toString() to it.second)
    }

    configurations = configurations.cartesianProduct(ALGORITHM_NAME.toString(), planners.map(Any::toString)).toMutableList()
    configurations = configurations.cartesianProduct(ACTION_DURATION.toString(), actionDurations).toMutableList()
    configurations = configurations.cartesianProduct(TERMINATION_TYPE.toString(), listOf(terminationType.toString())).toMutableList()
    configurations = configurations.cartesianProduct(LOOKAHEAD_TYPE.toString(), listOf(lookaheadType.toString())).toMutableList()
    configurations = configurations.cartesianProduct(TIME_LIMIT.toString(), listOf(timeLimit)).toMutableList()
    configurations = configurations.cartesianProduct(EXPANSION_LIMIT.toString(), listOf(expansionLimit)).toMutableList()
    configurations = configurations.cartesianProduct(STEP_LIMIT.toString(), listOf(stepLimit)).toMutableList()

    // Apply planner and domain specific extras
    fun <T> applyExtras(extras: Iterable<Triple<T, Any, Iterable<Any>>>?, matchKey: Any) {
        extras?.forEach { (matchValue, key, values) ->
            val irrelevantConfigurations = configurations.filter { it[matchKey.toString()] != matchValue.toString() }
            val relevantConfigurations = configurations.filter { it[matchKey.toString()] == matchValue.toString() }

            configurations = irrelevantConfigurations + relevantConfigurations.cartesianProduct(key.toString(), values).toMutableList()
        }
    }

    applyExtras(plannerExtras, ALGORITHM_NAME)
    applyExtras(domainExtras, DOMAIN_NAME)

    return  configurations
}