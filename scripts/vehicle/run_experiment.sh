#! /bin/bash


echo "Running SafeSearch $1 500 iterations..."
echo "Safety Flag $2..."

for i in {0..4}
do
  echo "Running $1 $2 $i experiment..."
  timeout 10 java -jar ../build/libs/SafeSearch-1.0-SNAPSHOT.jar $1 -l $2 < ../input/vehicle/vehicle$i.v > ../results/$2$i.results
done

echo "Finished!"
