#! /usr/bin/env python3

import os.path
import argparse
import random

parser = argparse.ArgumentParser()

parser.add_argument("height", help="the height of the Vehicle world")
parser.add_argument("width", help="the width of the Vehicle world")
parser.add_argument("-p", "--path", help="path to save the worlds")
parser.add_argument("-v", "--verbose", help="increase output verbosity", action="store_true")
parser.add_argument("-n", "--number", help="number of worlds to generate")
parser.add_argument("-o", "--obstacles", help="probability of obstacles to appear")
parser.add_argument("-d", "--dirt", help="number of dirt to appear")

args = parser.parse_args()
height = int(args.height) + 1
width = int(args.width) + 1
number = int(args.number)

if args.verbose:
    print(args.height)
    print(args.width)

obstaclePercentage = 0.25
numberOfDirtyCells = 10

startX = 1
startY = 1

endX = width
endY = height

path = args.path

if path is None:
    path = "../../src/main/resources/input/vacuum/gen/"

for iteration in range(0, number):
    dirt_x_locations = [x for x in random.sample(range(1, width + 1), numberOfDirtyCells)]
    dirt_y_locations = [y for y in random.sample(range(1, height + 1), numberOfDirtyCells)]
    dirt_locations = zip(dirt_x_locations, dirt_y_locations)
    tuple_locs = []

    for loc in dirt_locations:
        tuple_locs.append((loc[0], loc[1]))

    if args.verbose:
        for loc in tuple_locs:
            print(loc)

    world = []
    newDomain = "vacuum" + str(iteration)

    completeFile = os.path.join(path, newDomain + ".vw")

    aFile = open(completeFile, "w")

    preamble = args.height + "\n" + args.width + "\n"

    for y in range(1, height):
        world_line = []
        for x in range(1, width):
            flipObstacle = random.random()
            flipBunker = random.random()
            if (x == startX) and (y == startY):
                world_line.append('@')
            elif flipObstacle < obstaclePercentage and x != width - 1:
                world_line.append('#')
            else:
                world_line.append('_')
        world.append(world_line)

    if args.verbose:
        print(world)

    aFile.write(preamble)
    x = 0
    y = 0
    for line in world:
        for c in line:
            if (x, y) in tuple_locs:
                aFile.write('*')
            else:
                aFile.write(c)
            x = x + 1
        aFile.write('\n')
        y = y + 1
        x = 0

    aFile.close()
