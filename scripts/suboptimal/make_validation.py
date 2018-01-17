#! /usr/bin/env python

f = open("../../validator.sh", 'w')

f.write("#! /bin/bash\n")

for i in range(1, 101):
    f.write("java -Xmx7G -jar ./build/libs/real-time-search-1.0-SNAPSHOT.jar " + str(i) + "\n")

f.close()


