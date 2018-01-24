#!/usr/bin/env python3

import json
import matplotlib.pyplot as plt
import numpy as np
import pandas as pd
import re
import seaborn as sns
import statsmodels.stats.api as sms
import sys
from pandas import DataFrame
from statsmodels.stats.proportion import proportion_confint

# Changes the output name (suffix) of the plots generated
out_name = "racetrack"

# Changes the file format of the plots generated (change this to eps if you need that format)
file_format = "svg"

alg_map = {"A_STAR": "A*", "LSS_LRTA_STAR": "LSS-LRTA*", "SAFE_RTS": "SRTS", "S_ZERO": "S0", "SIMPLE_SAFE": "SS",
           "SINGLE_SAFE": "BEST_SAFE", "SAFE_RTS_TOP": "SRTS_TOP"}


def flatten(experiment):
    experiment_configuration = experiment.pop('configuration')
    return {**experiment, **experiment_configuration}


def construct_data_frame(data):
    flat_data = [flatten(experiment) for experiment in data]
    return DataFrame(flat_data)


def read_data(file_name):
    with open(file_name) as file:
        content = json.load(file)
    return content


def plot_errors(plot, xvalues, yvalues, yerr, lolims, uplims):
    (_, caps, _) = plot.errorbar(xvalues, yvalues, yerr=yerr, lolims=lolims, uplims=uplims)
    for cap in caps:
        cap.set_markeredgewidth(2)
        cap.set_marker("_")


def add_row(df, values):
    return df.append(dict(zip(df.columns.values, values)), ignore_index=True)


def make_survival_plots(df):
    survival_results = DataFrame(columns="actionDuration algorithmName survival lbound rbound".split())
    timeout_sum = 0

    for fields, action_group in df.groupby(['algorithmName', 'actionDuration']):
        total_trials = len(action_group)
        error_experiments = action_group[action_group["errorMessage"].notnull()]

        deaths = len(error_experiments[error_experiments["errorMessage"] != "Timeout"])
        timeouts = len(error_experiments[error_experiments["errorMessage"] == "Timeout"])
        timeout_sum += timeouts
        successes = len(action_group[~action_group["errorMessage"].notnull()])

        survival_confint = proportion_confint(successes, total_trials - timeouts, 0.05)
        survival_rate = (successes / (successes + deaths + timeouts))
        survival_results = add_row(survival_results,
                                   [fields[1], fields[0], survival_rate, abs(survival_rate - survival_confint[0]),
                                    abs(survival_rate - min(1, survival_confint[1]))])

    # print("Timeouts: {}\n".format(timeout_sum))

    fig, ax = plt.subplots()
    errors = []
    for alg, alg_group in survival_results.groupby('algorithmName'):
        errors.append([alg_group['lbound'],
                       alg_group['rbound']])

    errors = np.abs(errors)
    survival = survival_results.pivot(index='actionDuration', columns='algorithmName', values='survival')
    plot = survival.plot(ax=ax, yerr=errors,
                         ylim=[0, 1.05], clip_on=True, figsize=(4.5, 4),
                         # capsize=4, capthick=1, ecolor='black', colors=["red", "blue", "green", "orange"], elinewidth=1)
                         capsize=4, capthick=1, ecolor='black',
                         color=["red", "blue", "green", "orange", "purple", "cyan"],
                         elinewidth=1
                         )

    # Annoying bug forces me to explicitly do this instead of set rc_parameters
    plot.legend(title="")
    for i in plot.legend().get_texts():
        i.set_text(alg_map[i.get_text()])
        i.set_fontsize(14)
    plot.semilogx(basex=10)
    plot.set_xlabel('Action Duration (node expansions)', size=13)
    plot.set_ylabel('Survival Rate', size=13)
    plot.autoscale(tight=False)
    plt.tight_layout(pad=0, w_pad=0.0, h_pad=1.0)

    plt.savefig(out_name + '_survival' + "." + file_format, format=file_format)
    for label in (plot.get_xticklabels() + plot.get_yticklabels()):
        label.set_fontsize(10)  # Size here overrides font_prop


# Lots of nit-picky formatting for the plot.
def format_plot(plot, y_label='Goal Achievement Time (factor of optimal)'):
    plot.set_xlabel('Action Duration (node expansions)')
    plot.semilogx(basex=10)
    plot.autoscale(tight=False)
    plot.set_ylabel(y_label)
    # plot.legend(title="Planners", shadow=True, frameon=True, framealpha=1.0, facecolor='lightgrey')
    plot.legend(title="")

    # If I recall correctly, there's an annoying bug in matplotlib which forced me
    # to reside the legend using the method below instead of with an rc_parameter
    for i in plot.legend().get_texts():
        i.set_text(alg_map[i.get_text()])
        # i.set_fontsize(16)
    plt.tight_layout(pad=0, w_pad=0.0, h_pad=1.0)

    # for label in (plot.get_xticklabels() + plot.get_yticklabels()):
    #     label.set_fontsize(10)  # Size here overrides font_prop


def plot_velocity(df, pname):
    velocity = DataFrame(columns="actionDuration algorithmName averageVelocity lbound rbound".split())

    for fields, action_group in df.groupby(['algorithmName', 'actionDuration']):
        total_trials = len(action_group)
        error_experiments = action_group[action_group["errorMessage"].notnull()]
        mean = action_group["averageVelocity"].mean()
        v = list(filter(lambda x: x != 0, action_group["averageVelocity"].fillna(0).values))
        mean = np.mean(
            v)  # Wait, why do I rewrite mean here? You can probably safely remove the previous definition of mean
        bound = sms.DescrStatsW(v).zconfint_mean()  # generates left/right bounds of confidence interval
        velocity = add_row(velocity,
                           [fields[1], fields[0], mean, abs(mean - bound[0]), abs(mean - bound[1])])

    errors = []
    for alg, alg_group in velocity.groupby('algorithmName'):
        errors.append([alg_group['lbound'].values, alg_group['rbound'].values])

    fig, ax = plt.subplots()
    pivot = velocity.pivot(index='actionDuration', columns='algorithmName', values='averageVelocity')
    # plot = pivot.plot(ax=ax, figsize=(4.5,4),
    #                   color=["red", "blue", "green", "orange", "cyan"])
    # plot = pivot.plot(ax=ax, yerr=bound, figsize=(4.5,4),
    #                   capsize=4, capthick=1, ecolor='black', color=["red", "blue", "green", "orange", "cyan"], elinewidth=1)
    plot = pivot.plot(ax=ax, yerr=errors, figsize=(4.5, 4),
                      capsize=4, capthick=1, ecolor='black',
                      color=["red", "blue", "green", "orange", "cyan"],
                      elinewidth=1
                      )
    format_plot(plot, 'Average Velocity')
    plt.savefig(pname + "_velocity." + file_format, format=file_format)


def plot_gat(data):
    data.sort_values(['domainPath', 'actionDuration'], ascending=True, inplace=True)

    # To calculate "within optimum", we use A_STAR
    astar = data[data["algorithmName"] == "A_STAR"]
    astar = astar.assign(opt=astar.actionDuration * astar.pathLength)
    astar = astar[["domainPath", "domainSeed", "opt", "actionDuration"]]

    data = pd.merge(data, astar, how='inner', on=['domainPath', 'domainSeed', 'actionDuration'])
    data["withinOpt"] = data["goalAchievementTime"] / data["opt"]

    results = DataFrame(columns="actionDuration algorithmName withinOpt lbound rbound".split())

    for fields, action_group in data.groupby(['algorithmName', 'actionDuration']):

        # Don't represent the GAT of Action Durations where there's bean at least one error!
        # Also, I should replace this with .any() or something instead of summing it...
        if action_group["errorMessage"].notnull().sum() > 0:
            results = add_row(results, [fields[1], fields[0], np.nan, 0, 0])
            continue

        # This is used to generate confidence bounds
        bound = sms.DescrStatsW(action_group[~action_group["errorMessage"].notnull()]["withinOpt"]).tconfint_mean()
        mean = action_group["withinOpt"].mean()
        results = add_row(results, [fields[1], fields[0], mean, abs(mean - bound[0]), abs(mean - bound[1])])

    fig, ax = plt.subplots()

    errors = []
    for alg, alg_group in results.groupby('algorithmName'):
        errors.append([alg_group['lbound'].values, alg_group['rbound'].values])

    # TODO remove - just for testing
    # results = results[results.algorithmName != 'A_STAR']


    pivot = results.pivot(index='actionDuration', columns='algorithmName', values='withinOpt')
    # Bunch of tweaks to get the plot to look right
    # mpl.rcParams.update({'font.size': 60})
    plot = pivot.plot(ax=ax, yerr=errors, figsize=(4.5, 4),
                      capsize=4, capthick=1,# ecolor='black',
                      # ylim=(0,60),
                      # color=["red", "blue", "green", "orange", "cyan"],
                      elinewidth=1)
    # format_plot(plot) TODO
    # Um I can't recall why I call this twice. I probably don't need to, but just to be safe...
    # mpl.rcParams.update({'font.size': 60})
    # Saves the GAT plot
    plt.savefig(out_name + "." + file_format, format=file_format)
    # plt.show()


def main():
    # The script takes one or more jsons as an argument and creates a dataframe out of all of them
    # You will probably be passing fixed_A_STAR_result.json, fixed_SAFE_RTS_result.json, etc. to this function
    data = construct_data_frame(read_data(sys.argv[1]))
    for i in sys.argv[2:]:
        print('.')
        data2 = construct_data_frame(read_data(i))
        data = pd.concat([data, data2])

    # Name srts variants:
    data.loc[data.algorithmName == 'A_STAR', 'safetyWindowSize'] = ''
    data = data.loc[~((data.algorithmName == 'SAFE_RTS') & pd.isnull(data.safetyWindowSize)), :]  # remove error lines
    data.algorithmName = data.algorithmName + data.safetyWindowSize.map(str)

    # data.loc[(data.algorithmName == 'SAFE_RTS') & (data.safetyProof == 'TOP_OF_OPEN'), 'algorithmName'] = 'SAFE_RTS_TOP'
    data = data.loc[data.actionDuration <= 250]

    # Get rid of unneeded fields
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

    # Duplicate cleaning - might be necessary
    # data.drop_duplicates(subset=["domainPath", "domainSeed", "algorithmName", "actionDuration"], inplace=True,
    #                      keep='last')
    sns.set_style("white")

    # Uncomment to plot velocity, only works in racetrack domain
    # for path in data["domainPath"].unique():
    #     pname = re.sub("\/", "_", re.sub("\..*", "_", path))
    #     df = data[data["domainPath"] == path]
    #     plot_velocity(df, pname)

    # Constructs the survival plots using the dataframe
    # make_survival_plots(data)

    plot_gat(data)


if __name__ == "__main__":
    main()
