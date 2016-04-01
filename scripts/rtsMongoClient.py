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


def plot_gat_duration_error(db, algorithm, domain):
    astar_data_action_durations = data_for_algorithm(db, "A_STAR", domain)
    lss_lrta_star_data_action_durations = data_for_algorithm(db, algorithm, domain)

    astar_data_action_duration_means = [np.mean(x) for x in astar_data_action_durations]
    astar_data_action_duration_std = [stats.sem(x) for x in astar_data_action_durations]
    lss_lrta_star_data_action_duration_means = [np.mean(x) for x in lss_lrta_star_data_action_durations]

    astar_confidence_intervals = stats.t.interval(0.95, len(astar_data_action_durations) - 1, loc=astar_data_action_duration_means, scale=astar_data_action_duration_std)

    x = np.arange(1, len(astar_data_action_duration_means) + 1)

    plt.ylabel("GAT log10 factor of optimal")
    plt.xlabel("expansions per unit duration log10")
    plt.errorbar(x, astar_data_action_duration_means, yerr=(astar_data_action_duration_means - astar_confidence_intervals[0], astar_confidence_intervals[1] - astar_data_action_duration_means), label='A*', color='blue')
    plt.errorbar(x, lss_lrta_star_data_action_duration_means, label=plotUtils.translate_algorithm_name(algorithm), color='green')
    plt.legend()
    plt.show()


def data_for_algorithm(db, algorithm, domain):
    data_action_durations = []
    for i in reversed(range(1, 6)):
        action_duration = 100 * 10 ** i
        data_tiles_a_star = db.experimentResult.find({"result.experimentConfiguration.domainName": domain,
                                                      "result.experimentConfiguration.algorithmName": algorithm,
                                                      # "result.experimentConfiguration.domainInstanceName": "input/vacuum/dylan/",
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
    plot_gat_duration_error(db, "LSS_LRTA_STAR", "GRID_WORLD")
