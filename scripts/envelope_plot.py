#!/usr/bin/env python3

import json
import gzip
import matplotlib as mpl
import matplotlib.pyplot as plt
import numpy as np
import pandas as pd
import re
import seaborn as sns
import statistics
import statsmodels.stats.api as sms
from pandas import DataFrame
from statsmodels.stats.proportion import proportion_confint

__author__ = 'Bence Cserna, modified by Kevin C. Gall'


def flatten(experiment):
    experiment_configuration = experiment.pop('configuration')

    return {**experiment, **experiment_configuration}


def construct_data_frame(data):
    flat_data = [flatten(experiment) for experiment in data]
    return DataFrame(flat_data)


def read_data(file_name):
    if file_name.endswith('.gz'):
        with gzip.open("input.json.gz", "rb") as file:
            return json.loads(file.read().decode("utf-8"))

    with open(file_name) as file:
        return json.load(file)


def set_rc():
    mpl.rcParams['axes.labelsize'] = 10
    mpl.rcParams['xtick.top'] = True
    mpl.rcParams['font.family'] = 'Serif'


def add_row(df, values):
    return df.append(dict(zip(df.columns.values, values)), ignore_index=True)


def plot_domain_instances(data):
    #Each domain configuration
    for domain_path, domain_group in data.groupby(["domainPath"]):
        results = DataFrame(columns="actionDuration algorithmName withinOpt".split())
        domain_name = re.search("[^/]+$", domain_path).group(0).rstrip(".vw")

        plt.title(domain_name)
        plt.ylabel('Goal Achievement Time (Factor of Optimal)')
        plt.xlabel('Expansion Limit (Per Iteration)')

        expansion_series = domain_group["actionDuration"].unique()

        #Each algorithm (different backlog ratios count as different algorithm)
        palette = sns.color_palette(n_colors=10)
        count = 0
        for fields, alg_group in domain_group.groupby(['algorithmName', 'backlogRatio']):
            alg_name = fields[0]
            if fields[0] == "CES":
                alg_name += " Backup Ratio: " + str(fields[1])

            #Plot each action duration
            # for duration, action_group in alg_group.groupby('actionDuration'):
            #     results = add_row(results, [duration, alg_name, action_group["withinOpt"]])

            plt.plot(expansion_series, alg_group["withinOpt"], color=palette[count], label=alg_name)

            count += 1

        plt.legend()

        plt.savefig("../output/" + domain_name + ".png", format="png")
        plt.figure()



def main():
    results = read_data("../output/results.json")
    baseline_results = read_data("../output/base_results.json")

    results += baseline_results

    data = construct_data_frame(results)

    #we'll see if we want this...
    set_rc()

    data.drop(['commitmentType', "success", "timeLimit",
               "terminationType", 'timestamp', 'octileMovement', 'lookaheadType',
               'firstIterationDuration', 'generatedNodes', 'expandedNodes',
               "targetSelection", "safetyExplorationRatio", "safetyProof", "safetyWindowSize", "safetyBackup",
               'domainSeed', 'averageVelocity', "proofSuccessful", "rawDomain", "anytimeMaxCount",
               "systemProperties", "towardTopNode", "weight", "numberOfProofs"],
              axis=1,
              inplace=True,
              errors='ignore')

    data = data[~data['errorMessage'].notnull()]

    #Need to default backlogRatio so it is groupable later
    for i, row in data.iterrows():
        if np.isnan(row['backlogRatio']):
            data.at[i, 'backlogRatio'] = 1.0

    astar = data[data["algorithmName"] == "A_STAR"]
    astar["opt"] = astar["actionDuration"] * astar["pathLength"]
    astar = astar[["domainPath", "opt", "actionDuration"]]
    data = pd.merge(data, astar, how='inner', on=["domainPath", 'actionDuration'])
    data["withinOpt"] = data["goalAchievementTime"] / data["opt"]

    plot_domain_instances(data)


#Will be deleted when no longer needed for reference
def main_test():

    expansions = [50, 100, 400, 600, 900, 1000, 1200, 1500]

    plt.title('Local Minima Domains')
    plt.plot(expansions, [10, 8.7, 9.0, 6.6, 5.6, 3.4, 2.87, 1.2], 'ro-', label='RT-Comprehensive')
    plt.plot(expansions, [14, 12.11, 11.5, 10.8, 6.1, 7.2, 6.0, 5.2], 'bo-', label='LSS-LRTA*')
    plt.legend()

    plt.ylabel('Goal Achievement Time (Factor of Optimal)')
    plt.xlabel('Expansion Limit (Per Iteration)')

    plt.figure()
    plt.title('Uniform Obstacles')
    plt.plot(expansions, [12, 11.2, 10.1, 8.8, 9.3, 7.4, 5.97, 4.2], 'ro-', label='RT-Comprehensive')
    plt.plot(expansions, [11, 8.3, 7.2, 6.9, 6.1, 3.4, 2.2, 2.0], 'bo-', label='LSS-LRTA*')
    plt.legend()

    plt.ylabel('Goal Achievement Time (Factor of Optimal)')
    plt.xlabel('Expansion Limit (Per Iteration)')

    plt.show()


if __name__ == "__main__":
    main()
