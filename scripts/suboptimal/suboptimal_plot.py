#! /usr/bin/env python

import simplejson as json
import pandas as pd
import seaborn as sns
import matplotlib.pyplot as plt
import random
from pprint import pprint
from IPython.display import display, HTML

data_w = []
data = dict()
weights = [1.3, 1.5, 1.7, 2.0]
wFiles = ['results.wa.dps.1.3.json','results.wa.dps.1.5.json', 'results.wa.dps.1.7.json','results.wa.dps.2.json']
algorithms = ['wA*', 'dps']
begins = [0, 100]
ends = [100, 200]

def initDictionary(dataJson):
    for key in dataJson[0].keys():
        data[key] = []
    data["weight"] = []
    data["algorithm"] = []

def makeJson():
    for f in wFiles:
        json_file = f
        json_data = open(json_file)
        data_w.append(json.load(json_data))
        json_data.close()

def addInstances(weightIndex, alg, begin, end):
    for i in range(begin, end):
        data["weight"].append(weights[weightIndex])
        data["algorithm"].append(alg)
        for key in data_w[weightIndex][0].keys():
            try:
                data[key].append(data_w[weightIndex][i][str(key)])
            except KeyError:
                data[key].append(0)

makeJson()
initDictionary(data_w[0])

for i in range(0,4):
    addInstances(i, "wA*", 0, 100)
    addInstances(i, "dps", 100, 200)

df = pd.DataFrame(data)

sns.set_context("paper")
sns.set_style("dark", {"axes.facecolor": ".9"})

plt.figure()
success_plot = sns.pointplot(x="weight", y="success", hue="algorithm", data=df, capsize=.2)
plt.figure()
expanded_plot = sns.pointplot(x="weight", y="expandedNodes", hue="algorithm", data=df, capsize=.2)
expanded_plot.set(yscale="log")
plt.show()





