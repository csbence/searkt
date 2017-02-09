package edu.unh.cs.ai.realtimesearch.experiment.configuration

import edu.unh.cs.ai.realtimesearch.actionDurations
import edu.unh.cs.ai.realtimesearch.environment.Domains
import edu.unh.cs.ai.realtimesearch.experiment.configuration.realtime.LookaheadType
import edu.unh.cs.ai.realtimesearch.experiment.configuration.realtime.TerminationType
import edu.unh.cs.ai.realtimesearch.planner.CommitmentStrategy
import edu.unh.cs.ai.realtimesearch.planner.Planners
import edu.unh.cs.ai.realtimesearch.terminationType
import edu.unh.cs.ai.realtimesearch.timeLimit

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
        commitmentStrategy: Iterable<CommitmentStrategy>,
        actionDurations: Iterable<Long>,
        terminationType: TerminationType,
        lookaheadType: LookaheadType,
        timeLimit: Long): Collection<GeneralExperimentConfiguration> {

    var configurations: MutableCollection<Map<String, Any>> = mutableListOf()

    domains.mapTo(configurations, {
        mutableMapOf(Configurations.DOMAIN_NAME.toString() to it.first.toString(), Configurations.DOMAIN_PATH.toString() to it.second)
    })

    configurations = configurations.cartesianProduct(Configurations.ALGORITHM_NAME.toString(), planners.map(Any::toString)).toMutableList()
    configurations = configurations.cartesianProduct(Configurations.COMMITMENT_STRATEGY.toString(), commitmentStrategy.map(Any::toString)).toMutableList()
    configurations = configurations.cartesianProduct(Configurations.ACTION_DURATION.toString(), actionDurations).toMutableList()
    configurations = configurations.cartesianProduct(Configurations.TERMINATION_TYPE.toString(), listOf(terminationType.toString())).toMutableList()
    configurations = configurations.cartesianProduct(Configurations.LOOKAHEAD_TYPE.toString(), listOf(lookaheadType.toString())).toMutableList()
    configurations = configurations.cartesianProduct(Configurations.TIME_LIMIT.toString(), listOf(timeLimit)).toMutableList()

    return configurations.map { GeneralExperimentConfiguration(HashMap(it)) }
}