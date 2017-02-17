#!/usr/bin/env python3

import numpy as np
import matplotlib as mpl
import matplotlib.pyplot as plt
import scipy.stats as st
import seaborn as sns
from pandas import DataFrame
import statsmodels.stats.api as sms
from statsmodels.stats.proportion import proportion_confint
import pandas as pd
import json
import re
import statistics
import pylab
from math import log

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

def plot_errors(plot, xvalues, yvalues, yerr, lolims, uplims):
    (_, caps, _) = plot.errorbar(xvalues, yvalues, yerr=yerr, lolims=lolims, uplims=uplims)
    for cap in caps:
        cap.set_markeredgewidth(2)
        cap.set_marker("_")

def add_data(frame, group, key, algs, opt_gat, domain_path):
    new_frame = DataFrame()
    means = []
    bounds = []
    for k, action_group in (DataFrame(group).groupby(['actionDuration'])):
        seeds = []

        for j, seed_group in action_group.groupby('domainSeed'):
            cur_opt_gat = opt_gat[(k, domain_path, j)]
            actual_gat = action_group['goalAchievementTime'].iloc(0)[0]
            seeds.append(actual_gat / cur_opt_gat)

        bound = sms.DescrStatsW(seeds).tconfint_mean()
        mean = statistics.mean(seeds)
        bounds.append((abs(mean - bound[0]), abs(mean - bound[1])))
        means.append(statistics.mean(seeds))

        # mean_gat = action_group.mean()['goalAchievementTime']
        # means.append(mean_gat / opt_gat[(k, domain_path)])
    new_frame[key] = means
    new_frame[key + "_" + "lerror"] = [i[0] for i in bounds]
    new_frame[key + "_" + "rerror"] = [i[1] for i in bounds]
    algs.append(key)
    return pd.concat([frame, new_frame], axis=1)

def add_row(df, values):
    return df.append(dict(zip(df.columns.values, values)), ignore_index=True)

def print_survival_rate(df):
    for domain_path, domain_group in df.groupby(["domainPath"]):
        survival_results = DataFrame(columns="actionDuration algorithmName survival lbound rbound".split())
        domain_name = re.search("[^/]+$", domain_path).group(0).rstrip(".track")

        for fields, action_group in domain_group.groupby(['algorithmName', 'actionDuration']):
            total_trials = len(action_group)
            error_experiments = action_group[action_group["errorMessage"].notnull()]

            deaths = len(error_experiments[error_experiments["errorMessage"] != "Timeout"])
            timeouts = len(error_experiments) - deaths
            successes = len(action_group[~action_group["errorMessage"].notnull()])

            survival_confint = proportion_confint(successes, total_trials, 0.05)
            survival_rate = (successes / (successes + deaths))
            # survival_results = survival_results.append((action_duration, fields[1], survival_rate,
            #                                            survival_confint[0], survival_confint[1]), ignore_index=True)
            # srates.append(survival_rate)
            # lower_bounds.append(survival_confint[0])
            # upper_bounds.append(survival_confint[1])
            # durations.append(action_duration)
            survival_results = add_row(survival_results,
                                      [fields[1], fields[0], survival_rate, survival_confint[0], survival_confint[1]])


        # print(survival_results)
        fig, ax = plt.subplots()
        errors = []
        for alg, alg_group in survival_results.groupby('algorithmName'):
            errors.append([(alg_group['lbound'] - alg_group['survival']).values,
                           (alg_group['rbound'].values - alg_group['survival']).values])
        errors = np.abs(errors)
        print(errors)
        survival = survival_results.pivot(index='actionDuration', columns='algorithmName', values='survival')
        # survival = survival_results.pivot(index='actionDuration', columns='algorithmName', values='survival')


        # durations = survival_results.groupby(['actionDuration', 'algorithmName'])
        survival.plot(ax=ax, yerr=errors,
                      xlim=[0, 7000], ylim=[0, 1.0],
                      capsize=4, capthick=1, ecolor='black', cmap=plt.get_cmap("rainbow"), elinewidth=1)

        plt.savefig('test.png', format='png')


        # key = "{}_{}".format(domain_name, fields[1])
        # algs.append(key)
        # new_row = DataFrame([durations, srates, lower_bounds, upper_bounds],
        #                     columns="actionDuration survival lbound rbound".split())

        # new_row = DataFrame(
        #         {"actionDuration" : durations, "survival" : srates, "lbound" : lower_bounds, "rbound" : upper_bounds})
        # new_row = new_row.append({"actionDuration" : 300, "survival" : 30.0, "lbound" : 2, "rbound" : 1 }, ignore_index=True)
        # new_row["algorithmName"] = fields[1]
        # print(new_row)


        # survival_results[key + "_survival"] = srates
        # survival_results[key + "_lbound"] = lower_bounds
        # survival_results[key + "rbound"] = upper_bounds





def print_survival_rate2(df):
    cur_name = ""
    survival_results = DataFrame()
    for keys, action_group in df.groupby(["domainPath", "algorithmName", "actionDuration"]):
        if keys[1] != cur_name:
            cur_name = keys[1]
            print(cur_name)
        domain_name = re.search("[^/]+$", keys[0]).group(0).rstrip(".track")

        total_trials = len(action_group)
        error_experiments = action_group[action_group["errorMessage"].notnull()]

        deaths = len(error_experiments[error_experiments["errorMessage"] != "Timeout"])
        timeouts = len(error_experiments) - deaths
        successes = len(action_group[~action_group["errorMessage"].notnull()])

        survival_confint = proportion_confint(successes, total_trials, 0.05)
        print(survival_confint)
        survival_rate = (successes / (successes + deaths)) * 100
        timeout_rate = (successes / (successes + timeouts)) * 100
        survival_results["{}_{}_{}".format(keys[0], keys[1])]
        print("{}_{}: {}% / {}%".format(keys[2], domain_name, survival_rate, timeout_rate))


def get_optimal_gat(df):
    astar = df[df["algorithmName"] == "A_STAR"]
    opt_gat = {}
    for acols, action_group in astar.groupby(["actionDuration", "domainPath", "domainSeed"]):
        opt_gat[(acols[0], acols[1], acols[2])] = action_group["pathLength"].mean() * acols[0]
    return opt_gat

def format_plot(plot):
    plot.set_xlabel('Action Duration')
    plot.semilogx(basex=10)
    plot.autoscale(tight=False)
    plot.set_ylabel('Within Optimal GAT')
    plt.tight_layout(pad=0, w_pad=0.0, h_pad=1.0)
    # plot.set(xlim=[min_range - 1000, max_range + 1000])
    # plt.xticks([2 * 10**3, 4 * 10**3, 6 * 10**3], [])



def set_rc():
    # sns.axes_style({'xtic.major.size' : 10})
    mpl.rcParams['axes.labelsize'] = 10
    mpl.rcParams['xtick.top'] = True
    mpl.rcParams['font.family'] = 'Serif'
    # pylab.rcParams['font.family'] = 'Serif'



def main():
    data = construct_data_frame(read_data("../output/results.json"))
    set_rc()


    data.drop(['commitmentType', "success", "timeLimit",
               "terminationType", 'timestamp', 'octileMovement', 'lookaheadType',
               'firstIterationDuration', 'generatedNodes', 'expandedNodes', 'domainInstanceName', 'domain_name',
               'planningTime'],
              axis=1,
              inplace=True,
              errors='ignore')


    # this is a fix for the traffic domain which does not have domainSeed values, so I have to fake it
    if 'domainSeed' not in data:
        data['domainSeed'] = data['domainPath']
        data['domainPath'] = 'vehicle'



    # get min and max ranges for actionDuration for plotting later
    min_range = data.min()['actionDuration']
    max_range = data.max()['actionDuration']

    sns.set_style("white")



    # print_survival_rate(data)
    data = data[~data['errorMessage'].notnull()]
    data.sort_values(['domainPath', 'actionDuration'], ascending=True, inplace=True)
    # data = data[data['actionDuration'] > 1000]

    # print(data.groupby(["domainPath", "actionDuration", "algorithmName"]).mean())

    astar = data[data["algorithmName"] == "A_STAR"]
    astar["opt"] = astar["actionDuration"] * astar["pathLength"]
    astar = astar[["domainPath", "domainSeed", "opt", "actionDuration"]]
    data = pd.merge(data, astar, how='inner', on=['domainPath', 'domainSeed', 'actionDuration'])
    data["withinOpt"] = data["goalAchievementTime"] / data["opt"]

    for domain_path, domain_group in data.groupby(["domainPath"]):
        results = DataFrame(columns="actionDuration algorithmName withinOpt lbound rbound".split())
        domain_name = re.search("[^/]+$", domain_path).group(0).rstrip(".track")

        for fields, action_group in domain_group.groupby(['algorithmName', 'actionDuration']):

            bound = sms.DescrStatsW(action_group["withinOpt"]).tconfint_mean()
            mean = action_group["withinOpt"].mean()
            results = add_row(results, [fields[1], fields[0], mean, abs(mean - bound[0]), abs(mean - bound[1])])


        # print(survival_results)
        fig, ax = plt.subplots()
        errors = []
        for alg, alg_group in results.groupby('algorithmName'):
            errors.append([alg_group['lbound'].values, alg_group['rbound'].values])

        pivot = results.pivot(index='actionDuration', columns='algorithmName', values='withinOpt')
        plot = pivot.plot(ax=ax, yerr=errors,
                      capsize=4, capthick=1, ecolor='black', cmap=plt.get_cmap("rainbow"), elinewidth=1)


        format_plot(plot)
        plt.savefig(domain_name + ".png", format='png')


    # create one plot per domain
    # for domain_path, domain_group in data.groupby(['domainPath']):
    #     domain_name = re.search("[^/]+$", domain_path).group(0).rstrip(".track")
    #     algs = []
    #     df = DataFrame()
    #     df['actionDuration'] = [i for i in data['actionDuration'].unique()]
    #
    #
    #
    #     for algorithm, algorithm_group in DataFrame(domain_group).groupby(['algorithmName']):
    #         key = str(algorithm)
    #         if key == 'SAFE_RTS':
    #             for selection, selection_group in DataFrame(algorithm_group).groupby(
    #                     ['TARGET_SELECTION', 'commitmentStrategy', 'SAFETY_EXPLORATION_RATIO']):
    #                 if (selection[0] == "BEST_SAFE"):
    #                     continue
    #                 if (selection[1] == "MUlTIPLE"):
    #                     continue
    #                 if (selection[2] != 0.3):
    #                     continue
    #                 newkey = key + "_" + "_".join(map(str, selection))
    #                 df = add_data(df, selection_group, newkey, algs, opt_gat, domain_path)
    #         else:
    #             df = add_data(df, algorithm_group, key, algs, opt_gat, domain_path)
    #
    #     plot = df.plot(x='actionDuration', y=algs, marker='x', figsize=(4.05, 4.05), cmap=plt.get_cmap("rainbow"))
    #     # plot = df.boxplot(column=algs, figsize=(15, 15))
    #
    #     for alg in algs:
    #         xalg = []
    #         yalg = []
    #         ylower = []
    #         yupper = []
    #         yerror = []
    #         for index, action_duration in df['actionDuration'].iteritems():
    #             xalg.append(action_duration)
    #             yalg.append(df[alg][index])
    #             ylower.append(df[alg + "_lerror"][index])
    #             yupper.append(df[alg + "_rerror"][index])
    #             # yerror.append(1 + ((yupper[len(yupper) - 1] - ylower[len(ylower) - 1]) / 2))
    #             yerror.append(5)
    #         # print([i for i in zip(ylower, yupper)])
    #
    #
    #         mystuff = np.asarray(([i for i in ylower], [i for i in yupper]))
    #
    #
    #
    #         (_, caps, _) = plot.errorbar(xalg, yalg, yerr=mystuff, lolims=ylower, uplims=yupper)
    #         for cap in caps:
    #             cap.set_markeredgewidth(2)
    #             cap.set_marker("_")
    #
    #     format_plot(plot, min_range, max_range)
    #     plt.savefig(domain_name + '.png', format='png')



if __name__ == "__main__":
    main()
