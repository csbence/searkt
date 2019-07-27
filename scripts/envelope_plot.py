#!/usr/bin/env python3

import json
import gzip
import copy
import pandas as pd
from pandas import DataFrame
import statsmodels.stats.api as sms

__author__ = 'Kevin C. Gall'

alg_map = {"A_STAR": "A*", "LSS_LRTA_STAR": "LSS-LRTA*", "SAFE_RTS": "SRTS", "S_ZERO": "S0", "SIMPLE_SAFE": "SS",
           "SINGLE_SAFE": "BEST_SAFE", "SAFE_RTS_TOP": "SRTS_TOP", "TIME_BOUNDED_A_STAR": "TBA*", "CES": "CES",
           "BACK_ES": "Back-ES", "BI_ES": "Bi-ES"}


def read_data(file_name):
    if file_name.endswith('.gz'):
        with gzip.open(file_name, "rb") as file:
            return json.loads(file.read().decode("utf-8"))

    with open(file_name) as file:
        return json.load(file)


def construct_data_frame(data):
    flat_data = [flatten(experiment) for experiment in data]
    df = DataFrame(flat_data)
    remove_unused_columns(df)

    return df


def flatten(experiment):
    experiment_configuration = {}
    if 'configuration' in experiment:
        experiment_configuration = experiment.pop('configuration')

    return {**experiment, **experiment_configuration}


def remove_unused_columns(data):
    data.drop(['actions', 'commitmentType', "timeLimit",
               "terminationType", 'timestamp', 'octileMovement', 'lookaheadType',
               'firstIterationDuration',
               "targetSelection", "safetyExplorationRatio", "safetyProof", "safetyWindowSize", "safetyBackup",
               'averageVelocity', "proofSuccessful", "rawDomain", "anytimeMaxCount",
               "systemProperties", "towardTopNode", "numberOfProofs"],
              axis=1,
              inplace=True,
              errors='ignore')


'''
Analyze:
    - within optimal
    - cpu time per iteration
''' 
def analyze_metrics(exp, opt):
    exp_df = construct_data_frame(exp)
    opt_df = construct_data_frame(opt)

    opt_df['optimalPathLength'] = opt_df['pathLength']
    opt_df = opt_df[['domainPath', 'domainSeed', 'optimalPathLength']]

    exp_df = pd.merge(exp_df, opt_df, how='inner', on=['domainPath','domainSeed'])

    exp_df['withinOpt'] = exp_df['goalAchievementTime'] / (exp_df["actionDuration"] * exp_df["optimalPathLength"])

    return exp_df


'''
    Internal private function - common code for generating
    results data frames and allowing results to be grouped by
    configuration options
'''
def generate_results(data, group_by, custom_columns, calculate_metric):
    # remove errors
    data = data[data.success != False]


    columns = ['algCode', 'algorithmName']
    columns.extend(custom_columns)
    results = DataFrame(columns=columns)

    # prep config dict by making first pass through group_by
    config = {}
    for gp_dict in group_by.values():
        for column in gp_dict.keys():
            if column not in config:
                config[column] = list()

    # Change data structure such that within optimal is averaged,
    # grouped by action duration and algorithm as specified in group by options

    for algorithm_name, custom_grouping_map in group_by.items():
        alg_data = data[data['algorithmName'] == algorithm_name]
        groupings = [*custom_grouping_map]

        for fields, experiment_group in alg_data.groupby(groupings):

            transformed_alg_name = algorithm_name
            if transformed_alg_name in alg_map:
                transformed_alg_name = alg_map[transformed_alg_name]

            if len(groupings) == 1:
                fields = [fields]

            # iterate through our grouping field values and
            # record the value. We will add to dataframe later
            unused_config_keys = set(config.keys())
            for field_name, field_value in zip(groupings, fields):
                if field_name in custom_grouping_map:
                    config[field_name].append(field_value)
                    unused_config_keys.remove(field_name)

                    if custom_grouping_map[field_name] != None:
                        transformed_alg_name += ' ' + custom_grouping_map[field_name] + ': ' + str(field_value)

            # Append None for any config not present in this
            # algorithm
            for key in unused_config_keys:
                config[key].append(None) # blank value

            # calculate the metric being reported. Add results to standard fields
            row_data = [algorithm_name, transformed_alg_name]
            print(transformed_alg_name)
            row_data.extend(calculate_metric(experiment_group))
            results = add_row(results, row_data)

    # extend df for custom config we want to filter on later
    for column, value_list in config.items():
        results[column] = value_list

    return results


def expansion_calculator(experiment_group):
    mean_within_opt = experiment_group['withinOpt'].mean()
    within_opt_list = list(experiment_group['withinOpt'])
    bound = sms.DescrStatsW(within_opt_list).zconfint_mean()

    return [mean_within_opt, abs(mean_within_opt - bound[0]), abs(mean_within_opt - bound[1])]


def get_expansion_results(data, group_by):
    group_by = copy.deepcopy(group_by)
    # add actionDuration to user-specified groupings
    for _, groupings in group_by.items():
        groupings['actionDuration'] = None

    columns = "withinOpt yLbound yRbound".split()
    return generate_results(data, group_by, columns, expansion_calculator)


def cpu_calculator(experiment_group):
    mean_within_opt = experiment_group['withinOpt'].mean()
    within_opt_list = list(experiment_group['withinOpt'])
    opt_bound = sms.DescrStatsW(within_opt_list).zconfint_mean()

    mean_cpu = experiment_group['percentile95Cpu'].mean() / 1000000
    cpu_list = list(experiment_group['percentile95Cpu'] / 1000000)
    cpu_bound = sms.DescrStatsW(cpu_list).zconfint_mean()

    print(f'''
        Duration {experiment_group['actionDuration'].unique()}
        Percentile {mean_cpu}
        Within Opt {mean_within_opt}
        ''')

    print(bad_form)
    if bad_form == '8room':
        for idx, row in experiment_group.iterrows():
            print(f'Generated: {row["generatedNodes"]}')
            print(f'Path: {row["pathLength"]}')

    return [experiment_group['actionDuration'], mean_within_opt, abs(mean_within_opt - opt_bound[0]), abs(mean_within_opt - opt_bound[1]),
            mean_cpu, abs(mean_cpu - cpu_bound[0]), abs(mean_cpu - cpu_bound[1])]


def get_cpu_results(data, group_by):
    group_by = copy.deepcopy(group_by)
    # add actionDuration to user-specified groupings
    for _, groupings in group_by.items():
        groupings['actionDuration'] = None

    columns = "actionDuration withinOpt yLbound yRbound percentile95Cpu xLbound xRbound".split()
    return generate_results(data, group_by, columns, cpu_calculator)


def add_row(df, values):
    return df.append(dict(zip(df.columns.values, values)), ignore_index=True)


def analyze_results(optimal_plans, experiments):
    exp_data = []
    for path_name in experiments:
        exp_data += read_data(path_name)


    optimal_data = []
    for optimal_path_name in optimal_plans:
        optimal_data += read_data(optimal_path_name)

    df = analyze_metrics(exp_data, optimal_data)

    return df


# Prepare data for plotting - plot factor of optimal with confidence bounds grouped by algorithm and alg parameters.
# Algorithms grouped by algorithm name by default, additionally grouped by parameters as specified in second parameter
# data - The data
# domain_tokens - optional array of domain tokens to split the results by
# group_by - dict; each key is an algorithm name. Each value is another dict of key-value pairs: configuration to label.
#           Note that if an algorithm name is not a key in group_by, it will not be returned in the results
# returns if domain_tokens not passed, the results. If domain_tokens passed, a dict where each key is a token and each
#           value is the result for that domain
bad_form = None
def prepare_within_opt_plots(data, domain_tokens, group_by=None):
    global bad_form
    if group_by is None:
        group_by = {}

    expansion_results = {
        'xaxis': {
            'title': 'Action Duration',
            'data': 'actionDuration'
        }
    }
    cpu_results = {
        'xaxis': {
            'title': 'CPU time per Iteration (ms)',
            'data': 'percentile95Cpu'
        }
    }
    for domain in domain_tokens:
        domain_data = data[data['domainPath'].str.contains(domain)]
        expansion_results[domain] = get_expansion_results(domain_data, group_by)
        print(domain)
        bad_form = domain
        cpu_results[domain] = get_cpu_results(domain_data, group_by)

    return expansion_results, cpu_results



# Scratch "main" script
if __name__ == '__main__':
    print('No main operations')
