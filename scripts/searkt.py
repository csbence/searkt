# Generate configurations

# Run searkt with the configs

import copy
import json

algorithmsToRun = ['WEIGHTED_A_STAR']
actionDurations = [1, 5, 10]

def generate_racetrack():
    configurations = [{}] 

    
    racetracks = ['uniform.track', 'long.track']

    configurations = cartesian_product(configurations, 'domainName', ['RACETRACK'])



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


def cartesian_product(base, key, values):
    new_base = []
    for item in base:
        for value in values:
            new_configuration = copy.deepcopy(item)
            new_configuration[key] = value
            new_base.append(new_configuration)

    return new_base


def main():
    configurations = generate_configurations()
    print(json.dumps(configurations))
    pass


if __name__ == '__main__':
    main()
