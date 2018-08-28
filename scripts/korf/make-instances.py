#! /usr/bin/env python

import os

print ("Making Korf instances...")

korf_instances = open('master', 'r')

for line in korf_instances:
    instance = line.split(" ")
    instance_number = instance[0]
    print (instance_number)
    instance_file = open(str(instance_number), 'w')
    instance_file.write("4 4\n")
    instance_file.write("starting positions for each tile:\n")
    counter = 1
    for cell in instance[1:]:
        if counter != 16:
            instance_file.write(str(cell) + "\n")
        else:
            instance_file.write(str(cell))
        counter += 1
    instance_file.write("goal positions:\n")
    for tile in range(0,15):
        instance_file.write(str(tile) + "\n")
    instance_file.close()

korf_instances.close()
print ("Done.")

