#!/usr/bin/python3

import matplotlib.pyplot as plt
import numpy as np
from pymongo import MongoClient
from scipy import stats

import plotUtils


def open_connection():
    client = MongoClient('mongodb://aerials.cs.unh.edu:42830')
    client.rts.authenticate('rtsUser', 'VeLuvh4!', mechanism='SCRAM-SHA-1')
    return client.rts


def print_counts(db):
    configuration_status = db.command('collstats', 'experimentConfiguration')
    print('Configuration count: %d' % configuration_status['count'])
    task_status = db.command('collstats', 'experimentTask')
    print('Task count: %d' % task_status['count'])
    result_status = db.command('collstats', 'experimentResult')
    print('Result count: %d' % result_status['count'])
    # pprint.pprint(configuration_status, width=1)


def gat_per_duration_stats(data):
    means = [np.mean(x) for x in data]
    std = [stats.sem(x) for x in data]
    confidence_intervals = stats.t.interval(0.95, len(data) - 1, loc=means, scale=std)
    return means, means - confidence_intervals[0], confidence_intervals[1] - means


def plot_gat_duration_error(db, algorithms, domain, instance, graph_astar=True):
    # Gather required A* data
    astar_gat_per_duration = data_for_algorithm(db, "A_STAR", domain, instance)
    astar_gat_per_duration_means, astar_confidence_interval_low, astar_confidence_interval_high = \
        gat_per_duration_stats(astar_gat_per_duration)

    x = np.arange(1, len(astar_gat_per_duration_means) + 1)
    # Plot A* as well if requested
    if graph_astar:
        plt.errorbar(x, astar_gat_per_duration_means, label='A*',
                     yerr=(astar_confidence_interval_low, astar_confidence_interval_high))

    # Plot for each provided algorithm
    for algorithm in algorithms:
        algorithm_gat_per_duration = data_for_algorithm(db, algorithm, domain, instance)
        # algorithm_gat_per_duration_means = [np.mean(x) for x in algorithm_gat_per_duration]
        algorithm_gat_per_duration_means, algorithm_confidence_interval_low, algorithm_confidence_interval_high = \
            gat_per_duration_stats(algorithm_gat_per_duration)
        plt.errorbar(x, algorithm_gat_per_duration_means, label=plotUtils.translate_algorithm_name(algorithm),
                     yerr=(algorithm_confidence_interval_low, algorithm_confidence_interval_high))

    # Set labels and show
    plt.title(plotUtils.translate_domain_name(domain) + " - " + instance)
    plt.ylabel("GAT log10 factor of optimal")
    plt.xlabel("expansions per unit duration log10")
    plt.xlim([0.9, 5.1])
    plt.legend()
    return plt


def data_for_algorithm(db, algorithm, domain, instance):
    data_action_durations = []
    for i in reversed(range(1, 6)):
        action_duration = 100 * 10 ** i
        data_tiles_a_star = db.experimentResult.find({"result.experimentConfiguration.domainName": domain,
                                                      "result.experimentConfiguration.algorithmName": algorithm,
                                                      "result.experimentConfiguration.domainInstanceName": instance,
                                                      "result.experimentConfiguration.actionDuration": action_duration,
                                                      "result.success": True})
        times_for_durations = []
        for result in data_tiles_a_star:
            times_for_durations.append(plotUtils.cnv_ns_to_ms(result['result']['goalAchievementTime']))

        data_action_durations.append(times_for_durations)

    return data_action_durations


if __name__ == '__main__':
    db = open_connection()
    print_counts(db)
    plot_gat_duration_error(db, ["LSS_LRTA_STAR",
                                 "RTA_STAR",
                                 "DYNAMIC_F_HAT"], "GRID_WORLD", "input/vacuum/dylan/slalom.vw").show()
