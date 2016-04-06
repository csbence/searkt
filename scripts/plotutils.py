import matplotlib.cbook as cbook
import numpy as np
import os
import re
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
    means = np.nan_to_num([np.mean(x) for x in data])
    std = np.nan_to_num([stats.sem(x) if len(x) > 1 else 0.0 for x in data])
    confidence_intervals = stats.t.interval(0.95, len(data) - 1, loc=means, scale=std)
    confidence_intervals = [np.nan_to_num(ci) for ci in confidence_intervals]
    return means, means - confidence_intervals[0], confidence_intervals[1] - means


def save_plot(plot, filename):
    basename, ext = os.path.splitext(filename)
    if ext is '.pdf':
        from matplotlib.backends.backend_pdf import PdfPages

        pp = PdfPages(filename)
        plot.savefig(pp, format='pdf')
        pp.close()
    else:
        # Try and save it
        plot.savefig(filename)
