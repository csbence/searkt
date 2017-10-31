#! /usr/bin/env python

import simplejson as json
import pandas as pd
import seaborn as sns
import matplotlib.pyplot as plt
import random
from pprint import pprint
from IPython.display import display, HTML

data_wa = []
data_dps = []
data = dict()
weights = [1.4, 1.5, 1.6, 1.7, 1.8, 2.0]
dpsFiles = ['results.DYNAMIC_POTENTIAL_SEARCH.1.4.json','results.DYNAMIC_POTENTIAL_SEARCH.1.5.json', 
        'results.DYNAMIC_POTENTIAL_SEARCH.1.6.json','results.DYNAMIC_POTENTIAL_SEARCH.1.7.json',
        'results.DYNAMIC_POTENTIAL_SEARCH.1.8.json','results.DYNAMIC_POTENTIAL_SEARCH.1.9.json', 
        'results.DYNAMIC_POTENTIAL_SEARCH.2.0.json']

waFiles = ['results.WEIGHTED_A_STAR.1.4.json','results.WEIGHTED_A_STAR.1.5.json', 
        'results.WEIGHTED_A_STAR.1.6.json','results.WEIGHTED_A_STAR.1.7.json',
        'results.WEIGHTED_A_STAR.1.8.json','results.WEIGHTED_A_STAR.1.9.json', 
        'results.WEIGHTED_A_STAR.2.0.json']

algorithms = ['wA*', 'dps']
begins = [0, 100]
ends = [100, 200]

def initDictionary(dataJson):
    for key in dataJson[0].keys():
        data[key] = []
    data["weight"] = []
    data["algorithm"] = []

def makeJson(wFiles, datar):
    for f in wFiles:
        json_file = f
        json_data = open(json_file)
        datar.append(json.load(json_data))
        json_data.close()

def addInstances(weightIndex, alg, begin, end, datar):
    for i in range(begin, end):
        data["weight"].append(weights[weightIndex])
        data["algorithm"].append(alg)
        for key in datar[weightIndex][0].keys():
            try:
                data[key].append(datar[weightIndex][i][str(key)])
            except KeyError:
                data[key].append(0)

makeJson(waFiles, data_wa)
makeJson(dpsFiles, data_dps)
initDictionary(data_wa[0])

for i in range(0,6):
    addInstances(i, "wA*", 0, 100, data_wa)
    addInstances(i, "dps", 0, 100, data_dps)

df = pd.DataFrame(data)

sns.set_context("paper")
sns.set_style("dark", {"axes.facecolor": ".9"})

plt.figure()
success_plot = sns.pointplot(x="weight", y="success", hue="algorithm", data=df, capsize=.2)
plt.figure()
expanded_plot = sns.pointplot(x="weight", y="expandedNodes", hue="algorithm", data=df, capsize=.2)
expanded_plot.set(yscale="log")
plt.show()





