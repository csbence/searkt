#!/usr/bin/python3

import getopt
import matplotlib.pyplot as plt
import os
import plotutils
import sys
import warnings
from enum import Enum
from pymongo import MongoClient
from subprocess import call

warnings.filterwarnings("ignore", message=".*Source ID.*", module="matplotlib")
warnings.filterwarnings("ignore", message="Attempting to set identical bottom==top results", module="matplotlib")
warnings.filterwarnings("ignore", message="Mean of empty slice", module="numpy")
warnings.filterwarnings("ignore", message="invalid value encountered in double_scalars", module="numpy")
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
all_action_durations = (10000000, 20000000, 40000000, 80000000, 160000000, 320000000)
all_action_durations_ms = [plotutils.cnv_ns_to_ms(duration) for duration in all_action_durations]
all_algorithms = ["A_STAR", "ARA_STAR", "RTA_STAR", "LSS_LRTA_STAR", "DYNAMIC_F_HAT"]
plot_algorithms = ["A_STAR", "ARA_STAR", "RTA_STAR", "LSS_LRTA_STAR_STATIC_SINGLE", "LSS_LRTA_STAR_STATIC_MULTIPLE",
                   "LSS_LRTA_STAR_DYNAMIC_MULTIPLE", "DYNAMIC_F_HAT"]
no_RTA_STAR = [alg for alg in plot_algorithms if alg is not "RTA_STAR"]
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


def get_realtime_configuration(algorithm):
    if algorithm.__contains__("LSS_LRTA_STAR"):
        if algorithm.__contains__("SINGLE"):
            commitment_strategy = "SINGLE"
        elif algorithm.__contains__("MULTIPLE"):
            commitment_strategy = "MULTIPLE"
        else:
            commitment_strategy = None
        if algorithm.__contains__("DYNAMIC"):
            time_bound_type = "DYNAMIC"
        elif algorithm.__contains__("STATIC"):
            time_bound_type = "STATIC"
        else:
            time_bound_type = None
        return "LSS_LRTA_STAR", commitment_strategy, time_bound_type
    else:
        return algorithm, None, None


def get_realtime_gat_per_duration_data(db, algorithm, domain, instance, commitmentStrategy, timeBoundType):
    data_action_durations = []
    assert algorithm is "LSS_LRTA_STAR" or algorithm is "DYNAMIC_F_HAT" or algorithm is "RTA_STAR"

    for action_duration in all_action_durations:
        data_tiles = db.experimentResult.find({
            "result.experimentConfiguration.domainName": domain,
            "result.experimentConfiguration.algorithmName": algorithm,
            "result.experimentConfiguration.domainInstanceName": instance,
            "result.experimentConfiguration.actionDuration": action_duration,
            "result.experimentConfiguration.commitmentStrategy": commitmentStrategy,
            "result.experimentConfiguration.timeBoundType": timeBoundType,
            "result.success": True
        })

        times_for_durations = []
        for result in data_tiles:
            times_for_durations.append(plotutils.cnv_ns_to_ms(result['result']['goalAchievementTime']))

        # if times_for_durations:  # not empty
        data_action_durations.append(times_for_durations)

    return data_action_durations


def get_gat_per_duration_data(db, algorithm, domain, instance):
    algorithm, commitment_strategy, time_bound_type = get_realtime_configuration(algorithm)
    if commitment_strategy is not None and time_bound_type is not None:
        return get_realtime_gat_per_duration_data(db, algorithm, domain, instance, commitment_strategy,
                                                  time_bound_type)

    data_action_durations = []

    for action_duration in all_action_durations:
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

        # if times_for_durations:  # not empty
        data_action_durations.append(times_for_durations)

    return data_action_durations


def get_realtime_gat_data(db, algorithm, domain, instance, action_duration, commitmentStrategy, timeBoundType):
    data_tiles = db.experimentResult.find({
        "result.experimentConfiguration.domainName": domain,
        "result.experimentConfiguration.algorithmName": algorithm,
        "result.experimentConfiguration.domainInstanceName": instance,
        "result.experimentConfiguration.actionDuration": int(action_duration),
        "result.experimentConfiguration.commitmentStrategy": commitmentStrategy,
        "result.experimentConfiguration.timeBoundType": timeBoundType,
        "result.success": True
    })

    data = []
    for result in data_tiles:
        data.append(plotutils.cnv_ns_to_ms(result['result']['goalAchievementTime']))

    if data:  # not empty
        return data


def get_gat_data(db, algorithms, domain, instance, action_duration):
    gat_data = []
    labels = []
    for algorithm in algorithms:

        algorithm, commitment_strategy, time_bound_type = get_realtime_configuration(algorithm)
        if commitment_strategy is not None and time_bound_type is not None:
            data = get_realtime_gat_data(db, "LSS_LRTA_STAR", domain, instance, action_duration,
                                         commitment_strategy, time_bound_type)
        else:
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
    plotutils.plot_gat_duration_error(algorithm_data, astar_gat_per_duration, all_action_durations_ms,
                                      title=plotutils.translate_domain_name(domain) + " - " + instance)


def plot_gat_boxplots(db, algorithms, domain, instance, action_duration, showviolin=False):
    y, labels = get_gat_data(db, algorithms, domain, instance, action_duration)
    plotutils.plot_gat_boxplots(y, labels, showviolin=showviolin,
                                title=plotutils.translate_domain_name(domain) + "-" + instance)


def plot_gat_bars(db, algorithms, domain, instance, action_duration):
    y, labels = get_gat_data(db, algorithms, domain, instance, action_duration)
    plotutils.plot_gat_bars(y, labels, title=plotutils.translate_domain_name(domain) + "-" + instance)


def do_plot(file_header, file_suffix, plot):
    # file_header = "{}_{}".format(domain, instance_file_name)
    filename = "plots/{}_{}.pdf".format(file_header, file_suffix)
    lgd = plot()
    if lgd:
        plotutils.save_plot_with_outer_legend(plt, filename, lgd)
    else:
        plotutils.save_plot(plt, filename)
    plt.close('all')
    return "![{}]({})\n\n\\clearpage\n\n".format(file_header, filename)


def save_to_file(filename, text):
    text_file = open(filename, "w")
    text_file.write(text)
    text_file.close()


# http://stackoverflow.com/questions/377017/test-if-executable-exists-in-python#answer-377028
def which(program):
    def is_exe(fpath):
        return os.path.isfile(fpath) and os.access(fpath, os.X_OK)

    fpath, fname = os.path.split(program)
    if fpath:
        if is_exe(program):
            return program
    else:
        for path in os.environ["PATH"].split(os.pathsep):
            path = path.strip('"')
            exe_file = os.path.join(path, program)
            if is_exe(exe_file):
                return exe_file

    return None


def remove_empty_data(dict):
    for key, data in list(dict.items()):
        has_data = False
        for subdata in data:
            if subdata:
                has_data = True
                break
        if not has_data:
            del dict[key]


def plot_all_for_domain(db, domain, instances, plot_average=False, average_only=False, markdown_summary=True):
    if average_only:
        markdown_summary = False
        plot_average = True

    # all_gat_data = []
    all_error_data = {}
    all_astar_error_data = []
    markdown_document = "# Domain: {}\n\n".format(domain)

    for algorithm in plot_algorithms:
        all_error_data[algorithm] = []
        for action_duration in all_action_durations:
            all_error_data[algorithm].append([])
    for action_duration in all_action_durations:
        all_astar_error_data.append([])

    for instance in instances:
        instance_file_name = instance.replace("/", "_")
        # instance_gat_data = []

        markdown_document += "## Instance: {}\n\n".format(instance)
        print("Processing {} - {}".format(domain, instance))

        # Gather error data for each algorithm
        astar_gat_per_duration = get_gat_per_duration_data(db, "A_STAR", domain, instance)
        algorithm_gat_per_duration = {}
        for algorithm in all_error_data.keys():
            algorithm_gat_per_duration[algorithm] = get_gat_per_duration_data(db, algorithm, domain, instance)
        remove_empty_data(algorithm_gat_per_duration)

        # Errorbar plot
        if not average_only:
            if not quiet:
                print("Plotting error plot: {} - {}".format(domain, instance))
            file_header = "{}_{}".format(domain, instance_file_name)
            markdown_document += do_plot(file_header, "error", lambda:
            plotutils.plot_gat_duration_error(algorithm_gat_per_duration, astar_gat_per_duration,
                                              all_action_durations_ms,
                                              title=plotutils.translate_domain_name(domain) + " - " + instance))

        if not average_only:
            for action_duration in all_action_durations:
                # Gather data
                y_gat, labels_gat = get_gat_data(db, plot_algorithms, domain, instance, action_duration)
                # instance_gat_data.append(y_gat)

                markdown_document += "### Action Duration: {} ns\n\n".format(action_duration)

                # Boxplots
                if not quiet:
                    print("Plotting boxplot: {} - {} - {}".format(domain, instance, action_duration))
                file_header = "{}_{}_{}".format(domain, instance_file_name, action_duration)
                markdown_document += do_plot(file_header, "boxplots", lambda:
                plotutils.plot_gat_boxplots(y_gat, labels_gat,
                                            title=plotutils.translate_domain_name(domain) + " - " + instance))

                # Bars
                if not quiet:
                    print("Plotting bars: {} - {} - {}".format(domain, instance, action_duration))
                markdown_document += do_plot(file_header, "bars", lambda:
                plotutils.plot_gat_bars(y_gat, labels_gat,
                                        title=plotutils.translate_domain_name(domain) + " - " + instance))

                # Violin
                if not quiet:
                    print("Plotting violin: {} - {} - {}".format(domain, instance, action_duration))
                markdown_document += do_plot(file_header, "boxplots_dist", lambda:
                plotutils.plot_gat_boxplots(y_gat, labels_gat, showviolin=True,
                                            title=plotutils.translate_domain_name(domain) + " - " + instance))

        # all_gat_data.append(instance_gat_data)
        for algorithm, values in algorithm_gat_per_duration.items():
            count = 0
            for val in values:
                if val:
                    all_error_data[algorithm][count].append(val[0])
                else:
                    all_error_data[algorithm][count].append([])
                count += 1

        count = 0
        for val in astar_gat_per_duration:
            if val:
                all_astar_error_data[count].append(val[0])
            else:
                all_astar_error_data[count].append([])
            count += 1

    if markdown_summary:
        if not quiet:
            print("Saving markdown file")
        file_header = "plots/{}_plots".format(domain)
        markdown_file = "{}.md".format(file_header)
        pdf_file = "{}.pdf".format(file_header)
        save_to_file(markdown_file, markdown_document)
        pandoc = which("pandoc")
        if pandoc:
            call([pandoc, "-o", pdf_file, markdown_file])

    remove_empty_data(all_error_data)

    # Plot average data
    if plot_average:
        if not quiet:
            print("Plotting {} averages".format(domain))
        lgd = plotutils.plot_gat_duration_error(all_error_data, all_astar_error_data, all_action_durations_ms,
                                                title="{} data over all instances".format(
                                                    plotutils.translate_domain_name(domain)))
        plotutils.save_plot_with_outer_legend(plt, "plots/{}_average.pdf".format(domain), lgd)
        plt.close('all')

        # Plot Sliding tile puzzle average without RTA*
        if domain is "SLIDING_TILE_PUZZLE_4":
            all_error_data.pop("RTA_STAR")
            lgd = plotutils.plot_gat_duration_error(all_error_data, all_astar_error_data, all_action_durations_ms,
                                                    title="{} data over all instances".format(
                                                        plotutils.translate_domain_name(domain)))
            plotutils.save_plot_with_outer_legend(plt, "plots/{}_NO_RTA_STAR_average.pdf".format(domain), lgd)
            plt.close('all')


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
    plot_all_for_domain(db, "ACROBOT", all_acrobot_instances, plot_average=True)
    plot_all_for_domain(db, "SLIDING_TILE_PUZZLE_4", get_all_sliding_tile_instances(), plot_average=True)


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
