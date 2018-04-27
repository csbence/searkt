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
        # data_dict["logWeight"] = []
        data_dict["nodesPerSecond"] = []
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
                data_dict["algorithm"].append(str(alg) + '')
                data_dict["instance"].append(i)
                data_dict["weight"].append(weight)
                # data_dict["logWeight"].append(round(math.log10(weight), 2))
                data_dict["domainPath"].append(config)
                if instance["success"] == True:
                    generatedNodes = instance["generatedNodes"]
                    expandedNodes = instance["expandedNodes"]
                    totalNodes = generatedNodes + expandedNodes
                    runTime = instance["experimentRunTime"]
                    data_dict["nodesPerSecond"].append((expandedNodes)/runTime)
                else:
                    data_dict["nodesPerSecond"].append(0.0)
                i = i + 1
                for key in fields:
                    if key == "success" and instance[str(key)] is False:
                        # print("Failed Experiment! Algorithm: " + alg + " Weight: " + 
                        #         str(weight) + " Path: " + domain_path)
                        failed_experiments = failed_experiments + 1
                        # print(instance["errorMessage"])
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

def extractLowerTwo(num):
    ones = num % 10
    tens = int((num / 10) % 10)
    return ones + (10 * tens)

def filter_nodes_generated(df):
    over_five_million = dict()
    over_five_million["DPS"] = 0
    over_five_million["WEIGHTED_A_STAR"] = 0
    over_five_million["EES"] = 0
    over_five_million["EETS"] = 0
    over_five_million["EESD"] = 0
    over_five_million["EECS"] = 0
    offset = []
    for x in range(0,100):
        offset.append(99 - x)
    drop_index = []
    for key in df.keys():
        print(key)
        if key == "algorithm":
            index = 0
            for item in df[key]:
                generated_nodes = df.iloc[index, df.columns.get_loc("generatedNodes")]
                if generated_nodes >= 5000000:
                    over_five_million[item] = over_five_million[item] + 1
                index = index + 1
        if key == "success":
            index = 0
            for item in df[key]:
                alg_success = df.iloc[index, df.columns.get_loc("success")]
                assert(item == alg_success)
                if not alg_success:
                    li = extractLowerTwo(index)
                    alg = df.iloc[index, df.columns.get_loc("algorithm")]
                    weight = df.iloc[index, df.columns.get_loc("weight")]
                    print(alg + "@" + str(weight))
                    print("\tindex: {} | li: {}".format(index, li))
                    upper = index + offset[li]
                    lower = index - offset[-li] - 1
                    print("\tdropping {}->{}".format(lower, upper))
                    for i in range(lower, upper+1):
                        drop_index.append(i)
                index = index + 1
        # if key == "generatedNodes":
        #     index = 0
        #     for item in df[key]:
        #         if int(item) >= 5000000:
        #             df.iloc[index, df.columns.get_loc("success")] = False
        #         index = index + 1
        if key == "expandedNodes":
            index = 0
            for item in df[key]:
                expanded_nodes = df.iloc[index, df.columns.get_loc("expandedNodes")]
                if expanded_nodes != 0:
                    df.iloc[index, df.columns.get_loc("expandedNodes")] = math.log10(expanded_nodes)
                index = index + 1
        if key == "generatedNodes":
            index = 0
            for item in df[key]:
                generated_nodes = df.iloc[index, df.columns.get_loc("generatedNodes")]
                if generated_nodes != 0:
                    df.iloc[index, df.columns.get_loc("generatedNodes")] = math.log10(generated_nodes)
                index = index + 1
        if key == "experimentRunTime":
            index = 0
            for item in df[key]:
                seconds = df.iloc[index, df.columns.get_loc("experimentRunTime")]
                # if seconds >10:
                    # print(seconds)
                if seconds != 0:
                    df.iloc[index, df.columns.get_loc("experimentRunTime")] = math.log10(seconds)
                index = index + 1
    return over_five_million, drop_index


def lookUpInDataFrame(df, num):
    a = df.iloc[num, df.columns.get_loc("algorithm")]
    w = df.iloc[num, df.columns.get_loc("weight")]
    print(str(a) + "@" + str(w))

def filter_failed_sets(instance_set):
    if instance_set.success.all():
        return instance_set
    return None

def plot(data_dict):
    # print(data_dict)
    df = pd.DataFrame(data_dict)
    display(df)
    # display(df_gen)
    # success_plot = sns.pointplot(x="weight", y="success", hue="algorithm", data=df, capsize=0.1, palette="Set2")
    # plt.title('Success')
    # plt.figure()
    sns.set_context("paper")
    # display(df[(df.generatedNodes > 5000000) & (df.success == True) & (df.algorithm == "DPS")]['generatedNodes'])
    over_five_million, drop_index = filter_nodes_generated(df)
    drop_set  = set(drop_index)
    print(drop_set)

    # df2 = df.groupby(["weight", "algorithm"]).apply(filter_failed_sets)
    # lookUpInDataFrame(df, 3798)
    # lookUpInDataFrame(df, 3799)
    # lookUpInDataFrame(df, 3800)
    # lookUpInDataFrame(df, 5700)
    # lookUpInDataFrame(df, 7600)
    # unit for paper
    # df.drop(df.index[range(5700, 6100)], inplace=True)
    # df.drop(df.index[range(7200, 7500)], inplace=True)
    # heavy for paper
    # df.drop(df.index[range(7600, 7900)], inplace=True)

    display(df)

    # display(df[(df.generatedNodes > 5000000) & (df.success == True)])
    # display(over_five_million)

    success_plot = sns.pointplot(x="weight", y="success", hue="algorithm", data=df, capsize=0.1, palette="Set2")
    plt.title('Success')
    plt.tight_layout()
    sns.despine(ax=success_plot)
    plt.figure()

    success_plot = sns.pointplot(x="weight", y="success", hue="algorithm", data=df2, capsize=0.1, palette="Set2")
    plt.title('Success')
    plt.tight_layout()
    sns.despine(ax=success_plot)
    plt.figure()
 

    # axes = success_plot.axes
    # axes.set(ylim=(0, 1.01))
    success_plot = sns.pointplot(x="weight", y="expandedNodes", hue="algorithm", data=df2, capsize=0.1, palette="Set2")
    plt.title('Expanded Nodes')
    sns.despine(ax=success_plot)
    plt.ylabel('log10(expandedNodes')
    plt.savefig('exp-unit.eps', format='eps')
    plt.figure()

    success_plot = sns.pointplot(x="weight", y="generatedNodes", hue="algorithm", data=df2, capsize=0.1, palette="Set2")
    plt.title('Generated Nodes')
    sns.despine(ax=success_plot)
    plt.ylabel('log10(generatedNodes')
    plt.figure()



    df_gen = df2[df2.success == True]
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
    success_plot = sns.pointplot(x="weight", y="experimentRunTime", hue="algorithm", data=df_gen, capsize=.1, palette="Set2")
    plt.title('CPU time')
    plt.ylabel('log10(experimentRunTime)')
    sns.despine(ax=success_plot)
    plt.savefig('runtime-unit.eps', format='eps')
    plt.figure()

    success_plot = sns.pointplot(x="weight", y="nodesPerSecond", hue="algorithm", data=df_gen, capsize=0.1, palette="Set2")
    plt.title('Nodes per Second')
    sns.despine(ax=success_plot)
    plt.savefig('nps-unit.eps', format='eps')
    plt.show()


def main():
    plot(process_data())


if __name__ == '__main__':
    main()






















