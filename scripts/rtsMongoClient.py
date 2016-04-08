#!/usr/bin/python3

import getopt
import matplotlib.pyplot as plt
import os
import sys
import warnings
from enum import Enum
from pymongo import MongoClient

import plotutils

warnings.filterwarnings("ignore", message=".*Source ID.*", module="matplotlib")
warnings.filterwarnings("ignore", message="invalid value encountered in multiply", module="scipy")


class GraphType(Enum):
    all = 1
    gatPerDuration = 2
    gatBoxPlot = 3
    gatBars = 4
    gatViolin = 5


script = os.path.basename(sys.argv[0])
options = "hs:qa:d:i:t:c:"
default_graph_type = GraphType.gatPerDuration
all_action_durations = (20000000, 40000000, 80000000, 160000000, 320000000)
all_action_durations_ms = [plotutils.cnv_ns_to_ms(duration) for duration in all_action_durations]
all_algorithms = ["A_STAR", "ARA_STAR", "RTA_STAR", "LSS_LRTA_STAR", "DYNAMIC_F_HAT"]
all_domains = ["GRID_WORLD", "SLIDING_TILE_PUZZLE_4", "ACROBOT", "POINT_ROBOT", "POINT_ROBOT_WITH_INERTIA", "RACETRACK"]
all_acrobot_instances = ["0.07-0.07",
                         "0.08-0.08",
                         "0.09-0.09",
                         "0.1-0.1",
                         "0.3-0.3"]
all_dylan_instances = ["dylan/cups",
                       "dylan/slalom",
                       "dylan/uniform",
                       "dylan/wall"]
all_racetrack_instances = ["input/racetrack/barto-big.track",
                           "input/racetrack/barto-small.track"]
sliding_tile_4_map_root = "input/tiles/korf/4/all"
all_sliding_tile_4_instances = [2, 3, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16, 17, 18, 19, 21, 23, 24, 25, 26, 27, 28,
                                29, 30, 31, 32, 34, 35, 36, 37, 38, 39, 40, 41, 42, 43, 44, 45, 47, 48, 49, 50,
                                51, 52, 53, 54, 55, 56, 57, 58, 59, 62, 64, 65, 66, 67, 68, 69, 70, 71, 73, 75,
                                76, 77, 78, 79, 80, 81, 83, 84, 85, 86, 87, 89, 92, 93, 94, 95, 96, 97, 98, 99, 100]


def usage():
    print("usage:")
    print("{} [{}]".format(script, options))
    print("options:")
    print("  h          print this usage info")
    print("  a<alg>     specify an algorithm, one per switch")
    print("  d<domain>  specify domain")
    print("  i<name>    specify instance name")
    print("  c<nano>    specify action duration")
    print("  s<file>    save to file")
    print("  t<type>    specify type of plot; default {}".format(default_graph_type.name))
    print("  q          quiet mode; no logging or graph showing")
    print("valid graph types:")
    for graph_type in GraphType:
        print("  " + str(graph_type).replace("GraphType.", ""))
    print("valid action durations:")
    for action_duration in all_action_durations:
        print("  " + str(action_duration))


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


def get_gat_per_duration_data(db, algorithm, domain, instance):
    data_action_durations = []
    for action_duration in reversed(all_action_durations):
        data_tiles = db.experimentResult.find({
            "result.experimentConfiguration.domainName": domain,
            "result.experimentConfiguration.algorithmName": algorithm,
            "result.experimentConfiguration.domainInstanceName": instance,
            "result.experimentConfiguration.actionDuration": action_duration,
            "result.success": True
        })
        times_for_durations = []
        for result in data_tiles:
            times_for_durations.append(plotutils.cnv_ns_to_ms(result['result']['goalAchievementTime']))

        if times_for_durations:  # not empty
            data_action_durations.append(times_for_durations)

    return data_action_durations


def get_gat_data(db, algorithms, domain, instance, action_duration):
    gat_data = []
    labels = []
    for algorithm in algorithms:
        data_tiles = db.experimentResult.find({
            "result.experimentConfiguration.domainName": domain,
            "result.experimentConfiguration.algorithmName": algorithm,
            "result.experimentConfiguration.domainInstanceName": instance,
            "result.experimentConfiguration.actionDuration": int(action_duration),
            "result.success": True
        })

        data = []
        for result in data_tiles:
            data.append(plotutils.cnv_ns_to_ms(result['result']['goalAchievementTime']))

        if data:  # not empty
            gat_data.append(data)
            labels.append(plotutils.translate_algorithm_name(algorithm))

    return gat_data, labels


# TODO add factor A*
def plot_gat_duration_error(db, algorithms, domain, instance):
    # Gather required A* data
    astar_gat_per_duration = get_gat_per_duration_data(db, "A_STAR", domain, instance)
    algorithm_data = {}
    # Plot for each provided algorithm
    for algorithm in algorithms:
        algorithm_data[algorithm] = get_gat_per_duration_data(db, algorithm, domain, instance)
    plotutils.plot_gat_duration_error(algorithm_data, astar_gat_per_duration, algorithms, all_action_durations,
                                      title=plotutils.translate_domain_name(domain) + " - " + instance)


def plot_gat_boxplots(db, algorithms, domain, instance, action_duration, showviolin=False):
    y, labels = get_gat_data(db, algorithms, domain, instance, action_duration)
    plotutils.plot_gat_boxplots(y, labels, showviolin=showviolin,
                                title=plotutils.translate_domain_name(domain) + "-" + instance)


def plot_gat_bars(db, algorithms, domain, instance, action_duration):
    y, labels = get_gat_data(db, algorithms, domain, instance, action_duration)
    plotutils.plot_gat_bars(y, labels, title=plotutils.translate_domain_name(domain) + "-" + instance)


def plot_all_for_domain(db, domain, instances):
    # all_gat_data = []
    all_error_data = {}
    all_astar_error_data = []

    for algorithm in all_algorithms:
        all_error_data[algorithm] = []
        for action_duration in all_action_durations:
            all_error_data[algorithm].append([])
    for action_duration in all_action_durations:
        all_astar_error_data.append([])

    for instance in instances:
        instance_file_name = instance.replace("/", "_")
        # instance_gat_data = []

        astar_gat_per_duration = get_gat_per_duration_data(db, "A_STAR", domain, instance)
        algorithm_gat_per_duration = {}
        for algorithm in all_algorithms:
            algorithm_gat_per_duration[algorithm] = get_gat_per_duration_data(db, algorithm, domain, instance)

        # Errorbar plot
        if not quiet:
            print("Plotting error plot: {} - {}".format(domain, instance))
        plotutils.plot_gat_duration_error(algorithm_gat_per_duration, astar_gat_per_duration, all_algorithms,
                                          all_action_durations_ms,
                                          title=plotutils.translate_domain_name(domain) + " - " + instance)
        plotutils.save_plot(plt, "plots/{}_{}_error.pdf".format(domain, instance_file_name))
        plt.close('all')

        for action_duration in all_action_durations:
            # Gather data
            y_gat, labels_gat = get_gat_data(db, all_algorithms, domain, instance, action_duration)
            # instance_gat_data.append(y_gat)

            # Boxplots
            if not quiet:
                print("Plotting boxplot: {} - {} - {}".format(domain, instance, action_duration))
            plotutils.plot_gat_boxplots(y_gat, labels_gat,
                                        title=plotutils.translate_domain_name(domain) + " - " + instance)
            plotutils.save_plot(plt, "plots/{}_{}_{}_boxplots.pdf".format(domain, instance_file_name, action_duration))
            plt.close('all')

            # Bars
            if not quiet:
                print("Plotting bars: {} - {} - {}".format(domain, instance, action_duration))
            plotutils.plot_gat_bars(y_gat, labels_gat,
                                    title=plotutils.translate_domain_name(domain) + " - " + instance)
            plotutils.save_plot(plt, "plots/{}_{}_{}_bars.pdf".format(domain, instance_file_name, action_duration))
            plt.close('all')

            # Violin
            if not quiet:
                print("Plotting violin: {} - {} - {}".format(domain, instance, action_duration))
            plotutils.plot_gat_boxplots(y_gat, labels_gat, showviolin=True,
                                        title=plotutils.translate_domain_name(domain) + " - " + instance)
            plotutils.save_plot(plt,
                                "plots/{}_{}_{}_boxplots_dist.pdf".format(domain, instance_file_name, action_duration))
            plt.close('all')

        # all_gat_data.append(instance_gat_data)

        for algorithm in all_algorithms:
            count = 0
            for val in algorithm_gat_per_duration[algorithm]:
                # print(val[0])
                all_error_data[algorithm][count].append(val[0])
                count += 1
        print(all_error_data)

        count = 0
        for val in astar_gat_per_duration:
            all_astar_error_data[count].append(val[0])
            count += 1
        print(all_astar_error_data)

    # print(all_error_data)
    # for algorithm in all_algorithms:
    #     all_error_data[algorithm] = list(chain.from_iterable(chain.from_iterable(all_error_data[algorithm])))
    # all_astar_error_data = list(chain.from_iterable(chain.from_iterable(all_astar_error_data)))

    if not quiet:
        print("Plotting {} averages".format(domain))
    print(all_error_data)
    print(all_astar_error_data)
    plotutils.plot_gat_duration_error(all_error_data, all_astar_error_data, all_algorithms, all_action_durations,
                                      title="{} data over all instances".format(
                                          plotutils.translate_domain_name(domain)))
    plotutils.save_plot(plt, "plots/{}_average.pdf".format(domain))
    plt.clf()


def get_all_grid_world_instances():
    instances = []
    for instance in all_dylan_instances:
        instances.append("input/vacuum/{}.vw".format(instance))
    return instances


def get_all_point_robot_instances():
    instances = []
    for instance in all_dylan_instances:
        instances.append("input/pointrobot/{}.pr".format(instance))
    return instances


def get_all_sliding_tile_instances():
    instances = []
    for instance in all_sliding_tile_4_instances:
        instances.append("{}/{}".format(sliding_tile_4_map_root, instance))
    return instances


def plot_all(db):
    if not os.path.exists("plots"):
        os.makedirs("plots")

    plot_all_for_domain(db, "GRID_WORLD", get_all_grid_world_instances())
    plot_all_for_domain(db, "POINT_ROBOT", get_all_point_robot_instances())
    plot_all_for_domain(db, "POINT_ROBOT_WITH_INERTIA", get_all_point_robot_instances())
    plot_all_for_domain(db, "RACETRACK", all_racetrack_instances)
    plot_all_for_domain(db, "ACROBOT", all_acrobot_instances)
    plot_all_for_domain(db, "SLIDING_TILE_PUZZLE_4", get_all_sliding_tile_instances())


if __name__ == '__main__':
    save_file = None
    quiet = False
    algorithms = []
    domain = None
    instance = None
    action_duration = all_action_durations[0]
    graph_type = default_graph_type

    try:
        opts, args = getopt.gnu_getopt(sys.argv[1:], options)
    except getopt.GetoptError as e:
        print("Getopt error: {0}".format(e.strerror))
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
        elif opt in ('-c', '--action'):
            action_duration = arg
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

    if graph_type is not GraphType.all:
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
        GraphType.gatBoxPlot: lambda: plot_gat_boxplots(db, algorithms, domain, instance, action_duration),
        GraphType.gatBars: lambda: plot_gat_bars(db, algorithms, domain, instance, action_duration),
        GraphType.gatViolin: lambda: plot_gat_boxplots(db, algorithms, domain, instance, action_duration,
                                                       showviolin=True),
        GraphType.all: lambda: plot_all(db)
    }[graph_type]

    plotter()

    # Save before showing since show resets the figures
    if save_file is not None:
        plotutils.save_plot(plt, save_file)

    if not quiet:
        plt.show()
