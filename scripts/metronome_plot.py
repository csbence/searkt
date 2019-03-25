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
from matplotlib.backends.backend_pdf import PdfPages
from statsmodels.stats.proportion import proportion_confint

__author__ = 'Bence Cserna'


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
    if file_name.endswith('.gz'):
        with gzip.open("input.json.gz", "rb") as file:
            return json.loads(file.read().decode("utf-8"))

    with open(file_name) as file:
        return json.load(file)


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
            survival_results = add_row(survival_results,
                                       [fields[1], fields[0], survival_rate, survival_confint[0], survival_confint[1]])

        fig, ax = plt.subplots()
        errors = []
        for alg, alg_group in survival_results.groupby('algorithmName'):
            errors.append([(alg_group['lbound'] - alg_group['survival']).values,
                           (alg_group['rbound'].values - alg_group['survival']).values])
        errors = np.abs(errors)
        print(errors)
        survival = survival_results.pivot(index='actionDuration', columns='algorithmName', values='survival')

        survival.plot(ax=ax, yerr=errors,
                      # xlim=[0, 7000],
                      ylim=[0, 1.0],
                      capsize=4, capthick=1, ecolor='black', cmap=plt.get_cmap("rainbow"), elinewidth=1)

        plt.savefig('test.png', format='png')


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


def label_domain(row):
    domain_labels = ['uniform', 'quad', 'octa', 'wide', 'traffic']
    for domain_label in domain_labels:
        if domain_label in row.domainPath:
            row['domainLabel'] = domain_label
            break

    return row


def rename(row):
    # print(row)

    # if row.tbaStrategy == 'GBFS':
    #     row.algorithmLabel = 'TB-GBFS'

    if row.algorithmName == 'TIME_BOUNDED_A_STAR':
        if (row.weight != 1):
            row.algorithmLabel = 'TBwA* weight: ' + str(row.weight)

    return row


def plot_gat(data, plot_title, file_name):
    # print(data.algorithmLabel.unique())
    #
    data = data.apply(label_domain, axis=1)

    print(data.algorithmLabel.unique())

    # rescale action durations to ms
    # data['actionDuration'] = data['actionDuration'] / 1000000
    # data['goalAchievementTime'] = data['goalAchievementTime'] / 10e9

    flatui = sns.color_palette()

    color_palette = {
        'TBA*': flatui[4],
        'TBwA* weight: 1.5': flatui[1],
        'TBwA* weight: 2.0': flatui[3],
        'TBwA* weight: 2.5': flatui[6],
        'TB-GBFS': flatui[5],
        'Cluster-RTS': flatui[0]
    }

    data.sort_values(['algorithmLabel'], inplace=True)

    grid = sns.FacetGrid(data, col='domainLabel', hue='algorithmLabel', col_wrap=2)
    grid.map(sns.lineplot, 'actionDuration', 'goalAchievementTime').add_legend()

    # data.backtrack = data.backtrack.fillna(0.1)
    # plot = sns.lineplot(x="actionDuration", y="pathLength",
    #                     hue="algorithmLabel", data=data,
    #                     )

    # plot.set_xscale('log')
    # plot.set_yscale('log')

    # plot.set_xticks([50, 100, 150, 250, 500, 1000, 2000, 3200])
    # plot.set_xticks([1, 3.2, 6.4, 12.8, 25.6])
    # plot.set_yticks([1, 2, 3])
    # plot.set_ylim([1.1, 2.8])
    # plot.set_xlim([1000, 10000])

    # plot.get_xaxis().set_major_formatter(mpl.ticker.ScalarFormatter())
    # plot.get_yaxis().set_major_formatter(mpl.ticker.ScalarFormatter())
    #
    # plot.set_xlabel('Expansion Count per Iteration')
    # plot.set_ylabel('Path length')
    # plot.legend(title="")
    #
    # handles, labels = plot.get_legend_handles_labels()
    # plot.legend(handles=handles[1:], labels=labels[1:])

    pdf = PdfPages("../results/plots/" + file_name + ".pdf")
    plt.savefig(pdf, format='pdf')
    pdf.close()
    plt.show()


def main():
    results_paths = [
        "../results/d_filter_unsafe_1.json",
        "../results/d_filter_unsafe_2.json",
        # "../results/d_t_new.json",
        # "../results/d_t_oracle.json",
    ]

    raw_results = []
    for result_path in results_paths:
        raw_results += read_data(result_path)

    data = construct_data_frame(raw_results)

    set_rc()

    # this is a fix for the traffic domain which does not have domainSeed values, so I have to fake it
    if 'domainSeed' not in data:
        data['domainSeed'] = data['domainPath']
        data['domainPath'] = 'vehicle'

    sns.set_style("white")

    data = data[~data['errorMessage'].notnull()]
    data = data[~data['errorMessage'].notnull()]

    data['algorithmLabel'] = data.algorithmName + ' ' + data.safetyProof + ' ' + data.safetyExplorationRatio.astype(str) + ' ' + data.filterUnsafe.astype(str)
    # data['algorithmLabel'] = data.algorithmName
    plot_gat(data, 'racetracks', 'd_filter_unsafe_1')


if __name__ == "__main__":
    main()
