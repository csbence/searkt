#!/usr/bin/env python3

import numpy as np
import matplotlib as mpl
import matplotlib.pyplot as plt
import seaborn as sns
from pandas import DataFrame
import pandas as pd
import json
import re

__author__ = 'Bence Cserna'

small_durations = [10, 20, 50, 100]
algorithms = ["A_STAR", "LSS_LRTA_STAR", "MO_RTS"]
wall_path = '/input/vacuum/variants/wall-2/wall_'
cups_path = '/input/vacuum/variants/cups-2/cups_'
tiles_path = '/input/tiles/korf/4/all/'


def flatten(experiment):
    experiment_configuration = experiment.pop('configuration')

    return {**experiment, **experiment_configuration}


def plot_experiments(db, algorithms, domain, instance, termination_type, lookahead_type="DYNAMIC"):
    data = db.get_results(algorithms,
                          domain,
                          instance,
                          termination_type,
                          lookahead_type)

    if len(data) > 0:
        data_frame = construct_data_frame(data)
        plot_data_frame(data_frame)
    else:
        print("No results were found")


def construct_data_frame(data):
    flat_data = [flatten(experiment) for experiment in data]
    return DataFrame(flat_data)


def plot_data_frame(experiments):
    sns.set_style("white")

    boxplot = sns.boxplot(x="actionDuration",
                          y="goalAchievementTime",
                          hue="algorithmName",
                          data=experiments,
                          showmeans=True)
    plt.yscale('log')
    plt.show(boxplot)


def read_data(file_name):
    with open(file_name) as file:
        content = json.load(file)
    return content

def addData(frame, group, key, algs):
    new_frame = DataFrame()
    means = []
    for k, actionGroup in (DataFrame(group).groupby('actionDuration')):
        means.append(actionGroup.mean()['goalAchievementTime'])
    new_frame[key] = means
    algs.append(key)
    return pd.concat([frame, new_frame], axis=1)


def main():
    data = construct_data_frame(read_data("../output/results.json"))

    data.drop(['commitmentType', "success", "timeLimit",
               "terminationType", 'timestamp', 'octileMovement', 'lookaheadType',
               'firstIterationDuration', 'generatedNodes', 'expandedNodes', 'domainInstanceName', 'domainName',
               'planningTime'],
              axis=1,
              inplace=True,
              errors='ignore')

    data.sort_values(['domainPath', 'actionDuration'], ascending=True, inplace=True)


    sns.set_style("white")

    algs = []
    df = DataFrame()
    df['actionDuration'] = [i for i in data['actionDuration'].unique()]

    data = data[~data['errorMessage'].notnull()]

    for i, group in data.groupby(['domainPath', 'algorithmName']):
        key = i[0] + "_" + i[1]
        if i[1] == 'SAFE_RTS':
            for selection, selectionGroup in DataFrame(group).groupby('TARGET_SELECTION'):
                key = i[0] + "_" + i[1] + "_" + str(selection)
                df = addData(df, group, key, algs)
        else:
            df = addData(df, group, key, algs)

    plot = df.plot(x='actionDuration', y=algs, linestyle='', marker='o')

    plot.set_xlabel('Action Duration')
    plot.set_ylabel('Goal Achievement Time')
    # plot.set(xlim=(0, None))

    # plt.gcf().subplots_adjust(bottom=1)
    plt.tight_layout(pad=1, w_pad=0.5, h_pad=1.0)

    # plt.show()
    plt.savefig('highway_results.pdf', format='pdf')


    # db = MetronomeMongoClient()

    # tips = sns.load_dataset("tips")/
    # plot_experiments(db,
    #                  ["A_STAR", "LSS_LRTA_STAR", "MO_RTS", "MO_RTS_OLD"],
    #                  'SLIDING_TILE_PUZZLE',
    #                  '/input/tiles/korf/4/all/',
    #                  'EXPANSIONS',
    #                  'STATIC'
    #                  )

    # plot_experiments(db,
    #                  ["LSS_LRTA_STAR", "MO_RTS", "SLOW_RTS"],
    #                  'GRID_WORLD',
    #                  '/input/vacuum/variants/cups-2/cups_',
    #                  'EXPANSIONS',
    #                  'STATIC'
    #                  )


if __name__ == "__main__":
    main()
