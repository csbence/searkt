#! /usr/bin/env python

import gzip
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
weights = [1.2, 1.3, 1.4, 1.5, 1.6, 1.7, 1.8, 2.0]
dpsFiles = ['results.DYNAMIC_POTENTIAL_SEARCH.1.2.json.gz', 'results.DYNAMIC_POTENTIAL_SEARCH.1.3.json.gz',
        'results.DYNAMIC_POTENTIAL_SEARCH.1.4.json.gz','results.DYNAMIC_POTENTIAL_SEARCH.1.5.json.gz', 
        'results.DYNAMIC_POTENTIAL_SEARCH.1.6.json.gz','results.DYNAMIC_POTENTIAL_SEARCH.1.7.json.gz',
        'results.DYNAMIC_POTENTIAL_SEARCH.1.8.json.gz','results.DYNAMIC_POTENTIAL_SEARCH.1.9.json.gz', 
        'results.DYNAMIC_POTENTIAL_SEARCH.2.0.json.gz']

waFiles = ['results.WEIGHTED_A_STAR.1.2.json.gz','results.WEIGHTED_A_STAR.1.3.json.gz',
        'results.WEIGHTED_A_STAR.1.4.json.gz','results.WEIGHTED_A_STAR.1.5.json.gz', 
        'results.WEIGHTED_A_STAR.1.6.json.gz','results.WEIGHTED_A_STAR.1.7.json.gz',
        'results.WEIGHTED_A_STAR.1.8.json.gz','results.WEIGHTED_A_STAR.1.9.json.gz', 
        'results.WEIGHTED_A_STAR.2.0.json.gz']

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
        json_file = gzip.open(f, 'rb')
        datar.append(json.loads(json_file.read().decode("ascii")))
        json_file.close()

def addInstances(weightIndex, alg, begin, end, datar):
    for i in range(begin, end):
        data["weight"].append(weights[weightIndex])
        data["algorithm"].append(alg)
        for key in datar[weightIndex][0].keys():
            try:
                data[key].append(datar[weightIndex][i][str(key)])
            except KeyError:
                data[key].append(0)

def filterOnNodesGenerated(dataFrame):
    for key in dataFrame.keys():
        if key == "generatedNodes":
            index = 0
            for item in dataFrame[key]:
                if int(item) > 5000000:
                    dataFrame.iloc[index, dataFrame.columns.get_loc("success")] = False
                index = index + 1

makeJson(waFiles, data_wa)
makeJson(dpsFiles, data_dps)
initDictionary(data_wa[0])

for i in range(0,8):
    addInstances(i, "wA*", 0, 100, data_wa)
    addInstances(i, "dps", 0, 100, data_dps)

df = pd.DataFrame(data)
filterOnNodesGenerated(df)
df2 = df[(df.success == True) & (df.generatedNodes <= 5000000)]

sns.set_context("paper")
sns.set_style("dark", {"axes.facecolor": ".9"})

plt.figure()
success_plot = sns.pointplot(x="weight", y="success", hue="algorithm", data=df, capsize=.2)
plt.figure()
expanded_plot = sns.boxplot(x="weight", y="generatedNodes", notch=True, hue="algorithm", data=df2)
plt.figure()
expanded_plot.set(yscale="log")
plt.show()

