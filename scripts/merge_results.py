#!/usr/bin/env python3

# Generate configurations

# Run searkt with the configs

import argparse
import json
import gzip

__author__ = 'Bence Cserna'


def load_json(file_name):
    if file_name.endswith('.gz'):
        with gzip.open(file_name, "rb") as file:
            return json.loads(file.read().decode("utf-8"))

    with open(file_name) as file:
        return json.load(file)


def save_json(output_file, results):
    with open(output_file, 'w') as outfile:
        json.dump(results, outfile)


def main():
    input_files, output_file = parse_arguments()

    print(f'Merging the following files:')

    results = []
    for input_file in input_files:
        json = load_json(input_file)
        results += json
        print(f'\t{input_file} length: {len(json)}')

    save_json(output_file, results)

    print(f'\nMerged json saved to {output_file} with total length: {len(results)}')


def parse_arguments():
    parser = argparse.ArgumentParser()

    parser.add_argument("-i", "--inputs", nargs="+", help="Results json file paths to merge. ")
    parser.add_argument("-o", "--output", required=True, help="Output file path for merged results",
                        default="merged_results.json")

    args = parser.parse_args()
    return args.inputs, args.output


if __name__ == '__main__':
    main()
