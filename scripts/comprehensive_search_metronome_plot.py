#!/usr/bin/env python3

import json
import gzip
import matplotlib as mpl
import matplotlib.pyplot as plt
import numpy as np
import pandas as pd
import re
import seaborn as sns
import statistics
import statsmodels.stats.api as sms
from pandas import DataFrame
from statsmodels.stats.proportion import proportion_confint

__author__ = 'Kevin C. Gall'

def main():

    expansions = [50, 100, 400, 600, 900, 1000, 1200, 1500]

    plt.title('Local Minima Domains')
    plt.plot(expansions, [10, 8.7, 9.0, 6.6, 5.6, 3.4, 2.87, 1.2], 'ro-', label='RT-Comprehensive')
    plt.plot(expansions, [14, 12.11, 11.5, 10.8, 6.1, 7.2, 6.0, 5.2], 'bo-', label='LSS-LRTA*')
    plt.legend()

    plt.ylabel('Goal Achievement Time (Factor of Optimal)')
    plt.xlabel('Expansion Limit (Per Iteration)')
    plt.figure()

    plt.title('Uniform Obstacles')
    plt.plot(expansions, [12, 11.2, 10.1, 8.8, 9.3, 7.4, 5.97, 4.2], 'ro-', label='RT-Comprehensive')
    plt.plot(expansions, [11, 8.3, 7.2, 6.9, 6.1, 3.4, 2.2, 2.0], 'bo-', label='LSS-LRTA*')
    plt.legend()

    plt.ylabel('Goal Achievement Time (Factor of Optimal)')
    plt.xlabel('Expansion Limit (Per Iteration)')
    plt.figure()

    plt.show()

if __name__ == "__main__":
    main()
