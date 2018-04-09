#!/bin/sh

if [[ -z "$KAFKA_HOME" ]]; then echo "ERROR: KAFKA_HOME must be set."; exit 1; fi

# Run with "localhost" so Kafka is available if network not available
$KAFKA_HOME/bin/kafka-server-start.sh -daemon $KAFKA_HOME/config/server.properties \
	--override advertised.host.name=localhost \
	--override broker.id=2 \
	--override log.dirs=/tmp/kafka-logs-2 \
	--override port=9094
