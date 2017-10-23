#! /usr/bin/env python

import simplejson as json
import pandas as pd
import seaborn as sns
import matplotlib.pyplot as plt
import random
from pprint import pprint
from IPython.display import display, HTML

json_file = 'resultsl.json'
json_data = open(json_file)
data = json.load(json_data)
json_data.close()

tips = sns.load_dataset("tips")
display(tips)

dataDict = dict()

for key in data[0].keys():
    dataDict[key] = []

dataDict["weight"] = []

display(dataDict)

for i in range(0,100):
    dataDict["weight"].append(2.0)
    for key in data[i].keys():
        print(data[i][str(key)])
        dataDict[key].append(data[i][str(key)])

weightedAStarDf = pd.DataFrame(dataDict)
display(weightedAStarDf)
ax = sns.pointplot(x='weight',y='generatedNodes',data=weightedAStarDf)
plt.show()
