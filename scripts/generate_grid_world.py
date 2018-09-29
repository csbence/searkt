#! /usr/bin/env python3

import os.path
import argparse
import random

#4/1/18 NOTE Configurations with good size of minima and distributions:
# 1500 1500 -n 10 -o .00015 -m

__author__ = "Kevin C. Gall"


#minimum tracker is set: (int,int) members indicate the cell is an obstacle
def generateMinimum(obstacleTracker, start, sizeBound):
    firstWallSize = random.randint(1, sizeBound)
    midWallSize = random.randint(1, sizeBound)
    lastWallSize = random.randint(1, sizeBound)

    direction = random.randint(0, 3)
    firstVector = {"x": 0, "y": 0}

    if (direction == 0):
        firstVector['x'] = -1
        firstVector['y'] = 0
    elif (direction == 1):
        firstVector['x'] = 1
        firstVector['y'] = 0
    elif (direction == 2):
        firstVector['x'] = 0
        firstVector['y'] = -1
    elif (direction == 3):
        firstVector['x'] = 0
        firstVector['y'] = 1

    lastVector = {'x': -firstVector['x'], 'y': -firstVector['y']}

    nextDirection = random.randint(0, 1)
    midVector = {'x': 0, 'y': 0}

    if nextDirection == 0:
        nextDirection = -1

    if firstVector['x'] == 0:
        midVector['x'] = nextDirection
        midVector['y'] = 0
    else:
        midVector['y'] = nextDirection
        midVector['x'] = 0

    current = (start[0], start[1])
    for wall in [(firstVector, firstWallSize), (midVector, midWallSize), (lastVector, lastWallSize)]:
        for i in range(1, wall[1]):
            obstacleTracker.add(current)
            current = (current[0] + wall[0]['x'], current[1] + wall[0]['y'])


parser = argparse.ArgumentParser()

parser.add_argument("height", help="the height of the Vehicle world")
parser.add_argument("width", help="the width of the Vehicle world")
parser.add_argument("-p", "--path", help="path to save the worlds")
parser.add_argument("-v", "--verbose", help="increase output verbosity", action="store_true")
parser.add_argument("-n", "--number", help="number of worlds to generate")
parser.add_argument("-o", "--obstacles",
                    help="probability of obstacles to appear; if minima, probability a minimum will start in a square")
parser.add_argument("-m", "--minima", help="obstacles organized as local minima", action="store_true")

args = parser.parse_args()
height = int(args.height)
width = int(args.width)
number = int(args.number)


if args.verbose:
    print(args.height)
    print(args.width)

obstaclePercentage = float(args.obstacles)
minima = args.minima
startX = 1
startY = 1

endX = width
endY = height

path = args.path

# hard code size bound for now
sizeBound = int(endY / 20)
# sizeBound = 10

if type(path) == type(None):
    path = "../input/vacuum/minima"

configType = "uniform"
if minima:
    configType = "minima"

for iteration in range(0, number):

    newDomain = configType + str(height) + "_" + str(width) + "-" + str(iteration)

    completeFile = os.path.join(path, newDomain+".vw")

    aFile = open(completeFile, "w")

    preamble = args.width+"\n"+args.height+"\n"
    world = ""

    obstacleLocations = set()
    for y in range(0, height):
        for x in range(0, width):
            if random.random() < obstaclePercentage:
                if minima:
                    generateMinimum(obstacleLocations, (x, y), sizeBound)
                else:
                    obstacleLocations.add((x, y))

    for y in range(0, height):
        for x in range(0, width):
            if (x == startX) and (y == startY):
                world += "@"
            elif y == height-1 and x == width-1:
                world += "*"
            elif (x, y) in obstacleLocations:
                world += "#"
            else:
                world += "_"
        world += "\n"

    if args.verbose:
        print(world)

    aFile.write(preamble + world)

    aFile.close()
