import simplejson as json
from pprint import pprint

json_file = 'results.json'
json_data = open(json_file)
data = json.load(json_data)
json_data.close()

print(data[0].keys())
print(data[0]['goalAchievementTime'])
print(data[1]['goalAchievementTime'])
print(str(data[0]['planningTime']/1000000.0) + 'ms')
print(str(data[1]['planningTime']/1000000.0) + 'ms')

