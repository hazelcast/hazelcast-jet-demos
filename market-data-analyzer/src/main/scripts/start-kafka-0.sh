#!/bin/sh
cd /Applications/kafka_2.11-1.0.0/bin

# Run with "localhost" so Kafka is available if network not available
./kafka-server-start.sh ../config/server.properties \
	--override advertised.host.name=localhost \
	--override broker.id=0 \
	--override log.dirs=/tmp/kafka-logs-0 \
	--override port=9092
