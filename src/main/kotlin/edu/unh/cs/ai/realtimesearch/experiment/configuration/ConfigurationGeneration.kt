package edu.unh.cs.ai.realtimesearch.experiment.configuration

import edu.unh.cs.ai.realtimesearch.environment.Domains
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
        domainExtras: List<Triple<Domains, String, Iterable<Long>>>? = null,
        plannerExtras: Iterable<Triple<Planners, String, Iterable<Any>>>? = null): Collection<GeneralExperimentConfiguration> {

    var configurations: Collection<Map<String, Any>> = domains.map {
        mapOf(Configurations.DOMAIN_NAME.toString() to it.first.toString(), Configurations.DOMAIN_PATH.toString() to it.second)
    }

    configurations = configurations.cartesianProduct(Configurations.ALGORITHM_NAME.toString(), planners.map(Any::toString)).toMutableList()
    configurations = configurations.cartesianProduct(Configurations.ACTION_DURATION.toString(), actionDurations).toMutableList()
    configurations = configurations.cartesianProduct(Configurations.TERMINATION_TYPE.toString(), listOf(terminationType.toString())).toMutableList()
    configurations = configurations.cartesianProduct(Configurations.LOOKAHEAD_TYPE.toString(), listOf(lookaheadType.toString())).toMutableList()
    configurations = configurations.cartesianProduct(Configurations.TIME_LIMIT.toString(), listOf(timeLimit)).toMutableList()

    // Apply planner and domain specific extras
    fun <T> applyExtras(extras: Iterable<Triple<T, String, Iterable<Any>>>?, matchKey: String) {
        extras?.forEach { (matchValue, key, values) ->
            val irrelevantConfigurations = configurations.filter { it[matchKey] != matchValue.toString() }
            val relevantConfigurations = configurations.filter { it[matchKey] == matchValue.toString() }

            configurations = irrelevantConfigurations + relevantConfigurations.cartesianProduct(key, values).toMutableList()
        }
    }
    applyExtras(plannerExtras, Configurations.ALGORITHM_NAME.toString())
    applyExtras(domainExtras, Configurations.DOMAIN_NAME.toString())

    return configurations.map { GeneralExperimentConfiguration(HashMap(it)) }
}