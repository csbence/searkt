sendSlackNotification.bash "#experiments" "experiment_bot" "William just started running experiments."
java -Xmx7G -jar ./build/libs/real-time-search-1.0-SNAPSHOT.jar 2.0 wa*
java -Xmx7G -jar ./build/libs/real-time-search-1.0-SNAPSHOT.jar 1.9 wa*
java -Xmx7G -jar ./build/libs/real-time-search-1.0-SNAPSHOT.jar 1.8 wa*
java -Xmx7G -jar ./build/libs/real-time-search-1.0-SNAPSHOT.jar 1.7 wa*
java -Xmx7G -jar ./build/libs/real-time-search-1.0-SNAPSHOT.jar 1.6 wa*
java -Xmx7G -jar ./build/libs/real-time-search-1.0-SNAPSHOT.jar 1.5 wa*
java -Xmx7G -jar ./build/libs/real-time-search-1.0-SNAPSHOT.jar 1.4 wa*
java -Xmx7G -jar ./build/libs/real-time-search-1.0-SNAPSHOT.jar 1.3 wa*
java -Xmx7G -jar ./build/libs/real-time-search-1.0-SNAPSHOT.jar 1.2 wa*
java -Xmx7G -jar ./build/libs/real-time-search-1.0-SNAPSHOT.jar 1.1 wa*
java -Xmx7G -jar ./build/libs/real-time-search-1.0-SNAPSHOT.jar 1.0 wa*
java -Xmx7G -jar ./build/libs/real-time-search-1.0-SNAPSHOT.jar 2.0 dps
java -Xmx7G -jar ./build/libs/real-time-search-1.0-SNAPSHOT.jar 1.9 dps 
java -Xmx7G -jar ./build/libs/real-time-search-1.0-SNAPSHOT.jar 1.8 dps
java -Xmx7G -jar ./build/libs/real-time-search-1.0-SNAPSHOT.jar 1.7 dps
java -Xmx7G -jar ./build/libs/real-time-search-1.0-SNAPSHOT.jar 1.6 dps
java -Xmx7G -jar ./build/libs/real-time-search-1.0-SNAPSHOT.jar 1.5 dps
java -Xmx7G -jar ./build/libs/real-time-search-1.0-SNAPSHOT.jar 1.4 dps
java -Xmx7G -jar ./build/libs/real-time-search-1.0-SNAPSHOT.jar 1.3 dps
java -Xmx7G -jar ./build/libs/real-time-search-1.0-SNAPSHOT.jar 1.2 dps
java -Xmx7G -jar ./build/libs/real-time-search-1.0-SNAPSHOT.jar 1.1 dps
java -Xmx7G -jar ./build/libs/real-time-search-1.0-SNAPSHOT.jar 1.0 dps
endSlackNotification.bash "#experiments" "experiment_bot" "William's experiments just finished."


