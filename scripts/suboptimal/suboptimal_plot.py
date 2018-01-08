#! /usr/bin/env python

import gzip
import argparse
import simplejson as json
import pandas as pd
import seaborn as sns
import matplotlib.pyplot as plt
import random
from pprint import pprint
from IPython.display import display, HTML

parser = argparse.ArgumentParser()

parser.add_argument("datapath", help="the aboslute path of the data files to plot")
parser.add_argument("to_plot", help="the data to plot [regular]|[heavy]")
args = parser.parse_args()

data_wa = []
data_dps = []
data_comp = []
data = dict()
data_comp = dict()

domains = ['SLIDING_TILE_PUZZLE_4', 'SLIDING_TILE_PUZZLE_HEAVY']
algorithms = ['WEIGHTED_A_STAR', 'DYNAMIC_POTENTIAL_SEARCH']
regularTileWeights = [1.17, 1.20, 1.25, 1.33, 1.50, 1.78, 2.00, 2.33, 2.67, 2.75, 3.00]
heavyTileWeights = [1.11, 1.13, 1.14, 1.17, 1.20, 1.25, 1.50, 2.00, 2.67, 3.00]
dpsFiles = []
waFiles = []

fields = ["errorMessage", "success", "expandedNodes", "generatedNodes"]

regWAStarComp = [0.65, 0.72, 0.83, 0.93, 0.98, 1.0, 1.0, 1.0, 1.0, 1.0, 1.0]
regDpsComp = [0.8, 0.82, 0.91, 0.95, 0.99, 1.0, 1.0, 1.0, 1.0, 1.0, 1.0]

heavyWAStarComp = [0.58, 0.61, 0.65, 0.69, 0.76, 0.81, 0.92, 1.0, 1.0, 1.0]
heavyDpsComp = [0.62, 0.66, 0.7, 0.75, 0.79, 0.83, 0.98, 1.0, 1.0, 1.0]

plotRegular = True
domainToPlot = 'SLIDING_TILE_PUZZLE_4'
weightsToPlot = regularTileWeights
if args.to_plot == 'heavy':
    plotRegular = False
    domainToPlot = 'SLIDING_TILE_PUZZLE_HEAVY'
    weightsToPlot = heavyTileWeights

for algorithm in algorithms:
    for weight in weightsToPlot:
        if algorithm == 'WEIGHTED_A_STAR':
            waFiles.append(args.datapath + algorithm + "." + domainToPlot + "." + str(weight) + "." +
                    "results.json.gz")
        elif algorithm == 'DYNAMIC_POTENTIAL_SEARCH':
            dpsFiles.append(args.datapath + algorithm + "." + domainToPlot + "." + str(weight) + "." +
                    "results.json.gz")
        else:
            print("Unsupported algorithm " + algorithm + "!")
            exit()
# print("---")
# for f in waFiles:
#     print(f + "\n")
# for f in dpsFiles:
#     print(f + "\n")
# print("---")


weights = weightsToPlot
# print(weights)

# weights = [1.2, 1.3, 1.4, 1.5, 1.6, 1.7, 1.8, 2.0]
# dpsFiles = ['results.DYNAMIC_POTENTIAL_SEARCH.1.2.json.gz', 'results.DYNAMIC_POTENTIAL_SEARCH.1.3.json.gz',
#        'results.DYNAMIC_POTENTIAL_SEARCH.1.4.json.gz','results.DYNAMIC_POTENTIAL_SEARCH.1.5.json.gz', 
#        'results.DYNAMIC_POTENTIAL_SEARCH.1.6.json.gz','results.DYNAMIC_POTENTIAL_SEARCH.1.7.json.gz',
#        'results.DYNAMIC_POTENTIAL_SEARCH.1.8.json.gz','results.DYNAMIC_POTENTIAL_SEARCH.1.9.json.gz', 
#        'results.DYNAMIC_POTENTIAL_SEARCH.2.0.json.gz']

# waFiles = ['results.WEIGHTED_A_STAR.1.2.json.gz','results.WEIGHTED_A_STAR.1.3.json.gz',
#        'results.WEIGHTED_A_STAR.1.4.json.gz','results.WEIGHTED_A_STAR.1.5.json.gz', 
#        'results.WEIGHTED_A_STAR.1.6.json.gz','results.WEIGHTED_A_STAR.1.7.json.gz',
#        'results.WEIGHTED_A_STAR.1.8.json.gz','results.WEIGHTED_A_STAR.1.9.json.gz', 
#        'results.WEIGHTED_A_STAR.2.0.json.gz']

algorithms = ['wA*', 'dps']
begins = [0, 100]
ends = [100, 200]

def initDictionary():
    print("Initing Dictionary")
    for key in fields:
        print(key)
        data[key] = []
    data["weight"] = []
    data["algorithm"] = []
    data_comp["weight"] = []
    data_comp["algorithm"] = []
    data_comp["success"] = []
    print("Done Initing Dictionary")


def makeComp(alg):
    success_list = []
    # print(alg)
    # print(args.to_plot)
    if alg == "wA*-comp" and args.to_plot == "heavy":
        success_list = heavyWAStarComp
    elif alg == "wA*-comp" and args.to_plot == "regular":
        success_list = regWAStarComp
    elif alg == "dps-comp" and args.to_plot == "heavy":
        success_list = heavyDpsComp
    elif alg == "dps-comp" and args.to_plot == "regular":
        success_list = regDpsComp
    index = 0
    # print(success_list)
    # print(weights)
    # print(str(len(success_list) == len(weights)))
    for weight in weights:
        data_comp["algorithm"].append(alg)
        data_comp["weight"].append(weight)
        data_comp["success"].append(success_list[index])
        index = index + 1


def makeJson(wFiles, datar):
    for f in wFiles:
        json_file = gzip.open(f, 'rb')
        datar.append(json.loads(json_file.read().decode("ascii")))
        json_file.close()

def addInstances(weightIndex, alg, begin, end, datar):
    for i in range(begin, end):
        data["weight"].append(weights[weightIndex])
        data["algorithm"].append(alg)
        for key in fields:
            try:
#                 if str(key) == "success" and str(datar[weightIndex][i]["errorMessage"]) != "null" and datar[weightIndex][i]["success"] == False:
#                         data[key].append(False)
#                 else:
                data[key].append(datar[weightIndex][i][str(key)])
            except KeyError:
                data[key].append(0)


overFiveMillion = dict()
overFiveMillion["dps"] = 0
overFiveMillion["wA*"] = 0
def filterOnNodesGenerated(dataFrame):
    for key in dataFrame.keys():
        # print(key)
        if key == "algorithm":
            index = 0
            for item in dataFrame[key]:
                genNodes = dataFrame.iloc[index, dataFrame.columns.get_loc("generatedNodes")] 
                if genNodes >= 5000000:
                    overFiveMillion[item] = overFiveMillion[item] + 1
                index = index + 1
        if key == "generatedNodes":
            index = 0
            for item in dataFrame[key]:
                if int(item) >= 5000000:
                    # print(item)
                    # print(dataFrame.iloc[index, dataFrame.columns.get_loc("success")])
                    dataFrame.iloc[index, dataFrame.columns.get_loc("success")] = False
                    # print(dataFrame.iloc[index, dataFrame.columns.get_loc("success")])
                index = index + 1


makeJson(waFiles, data_wa)
makeJson(dpsFiles, data_dps)
initDictionary()
initDictionary()

for i in range(0,10):
    addInstances(i, "wA*", 0, 100, data_wa)
    addInstances(i, "dps", 0, 100, data_dps)
    makeComp("dps-comp")
    makeComp("wA*-comp")

for key in data.keys():
    print(str(key) + ": len = " + str(len(data[key])))

df = pd.DataFrame(data)
df_comp = pd.DataFrame(data_comp)

sns.set_context("poster")
sns.set_style("dark", {"axes.facecolor": ".9"})

# success_plot = sns.pointplot(x="weight", y="success", hue="algorithm", data=df, capsize=.2)
# plt.figure()

# filterOnNodesGenerated(df)

# plt.figure()
success_plot = sns.pointplot(x="weight", y="success", hue="algorithm", data=df, capsize=.2)
# plt.figure()

sns.set_palette(sns.color_palette("husl", 8))

success_plot = sns.pointplot(x="weight", y="success", hue="algorithm", data=df_comp)
print(overFiveMillion)

# expanded_plot = sns.boxplot(x="weight", y="generatedNodes", notch=True, hue="algorithm", data=df2)
# plt.figure()
# expanded_plot.set(yscale="log")

plt.show()


