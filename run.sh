#sendSlackNotification.bash "#experiments" "experiment_bot" "William just started running experiments."
java -Xmx7G -jar ./build/libs/real-time-search-1.0-SNAPSHOT.jar 2.0 wa*
java -Xmx7G -jar ./build/libs/real-time-search-1.0-SNAPSHOT.jar 1.9 wa*
java -Xmx7G -jar ./build/libs/real-time-search-1.0-SNAPSHOT.jar 1.8 wa*
echo "WA* 1.8 done"
java -Xmx7G -jar ./build/libs/real-time-search-1.0-SNAPSHOT.jar 1.7 wa*
java -Xmx7G -jar ./build/libs/real-time-search-1.0-SNAPSHOT.jar 1.6 wa*
java -Xmx7G -jar ./build/libs/real-time-search-1.0-SNAPSHOT.jar 1.5 wa*
echo "WA* 1.5 done"
java -Xmx7G -jar ./build/libs/real-time-search-1.0-SNAPSHOT.jar 1.4 wa*
echo "WA* 1.4 done"
java -Xmx7G -jar ./build/libs/real-time-search-1.0-SNAPSHOT.jar 1.3 wa*
echo "WA* 1.3 done"
java -Xmx7G -jar ./build/libs/real-time-search-1.0-SNAPSHOT.jar 1.2 wa*
echo "WA* 1.2 done"
java -Xmx7G -jar ./build/libs/real-time-search-1.0-SNAPSHOT.jar 1.1 wa*
echo "WA* 1.1 done"
java -Xmx7G -jar ./build/libs/real-time-search-1.0-SNAPSHOT.jar 1.0 wa*
echo "WA* 1.0 done"
java -Xmx7G -jar ./build/libs/real-time-search-1.0-SNAPSHOT.jar 2.0 dps
java -Xmx7G -jar ./build/libs/real-time-search-1.0-SNAPSHOT.jar 1.9 dps 
java -Xmx7G -jar ./build/libs/real-time-search-1.0-SNAPSHOT.jar 1.8 dps
echo "DPS 1.8 done"
java -Xmx7G -jar ./build/libs/real-time-search-1.0-SNAPSHOT.jar 1.7 dps
java -Xmx7G -jar ./build/libs/real-time-search-1.0-SNAPSHOT.jar 1.6 dps
java -Xmx7G -jar ./build/libs/real-time-search-1.0-SNAPSHOT.jar 1.5 dps
echo "DPS 1.5 done"
java -Xmx7G -jar ./build/libs/real-time-search-1.0-SNAPSHOT.jar 1.4 dps
echo "DPS 1.4 done"
java -Xmx7G -jar ./build/libs/real-time-search-1.0-SNAPSHOT.jar 1.3 dps
echo "DPS 1.3 done"
java -Xmx7G -jar ./build/libs/real-time-search-1.0-SNAPSHOT.jar 1.2 dps
echo "DPS 1.2 done"
java -Xmx7G -jar ./build/libs/real-time-search-1.0-SNAPSHOT.jar 1.1 dps
echo "DPS 1.1 done"
java -Xmx7G -jar ./build/libs/real-time-search-1.0-SNAPSHOT.jar 1.0 dps
#endSlackNotification.bash "#experiments" "experiment_bot" "William's experiments just finished."


