import math
import matplotlib.cbook as cbook
import matplotlib.pyplot as plt
import numpy as np
import os
import re
from matplotlib.backends.backend_pdf import PdfPages
from scipy import stats


def cnv_ns_to_ms(ns):
    return ns / 1000000.0


class Results:
    ALGORITHM = 0
    DOMAIN = 1

    def __init__(self, parsedJson):
        self.configuration = (parsedJson['experimentConfiguration']['algorithmName'],
                              parsedJson['experimentConfiguration']['domainName'])
        self.generatedNodes = parsedJson['generatedNodes']
        self.expandedNodes = parsedJson['expandedNodes']
        self.actions = parsedJson['actions']
        self.time = cnv_ns_to_ms(parsedJson['goalAchievementTime'])


def translate_algorithm_name(alg_name):
    # Handle hat (^) names
    if "HAT" in alg_name:
        alg_name = re.sub(r"(.*)_(.*)_(HAT)", r"\1", alg_name) \
                   + re.sub(r"(.*)_(.*)_(HAT)", r"_$\\hat{\2}$", alg_name).lower()
    # Specific word formatting
    alg_name = alg_name.replace('DYNAMIC', 'Dynamic')
    alg_name = alg_name.replace('WEIGHTED', 'Weighted')
    alg_name = alg_name.replace('LSS_', 'LSS-')
    # Handle star (*) names
    alg_name = alg_name.replace('_STAR', '*')
    # Replace rest of underscores
    alg_name = alg_name.replace('_', ' ')
    return alg_name


def translate_domain_name(domain_name):
    # Replace underscores
    domain_name = domain_name.replace('_', ' ')
    # Convert case
    domain_name = domain_name.title()
    return domain_name


def median_confidence_intervals(data):
    if not data:  # empty
        return [0], [0], [0]
    bxpstats = cbook.boxplot_stats(data)
    confidence_intervals = [[], []]
    medians = []
    for stat in bxpstats:
        confidence_intervals[0].append(stat['cilo'])
        confidence_intervals[1].append(stat['cihi'])
        medians.append(stat['med'])
    confidence_intervals[0] = np.array(confidence_intervals[0])
    confidence_intervals[1] = np.array(confidence_intervals[1])
    return medians, medians - confidence_intervals[0], confidence_intervals[1] - medians


def mean_confidence_intervals(data):
    if not data:  # empty
        return [0], [0], [0]
    means = np.nan_to_num([np.mean(x) for x in data])
    std = np.nan_to_num([stats.sem(x) if len(x) > 1 else 0.0 for x in data])
    confidence_intervals = stats.t.interval(0.95, len(data) - 1, loc=means, scale=std)
    confidence_intervals = [np.nan_to_num(ci) for ci in confidence_intervals]
    return means, means - confidence_intervals[0], confidence_intervals[1] - means


def save_plot(plot, filename):
    basename, ext = os.path.splitext(filename)
    if ext is '.pdf':
        pp = PdfPages(filename)
        plot.savefig(pp, format='pdf')
        pp.close()
    else:
        # Try and save it
        plot.savefig(filename)


def plot_gat_bars(data, labels):
    y = data
    x = np.arange(1, len(y) + 1)
    med, med_confidence_interval_low, med_confidence_interval_high = median_confidence_intervals(y)
    mean, mean_confidence_interval_low, mean_confidence_interval_high = mean_confidence_intervals(y)
    width = 0.35

    fig, axis = plt.subplots()
    med_bars = axis.bar(x, med, width, color='r', yerr=(med_confidence_interval_low, med_confidence_interval_high))
    mean_bars = axis.bar(x + width, mean, width, color='y',
                         yerr=(mean_confidence_interval_low, mean_confidence_interval_high))

    # Set labels
    # axis.set_title(plotutils.translate_domain_name(domain) + "-" + instance)
    # axis.set_ylabel("Goal Achievement Time (ms)")
    # axis.set_xlabel("Algorithms")
    axis.set_xticks(x + width)
    axis.set_xticklabels(labels)
    axis.legend((med_bars, mean_bars), ('Median', 'Mean'))

    # Set ylims so we aren't at the top of the graph space for even data
    low = min(min(y))
    high = max(max(y))
    plt.ylim([math.ceil(low - 0.5 * (high - low)), math.ceil(high + 0.5 * (high - low))])

    # Add numbers to top of bars
    def autolabel(rects):
        for rect in rects:
            height = rect.get_height()
            axis.text(rect.get_x() + rect.get_width() / 2., 1.0 * height, '%d' % int(height), ha='center', va='bottom')

    autolabel(med_bars)
    autolabel(mean_bars)


def plot_gat_boxplots(data, labels, showviolin=False):
    y = data
    med, confidence_interval_low, confidence_interval_high = median_confidence_intervals(y)

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

    ymin, ymax = plt.ylim()
    plt.ylim(ymin - 0.1, ymax + 0.1)


# TODO add factor A*
def plot_gat_duration_error(data_dict, astar_data, algorithms, action_durations):  # algorithms, domain, instance
    astar_gat_per_duration = astar_data
    astar_gat_per_duration_means, astar_confidence_interval_low, astar_confidence_interval_high = \
        mean_confidence_intervals(astar_gat_per_duration)
    x_astar = np.arange(1, len(astar_gat_per_duration_means) + 1)

    # Plot for each provided algorithm
    for algorithm in algorithms:
        algorithm_gat_per_duration = data_dict[algorithm]
        if not algorithm_gat_per_duration:  # empty
            print("No data for " + algorithm)
            continue
        algorithm_gat_per_duration = [np.log10(gat) for gat in algorithm_gat_per_duration]
        algorithm_gat_per_duration_means, algorithm_confidence_interval_low, algorithm_confidence_interval_high = \
            mean_confidence_intervals(algorithm_gat_per_duration)
        x = np.arange(1, len(algorithm_gat_per_duration_means) + 1)
        plt.errorbar(x, algorithm_gat_per_duration_means, label=translate_algorithm_name(algorithm),
                     yerr=(algorithm_confidence_interval_low, algorithm_confidence_interval_high))

    # Set labels
    plt.legend()
    plt.xticks(x_astar, reversed(action_durations))

    # Adjust x limits so end errors are visible
    xmin, xmax = plt.xlim()
    plt.xlim(xmin - 0.1, xmax + 0.1)