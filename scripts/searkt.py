# Generate configurations

# Run searkt with the configs

import copy
import json
from subprocess import run, TimeoutExpired, PIPE
from tqdm import tqdm
from concurrent.futures import ThreadPoolExecutor
from os import path, makedirs

def generate_base_configuration():
    # required configuration parameters
    algorithms_to_run = ['WEIGHTED_A_STAR', 'SAFE_REAL_TIME_SEARCH']
    expansion_limit = [1000000]
    lookahead_type = ['DYNAMIC']
    time_limit = [120000000]
    action_durations = [1, 5, 10]

    base_configuration = dict()
    base_configuration['algorithmName'] = algorithms_to_run
    base_configuration['expansionLimit'] = expansion_limit
    base_configuration['lookaheadType'] = lookahead_type
    base_configuration['actionDuration'] = action_durations
    
    return base_configuration


def generate_racetrack():
    configurations = [{}]

    racetracks = ['uniform.track', 'long.track']
    configurations = cartesian_product(configurations, 'domainName', ['RACETRACK'])
    configurations = cartesian_product(configurations, 'domainPath', racetracks)

    for key, value in generate_base_configuration().items():
        configurations = cartesian_product(configurations, key, value)

    # Algorithm specific configurations
    weight = [3.0]
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


def cartesian_product(base, key, values, filter_key=None, filter_value=None):
    new_base = []
    if filter_key is None and filter_value is None:
        for item in base:
            for value in values:
                new_configuration = copy.deepcopy(item)
                new_configuration[key] = value
                new_base.append(new_configuration)
    else:
        for configuration in base:
            if filter_key in configuration and configuration[filter_key] == filter_value:
                new_base.extend(cartesian_product([configuration], key, values))
            else:
                new_base.append(configuration)

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
    run(['../gradlew', 'build -x test'])


def main():
    # build_searkt()
    configurations = generate_configurations()
    configurations = generate_racetrack()
    print(json.dumps(configurations))
    # execute_configurations("\n", 100)


if __name__ == '__main__':
    main()
