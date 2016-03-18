#!/usr/bin/python

import os
import sys
import getopt
import json
import numpy


class Result:
    def __init__(self, algorithm, domain, time):
        self.experimentConfiguration = {'algorithmName': algorithm, 'domainName': domain}
        self.generatedNodes = 10
        self.expandedNodes = 12
        self.actions = ['1', '2']
        self.nanoTime = time


script = os.path.basename(sys.argv[0])
options = "ha:n:d:D:"


def usage():
    print "usage:"
    print "{} [{}]".format(script, options)
    print "options:"
    print "  h         print this usage info"
    print "  a <alg>   specify the algorithm to store"
    print "  d <dom>   specify the domain to store"
    print "  n <num>   specify the number of files to generate"
    print "  D <dir>   specify a directory to put output files in"
    print "algorithm, domain, and number arguments are mandatory"


try:
    opts, args = getopt.gnu_getopt(sys.argv[1:], options, ['help=', 'algorithm=', 'domain=', 'number=', "Dir="])
except getopt.GetoptError:
    usage()
    sys.exit(2)

directory = ''
algorithm = None
domain = None
number = None
for opt, arg in opts:
    if opt in ('-h', '--help'):
        usage()
        sys.exit(0)
    elif opt in ('-a', '--algorithm'):
        algorithm = arg
    elif opt in ('-d', '--domain'):
        domain = arg
    elif opt in ('-n', '--number'):
        number = int(arg)
    elif opt in ('-D', '--Dir'):
        directory = arg
    else:
        usage()
        sys.exit(2)

if algorithm is None or domain is None or number is None:
    usage()
    sys.exit(2)


def get_filenum(num):
    if num < 10:
        return "_0" + str(num)
    else:
        return "_" + str(num)


def get_filename(outFile, count, extension, directory=None):
    if directory is None:
        return outFile + get_filenum(count) + ext
    else:
        return directory + '/' + outFile + get_filenum(count) + ext


for i in range(0, number):
    time = numpy.random.random_integers(1, 100000000000)
    result = Result(algorithm, domain, time)

    # Get unique file name
    count = 0
    outFile = algorithm.replace(' ', '-') + "_" + domain.replace(' ', '-')
    ext = ".json"
    while os.path.exists(get_filename(outFile, count, ext, directory)):
        count += 1

    f = open(get_filename(outFile, count, ext, directory), 'w')
    print "Writing to file " + f.name
    f.write(json.dumps(result.__dict__))
