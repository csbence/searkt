#!/usr/bin/python3

import pprint
from pymongo import MongoClient


def open_connection():
    client = MongoClient('mongodb://aerials.cs.unh.edu:42830')
    client.rts.authenticate('rtsUser', 'VeLuvh4!', mechanism='SCRAM-SHA-1')
    return client.rts


def print_status(db):
    configuration_status = db.command('collstats', 'experimentConfiguration')
    print('Configuration count: %d' % configuration_status['count'])
    task_status = db.command('collstats', 'experimentTask')
    print('Configuration count: %d' % task_status['count'])
    result_status = db.command('collstats', 'experimentResult')
    print('Configuration count: %d' % result_status['count'])
    # pprint.pprint(configuration_status, width=1)

    pass


if __name__ == '__main__':
    db = open_connection()
    print_status(db)
