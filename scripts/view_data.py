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
    print("Done Initing Dictionary!")

    for f in sys.argv[1:]:
        fi = open(f)
        data.append(json.load(fi))
        fi.close()

        for experiment in data:
            i = 0
            for instance in experiment:
                alg = instance["configuration"]["algorithmName"]
                window_size = instance["configuration"]["safetyWindowSize"]
                data_dict["algorithm"].append(str(alg) + str(window_size))
                data_dict["instance"].append(i)
                i = i + 1
                for key in fields:
                    if key == "success" and instance[str(key)] is False:
                        print("Failed Experiment!")
                        display(instance)
                    try:
                        data_dict[key].append(instance[str(key)])
                    except KeyError:
                        data_dict[key].append(0)


    return data_dict


def plot(data_dict):
    df = pd.DataFrame(data_dict)
    sns.set_context("paper")
    sns.set_style("dark", {"axes.facecolor": ".9"})
    # success_plot = sns.pointplot(x="instance", y="success", hue="algorithm", data=df, capsize=.2)
    # plt.figure()
    # success_plot = sns.pointplot(x="instance", y="numberOfProofs", hue="algorithm", data=df, capsize=.2)
    # plt.figure()
    # success_plot = sns.pointplot(x="instance", y="towardTopNode", hue="algorithm", data=df, capsize=.2)
    # plt.figure()
    success_plot = sns.pointplot(x="instance", y="proofSuccessful", hue="algorithm", data=df, capsize=.2)
    axes = success_plot.axes
    axes.set(xlim=(0, 330))
    plt.figure()
    plt.show()


def main():
    plot(process_data())


if __name__ == '__main__':
    main()
