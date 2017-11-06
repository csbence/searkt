#! /bin/bash

echo "NAME TIMES : $1 $2..."

echo "Safe Version $3.."
echo "Running SafeSearch $1 $2 iterations..."

for i in $(eval echo {1..$2})
do
  echo "Running $1 $2 $i experiment..."
  timeout 10 java -jar ../build/libs/SafeSearch-1.0-SNAPSHOT.jar -v -l -s $3 < ../input/vehicle/vehicle$i.v > ../results/$i.results
done

echo "Finished!"
