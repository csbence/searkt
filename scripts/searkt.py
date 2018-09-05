#!/usr/bin/env python3

# Generate configurations

# Run searkt with the configs

import copy
import json
import os
from subprocess import run, TimeoutExpired, PIPE
from tqdm import tqdm
from concurrent.futures import ThreadPoolExecutor
import pandas as pd
import itertools

__author__ = 'Bence Cserna, William Doyle, Kevin C. Gall'


def generate_base_suboptimal_configuration():
    algorithms_to_run = ['WEIGHTED_A_STAR', 'DPS']
    expansion_limit = [100000000]
    lookahead_type = ['DYNAMIC']
    time_limit = [1000000000000]
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
    weight = [3.0]
    compiled_configurations = cartesian_product(compiled_configurations,
                                                'weight', weight,
                                                [['algorithmName', 'WEIGHTED_A_STAR']])

    compiled_configurations = cartesian_product(compiled_configurations,
                                                'weight', weight,
                                                [['algorithmName', 'DPS']])

    return compiled_configurations


def generate_base_configuration():
    # required configuration parameters
    # algorithms_to_run = ['A_STAR']
    algorithms_to_run = ['ES']
    # algorithms_to_run = ['ES', 'LSS_LRTA_STAR', 'TIME_BOUNDED_A_STAR']
    # algorithms_to_run = ['ES', 'TIME_BOUNDED_A_STAR']
    # algorithms_to_run = ['LSS_LRTA_STAR']
    # algorithms_to_run = ['LSS_LRTA_STAR', 'TIME_BOUNDED_A_STAR']
    #algorithms_to_run = ['TIME_BOUNDED_A_STAR']
    expansion_limit = [100000000]
    lookahead_type = ['DYNAMIC']
    time_limit = [300000000000]
    #action_durations = [1] # Use this for A*
    # action_durations = [10000000]
    action_durations = [10000000, 12000000, 16000000, 20000000, 25000000, 32000000]
    # action_durations = [40000000]
    # action_durations = [50, 100, 150, 200, 250, 400, 800, 1600, 3200, 6400, 12800]
    termination_types = ['TIME']
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
    base_configuration['terminationTimeEpsilon'] = [5000000]  # 4ms

    # base_configuration['expansionDelay'] = [0, 200, 400, 600, 800, 1000]
    base_configuration['expansionDelay'] = [1000, 10000, 50000]

    compiled_configurations = [{}]

    for key, value in base_configuration.items():
        compiled_configurations = cartesian_product(compiled_configurations, key, value)

    # Algorithm specific configurations
    weight = [3.0]
    compiled_configurations = cartesian_product(compiled_configurations,
                                                'weight', weight,
                                                [['algorithmName', 'WEIGHTED_A_STAR']])

    # Envelope-based
    # No configurable resource ratio for RES at this time

    compiled_configurations = cartesian_product(compiled_configurations,
                                                'backlogRatio', [0.2],
                                                [['algorithmName', 'TIME_BOUNDED_A_STAR']])

    # TBA*
    optimizations = ['THRESHOLD']
    compiled_configurations = cartesian_product(compiled_configurations,
                                                'tbaOptimization', optimizations,
                                                [['algorithmName', 'TIME_BOUNDED_A_STAR']])

    compiled_configurations = cartesian_product(compiled_configurations,
                                                'backupComparator', ['F'],#, 'PSEUDO_F'],
                                                [['algorithmName', 'ES']])
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
    for puzzle in range(1, 11):
        puzzles.append(str(puzzle))

    puzzle_base_path = 'input/tiles/korf/4/real/'
    full_puzzle_paths = [puzzle_base_path + puzzle for puzzle in puzzles]

    configurations = cartesian_product(configurations, 'domainName', ['SLIDING_TILE_PUZZLE_4'])
    configurations = cartesian_product(configurations, 'domainPath', full_puzzle_paths)

    return configurations


# Generates configs for Dragon Age Origins game map (Nathan Sturtevant)
def generate_grid_world():
    configurations = generate_base_configuration()

    domain_paths = []

    # Build all domain paths
    dao_base_path = 'input/vacuum/orz100d/orz100d.map_scen_'
    dao_paths = []
    minima1500_base_path = 'input/vacuum/minima1500/minima1500_1500-'
    minima1500_paths = []
    minima3000_base_path = 'input/vacuum/minima3k_300/minima3000_300-'
    minima3000_paths = []
    uniform1500_base_path = 'input/vacuum/uniform1500/uniform1500_1500-'
    uniform1500_paths = []
    for scenario_num in range(0, 15): # large set 25
        n = str(scenario_num)
        dao_paths.append(dao_base_path + n)
        minima1500_paths.append(minima1500_base_path + n + '.vw')
        minima3000_paths.append(minima3000_base_path + n + '.vw')
        uniform1500_paths.append(uniform1500_base_path + n + '.vw')

    # domain_paths.extend(dao_paths)
    domain_paths.extend(minima1500_paths)
    # domain_paths.extend(minima3000_paths) # this was not included in the large set
    # domain_paths.extend(uniform1500_paths)

    configurations = cartesian_product(configurations, 'domainName', ['GRID_WORLD'])
    configurations = cartesian_product(configurations, 'domainPath', domain_paths)

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
    # OpenJ9 Command
    # Metronome configuration allocates maximum of 2ms out of every 10ms
    # to gc. When all configurations tested have time limits that are divisible by 10,
    # this allows us to split the time spent on garbage collecting throughout the algorithm,
    # which allows for more even breaking. Additionally, we do not need to manage different
    # configurations for termination epsilon since all GC ops can only take 2ms at any given
    # time which falls within the ability of the termination checker to guard against.
    command = ['/home/aifs2/group/jvms/jdk8u181-b13/bin/java', '-Xms7G', '-Xmx7G', '-jar',
               '-Xgcpolicy:metronome', '-Xgc:targetPauseTime=2', '-Xgc:targetUtilization=80',
               '-server', '-Xgc:nosynchronousGCOnOOM', '-Xdisableexplicitgc',
               '/home/aifs2/group/code/real_time_search/searkt/build/libs/real-time-search-1.0-SNAPSHOT.jar']
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
    # Below is for debugging with console outputs
    # with open('scripts/raw.log', 'w') as f:
    #     for line in raw_output:
    #         f.write(line+'\n')

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


def distributed_execution(configurations):
    from slack_notification import start_experiment_notification, end_experiment_notification
    from distlre.distlre import DistLRE, Task, RemoteHost
    import getpass

    progress_bar = tqdm(total=len(configurations))
    HOSTS = ['ai' + str(i) + '.cs.unh.edu' for i in [1, 2, 3, 4, 5, 6, 8, 9, 10, 11, 12, 13, 14, 15]]
    #HOSTS = ['ai' + str(i) + '.cs.unh.edu' for i in [1, 2, 3, 4, 5, 6, 8]]
    #HOSTS = ['ai' + str(i) + '.cs.unh.edu' for i in [9, 10, 11, 12, 13, 14, 15]]

    print('\nExecuting configurations on the following ai servers: ')
    print(HOSTS)

    # I would recommend setting up public key auth for your ssh
    # password = getpass.getpass("Password to connect to [ai.cs.unh.edu]")
    password = None

    remote_hosts = [RemoteHost(host, port=22, password=password) for host in HOSTS]
    executor = DistLRE(remote_hosts=remote_hosts)

    futures = []
    for configuration in configurations:
        # TODO remove hardcoded java home
        command = ' '.join(
            ['/home/aifs2/group/jvms/jdk8u181-b13/bin/java', '-Xms7G', '-Xmx7G', '-Xgcpolicy:metronome',
             '-Xgc:targetPauseTime=2', '-Xgc:targetUtilization=80',
             '-Xdisableexplicitgc', '-Xgc:nosynchronousGCOnOOM',
             '-jar', '/home/aifs2/group/code/real_time_search/searkt/build/libs/real-time-search-1.0-SNAPSHOT.jar'])

        json_configuration = f'[{json.dumps(configuration)}]\n'

        task = Task(command=command, meta='META', time_limit=10, memory_limit=10)
        task.input = json_configuration

        future = executor.submit(task)
        future.add_done_callback(lambda _: progress_bar.update())

        futures.append(future)

    start_experiment_notification(experiment_count=len(configurations))
    print('Experiments started')
    executor.execute_tasks()

    executor.wait()

    print('Experiments finished')
    end_experiment_notification()

    results = []

    for future in futures:
        exception = future.exception()
        if exception:
            results.append({
                'configuration': configuration,
                'success': False,
                'errorMessage': 'exception ::' + str(exception)
            })
            continue

        result = future.result()
        # print(f'output: {result.output}')

        # if result.error:
        #     results.append({
        #         'configuration': configuration,
        #         'success': False,
        #         'errorMessage': 'unknown error ::' + str(result.error)
        #     })
        #     continue

        raw_output = result.output.splitlines()
        result_offset = raw_output.index('#') + 1
        output = json.loads(raw_output[result_offset])
        results += output

    return results


def read_results_from_file(file_name):
    if file_name.endswith('.gz'):
        with gzip.open("input.json.gz", "rb") as file:
            return json.loads(file.read().decode("utf-8"))

    with open(file_name) as file:
        return json.load(file)


def inplace_merge_experiments(old_results, new_results):
    for new_result in new_results:
        replaced = False
        for i, old_result in enumerate(old_results):
            if old_result['configuration'] == new_result['configuration']:
                old_results[i] = new_result
                replaced = True
                break

        if not replaced:
            old_results.append(new_result)


def extract_configurations_from_failed_results(results):
    return [result['configuration'] for result in results if not result['success']]


def build_searkt():
    env = os.environ.copy()
    env['JAVA_HOME'] = '/home/aifs2/group/jvms/jdk8u181-b13'

    return_code = run(['./gradlew', 'jar', '-x', 'test'], env=env).returncode
    return return_code == 0


def print_summary(results_json):
    results = pd.read_json(json.dumps(results_json))
    print('Successful: {}/{}'.format(results.success.sum(), len(results_json)))


def save_results(results_json, file_name):
    with open(file_name, 'w') as outfile:
        json.dump(results_json, outfile)
    print(f'Results saved to {file_name}')


def main():
    os.chdir('..')

    if not build_searkt():
        raise Exception('Build failed. Make sure the jar generation is functioning. ')
    print('Build complete!')

    #configurations = generate_grid_world()  # generate_racetrack()
    file_name = 'output/results_es_m15_15_dur_10_12_16_20_25_32_delay_1_10_50.json'
    old_results = read_results_from_file(file_name)

    configurations = extract_configurations_from_failed_results(old_results)

    print('{} configurations has been generated '.format(len(configurations)))

    results = distributed_execution(configurations)

    inplace_merge_experiments(old_results, results)
    results = old_results

    for result in results:
        result.pop('actions', None)
        result.pop('systemProperties', None)

    save_results(results, 'output/results_temp.json')

    save_results(results, file_name)
    print_summary(results)

    print('{} results has been received.'.format(len(results)))


if __name__ == '__main__':
    main()
