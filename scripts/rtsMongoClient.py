#!/usr/bin/python3

import matplotlib.pyplot as plt
import numpy as np
from pymongo import MongoClient


def open_connection():
    client = MongoClient('mongodb://aerials.cs.unh.edu:42830')
    client.rts.authenticate('rtsUser', 'VeLuvh4!', mechanism='SCRAM-SHA-1')
    return client.rts


def print_counts(db):
    configuration_status = db.command('collstats', 'experimentConfiguration')
    print('Configuration count: %d' % configuration_status['count'])
    task_status = db.command('collstats', 'experimentTask')
    print('Configuration count: %d' % task_status['count'])
    result_status = db.command('collstats', 'experimentResult')
    print('Configuration count: %d' % result_status['count'])
    # pprint.pprint(configuration_status, width=1)


def collect_data(db):
    astar_data_action_durations = data_for_algorithm("A_STAR", db)
    lss_lrta_star_data_action_durations = data_for_algorithm("LSS_LRTA_STAR", db)

    astar_data_action_duration_means = [np.mean(x) for x in astar_data_action_durations]
    lss_lrta_star_data_action_duration_means = [np.mean(x) for x in lss_lrta_star_data_action_durations]

    x = np.arange(1, len(astar_data_action_duration_means) + 1)

    plt.errorbar(x, astar_data_action_duration_means, xerr=0.2, yerr=100, label='A*', color='blue')
    plt.errorbar(x, lss_lrta_star_data_action_duration_means, yerr=100, label='LSS-LRTA*', color='green')
    plt.show()


def data_for_algorithm(algorithm, db):
    data_action_durations = []
    for i in reversed(range(1, 6)):
        action_duration = 100 * 10 ** i
        data_tiles_a_star = db.experimentResult.find({"result.experimentConfiguration.domainName": "GRID_WORLD",
                                                      "result.experimentConfiguration.algorithmName": algorithm,
                                                      # "result.experimentConfiguration.domainInstanceName": "input/vacuum/dylan/",
                                                      "result.experimentConfiguration.actionDuration": action_duration,
                                                      "result.success": True})
        times_for_durations = []
        for result in data_tiles_a_star:
            times_for_durations.append(result['result']['goalAchievementTime'])

        data_action_durations.append(times_for_durations)

    return data_action_durations


if __name__ == '__main__':
    db = open_connection()
    print_counts(db)
    collect_data(db)
