#! /usr/bin/env python

import os.path
import getpass
import pandas as pd
import simplejson as json
from datetime import datetime
from tqdm import tqdm
from searkt_im import *
from view_data import process_data
from distlre.distlre import DistLRE, Task, RemoteHost
from slack_notification import start_experimental_notification, end_experiment_notification

# set up and log into the remote machines
HOSTS = ['ai' + str(i) + '.cs.unh.edu' for i in [1, 2, 3, 4, 5, 6, 8, 9, 10, 11, 12, 13, 14, 15]]
password = getpass.getpass("Password to connect to [ai.cs.unh.edu]")
remote_hosts = [RemoteHost(host, port=22, password=password) for host in HOSTS]
executor = DistLRE(remote_hosts=remote_hosts)

# check the idle state of the machines




