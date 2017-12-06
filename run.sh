sendSlackNotification.bash "#experiments" "experiment_bot" "William just started running experiments."

java -Xmx7G -jar ./build/libs/real-time-search-1.0-SNAPSHOT.jar 3.0 wa* stp
java -Xmx7G -jar ./build/libs/real-time-search-1.0-SNAPSHOT.jar 2.75 wa* stp
java -Xmx7G -jar ./build/libs/real-time-search-1.0-SNAPSHOT.jar 2.67 wa* stp
java -Xmx7G -jar ./build/libs/real-time-search-1.0-SNAPSHOT.jar 2.33 wa* stp
java -Xmx7G -jar ./build/libs/real-time-search-1.0-SNAPSHOT.jar 2.0 wa* stp
java -Xmx7G -jar ./build/libs/real-time-search-1.0-SNAPSHOT.jar 1.78 wa* stp
java -Xmx7G -jar ./build/libs/real-time-search-1.0-SNAPSHOT.jar 1.5 wa* stp
java -Xmx7G -jar ./build/libs/real-time-search-1.0-SNAPSHOT.jar 1.33 wa* stp
java -Xmx7G -jar ./build/libs/real-time-search-1.0-SNAPSHOT.jar 1.25 wa* stp
java -Xmx7G -jar ./build/libs/real-time-search-1.0-SNAPSHOT.jar 1.20 wa* stp
java -Xmx7G -jar ./build/libs/real-time-search-1.0-SNAPSHOT.jar 1.17 wa* stp

java -Xmx7G -jar ./build/libs/real-time-search-1.0-SNAPSHOT.jar 3.0 dps stp
java -Xmx7G -jar ./build/libs/real-time-search-1.0-SNAPSHOT.jar 2.75 dps stp
java -Xmx7G -jar ./build/libs/real-time-search-1.0-SNAPSHOT.jar 2.67 dps stp
java -Xmx7G -jar ./build/libs/real-time-search-1.0-SNAPSHOT.jar 2.33 dps stp
java -Xmx7G -jar ./build/libs/real-time-search-1.0-SNAPSHOT.jar 2.0 dps stp
java -Xmx7G -jar ./build/libs/real-time-search-1.0-SNAPSHOT.jar 1.78 dps stp
java -Xmx7G -jar ./build/libs/real-time-search-1.0-SNAPSHOT.jar 1.5 dps stp
java -Xmx7G -jar ./build/libs/real-time-search-1.0-SNAPSHOT.jar 1.33 dps stp
java -Xmx7G -jar ./build/libs/real-time-search-1.0-SNAPSHOT.jar 1.25 dps stp
java -Xmx7G -jar ./build/libs/real-time-search-1.0-SNAPSHOT.jar 1.20 dps stp
java -Xmx7G -jar ./build/libs/real-time-search-1.0-SNAPSHOT.jar 1.17 dps stp

java -Xmx7G -jar ./build/libs/real-time-search-1.0-SNAPSHOT.jar 3.0 wa* htp
java -Xmx7G -jar ./build/libs/real-time-search-1.0-SNAPSHOT.jar 2.67 wa* htp
java -Xmx7G -jar ./build/libs/real-time-search-1.0-SNAPSHOT.jar 2.0 wa* htp
java -Xmx7G -jar ./build/libs/real-time-search-1.0-SNAPSHOT.jar 1.5 wa* htp
java -Xmx7G -jar ./build/libs/real-time-search-1.0-SNAPSHOT.jar 1.25 wa* htp
java -Xmx7G -jar ./build/libs/real-time-search-1.0-SNAPSHOT.jar 1.20 wa* htp
java -Xmx7G -jar ./build/libs/real-time-search-1.0-SNAPSHOT.jar 1.17 wa* htp
java -Xmx7G -jar ./build/libs/real-time-search-1.0-SNAPSHOT.jar 1.14 wa* htp
java -Xmx7G -jar ./build/libs/real-time-search-1.0-SNAPSHOT.jar 1.13 wa* htp
java -Xmx7G -jar ./build/libs/real-time-search-1.0-SNAPSHOT.jar 1.11 wa* htp

java -Xmx7G -jar ./build/libs/real-time-search-1.0-SNAPSHOT.jar 3.0 dps htp
java -Xmx7G -jar ./build/libs/real-time-search-1.0-SNAPSHOT.jar 2.67 dps htp
java -Xmx7G -jar ./build/libs/real-time-search-1.0-SNAPSHOT.jar 2.0 dps htp
java -Xmx7G -jar ./build/libs/real-time-search-1.0-SNAPSHOT.jar 1.5 dps htp
java -Xmx7G -jar ./build/libs/real-time-search-1.0-SNAPSHOT.jar 1.25 dps htp
java -Xmx7G -jar ./build/libs/real-time-search-1.0-SNAPSHOT.jar 1.20 dps htp
java -Xmx7G -jar ./build/libs/real-time-search-1.0-SNAPSHOT.jar 1.17 dps htp
java -Xmx7G -jar ./build/libs/real-time-search-1.0-SNAPSHOT.jar 1.14 dps htp
java -Xmx7G -jar ./build/libs/real-time-search-1.0-SNAPSHOT.jar 1.13 dps htp
java -Xmx7G -jar ./build/libs/real-time-search-1.0-SNAPSHOT.jar 1.11 dps htp

endSlackNotification.bash "#experiments" "experiment_bot" "William's experiments just finished."


