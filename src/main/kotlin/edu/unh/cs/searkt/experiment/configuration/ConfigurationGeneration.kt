package edu.unh.cs.searkt.experiment.configuration

import edu.unh.cs.searkt.environment.Domains
import edu.unh.cs.searkt.experiment.configuration.Configurations.*
import edu.unh.cs.searkt.experiment.configuration.realtime.LookaheadType
import edu.unh.cs.searkt.experiment.configuration.realtime.TerminationType
import edu.unh.cs.searkt.planner.Planners

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
        timeLimit: Long,
        expansionLimit: Long,
        actionDurations: Iterable<Long>,
        terminationType: TerminationType? = null,
        lookaheadType: LookaheadType? = null,
        stepLimit: Long? = null,
        domainExtras: List<Triple<Domains, String, Iterable<Long>>>? = null,
        plannerExtras: Iterable<Triple<Planners, Any, Iterable<Any>>>? = null): Collection<Map<String, Any>> {

    var configurations: Collection<Map<String, Any>> = domains.map {
        mapOf(DOMAIN_NAME.toString() to it.first.toString(), DOMAIN_PATH.toString() to it.second)
    }

    configurations = configurations.cartesianProduct(ALGORITHM_NAME.toString(), planners.map(Any::toString)).toMutableList()
    configurations = configurations.cartesianProduct(ACTION_DURATION.toString(), actionDurations).toMutableList()
    configurations = configurations.cartesianProduct(EXPANSION_LIMIT.toString(), listOf(expansionLimit)).toMutableList()
    configurations = configurations.cartesianProduct(TIME_LIMIT.toString(), listOf(timeLimit)).toMutableList()

    if (terminationType != null) {
        configurations = configurations.cartesianProduct(TERMINATION_TYPE.toString(), listOf(terminationType.toString())).toMutableList()
    }

    if (lookaheadType != null) {
        configurations = configurations.cartesianProduct(LOOKAHEAD_TYPE.toString(), listOf(lookaheadType.toString())).toMutableList()
    }

    if (stepLimit != null) {
        configurations = configurations.cartesianProduct(STEP_LIMIT.toString(), listOf(stepLimit)).toMutableList()
    }

    val extraConfigurations = listOf(Pair(plannerExtras, ALGORITHM_NAME), Pair(domainExtras, DOMAIN_NAME))

    extraConfigurations.forEach { (extras, matchKey) ->
        extras?.forEach { (matchValue, key, values) ->
            val (relevantConfigurations, irrelevantConfigurations) = configurations.partition { it[matchKey.toString()] == matchValue.toString() }
            configurations = irrelevantConfigurations + relevantConfigurations.cartesianProduct(key.toString(), values).toMutableList()
        }
    }

    return configurations
}