#! /usr/bin/env python

import sys
import pandas as pd
import simplejson as json
import seaborn as sns
import matplotlib.pyplot as plt

from pprint import pprint
from IPython.display import display


def process_data():
    data_dict = dict()
    data = []

    fields = ["errorMessage", "success", "expandedNodes", "generatedNodes", "numberOfProofs",
              "proofSuccessful", "towardTopNode"]

    print("Initing Dictionary...")
    for field in fields:
        data_dict[field] = []
        data_dict["algorithm"] = []
        data_dict["instance"] = []
        data_dict["weight"] = []
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
                weight = instance["configuration"]["weight"]
                alg = instance["configuration"]["algorithmName"]
                window_size = instance["configuration"]["safetyWindowSize"]
                data_dict["algorithm"].append(str(alg))
                data_dict["instance"].append(i)
                data_dict["weight"].append(weight)
                i = i + 1
                for key in fields:
                    if key == "success" and instance[str(key)] is False:
                        print("Failed Experiment!")
                        failed_experiments = failed_experiments + 1
                        if instance["configuration"]["algorithmName"] == "DPS":
                            display(instance)
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
    return over_five_million


def plot(data_dict):
    df = pd.DataFrame(data_dict)
    df_gen = df[df.success == True]
    display(df_gen)
    success_plot = sns.pointplot(x="weight", y="success", hue="algorithm", data=df, capsize=0.1, palette="Set2")
    plt.title('Success')
    plt.figure()
    over_five_million = filter_nodes_generated(df)
    display(over_five_million)
    # sns.set_context("paper")
    # sns.set_style("dark", {"axes.facecolor": ".9"})
    # success_plot = sns.pointplot(x="instance", y="success", hue="algorithm", data=df, capsize=.2)
    # plt.figure()
    # success_plot = sns.pointplot(x="instance", y="numberOfProofs", hue="algorithm", data=df, capsize=.2)
    # plt.figure()
    # success_plot = sns.pointplot(x="instance", y="towardTopNode", hue="algorithm", data=df, capsize=.2)
    # plt.figure()
    success_plot = sns.pointplot(x="weight", y="success", hue="algorithm", data=df, capsize=0.1, palette="Set2")
    plt.title('Success with Filter')
    # axes = success_plot.axes
    # axes.set(ylim=(0, 1.01))
    plt.figure()
    expand_plot = sns.pointplot(x="weight", y="expandedNodes", hue="algorithm", data=df_gen, capsize=0.1, palette="Set2")
    plt.title('Nodes Expanded')
    # axes = expand_plot.axes
    # axes.set(ylim=(0,2000000))
    plt.show()


def main():
    plot(process_data())


if __name__ == '__main__':
    main()


