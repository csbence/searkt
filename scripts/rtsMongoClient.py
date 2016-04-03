#!/usr/bin/python3

import getopt
import matplotlib.pyplot as plt
import numpy as np
import os
import sys
from enum import Enum
from pymongo import MongoClient

import plotutils


class GraphType(Enum):
    gatPerDuration = 1
    gatBoxPlot = 2
    gatBars = 3
    gatLines = 4

script = os.path.basename(sys.argv[0])
options = "hs:qa:d:i:t:"
default_graph_type = GraphType.gatPerDuration


def usage():
    print("usage:")
    print("{} [{}]".format(script, options))
    print("options:")
    print("  h          print this usage info")
    print("  a<alg>     specify an algorithm, one per switch")
    print("  d<domain>  specify domain")
    print("  i<name>    specify instance name")
    print("  s<file>    save to file")
    print("  t<type>    specify type of plot; default {}".format(default_graph_type.name))
    print("  q          quiet mode; no logging or graph showing")


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


def get_gat_data(db, algorithms, domain, instance):
    gat_data = []
    labels = []

    for algorithm in algorithms:
        data_tiles = db.experimentResult.find({
            "result.experimentConfiguration.domainName": domain,
            "result.experimentConfiguration.algorithmName": algorithm,
            "result.experimentConfiguration.domainInstanceName": instance,
            "result.success": True
        })

        data = []
        for result in data_tiles:
            data.append(plotutils.cnv_ns_to_ms(result['result']['goalAchievementTime']))
        gat_data.append(data)
        labels.append(plotutils.translate_algorithm_name(algorithm))

    return gat_data, labels


# TODO add log10, factor A*, expansions per duration
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


def plot_gat_boxplots(db, algorithms, domain, instance, showviolin=False):
    y, labels = get_gat_data(db, algorithms, domain, instance)
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

    if showviolin:
        mean, mean_confidence_interval_low, mean_confidence_interval_high = plotutils.mean_confidence_intervals(y)
        plt.violinplot(y, showmeans=True, showmedians=True)
        plt.errorbar(x, mean, yerr=(mean_confidence_interval_low, mean_confidence_interval_high), fmt='none',
                     linewidth=3, color='g')

    return plt


def plot_gat_bars(db, algorithms, domain, instance):
    y, labels = get_gat_data(db, algorithms, domain, instance)
    x = np.arange(1, len(y) + 1)
    med, med_confidence_interval_low, med_confidence_interval_high = plotutils.median_confidence_intervals(y)
    mean, mean_confidence_interval_low, mean_confidence_interval_high = plotutils.mean_confidence_intervals(y)
    width = 0.35

    fig, axis = plt.subplots()
    med_bars = axis.bar(x, med, width, color='r', yerr=(med_confidence_interval_low, med_confidence_interval_high))
    mean_bars = axis.bar(x + width, mean, width, color='y',
                         yerr=(mean_confidence_interval_low, mean_confidence_interval_high))

    axis.set_title(plotutils.translate_domain_name(domain) + "-" + instance)
    axis.set_ylabel("Goal Achievement Time (ms)")
    axis.set_xlabel("Algorithms")
    axis.set_xticks(x + width)
    axis.set_xticklabels(labels)
    axis.legend((med_bars, mean_bars), ('Median', 'Mean'))

    return plt


def plot_gat_lines(db, algorithms, domain, instance):
    y, labels = get_gat_data(db, algorithms, domain, instance)
    x = np.arange(1, len(y) + 1)
    med, med_confidence_interval_low, med_confidence_interval_high = plotutils.median_confidence_intervals(y)
    plt.title(plotutils.translate_domain_name(domain) + "-" + instance)
    plt.ylabel("Goal Achievement Time (ms)")
    plt.xlabel("Algorithms")
    plt.xlim(x[0] - 0.1, x[len(x) - 1] + 0.1)
    plt.errorbar(x, med, yerr=(med_confidence_interval_low, med_confidence_interval_high))
    plt.xticks(x, labels)

    return plt


if __name__ == '__main__':
    save_file = None
    quiet = False
    algorithms = []
    domain = None
    instance = None
    graph_type = default_graph_type

    try:
        opts, args = getopt.gnu_getopt(sys.argv[1:], options)
    except getopt.GetoptError:
        usage()
        sys.exit(2)

    for opt, arg in opts:
        if opt in ('-h', '--help'):
            usage()
            sys.exit(0)
        elif opt in ('-a', '--algorithm'):
            algorithms.append(arg)
        elif opt in ('-d', '--domain'):
            domain = arg
        elif opt in ('-i', '--instance'):
            instance = arg
        elif opt in ('-t', '--type'):
            graph_type = getattr(GraphType, arg)
        elif opt in ('-s', '--save'):
            save_file = arg
        elif opt in ('-q', '--quiet'):
            quiet = True
        else:
            print("invalid switch '%s'" % opt)
            usage()
            sys.exit(2)

    if not algorithms:
        print("Must provide at least 1 algorithm")
        usage()
        sys.exit(2)
    elif domain is None or instance is None:
        print("Must provide domain and instance")
        usage()
        sys.exit(2)

    db = open_connection()
    if not quiet:
        print_counts(db)

    plotter = {
        GraphType.gatPerDuration: lambda: plot_gat_duration_error(db, algorithms, domain, instance),
        GraphType.gatBoxPlot: lambda: plot_gat_boxplots(db, algorithms, domain, instance),
        GraphType.gatBars: lambda: plot_gat_bars(db, algorithms, domain, instance),
        GraphType.gatLines: lambda: plot_gat_lines(db, algorithms, domain, instance)
    }[graph_type]

    plot = plotter()

    # Save before showing since show resets the figures
    if save_file is not None:
        plotutils.save_plot(plt, save_file)

    if not quiet:
        plot.show()

    # plot_gat_duration_error(db, ["LSS_LRTA_STAR",
    #                              "RTA_STAR",
    #                              "DYNAMIC_F_HAT"], "GRID_WORLD", "input/vacuum/dylan/slalom.vw")
    # plot_gat_boxplots(db, ["LSS_LRTA_STAR",
    #                        "RTA_STAR",
    #                        "DYNAMIC_F_HAT"], "GRID_WORLD", "input/vacuum/dylan/slalom.vw").show()
