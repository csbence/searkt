import simplejson as json
import pandas as pd
import seaborn as sns
import matplotlib.pyplot as plt
import random
from pprint import pprint

json_file = 'results.json'
json_data = open(json_file)
data = json.load(json_data)
json_data.close()

for key in data[0].keys():
    print(key)

df = pd.DataFrame()
df['x'] = [2.0, 2.0]
df['y'] = [data[0]['goalAchievementTime'], data[1]['goalAchievementTime']]

sns.set_style('darkgrid')
ax = sns.pointplot(x="Suboptimality_Bound", y="Nodes_Expanded", data=df)

print(str(data[0]['configuration']))
config_dict = data[0]['configuration']
print(str(data_config['algorithmName']))
print(str(data[0]['planningTime']/1000000.0) + 'ms')
print(str(data[0]['goalAchievementTime']) + ' nodes expanded')


print(str(data[1]['planningTime']/1000000.0) + 'ms')
print(str(data[1]['goalAchievementTime']) + ' nodes expanded')

