#!/usr/bin/env python3

import datetime
import getpass
import os
from subprocess import run, PIPE

import simplejson as json
import numpy as np
from distlre.distlre import DistLRE, Task, RemoteHost
from slack_notification import start_experiment_notification, end_experiment_notification
from tqdm import tqdm
import gzip

projectRoot = '/home/aifs1/kch29/repos/searkt'
configFile = 'refresh_configs.json'
tag = "FRESH_BENCHMARK"

print('Building project')
os.chdir(projectRoot)
proc = run(['./gradlew', 'jar', '-x', 'test'])
assert proc.returncode == 0

HOSTS = ['ai' + str(i) + '.cs.unh.edu' for i in [1, 2, 3, 4, 5, 6, 8, 9, 10, 11, 12, 13, 14, 15]]
port_number = 22

# use if your ssh auth key isn't set up
# password = getpass.getpass("Password to connect to [ai.cs.unh.edu]")
remote_hosts = [RemoteHost(host, port=port_number) for host in HOSTS]
executor = DistLRE(remote_hosts=remote_hosts)
print(HOSTS)


file_lock_path = '/home/aifs2/group/exp/distlre.lock'

def execute_tasks():
    
#    if not os.path.isfile(file_lock_path):
    #    lock_file = open(file_lock_path, 'w')
    #    lock_file.write(getpass.getuser() + '@' + str(datetime.datetime.now()))
    #    lock_file.close()
        executor.execute_tasks()
        executor.wait()
    #    os.remove(file_lock_path)
#    else:
#        lock_file = open(file_lock_path, 'r')
#        contents = lock_file.read().strip().split('@')
#        user, time = (contents[0], contents[1])
#        raise Exception(f"{user} started experiments @ {time}, check #experiments on Slack for updates")
    


vmstat_command = "vmstat 3 3"
commands = [(vmstat_command, machine) for machine in HOSTS]
tasks = [Task(command=command, meta=f"VMcheck@{host}", time_limit=10, memory_limit=10) for command, host in commands]
futures = []
for task in tasks:
    future = executor.submit(task)
    futures.append(future)
execute_tasks()
for future in futures:
    result = future.result()
    output = result.output.split('\n')
    last_line = output[-2].split()
    idle = last_line[-3]
    print(idle, end=' ')
    assert int(idle) >= 90
    print(result.meta, "is idle!")

remote_hosts = [RemoteHost(host, port=port_number) for host in HOSTS]
executor = DistLRE(remote_hosts=remote_hosts)
f = open(f"{projectRoot}/configs/{configFile}")
worlds = json.load(f)
worlds = worlds[:10] # for testing
print(f'Loaded {len(worlds)} configurations')
# experiments = create_experiments(worlds)
progress_bar = tqdm(total=len(worlds))
configs = ['[' + json.dumps(config) + ']' + '\n' for config in worlds]
# commands = [experiment.command for experiment in experiments]
start_experiment_notification(experiment_count=len(worlds), machine=str(HOSTS))


def update_progress_bar(future):
    future.result().meta.update(1)
    future.result().meta


command = f"/home/aifs2/group/jvms/jdk-12/bin/java -Xms7500m -Xmx7500m -jar {projectRoot}/build/libs/real-time-search-1.0-SNAPSHOT.jar -stdinConfiguration"
tasks = [Task(command=command, meta=progress_bar, time_limit=3600, memory_limit=10) for config in worlds]
for task, config in zip(tasks, configs):
    task.input = config

futures = []
for task in tasks:
    future = executor.submit(task)
    future.add_done_callback(update_progress_bar)
    futures.append(future)
execute_tasks()

end_experiment_notification()

results = []
for future in futures:
    result = future.result()
    results.append(result)


def save_results(results, tag, path_prefix=None):
    o_results = []
    file_handle = ""
    if path_prefix is not None:
        file_handle += path_prefix
    file_handle += "output/data{}-{:%H-%M-%d-%m-%y}.json.gz".format(tag, datetime.datetime.now())
    f = gzip.open(file_handle, 'wt')
    for exp_result in results:
        res_dict = json.loads(exp_result)[0]  # loads as array
        res_dict.pop('actions', None)  # don't store on disk - too much space!

        # Don't store CPU list on disk, but do do some analysis
        cpu_list = res_dict.pop('iterationCpuTimeList')
        if len(cpu_list) > 0:
            res_dict['maxIterationCpu'] = int(np.max(cpu_list))
            res_dict['avgIterationCpu'] = int(np.mean(cpu_list))
            res_dict['minIterationCpu'] = int(np.min(cpu_list))
            percentile_list = np.percentile(cpu_list, [i for i in range(1, 99)])
            for i in range(1, 99):
                res_dict['percentile' + str(i) + 'Cpu'] = int(percentile_list[i])

        attributes = res_dict['attributes']
        goal_found = attributes.get('goalPathExtracted', 0)
        narrow_cpu_list = cpu_list[:goal_found]
        if len(narrow_cpu_list) > 0:
            res_dict['preGoalMaxIterationCpu'] = int(np.max(narrow_cpu_list))
            res_dict['preGoalAvgIterationCpu'] = int(np.mean(narrow_cpu_list))
            res_dict['preGoalMinIterationCpu'] = int(np.min(narrow_cpu_list))
            percentile_list = np.percentile(narrow_cpu_list, [i for i in range(1, 99)])
            for i in range(1, 99):
                res_dict['preGoalPercentile' + str(i) + 'Cpu'] = int(percentile_list[i])

        o_results.append(res_dict)

    f.write(json.dumps(o_results))
    f.close()
    return file_handle


results_file = save_results([result.output.split('#')[-1].strip() for result in results], tag,
                            path_prefix=projectRoot+"/")
print(results_file)
