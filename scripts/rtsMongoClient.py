#!/usr/bin/python3

import matplotlib.pyplot as plt
import numpy as np
from pymongo import MongoClient

import plotutils


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


def get_get_per_duration_data(db, algorithm, domain, instance):
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
            times_for_durations.append(plotutils.cnv_ns_to_ms(result['result']['goalAchievementTime']))

        data_action_durations.append(times_for_durations)

    return data_action_durations


def get_gat_data(db, algorithm, domain, instance):
    gat_data = []
    data_tiles = db.experimentResult.find({
        "result.experimentConfiguration.domainName": domain,
        "result.experimentConfiguration.algorithmName": algorithm,
        "result.experimentConfiguration.domainInstanceName": instance,
        "result.success": True
    })

    for result in data_tiles:
        gat_data.append(plotutils.cnv_ns_to_ms(result['result']['goalAchievementTime']))

    return gat_data


def plot_gat_duration_error(db, algorithms, domain, instance, graph_astar=True):
    # Gather required A* data
    astar_gat_per_duration = get_get_per_duration_data(db, "A_STAR", domain, instance)
    astar_gat_per_duration_means, astar_confidence_interval_low, astar_confidence_interval_high = \
        plotutils.mean_confidence_intervals(astar_gat_per_duration)

    x = np.arange(1, len(astar_gat_per_duration_means) + 1)
    # Plot A* as well if requested
    if graph_astar:
        plt.errorbar(x, astar_gat_per_duration_means, label='A*',
                     yerr=(astar_confidence_interval_low, astar_confidence_interval_high))

    # Plot for each provided algorithm
    for algorithm in algorithms:
        algorithm_gat_per_duration = get_get_per_duration_data(db, algorithm, domain, instance)
        algorithm_gat_per_duration_means, algorithm_confidence_interval_low, algorithm_confidence_interval_high = \
            plotutils.mean_confidence_intervals(algorithm_gat_per_duration)
        plt.errorbar(x, algorithm_gat_per_duration_means, label=plotutils.translate_algorithm_name(algorithm),
                     yerr=(algorithm_confidence_interval_low, algorithm_confidence_interval_high))

    # Set labels
    plt.title(plotutils.translate_domain_name(domain) + " - " + instance)
    plt.ylabel("GAT log10 factor of optimal")
    plt.xlabel("expansions per unit duration log10")
    plt.xlim([0.9, 5.1])
    plt.legend()
    return plt


def plot_gat_boxplots(db, algorithms, domain, instance):
    y = []
    labels = []

    # Plot for each provided algorithm
    for algorithm in algorithms:
        data = get_gat_data(db, algorithm, domain, instance)
        y.append(data)
        labels.append(plotutils.translate_algorithm_name(algorithm))

    med, confidence_interval_low, confidence_interval_high = plotutils.median_confidence_intervals(y)

    # Set labels and build plots
    plt.title(plotutils.translate_domain_name(domain) + "-" + instance)
    plt.ylabel("Goal Achievement Time (ms)")
    plt.xlabel("Algorithms")
    plt.boxplot(y, notch=False, labels=labels)
    # Plot separate error bars without line to show median confidence intervals
    x = np.arange(1, len(y) + 1)
    plt.errorbar(x, med, yerr=(confidence_interval_low, confidence_interval_high), fmt='none',
                 linewidth=3)

    return plt


if __name__ == '__main__':
    db = open_connection()
    print_counts(db)
    plot_gat_duration_error(db, ["LSS_LRTA_STAR",
                                 "RTA_STAR",
                                 "DYNAMIC_F_HAT"], "GRID_WORLD", "input/vacuum/dylan/slalom.vw").show()
    # plot_gat_boxplots(db, ["LSS_LRTA_STAR",
    #                        "RTA_STAR",
    #                        "DYNAMIC_F_HAT"], "GRID_WORLD", "input/vacuum/dylan/slalom.vw").show()
