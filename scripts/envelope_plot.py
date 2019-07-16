#!/usr/bin/env python3

import json
import gzip
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
    data.drop(['actions', 'commitmentType', "success", "timeLimit",
               "terminationType", 'timestamp', 'octileMovement', 'lookaheadType',
               'firstIterationDuration',
               "targetSelection", "safetyExplorationRatio", "safetyProof", "safetyWindowSize", "safetyBackup",
               'domainSeed', 'averageVelocity', "proofSuccessful", "rawDomain", "anytimeMaxCount",
               "systemProperties", "towardTopNode", "numberOfProofs"],
              axis=1,
              inplace=True,
              errors='ignore')


def analyze_within_optimal(exp, opt):
    exp_df = construct_data_frame(exp)
    opt_df = construct_data_frame(opt)

    opt_df['optimalPathLength'] = opt_df['pathLength']
    opt_df = opt_df[['domainPath', 'optimalPathLength']]

    exp_df = pd.merge(exp_df, opt_df, how='inner', on=['domainPath'])
    exp_df['withinOpt'] = exp_df['goalAchievementTime'] / (exp_df["actionDuration"] * exp_df["optimalPathLength"])

    return exp_df


def get_within_opt_results(data, group_by):
    results = DataFrame(columns="actionDuration withinOpt algorithmName lbound rbound algCode".split())

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
        groupings = ['actionDuration']

        for field in custom_grouping_map:
            groupings.append(field)

        for fields, duration_group in alg_data.groupby(groupings):
            transformed_alg_name = algorithm_name
            if transformed_alg_name in alg_map:
                transformed_alg_name = alg_map[transformed_alg_name]

            unused_config_keys = set(config.keys())
            action_duration = fields
            if len(groupings) > 1:
                action_duration = fields[0]

                for field_name, field_value in zip(groupings, fields):
                    if field_name in custom_grouping_map:
                        config[field_name].append(field_value)
                        unused_config_keys.remove(field_name)

                        transformed_alg_name += ' ' + custom_grouping_map[field_name] + ': ' + str(field_value)

            for key in unused_config_keys:
                config[key].append(None) # blank value

            # Get mean of within optimal calculation, add row to results dataframe
            mean_within_opt = duration_group['withinOpt'].mean()
            within_opt_list = list(duration_group['withinOpt'])
            bound = sms.DescrStatsW(within_opt_list).zconfint_mean()
            results = add_row(results, [action_duration, mean_within_opt, transformed_alg_name,
                                        abs(mean_within_opt - bound[0]), abs(mean_within_opt - bound[1]),
                                        algorithm_name])

    # extend df for custom config we want to filter on later
    for column, value_list in config.items():
        results[column] = value_list

    return results


def add_row(df, values):
    return df.append(dict(zip(df.columns.values, values)), ignore_index=True)


def analyze_results(optimal_plans, experiments):
    exp_data = []
    for path_name in experiments:
        exp_data += read_data(path_name)

    optimal_data = []
    for optimal_path_name in optimal_plans:
        optimal_data += read_data(optimal_path_name)

    df = analyze_within_optimal(exp_data, optimal_data)

    return df


# Prepare data for plotting - plot factor of optimal with confidence bounds grouped by algorithm and alg parameters.
# Algorithms grouped by algorithm name by default, additionally grouped by parameters as specified in second parameter
# data - The data
# domain_tokens - optional array of domain tokens to split the results by
# group_by - dict; each key is an algorithm name. Each value is another dict of key-value pairs: configuration to label.
#           Note that if an algorithm name is not a key in group_by, it will not be returned in the results
# returns if domain_tokens not passed, the results. If domain_tokens passed, a dict where each key is a token and each
#           value is the result for that domain
def prepare_for_within_opt_plot(data, domain_tokens=None, group_by=None):
    if group_by is None:
        group_by = {}

    if domain_tokens is None:
        return get_within_opt_results(data, group_by)
    else:
        results = {}
        for domain in domain_tokens:
            domain_data = data[data['domainPath'].str.contains(domain)]
            results[domain] = get_within_opt_results(domain_data, group_by)

        return results


# Scratch "main" script
if __name__ == '__main__':
    df = construct_data_frame(read_data("../results/dataLIMITED2-21-01-01-07-19.json"))
    df = df[~(df.domainName == 'RACETRACK')]
    df.to_json('../results/test.json', orient='records')
