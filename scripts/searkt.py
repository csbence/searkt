#!/usr/bin/env python3

# Generate configurations

# Run searkt with the configs

import copy
import json
import os
import sys
from subprocess import run, TimeoutExpired, PIPE
from tqdm import tqdm
from concurrent.futures import ThreadPoolExecutor
import pandas as pd
import itertools
import notify2


def generate_base_suboptimal_configuration():
    algorithms_to_run = ['EES', 'DPS', 'WEIGHTED_A_STAR']
    expansion_limit = [100000000]
    lookahead_type = ['DYNAMIC']
    time_limit = [sys.maxsize]
    action_durations = [1]
    termination_types = ['EXPANSION']
    step_limits = [100000000]

    base_configuration = dict()
    base_configuration['algorithmName'] = algorithms_to_run
    base_configuration['expansionLimit'] = expansion_limit
    base_configuration['lookaheadType'] = lookahead_type
    base_configuration['actionDuration'] = action_durations
    base_configuration['terminationType'] = termination_types
    base_configuration['stepLimit'] = step_limits
    base_configuration['timeLimit'] = time_limit
    base_configuration['commitmentStrategy'] = ['SINGLE']

    compiled_configurations = [{}]

    for key, value in base_configuration.items():
        compiled_configurations = cartesian_product(compiled_configurations, key, value)

    # Algorithm specific configurations
    # weight = [3.0]
    weight = [1.17, 1.2, 1.25, 1.33, 1.5, 1.78, 2.0, 2.33, 2.67, 2.75, 3.0]  # Unit tile weights
    # weight = [1.11, 1.13, 1.14, 1.17, 1.2, 1.25, 1.5, 2.0, 2.67, 3.0]  # Heavy tile weights
    compiled_configurations = cartesian_product(compiled_configurations,
                                                'weight', weight,
                                                [['algorithmName', 'WEIGHTED_A_STAR']])

    compiled_configurations = cartesian_product(compiled_configurations,
                                                'weight', weight,
                                                [['algorithmName', 'DPS']])

    compiled_configurations = cartesian_product(compiled_configurations,
                                                'weight', weight,
                                                [['algorithmName', 'EES']])

    compiled_configurations = cartesian_product(compiled_configurations,
                                                'weight', weight,
                                                [['algorithmName', 'EETS']])


    return compiled_configurations


def generate_base_configuration():
    # required configuration parameters
    algorithms_to_run = ['SAFE_RTS']
    expansion_limit = [100000000]
    lookahead_type = ['DYNAMIC']
    time_limit = [100000000000]
    action_durations = [50, 100, 150, 200, 250, 400, 800, 1600, 3200, 6400, 12800]
    # action_durations = [50, 100, 150, 200, 250]
    termination_types = ['EXPANSION']
    step_limits = [100000000]

    base_configuration = dict()
    base_configuration['algorithmName'] = algorithms_to_run
    base_configuration['expansionLimit'] = expansion_limit
    base_configuration['lookaheadType'] = lookahead_type
    base_configuration['actionDuration'] = action_durations
    base_configuration['terminationType'] = termination_types
    base_configuration['stepLimit'] = step_limits
    base_configuration['timeLimit'] = time_limit
    base_configuration['commitmentStrategy'] = ['SINGLE']

    compiled_configurations = [{}]

    for key, value in base_configuration.items():
        compiled_configurations = cartesian_product(compiled_configurations, key, value)

    # Algorithm specific configurations
    weight = [3.0]
    compiled_configurations = cartesian_product(compiled_configurations,
                                                'weight', weight,
                                                [['algorithmName', 'WEIGHTED_A_STAR']])

    # S-RTS specific attributes
    compiled_configurations = cartesian_product(compiled_configurations,
                                                'targetSelection', ['SAFE_TO_BEST'],
                                                [['algorithmName', 'SAFE_RTS']])
    compiled_configurations = cartesian_product(compiled_configurations,
                                                # 'safetyProof', ['LOW_D_LOW_H_OPEN'],
                                                'safetyProof', ['TOP_OF_OPEN'],
                                                # 'safetyProof', ['LOW_D_LOW_H'],
                                                # 'safetyProof', ['LOW_D_TOP_PREDECESSOR'],
                                                # 'safetyProof', ['LOW_D_WINDOW', 'TOP_OF_OPEN'],
                                                [['algorithmName', 'SAFE_RTS']])

    compiled_configurations = cartesian_product(compiled_configurations,
                                                'safetyWindowSize', [1, 2, 5, 10, 15, 100],
                                                [['algorithmName', 'SAFE_RTS'], ['safetyProof', 'LOW_D_WINDOW']])
    compiled_configurations = cartesian_product(compiled_configurations,
                                                'safetyWindowSize', [1, 2, 5, 10, 15, 100],
                                                [['algorithmName', 'SAFE_RTS'],
                                                 ['safetyProof', 'LOW_D_TOP_PREDECESSOR']])
    compiled_configurations = cartesian_product(compiled_configurations,
                                                'safetyWindowSize', [1, 2, 5, 10, 15, 100],
                                                [['algorithmName', 'SAFE_RTS'], ['safetyProof', 'LOW_D_LOW_H']])
    compiled_configurations = cartesian_product(compiled_configurations,
                                                'safetyWindowSize', [0],
                                                [['algorithmName', 'SAFE_RTS'], ['safetyProof', 'LOW_D_LOW_H_OPEN']])

    compiled_configurations = cartesian_product(compiled_configurations,
                                                'safetyWindowSize', [0],
                                                [['algorithmName', 'SAFE_RTS'], ['safetyProof', 'TOP_OF_OPEN']])
    compiled_configurations = cartesian_product(compiled_configurations,
                                                'safetyExplorationRatio', [1],
                                                [['algorithmName', 'SAFE_RTS']])

    return compiled_configurations


def generate_racetrack():
    configurations = generate_base_configuration()

    racetracks = ['uniform.track', 'barto-big.track', 'hansen-bigger-quad.track']

    racetrack_base_path = 'input/racetrack/'
    full_racetrack_paths = [racetrack_base_path + racetrack for racetrack in racetracks]

    configurations = cartesian_product(configurations, 'domainName', ['RACETRACK'])
    configurations = cartesian_product(configurations, 'domainPath', full_racetrack_paths)
    configurations = cartesian_product(configurations, 'domainSeed', range(10))

    return configurations


def generate_tile_puzzle():
    configurations = generate_base_suboptimal_configuration()

    puzzles = []
    for puzzle in range(1, 101):
        puzzles.append(str(puzzle))

    puzzle_base_path = 'input/tiles/korf/4/real/'
    full_puzzle_paths = [puzzle_base_path + puzzle for puzzle in puzzles]

    configurations = cartesian_product(configurations, 'domainName', ['SLIDING_TILE_PUZZLE_4'])
    configurations = cartesian_product(configurations, 'domainPath', full_puzzle_paths)

    return configurations


def cartesian_product(base, key, values, filters=None):
    new_base = []
    if filters is None:
        for item in base:
            for value in values:
                new_configuration = copy.deepcopy(item)
                new_configuration[key] = value
                new_base.append(new_configuration)
    else:
        for item in base:
            if all(filter_key in item and item[filter_key] == filter_value for filter_key, filter_value in filters):
                new_base.extend(cartesian_product([item], key, values))
            else:
                new_base.append(item)

    return new_base


def execute_configurations(configurations, timeout=100000):
    command = ['java', '-Xms7G', '-Xmx7G', '-jar', 'build/libs/real-time-search-1.0-SNAPSHOT.jar']
    json_configurations = json.dumps(configurations)

    try:
        completed_process = run(command, input=json_configurations.encode('utf-8'), stdout=PIPE, timeout=timeout)
    except TimeoutExpired:
        return [{'configuration': configuration, 'success': False, 'errorMessage': 'timeout'}
                for configuration in configurations]
    except Exception as e:
        return [{'configuration': configuration, 'success': False, 'errorMessage': 'unknown error ::' + str(e)}
                for configuration in configurations]

    # Create error configurations if the execution failed
    if completed_process.returncode != 0:
        message = completed_process.stdout.decode('utf-8')
        return [{'configuration': configuration, 'success': False, 'errorMessage': 'execution failed ::' + message}
                for configuration in configurations]

    raw_output = completed_process.stdout.decode('utf-8').splitlines()

    result_offset = raw_output.index('#') + 1
    results = json.loads(raw_output[result_offset])

    return results


def parallel_execution(configurations, threads=1):
    progress_bar = tqdm(total=len(configurations))
    if threads == 1:
        results = []
        for configuration in configurations:
            results.extend(execute_configurations([configuration]))
            progress_bar.update()

        return results

    futures = []
    with ThreadPoolExecutor(max_workers=threads) as executor:
        for configuration in configurations:
            future = executor.submit(execute_configurations, [configuration])
            future.add_done_callback(lambda _: progress_bar.update())
            futures.append(future)

    result_lists = [future.result() for future in futures]
    return list(itertools.chain.from_iterable(result_lists))


def build_searkt():
    return_code = run(['./gradlew', 'jar', '-x', 'test']).returncode
    return return_code == 0


def print_summary(results_json):
    results = pd.read_json(json.dumps(results_json))
    print('Successful: {}/{}'.format(results.success.sum(), len(results_json)))


def save_results(results_json):
    with open('output/results_srts.json', 'w') as outfile:
        json.dump(results_json, outfile)


def main():
    notify2.init('searKt')
    os.chdir('..')

    if not build_searkt():
        raise Exception('Build failed. Make sure the jar generation is functioning. ')
    print('Build complete!')

    configurations = generate_tile_puzzle()  # generate_racetrack()
    print('{} configurations has been generated '.format(len(configurations)))

    results = parallel_execution(configurations, 1)

    for result in results:
        result.pop('actions', None)
        result.pop('systemProperties', None)

    save_results(results)
    print_summary(results)

    print('{} results have been received.'.format(len(results)))
    n = notify2.Notification("searKt has finished running", '{} results have been received'.format(len(results)),
                                 "notification-message-email")
    n.show()


if __name__ == '__main__':
    main()



