#!/usr/bin/env python3

# Generate configurations

# Run searkt with the configs

import copy
import datetime
import getpass
import itertools
import json
import os
import sys
from os import path
from subprocess import run, TimeoutExpired, PIPE
from threading import Thread

import notify2
import pandas as pd
import paramiko
from concurrent.futures import ThreadPoolExecutor
from queue import Queue, Empty
from tqdm import tqdm

import slack_notification

HOSTS = ['ai' + str(i) + '.cs.unh.edu' for i in [1, 2, 3, 4, 5, 6, 8, 9, 10, 11, 12, 13, 14, 15]]


class Experiment:
    def __init__(self, configuration, command):
        self.configuration = configuration
        self.command = command
        self.raw_result = None
        self.result = None
        self.error = None


class Worker(Thread):
    def __init__(self, hostname, job_queue, result_queue, progress_bar, password):
        super(Worker, self).__init__()
        self.progress_bar = progress_bar
        self.processed_experiment_queue = result_queue
        self.experiment_queue = job_queue
        self.hostname = hostname
        self.password = password

    def run(self):
        client = spawn_ssh_client(self.hostname, self.password)
        while True:
            try:
                experiment = self.experiment_queue.get(block=False)
                command_to_run = experiment.command
                # print("RUNNING: {}".format(command_to_run))
                stdin, stdout, stderr = client.exec_command(command_to_run)
                r = json.dumps(experiment.configuration)
                stdin.write('[' + r + ']' + '\n')
                stdin.flush()
                result = {'stdout': stdout.readlines(), 'stderr': stderr.readlines()}
                experiment.raw_result = result
                experiment.error = result['stderr']
                experiment.result = result['stdout']
                self.processed_experiment_queue.put(experiment)
                self.progress_bar.update()
                self.experiment_queue.task_done()
            except Empty:
                break


def spawn_ssh_client(hostname, password):
    key = paramiko.RSAKey.from_private_key_file(path.expanduser("~/.ssh/id_rsa"))
    client = paramiko.SSHClient()
    client.set_missing_host_key_policy(paramiko.AutoAddPolicy())
    client.connect(hostname=hostname, pkey=key, port=22, password=password)
    return client


def generate_base_suboptimal_configuration():
    algorithms_to_run = ['EES', 'EETS', 'WEIGHTED_A_STAR']
    expansion_limit = [sys.maxsize]
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
    base_configuration['errorModel'] = ['path']

    compiled_configurations = [{}]

    for key, value in base_configuration.items():
        compiled_configurations = cartesian_product(compiled_configurations, key, value)

    # Algorithm specific configurations
    weight = [1.5, 2.0, 2.5, 3.0, 3.5, 4.0, 4.5, 5.0]
    # weight = [1.17, 1.2, 1.25, 1.33, 1.5, 1.78, 2.0, 2.33, 2.67, 2.75, 3.0]  # Unit tile weights
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
    weight = [1.5, 2.0, 2.5, 3.0]
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


def create_experiments(configurations):
    command = "cd IdeaProjects/real-time-search && java -Xms7G -Xmx7G -jar build/libs/real-time-search-1.0-SNAPSHOT.jar"
    return [Experiment(configuration, command) for configuration in configurations]


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


def execute_configuration(configuration, timeout=100000):
    command = ['java', '-Xms7G', '-Xmx7G', '-jar', 'build/libs/real-time-search-1.0-SNAPSHOT.jar']
    json_configuration = json.dumps(configuration)

    try:
        completed_process = run(command, input=json_configuration.encode('utf-8'), stdout=PIPE, timeout=timeout)
    except TimeoutExpired:
        return [{'configuration': configuration, 'success': False, 'errorMessage': 'timeout'}
                for configuration in configuration]
    except Exception as e:
        return [{'configuration': configuration, 'success': False, 'errorMessage': 'unknown error ::' + str(e)}
                for configuration in configuration]

    # Create error configurations if the execution failed
    if completed_process.returncode != 0:
        message = completed_process.stdout.decode('utf-8')
        return [{'configuration': configuration, 'success': False, 'errorMessage': 'execution failed ::' + message}
                for configuration in configuration]

    raw_output = completed_process.stdout.decode('utf-8').splitlines()

    result_offset = raw_output.index('#') + 1
    results = json.loads(raw_output[result_offset])

    return results


def create_workers_for_hosts(hosts, command_queue, result_queue, progress_bar, password):
    return [Worker(hostname=host, job_queue=command_queue, result_queue=result_queue,
                   progress_bar=progress_bar, password=password) for host in hosts]


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


def save_results(results):
    l_results = []
    o_results = []
    f = open("output/data-{:%H-%M-%d-%m-%y}.json".format(datetime.datetime.now()), 'w')
    for result in results:
        parse_result = [result.result[0].strip(), result.result[1].strip()]
        result_offset = parse_result.index('#') + 1
        l_results.append((parse_result[result_offset]))
    for l in l_results:
        o_results.append((json.loads(l)[0]))
    f.write(json.dumps(o_results))
    f.close()


def main():
    notify2.init('searKt')
    os.chdir('..')

    command_queue = Queue()
    result_queue = Queue()

    configurations = generate_tile_puzzle()  # generate_racetrack()

    experiments = create_experiments(configurations)
    for experiment in experiments:
        command_queue.put(experiment)

    progress_bar = tqdm(total=command_queue.qsize())

    if not build_searkt():
        raise Exception('Build failed. Make sure the jar generation is functioning. ')
    print('Build complete!')
    print('{} configurations has been generated '.format(len(configurations)))

    slack_notification.start_experiment_notification(len(configurations))
    password = getpass.getpass("SSH Authentication Password to start experiments [ai.cs.unh.edu]:")
    workers = create_workers_for_hosts(HOSTS, command_queue, result_queue, progress_bar, password)
    # Start workers
    for worker in workers:
        worker.start()

    # Wait for workers
    for worker in workers:
        worker.join()

    command_queue.join()

    results = [result_queue.get() for i in range(result_queue.qsize())]

    save_results(results)
    # print_summary(results)

    print('{} results have been received.'.format(len(results)))
    n = notify2.Notification("searKt has finished running", '{} results have been received'.format(len(results)),
                             "notification-message-email")
    n.show()
    slack_notification.end_experiment_notification()


if __name__ == '__main__':
    main()
