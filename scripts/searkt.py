# Generate configurations

# Run searkt with the configs

import copy
import json
import os
from subprocess import run, TimeoutExpired, PIPE
from tqdm import tqdm
from concurrent.futures import ThreadPoolExecutor


def generate_base_configuration():
    # required configuration parameters
    algorithms_to_run = ['SAFE_RTS']
    expansion_limit = [100000000]
    lookahead_type = ['DYNAMIC']
    time_limit = [120000000]
    action_durations = [50, 100, 150]
    termination_types = ['EXPANSION']
    step_limits = [100000000]

    base_configuration = dict()
    base_configuration['algorithmName'] = algorithms_to_run
    base_configuration['expansionLimit'] = expansion_limit
    base_configuration['lookaheadType'] = lookahead_type
    base_configuration['actionDuration'] = action_durations
    base_configuration['terminationType'] = termination_types
    base_configuration['stepLimit'] = step_limits
    base_configuration['timeLimit'] = [100000000000]

    compiled_configurations = [{}]

    for key, value in base_configuration.items():
        compiled_configurations = cartesian_product(compiled_configurations, key, value)

    # Algorithm specific configurations
    weight = [3.0]
    compiled_configurations = cartesian_product(compiled_configurations,
                                                'weight', weight,
                                                'algorithmName', 'WEIGHTED_A_STAR')

    # S-RTS specific attributes
    compiled_configurations = cartesian_product(compiled_configurations,
                                                'targetSelection', ['SAFE_TO_BEST'],
                                                'algorithmName', 'SAFE_RTS')
    compiled_configurations = cartesian_product(compiled_configurations,
                                                'safetyProof', ['LOW_D_WINDOW', 'TOP_OF_OPEN'],
                                                'algorithmName', 'SAFE_RTS')
    compiled_configurations = cartesian_product(compiled_configurations,
                                                'safetyExplorationRatio', [1],
                                                'algorithmName', 'SAFE_RTS')

    print(compiled_configurations)
    return compiled_configurations


def generate_racetrack():
    configurations = generate_base_configuration()

    racetracks = ['uniform.track', 'long.track']

    racetrack_base_path = 'input/racetrack/'
    full_racetrack_paths = [racetrack_base_path + racetrack for racetrack in racetracks]

    configurations = cartesian_product(configurations, 'domainName', ['RACETRACK'])
    configurations = cartesian_product(configurations, 'domainPath', full_racetrack_paths)

    return configurations


def cartesian_product(base, key, values, filter_key=None, filter_value=None):
    new_base = []
    if filter_key is None or filter_value is None:
        for item in base:
            for value in values:
                new_configuration = copy.deepcopy(item)
                new_configuration[key] = value
                new_base.append(new_configuration)
    else:
        for item in base:
            if filter_key in item and item[filter_key] == filter_value:
                new_base.extend(cartesian_product([item], key, values))
            else:
                new_base.append(item)

    return new_base


def execute_configurations(configurations, timeout):
    command = ['java', '-jar', '../build/libs/real-time-search-1.0-SNAPSHOT.jar']
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

    print('\n')
    print(raw_output)
    print('\n')

    result_offset = raw_output.index('#') + 1
    results = json.loads(raw_output[result_offset])

    return results


def parallel_execution(configurations, threads=1):
    progress_bar = tqdm(total=len(configurations))
    if threads == 1:
        results = []
        for configuration in configurations:
            results.append(execute_configuration(configuration))
            progress_bar.update()
        return results

    futures = []
    with ThreadPoolExecutor(max_workers=threads) as executor:
        for configuration in configurations:
            future = executor.submit(execute_configurations, configuration)
            future.add_done_callback(lambda _: progress_bar.update())
            futures.append(future)

    return [future.result() for future in futures]


def build_searkt():
    os.chdir('..')
    return_code = run(['./gradlew', 'jar', '-x', 'test']).returncode
    os.chdir('scripts')
    return return_code == 0


def main():
    if not build_searkt():
        raise Exception('Build failed. Make sure the jar generation is functioning. ')

    configurations = generate_racetrack()
    print(json.dumps(configurations))
    results = execute_configurations(configurations, 1000)

    for result in results:
        result.pop('actions', None)
        result.pop('systemProperties', None)

    print(results)


if __name__ == '__main__':
    main()
