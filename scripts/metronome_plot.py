#!/usr/bin/env python3

import gzip
import json
import matplotlib as mpl
import matplotlib.pyplot as plt
import numpy as np
import pandas as pd
import re
import seaborn as sns
import statistics
import statsmodels.stats.api as sms
from matplotlib.backends.backend_pdf import PdfPages
from pandas import DataFrame
from statsmodels.stats.proportion import proportion_confint

__author__ = 'Bence Cserna'


def flatten(experiment):
    experiment_configuration = experiment.pop('configuration')
    experiment_attributes = experiment.pop('attributes')

    return {**experiment, **experiment_configuration, **experiment_attributes}


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
    mpl.rcParams['axes.labelsize'] = 14
    mpl.rcParams['xtick.top'] = True
    mpl.rcParams['font.family'] = 'Serif'
    mpl.rcParams['font.size'] = 14
    # pylab.rcParams['font.family'] = 'Serif'


def label_domain(row):
    domain_labels = [
        'uniform',
        'quad',
        'octa',
        'wide',
        'traffic',
        'orz',
        '100k8',
        '100k10',
        '100k14',
        '100k20',
        'airspace/100k100p001/',
        'gap5-5', 'gap10-5', 'gap15-5']

    for domain_label in domain_labels:
        if domain_label in row.domainPath:
            row['domainLabel'] = domain_label
            break

    return row


def label_algorithm(row):
    # if row.algorithmName == 'SAFE_RTS':
    #         row.algorithmLabel = 'SafeRTS'

    if row.algorithmName == 'SAFE_RTS':
        if row.safetyProof == 'TOP_OF_OPEN':
            if row.filterUnsafe:
                row.algorithmLabel = 'SafeRTS-Filter'
                return # Drop the filtered version
            else:
                row.algorithmLabel = 'SafeRTS'
        elif row.safetyProof == 'A_STAR_FIRST':
            # if row.filterUnsafe:
            # row.algorithmLabel = 'RTFS-0-Filter'

            row.algorithmLabel = 'RTFS-0'
            row.algorithmLabel = 'RTFS r: ' + str(row.safetyExplorationRatio)

            if row.safetyExplorationRatio == 0.1:
                row.algorithmLabel = 'RTFS w: 1.1 r: 0.1'
            else:
                row.algorithmLabel = 'RTFS-0'

            weight_map = {
                1.0: "RTFS-A*",
                0.0: "RTFS-GBFS",
                1.01: "RTFS-wA* w: 1.01:",
                1.1: "RTFS-wA* w: 1.1:",
                1.4: "RTFS-wA* w: 1.4:",
                2.0: "RTFS-wA* w: 2.0:",
            }

            # row.algorithmLabel = weight_map[row.weight]


    elif row.algorithmName == 'LSS_LRTA_STAR':
        row.algorithmLabel = 'Safe-LSS-LRTA*'

    elif row.algorithmName == 'A_STAR':
        row.algorithmLabel = 'A*'
        # return # drop A*

    return row


def plot_gat(data, plot_title, file_name):
    data = data.apply(label_domain, axis=1)
    data = data.apply(label_algorithm, axis=1)

    data = data[~data.domainLabel.isna()]

    print(data.algorithmLabel.unique())
    print(data.domainLabel.unique())

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

    color_palette = {
        'SafeRTS': flatui[6],
        'SafeRTS-Filter': flatui[9],
        'RTFS-0': flatui[0],
        'RTFS-0-Filter': flatui[1],
        'Safe-LSS-LRTA*': flatui[5],
        'A*': flatui[7],
    }

    data.sort_values(['algorithmLabel'], inplace=True)

    # data.domainLabel += data.weight.astype(str)
    # data.domainLabel += data.safetyExplorationRatio.astype(str)

    # grid = sns.FacetGrid(data, col='domainLabel', hue='algorithmLabel', col_wrap=2)
    # grid.map(sns.lineplot, 'actionDuration', 'unsafeReexpansions').add_legend()
    # grid.map(sns.lineplot, 'actionDuration', 'averageVelocity').add_legend()
    # grid.map(sns.lineplot, 'actionDuration', 'goalAchievementTime').add_legend()

    # cut = data[data.actionDuration == 20]
    # print(cut.groupby(['algorithmLabel']).mean().targetDepth)
    # print(cut.groupby(['algorithmLabel']).mean().targetRank)

    data['inferredVelocity'] = 100000.0 / data.pathLength

    for domain_label in data.domainLabel.unique():
        domain_data = data[data.domainLabel == domain_label]

        # data.backtrack = data.backtrack.fillna(0.1)
        plot = sns.lineplot(x="actionDuration", y="goalAchievementTime",
                            hue="algorithmLabel", data=domain_data,
                            # palette=color_palette
                            )

        # plot.set_xscale('log')
        # plot.set_yscale('log')

        # plot.set_xticks([50, 100, 150, 250, 500, 1000, 2000, 3200])
        # plot.set_xticks([1, 3.2, 6.4, 12.8, 25.6])
        # plot.set_yticks([1, 2, 3])
        # plot.set_ylim([1.1, 2.8])
        # plot.set_xlim([200, 1600])

        # plot.get_xaxis().set_major_formatter(mpl.ticker.ScalarFormatter())
        # plot.get_yaxis().set_major_formatter(mpl.ticker.ScalarFormatter())
        #
        # plot.set_xlabel('Re-expanded dead-end state ratio')
        plot.set_xlabel('Expansion Count per Iteration')
        # plot.set_ylabel('Re-expanded dead-end state ratio ')
        plot.set_ylabel('Average Velocity')
        # plot.set_ylabel('Goal Achievement Time (iterations)')
        # plot.legend(title="")
        #
        handles, labels = plot.get_legend_handles_labels()
        plot.legend(handles=handles[1:], labels=labels[1:])

        # pdf = PdfPages("../results/plots/" + file_name + "_gat_" + domain_label + ".pdf")
        pdf = PdfPages("../results/plots/" + file_name + "_" + domain_label + ".pdf")
        plt.savefig(pdf, format='pdf')
        pdf.close()
        plt.show()


def main():
    results_paths = [
        # "../results/limitx_1.json",
        # "../results/limitx_2.json",
        # "../results/limitx_gap_3.json",
        # "../results/limitx_4.json",
        # "../results/limitx_srts.json",
        # "../results/limit20_duplicates.json",

        # "../results/limit_5allw100_20-200d10_srts.json",

        # The ws are meaningless everything is 1.0
        # "../results/airspace_rtfs_w.json",

        # Airspace 100 only results
        # "../results/airspace100_ratio_srts_rtfs.json",

        # Airspace p05 only results
        # "../results/airspace05_ratio_srts_rtfs.json",

        # Track uni and quad only rt
        # "../results/track_rt.json",
        # "../results/track_quad_lss.json", # Safe-LSS on quad 20..100 step 5

        # Testing the target selection on quad
        # "../results/quad_rank.json",

        # Airspace p05
        # "../results/airspace8-20_rt_a_with_nodead.json",
        # "../results/airspace8-20_lss.json",

        # Exploration variance Airspace100
        # "../results/as100_rtfs_w.json",
        # "../results/as100_rtfs_r_w11.json",

        # Airspace100 fitted
        # "../results/as100_r01_w11.json",
        # "../results/as100_rtfs.json",
        # "../results/as100_srts.json",
        # "../results/as100_lss.json",

        # "../results/results_test.json",

        "../results/traffic_ratio_rtfs_only_high.json",
        "../results/traffic_ratio_srts_only_high.json",
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
    # data = data[~(data['actionDuration'] < 200)]

    data['algorithmLabel'] = data.algorithmName + ' ' + data.safetyProof.astype(
        str) + ' ' + data.safetyExplorationRatio.astype(str) + ' ' + data.filterUnsafe.astype(str)
    # data['algorithmLabel'] = data.algorithmName + ' ' + data.safetyProof.astype(str) + ' ' + data.safetyExplorationRatio.astype(str) + ' ' + data.filterUnsafe.astype(str) + ' w' + data.weight.astype(str)
    # data['algorithmLabel'] = data.algorithmName + ' ' + data.safetyProof.astype(str) + ' ' + data.filterUnsafe.astype(str) + ' w' + data.weight.astype(str)
    # data['algorithmLabel'] = data.algorithmName

    data['unsafeReexpansions'] = (data.unsafeProofReexpansion + data.unsafeSearchReexpansion) / data.expandedNodes
    plot_gat(data, 'racetracks', 'traffic')


if __name__ == "__main__":
    main()
