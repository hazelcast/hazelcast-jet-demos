#!/bin/sh

if [[ -z "$KAFKA_HOME" ]]; then echo "ERROR: KAFKA_HOME must be set."; exit 1; fi

$KAFKA_HOME/bin/kafka-server-stop.sh $KAFKA_HOME/bin/config/server.properties

