#!/bin/sh

TOPIC="precious"
echo ================
echo Topic name : $TOPIC
echo ================

if [[ -z "$KAFKA_HOME" ]]; then echo "ERROR: KAFKA_HOME must be set."; exit 1; fi

KAFKA_TOPICS="${KAFKA_HOME}/bin/kafka-topics.sh --zookeeper 127.0.0.1:2181"

if [[ -z `${KAFKA_TOPICS} --list --topic $TOPIC` ]]; then
  echo 'Creating topic "${TOPIC}"'
  ${KAFKA_TOPICS} --create --partitions 3 --replication-factor 1 --topic $TOPIC
  # Allow leadership election
  echo "Waiting for leadership election..."
  sleep 10
  echo Done.
fi

${KAFKA_TOPICS} --describe --topic $TOPIC
echo \^C to cancel...
${KAFKA_HOME}/bin/kafka-console-consumer.sh --bootstrap-server 127.0.0.1:9092 --topic $TOPIC --from-beginning
