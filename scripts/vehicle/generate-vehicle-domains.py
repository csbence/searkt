#! /usr/bin/env python3

import argparse
import os.path
import random

parser = argparse.ArgumentParser()

parser.add_argument("height", help="the height of the Vehicle world")
parser.add_argument("width", help="the width of the Vehicle world")
parser.add_argument("-p", "--path", help="path to save the worlds")
parser.add_argument("-v", "--verbose", help="increase output verbosity", action="store_true")
parser.add_argument("-n", "--number", help="number of worlds to generate")
parser.add_argument("-o", "--obstacles", help="probability of obstacles to appear")
parser.add_argument("-b", "--bunkers", help="probability of bunkers to appear")

args = parser.parse_args()
height = int(args.height) + 1
width = int(args.width) + 1
number = int(args.number)


if args.verbose:
  print(args.height)
  print(args.width)

obstaclePercentage = float(args.obstacles)
bunkerPercentage = float(args.bunkers)
startX = 1
startY = (height - 1)

endX = (width - 1)
endY = (height - 1)

print("start:" + str(startX) + ", " + str(startY))
print("end: " + str(endX) + ", " + str(endY))

path = args.path

if type(path) == type(None):
    path = "../../src/main/resources/input/lifegrids"

for iteration in range(0,number):
    newDomain = "lifegrids" + str(iteration)

    completeFile = os.path.join(path, newDomain + ".lg")

  aFile = open(completeFile, "w")

    preamble = args.width + "\n" + args.height + "\n"
  world = ""

  for y in range(1,height):
    for x in range(1,width):
      flipObstacle = random.random()
      flipBunker = random.random()
      if x == startX and y == startY:
        world += "@"
      elif flipObstacle < obstaclePercentage and x != width-1:
        world += "#"
      elif flipBunker < bunkerPercentage and x != width-1:
        world += "$"
      elif y == endY and x == endX:
        world += "*"
      else:
        world += "_"
    world += "\n"

  if args.verbose:
    print(world)
    
  aFile.write(preamble + world)

  aFile.close()
