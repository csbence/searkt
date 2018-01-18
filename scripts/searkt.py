# Generate configurations

# Run searkt with the configs

import copy
import json

algorithmsToRun = ['WEIGHTED_A_STAR', 'SAFE_REAL_TIME_SEARCH']
weight = [3.0]
actionDuration = [1000]
expansionLimit = [1000000]
lookaheadType = ['DYNAMIC']
timeLimit = [120000000]
actionDurations = [1, 5, 10]

defaultConfiguration = dict()
defaultConfiguration['algorithmName'] = algorithmsToRun
defaultConfiguration['actionDuration'] = actionDuration
defaultConfiguration['expansionLimit'] = expansionLimit
defaultConfiguration['lookaheadType'] = lookaheadType
defaultConfiguration['actionDuration'] = actionDurations

def generate_racetrack():
    configurations = [{}] 

    racetracks = ['uniform.track', 'long.track']
    configurations = cartesian_product(configurations, 'domainName', ['RACETRACK'])
    configurations = cartesian_product(configurations, 'domainPath', racetracks)

    for key, value in defaultConfiguration.items():
        configurations = cartesian_product(configurations, key, value)

    configurations = cartesian_product(configurations, 'weight', weight, 'algorithmName', 'WEIGHTED_A_STAR')
    return configurations



def generate_configurations():
    configurations = [{}]

    configurations = cartesian_product(configurations, 'domainName', ['RACETRACK'])
    configurations = cartesian_product(configurations, 'domainPath', [''])
    configurations = cartesian_product(configurations, 'algorithmName', ['a', 'b'])
    configurations = cartesian_product(configurations, 'actionDuration', ['a', 'b'])
    configurations = cartesian_product(configurations, 'lookaheadType', ['a', 'b'])
    configurations = cartesian_product(configurations, 'timeLimit', ['a', 'b'])
    configurations = cartesian_product(configurations, 'expansionLimit', ['1', '2'])

    return configurations


def cartesian_product(base, key, values, filter_key = '', filter_value = ''):
    new_base = []
    if filter_key == '' and filter_value ==  '':
       for item in base:
            for value in values:
                new_configuration = copy.deepcopy(item)
                new_configuration[key] = value
                new_base.append(new_configuration)
    else:
        new_base = copy.deepcopy(base)
        for item in new_base:
            iterCopy = dict(item)
            for a_key, value in iterCopy.items():
                if str(a_key) == str(filter_key) and filter_value in item[str(a_key)]:
                    item[str(key)] = values

    return new_base


def main():
    configurations = generate_configurations()
    configurations = generate_racetrack()
    print(json.dumps(configurations))
    pass


if __name__ == '__main__':
    main()
