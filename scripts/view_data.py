#! /usr/bin/env python

import sys
import math
import pandas as pd
import simplejson as json
import seaborn as sns
import matplotlib.pyplot as plt

from pprint import pprint
from IPython.display import display


def process_data():
    data_dict = dict()
    data = []

    fields = ["success", "expandedNodes", "generatedNodes", "experimentRunTime"]

    print("Initing Dictionary...")
    for field in fields:
        data_dict[field] = []
        data_dict["algorithm"] = []
        data_dict["instance"] = []
        data_dict["weight"] = []
        data_dict["domainPath"] = []
        data_dict["logWeight"] = []
    print("Done Initing Dictionary!")

    failed_experiments = 0
    successful_experiments = 0

    for f in sys.argv[1:]:
        fi = open(f)
        data.append(json.load(fi))
        fi.close()

        for experiment in data:
            i = 0
            for instance in experiment:
                config = instance["configuration"]["domainPath"]
                weight = instance["configuration"]["weight"]
                alg = instance["configuration"]["algorithmName"]
                domain_path = instance["configuration"]["domainPath"]
                data_dict["algorithm"].append(str(alg))
                data_dict["instance"].append(i)
                data_dict["weight"].append(weight)
                data_dict["logWeight"].append(round(math.log10(weight), 2))
                data_dict["domainPath"].append(config)
                i = i + 1
                for key in fields:
                    if key == "success" and instance[str(key)] is False:
                        print("Failed Experiment! Algorithm: " + alg + " Weight: " + 
                                str(weight) + " Path: " + domain_path)
                        failed_experiments = failed_experiments + 1
                        print(instance["errorMessage"])
                        # print(instance["errorDetails"])
                    try:
                        if key == "success" and instance[str(key)] is True:
                            # print('{} -> {}'.format(str(key), str(instance[str(key)])))
                            successful_experiments = successful_experiments + 1
                        data_dict[key].append(instance[str(key)])
                    except KeyError:
                        data_dict[key].append(0)

    print('Successful experiments {}'.format(successful_experiments))
    print('Failed experiments {}'.format(failed_experiments))
    return data_dict


def filter_nodes_generated(df):
    over_five_million = dict()
    over_five_million["DPS"] = 0
    over_five_million["WEIGHTED_A_STAR"] = 0
    over_five_million["EES"] = 0
    over_five_million["EETS"] = 0
    for key in df.keys():
        # print(key)
        if key == "algorithm":
            index = 0
            for item in df[key]:
                generated_nodes = df.iloc[index, df.columns.get_loc("generatedNodes")]
                if generated_nodes >= 5000000:
                    over_five_million[item] = over_five_million[item] + 1
                index = index + 1
        if key == "generatedNodes":
            index = 0
            for item in df[key]:
                if int(item) >= 5000000:
                    df.iloc[index, df.columns.get_loc("success")] = False
                index = index + 1
        if key == "experimentRunTime":
            index = 0
            for item in df[key]:
                nano_seconds = df.iloc[index, df.columns.get_loc("experimentRunTime")]
                seconds = nano_seconds/1000000000.0
                # if seconds >10:
                    # print(seconds)
                if seconds != 0:
                    df.iloc[index, df.columns.get_loc("experimentRunTime")] = math.log10(seconds)
                index = index + 1
    return over_five_million


def plot(data_dict):
    df = pd.DataFrame(data_dict)
    # display(df_gen)
    # success_plot = sns.pointplot(x="weight", y="success", hue="algorithm", data=df, capsize=0.1, palette="Set2")
    # plt.title('Success')
    # plt.figure()
    sns.set_context("talk")
    display(df[(df.generatedNodes > 5000000) & (df.success == True) & (df.algorithm == "DPS")]['generatedNodes'])
    over_five_million = filter_nodes_generated(df)
    display(df[(df.generatedNodes > 5000000) & (df.success == True)])
    display(over_five_million)

    success_plot = sns.pointplot(x="weight", y="success", hue="algorithm", data=df, capsize=0.1, palette="Set2")
    plt.title('Success')
    # axes = success_plot.axes
    # axes.set(ylim=(0, 1.01))
    plt.figure()
    success_plot = sns.pointplot(x="weight", y="expandedNodes", hue="algorithm", data=df, capsize=0.1, palette="Set2")
    plt.title('Expanded Nodes')
    plt.figure()


    df_gen = df[df.success == True]
    # df = df[(df.generatedNodes >= 5000000) & (df.success == True)]
    # expand_plot = sns.pointplot(x="weight", y="expandedNodes", hue="algorithm", data=df_gen, capsize=0.1,
    #                             palette="Set2")

    # plt.title('Nodes Expanded')
    # plt.figure()
 
    # sns.set_context("paper")
    # sns.set_style("dark", {"axes.facecolor": ".9"})
    # success_plot = sns.pointplot(x="instance", y="success", hue="algorithm", data=df, capsize=.2)
    # plt.figure()
    # success_plot = sns.pointplot(x="instance", y="numberOfProofs", hue="algorithm", data=df, capsize=.2)
    # plt.figure()

    # axes = expand_plot.axes
    # axes.set(ylim=(0,2000000))
    success_plot = sns.pointplot(x="logWeight", y="experimentRunTime", hue="algorithm", data=df_gen, capsize=.1, palette="Set2")
    plt.title('CPU time')
    plt.figure()

    success_plot = sns.pointplot(x="weight", y="success", hue="algorithm", data=df_gen, capsize=0.1, palette="Set2")
    plt.title('Success test')

    plt.show()


def main():
    plot(process_data())


if __name__ == '__main__':
    main()






















