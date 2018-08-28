#!/usr/bin/env python3

import json
import gzip
import os
import shutil


def read_data(file_name):
    if file_name.endswith('.gz'):
        with gzip.open("input.json.gz", "rb") as file:
            return json.loads(file.read().decode("utf-8"))

    with open(file_name) as file:
        return json.load(file)


def main(result_path, src_path_prefix, output_dir):
    if not os.path.exists(output_dir):
        os.makedirs(output_dir)

    experiments = read_data(result_path)

    count = 0
    for exp in experiments:
        if exp["success"]:
            file_name = exp["configuration"]["domainPath"]
            file_name = output_dir + file_name[file_name.rfind("/"):]

            file_name = file_name[0:file_name.rfind("-")] + "-" + str(count) + ".vw"

            original_file_name = src_path_prefix + exp["configuration"]["domainPath"]
            shutil.copy(original_file_name, file_name)
            count += 1


if __name__ == "__main__":
    main('../output/reachability.json', '../src/main/resources/', '../filtered_domains')
